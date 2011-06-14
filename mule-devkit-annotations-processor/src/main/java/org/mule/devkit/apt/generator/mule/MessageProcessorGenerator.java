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

package org.mule.devkit.apt.generator.mule;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import org.apache.commons.lang.StringUtils;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.annotations.SourceCallback;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.AbstractCodeGenerator;
import org.mule.devkit.apt.generator.GenerationException;
import org.mule.devkit.apt.util.CodeModelUtils;
import org.mule.transformer.TransformerTemplate;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;
import org.mule.util.TemplateParser;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageProcessorGenerator extends AbstractMessageGenerator {
    public MessageProcessorGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement typeElement) throws GenerationException {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(typeElement.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Processor processor = executableElement.getAnnotation(Processor.class);

            if (processor == null)
                continue;

            generateMessageProcessor(typeElement, executableElement);
        }
    }

    private void generateMessageProcessor(TypeElement typeElement, ExecutableElement executableElement) {
        // get class
        JDefinedClass messageProcessorClass = getMessageProcessorClass(executableElement);

        // add a field for each argument of the method
        Map<String, AbstractMessageGenerator.FieldVariableElement> fields = generateFieldForEachParameter(messageProcessorClass, executableElement);

        // add standard fields
        JFieldVar object = generateFieldForPojo(messageProcessorClass, typeElement);
        JFieldVar muleContext = generateFieldForMuleContext(messageProcessorClass);
        JFieldVar expressionManager = generateFieldForExpressionManager(messageProcessorClass);
        JFieldVar patternInfo = generateFieldForPatternInfo(messageProcessorClass);

        // add initialise
        generateInitialiseMethod(messageProcessorClass, getLifecycleWrapperClass(typeElement), muleContext, expressionManager, patternInfo, object);

        // add setmulecontext
        generateSetMuleContextMethod(messageProcessorClass, muleContext);

        // add setobject
        generateSetObjectMethod(messageProcessorClass, object);

        // generate setters for all parameters
        for (String fieldName : fields.keySet()) {
            generateSetter(messageProcessorClass, fields.get(fieldName).getField());
        }

        // add process method
        generateProcessMethod(executableElement, messageProcessorClass, fields, muleContext, object, expressionManager, patternInfo);
    }

    private JInvocation generateNullPayload(JFieldVar muleContext, JVar event) {
        JInvocation defaultMuleEvent = JExpr._new(ref(DefaultMuleEvent.class));
        JInvocation defaultMuleMessage = JExpr._new(ref(DefaultMuleMessage.class));
        JInvocation nullPayload = ref(NullPayload.class).staticInvoke("getInstance");
        defaultMuleMessage.arg(nullPayload);
        defaultMuleMessage.arg(muleContext);
        defaultMuleEvent.arg(defaultMuleMessage);
        defaultMuleEvent.arg(event);

        return defaultMuleEvent;
    }

    private void generateProcessMethod(ExecutableElement executableElement, JDefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, JFieldVar muleContext, JFieldVar object, JFieldVar expressionManager, JFieldVar patternInfo) {
        String methodName = executableElement.getSimpleName().toString();
        JType muleEvent = ref(MuleEvent.class);


        JMethod process = messageProcessorClass.method(JMod.PUBLIC, muleEvent, "process");
        process._throws(MuleException.class);
        JVar event = process.param(muleEvent, "event");
        JVar muleMessage = process.body().decl(ref(MuleMessage.class), "muleMessage");
        process.body().assign(muleMessage, event.invoke("getMessage"));

        JTryBlock callProcessor = process.body()._try();

        List<JVar> parameters = new ArrayList<JVar>();
        for (VariableElement variable : executableElement.getParameters()) {
            if (variable.asType().toString().contains(SourceCallback.class.getName()))
                continue;

            String fieldName = variable.getSimpleName().toString();
            if (isTypeSupported(fields.get(fieldName).getVariableElement()) || CodeModelUtils.isXmlType(fields.get(fieldName).getVariableElement())) {

                JVar evaluated = callProcessor.body().decl(ref(Object.class), "evaluated" + StringUtils.capitalize(fieldName), JExpr._null());
                JVar transformed = callProcessor.body().decl(ref(fields.get(fieldName).getVariableElement().asType()).boxify(), "transformed" + StringUtils.capitalize(fieldName), JExpr._null());

                JConditional notNull = callProcessor.body()._if(JOp.ne(fields.get(fieldName).getField(), JExpr._null()));

                generateExpressionEvaluator(notNull._then(), evaluated, fields.get(fieldName).getField(), patternInfo, expressionManager, muleMessage);
                generateTransform(notNull._then(), transformed, evaluated, fields.get(fieldName).getVariableElement().asType(), muleContext);

                parameters.add(transformed);
            } else {
                //JVar ref = callProcessor.body().decl(ref(fields.get(fieldName).getVariableElement().asType()).boxify(), "ref" + StringUtils.capitalize(fieldName));
                parameters.add(fields.get(fieldName).getField());
            }
        }

        JType returnType = ref(executableElement.getReturnType());
        generateMethodCall(callProcessor.body(), object, methodName, parameters, muleContext, event, returnType);
        generateThrow("failedToInvoke", MessagingException.class, callProcessor._catch((JClass) ref(Exception.class)), event, methodName);
    }

    private JVar generateMethodCall(JBlock body, JFieldVar object, String methodName, List<JVar> parameters, JFieldVar muleContext, JVar event, JType returnType) {
        JVar resultPayload = null;
        if (returnType != getContext().getCodeModel().VOID) {
            resultPayload = body.decl(ref(Object.class), "resultPayload");
        }
        JInvocation methodCall = object.invoke(methodName);
        for (int i = 0; i < parameters.size(); i++) {
            methodCall.arg(parameters.get(i));
        }

        if (returnType != getContext().getCodeModel().VOID) {
            body.assign(resultPayload, methodCall);
            body._if(resultPayload.eq(JExpr._null()))._then()._return(generateNullPayload(muleContext, event));
            generatePayloadOverwrite(body, event, resultPayload);
            body._return(event);
        } else {
            body.add(methodCall);
            body._return(event);
        }

        return resultPayload;
    }

    private void generatePayloadOverwrite(JBlock block, JVar event, JVar resultPayload) {
        JInvocation applyTransformers = event.invoke("getMessage").invoke("applyTransformers");
        applyTransformers.arg(event);
        JInvocation newTransformerTemplate = JExpr._new(ref(TransformerTemplate.class));
        JInvocation newOverwritePayloadCallback = JExpr._new(ref(TransformerTemplate.OverwitePayloadCallback.class));
        newOverwritePayloadCallback.arg(resultPayload);
        newTransformerTemplate.arg(newOverwritePayloadCallback);

        JVar transformerList = block.decl(ref(List.class).narrow(Transformer.class), "transformerList");
        block.assign(transformerList, JExpr._new(ref(ArrayList.class).narrow(Transformer.class)));
        block.add(transformerList.invoke("add").arg(newTransformerTemplate));

        applyTransformers.arg(transformerList);
        block.add(applyTransformers);
    }
}
