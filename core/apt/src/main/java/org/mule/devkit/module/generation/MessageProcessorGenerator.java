/**
 * Mule Development Kit
 * Copyright 2010-2011 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule.devkit.module.generation;

import org.apache.commons.lang.StringUtils;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.callback.SourceCallback;
import org.mule.api.transformer.Transformer;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Type;
import org.mule.devkit.model.code.Variable;
import org.mule.transformer.TransformerTemplate;
import org.mule.transport.NullPayload;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageProcessorGenerator extends AbstractMessageGenerator {
    public void generate(Element typeElement) throws GenerationException {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(typeElement.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Processor processor = executableElement.getAnnotation(Processor.class);

            if (processor == null)
                continue;

            generateMessageProcessor(typeElement, executableElement);
        }
    }

    private void generateMessageProcessor(Element typeElement, ExecutableElement executableElement) {
        // get class
        DefinedClass messageProcessorClass = getMessageProcessorClass(executableElement);

        // add javadoc
        generateMessageProcessorClassDoc(executableElement, messageProcessorClass);

        // add a field for each argument of the method
        Map<String, AbstractMessageGenerator.FieldVariableElement> fields = generateFieldForEachParameter(messageProcessorClass, executableElement);

        // add standard fields
        FieldVariable object = generateFieldForPojo(messageProcessorClass, typeElement);
        FieldVariable muleContext = generateFieldForMuleContext(messageProcessorClass);
        FieldVariable expressionManager = generateFieldForExpressionManager(messageProcessorClass);
        FieldVariable patternInfo = generateFieldForPatternInfo(messageProcessorClass);

        // add initialise
        generateInitialiseMethod(messageProcessorClass, typeElement, muleContext, expressionManager, patternInfo, object);

        // add setmulecontext
        generateSetMuleContextMethod(messageProcessorClass, muleContext);

        // add setobject
        generateSetPojoMethod(messageProcessorClass, object);

        // generate setters for all parameters
        for (String fieldName : fields.keySet()) {
            generateSetter(messageProcessorClass, fields.get(fieldName).getField());
        }

        // add process method
        generateProcessMethod(executableElement, messageProcessorClass, fields, muleContext, object, expressionManager, patternInfo);
    }

    private void generateMessageProcessorClassDoc(ExecutableElement executableElement, DefinedClass messageProcessorClass) {
        messageProcessorClass.javadoc().add(messageProcessorClass.name() + " invokes the ");
        messageProcessorClass.javadoc().add("{@link " + ((TypeElement)executableElement.getEnclosingElement()).getQualifiedName().toString() + "#");
        messageProcessorClass.javadoc().add(executableElement.getSimpleName().toString() + "(");
        boolean first = true;
        for (VariableElement variable : executableElement.getParameters()) {
            if( !first ) {
                messageProcessorClass.javadoc().add(", ");
            }
            messageProcessorClass.javadoc().add(variable.asType().toString().replaceAll("<[a-zA-Z\\-\\.\\<\\>\\s\\,]*>", ""));
            first = false;
        }
        messageProcessorClass.javadoc().add(")} method in ");
        messageProcessorClass.javadoc().add(ref(executableElement.getEnclosingElement().asType()));
        messageProcessorClass.javadoc().add(". For each argument there is a field in this processor to match it. ");
        messageProcessorClass.javadoc().add(" Before invoking the actual method the processor will evaluate and transform");
        messageProcessorClass.javadoc().add(" where possible to the expected argument type.");
    }

    private Invocation generateNullPayload(FieldVariable muleContext, Variable event) {
        Invocation defaultMuleEvent = ExpressionFactory._new(ref(DefaultMuleEvent.class));
        Invocation defaultMuleMessage = ExpressionFactory._new(ref(DefaultMuleMessage.class));
        Invocation nullPayload = ref(NullPayload.class).staticInvoke("getInstance");
        defaultMuleMessage.arg(nullPayload);
        defaultMuleMessage.arg(muleContext);
        defaultMuleEvent.arg(defaultMuleMessage);
        defaultMuleEvent.arg(event);

        return defaultMuleEvent;
    }

    private void generateProcessMethod(ExecutableElement executableElement, DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, FieldVariable muleContext, FieldVariable object, FieldVariable expressionManager, FieldVariable patternInfo) {
        String methodName = executableElement.getSimpleName().toString();
        Type muleEvent = ref(MuleEvent.class);

        Method process = messageProcessorClass.method(Modifier.PUBLIC, muleEvent, "process");
        process.javadoc().add("Invokes the MessageProcessor.");
        process.javadoc().addParam("event MuleEvent to be processed");
        process.javadoc().addThrows(ref(MuleException.class));

        process._throws(MuleException.class);
        Variable event = process.param(muleEvent, "event");
        Variable muleMessage = process.body().decl(ref(MuleMessage.class), "muleMessage");
        process.body().assign(muleMessage, event.invoke("getMessage"));

        TryStatement callProcessor = process.body()._try();

        List<Variable> parameters = new ArrayList<Variable>();
        for (VariableElement variable : executableElement.getParameters()) {
            if (variable.asType().toString().contains(SourceCallback.class.getName()))
                continue;

            String fieldName = variable.getSimpleName().toString();
            if (SchemaTypeConversion.isSupported(fields.get(fieldName).getVariableElement().asType().toString()) ||
                    context.getTypeMirrorUtils().isXmlType(fields.get(fieldName).getVariableElement().asType()) ||
                    context.getTypeMirrorUtils().isEnum(fields.get(fieldName).getVariableElement().asType())) {

                Variable evaluated = callProcessor.body().decl(ref(Object.class), "evaluated" + StringUtils.capitalize(fieldName), ExpressionFactory._null());
                Variable transformed = callProcessor.body().decl(ref(fields.get(fieldName).getVariableElement().asType()).boxify(), "transformed" + StringUtils.capitalize(fieldName), ExpressionFactory._null());

                Conditional notNull = callProcessor.body()._if(Op.ne(fields.get(fieldName).getField(), ExpressionFactory._null()));

                generateExpressionEvaluator(notNull._then(), evaluated, fields.get(fieldName).getField(), patternInfo, expressionManager, muleMessage);
                generateTransform(notNull._then(), transformed, evaluated, fields.get(fieldName).getVariableElement().asType(), muleContext);

                parameters.add(transformed);
            } else {
                //Variable ref = callProcessor.body().decl(ref(fields.get(fieldName).getVariableElement().asType()).boxify(), "ref" + StringUtils.capitalize(fieldName));
                parameters.add(fields.get(fieldName).getField());
            }
        }

        Type returnType = ref(executableElement.getReturnType());
        generateMethodCall(callProcessor.body(), object, methodName, parameters, muleContext, event, returnType);
        generateThrow("failedToInvoke", MessagingException.class, callProcessor._catch((TypeReference) ref(Exception.class)), event, methodName);
    }

    private Variable generateMethodCall(Block body, FieldVariable object, String methodName, List<Variable> parameters, FieldVariable muleContext, Variable event, Type returnType) {
        Variable resultPayload = null;
        if (returnType != context.getCodeModel().VOID) {
            resultPayload = body.decl(ref(Object.class), "resultPayload");
        }
        Invocation methodCall = object.invoke(methodName);
        for (int i = 0; i < parameters.size(); i++) {
            methodCall.arg(parameters.get(i));
        }

        if (returnType != context.getCodeModel().VOID) {
            body.assign(resultPayload, methodCall);
            body._if(resultPayload.eq(ExpressionFactory._null()))._then()._return(generateNullPayload(muleContext, event));
            generatePayloadOverwrite(body, event, resultPayload);
            body._return(event);
        } else {
            body.add(methodCall);
            body._return(event);
        }

        return resultPayload;
    }

    private void generatePayloadOverwrite(Block block, Variable event, Variable resultPayload) {
        Invocation applyTransformers = event.invoke("getMessage").invoke("applyTransformers");
        applyTransformers.arg(event);
        Invocation newTransformerTemplate = ExpressionFactory._new(ref(TransformerTemplate.class));
        Invocation newOverwritePayloadCallback = ExpressionFactory._new(ref(TransformerTemplate.OverwitePayloadCallback.class));
        newOverwritePayloadCallback.arg(resultPayload);
        newTransformerTemplate.arg(newOverwritePayloadCallback);

        Variable transformerList = block.decl(ref(List.class).narrow(Transformer.class), "transformerList");
        block.assign(transformerList, ExpressionFactory._new(ref(ArrayList.class).narrow(Transformer.class)));
        block.add(transformerList.invoke("add").arg(newTransformerTemplate));

        applyTransformers.arg(transformerList);
        block.add(applyTransformers);
    }
}
