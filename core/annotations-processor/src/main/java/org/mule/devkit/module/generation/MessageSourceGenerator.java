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
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.callback.SourceCallback;
import org.mule.api.processor.MessageProcessor;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.Expression;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.mule.session.DefaultMuleSession;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageSourceGenerator extends AbstractMessageGenerator {


    public void generate(Element typeElement) throws GenerationException {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(typeElement.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Source source = executableElement.getAnnotation(Source.class);

            if (source == null)
                continue;

            generateMessageSource(typeElement, executableElement);
        }
    }

    private void generateMessageSource(Element typeElement, ExecutableElement executableElement) {
        // get class
        DefinedClass messageSourceClass = getMessageSourceClass(executableElement);

        messageSourceClass.javadoc().add(messageSourceClass.name() + " wraps ");
        messageSourceClass.javadoc().add("{@link " + ((TypeElement) executableElement.getEnclosingElement()).getQualifiedName().toString() + "#");
        messageSourceClass.javadoc().add(executableElement.getSimpleName().toString() + "(");
        boolean first = true;
        for (VariableElement variable : executableElement.getParameters()) {
            if (!first) {
                messageSourceClass.javadoc().add(", ");
            }
            messageSourceClass.javadoc().add(variable.asType().toString().replaceAll("<[a-zA-Z\\-\\.\\<\\>\\s\\,]*>", ""));
            first = false;
        }
        messageSourceClass.javadoc().add(")} method in ");
        messageSourceClass.javadoc().add(ref(executableElement.getEnclosingElement().asType()));
        messageSourceClass.javadoc().add(" as a message source capable of generating Mule events. ");
        messageSourceClass.javadoc().add(" The POJO's method is invoked in its own thread.");

        // add a field for each argument of the method
        Map<String, FieldVariableElement> fields = generateFieldForEachParameter(messageSourceClass, executableElement);

        // add standard fields
        FieldVariable object = generateFieldForPojo(messageSourceClass, typeElement);
        FieldVariable muleContext = generateFieldForMuleContext(messageSourceClass);
        FieldVariable flowConstruct = generateFieldForFlowConstruct(messageSourceClass);
        FieldVariable messageProcessor = messageSourceClass.field(Modifier.PRIVATE, ref(MessageProcessor.class), "messageProcessor");
        messageProcessor.javadoc().add("Message processor that will get called for processing incoming events");
        FieldVariable thread = messageSourceClass.field(Modifier.PRIVATE, ref(Thread.class), "thread");
        thread.javadoc().add("Thread under which this message source will execute");

        // add initialise
        generateInitialiseMethod(messageSourceClass, fields, typeElement, muleContext, null, null, object);

        // add setmulecontext
        generateSetMuleContextMethod(messageSourceClass, muleContext);

        // add setobject
        generateSetPojoMethod(messageSourceClass, object);

        // add setlistener
        generateSetListenerMethod(messageSourceClass, messageProcessor);

        // add setflowconstruct
        generateSetFlowConstructMethod(messageSourceClass, flowConstruct);

        // generate setters for all parameters
        for (String fieldName : fields.keySet()) {
            generateSetter(messageSourceClass, fields.get(fieldName).getField());
        }

        // add process method
        generateSourceCallbackMethod(messageSourceClass, executableElement, messageProcessor, muleContext, flowConstruct);

        // add start method
        generateStartMethod(messageSourceClass, thread);

        // add stop method
        generateStopMethod(messageSourceClass, thread);

        // get pool object if poolable
        if (typeElement.getAnnotation(Module.class).poolable()) {
            DefinedClass poolObjectClass = context.getClassForRole(context.getNameUtils().generatePoolObjectRoleKey((TypeElement) typeElement));

            // add run method
            generateRunMethod(messageSourceClass, executableElement, fields, object, muleContext, poolObjectClass);
        } else {
            // add run method
            generateRunMethod(messageSourceClass, executableElement, fields, object, muleContext);
        }
    }

    private void generateRunMethod(DefinedClass messageSourceClass, ExecutableElement executableElement, Map<String, FieldVariableElement> fields, FieldVariable object, FieldVariable muleContext) {
        generateRunMethod(messageSourceClass, executableElement, fields, object, muleContext, null);
    }

    private void generateRunMethod(DefinedClass messageSourceClass, ExecutableElement executableElement, Map<String, FieldVariableElement> fields, FieldVariable object, FieldVariable muleContext, DefinedClass poolObjectClass) {
        Method run = messageSourceClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "run");
        run.javadoc().add("Implementation {@link Runnable#run()} that will invoke the method on the pojo that this message source wraps.");

        Variable poolObject = null;
        if (poolObjectClass != null) {
            poolObject = run.body().decl(poolObjectClass, "poolObject", ExpressionFactory._null());
        }

        TryStatement callSource = run.body()._try();

        String methodName = executableElement.getSimpleName().toString();

        List<Expression> parameters = new ArrayList<Expression>();
        for (VariableElement variable : executableElement.getParameters()) {
            if (variable.asType().toString().contains(SourceCallback.class.getName())) {
                parameters.add(ExpressionFactory._this());
            } else {
                String fieldName = variable.getSimpleName().toString();
                if (SchemaTypeConversion.isSupported(fields.get(fieldName).getVariableElement().asType().toString()) ||
                        context.getTypeMirrorUtils().isXmlType(fields.get(fieldName).getVariableElement().asType()) ||
                        context.getTypeMirrorUtils().isCollection(fields.get(fieldName).getVariableElement().asType()) ||
                        context.getTypeMirrorUtils().isEnum(fields.get(fieldName).getVariableElement().asType())) {
                    Variable transformed = callSource.body().decl(ref(fields.get(fieldName).getVariableElement().asType()).boxify(), "transformed" + StringUtils.capitalize(fieldName), ExpressionFactory._null());
                    Conditional notNull = callSource.body()._if(Op.ne(fields.get(fieldName).getField(), ExpressionFactory._null()));
                    generateTransform(notNull._then(), transformed, fields.get(fieldName).getField(), fields.get(fieldName).getVariableElement().asType(), muleContext);
                    parameters.add(transformed);
                } else {
                    parameters.add(fields.get(fieldName).getField());
                }
            }
        }

        Invocation methodCall = null;
        if (poolObject != null) {
            callSource.body().assign(poolObject, ExpressionFactory.cast(poolObject.type(), object.invoke("getLifecyleEnabledObjectPool").invoke("borrowObject")));
            methodCall = poolObject.invoke(methodName);
        } else {
            methodCall = object.invoke(methodName);
        }

        for (int i = 0; i < parameters.size(); i++) {
            methodCall.arg(parameters.get(i));
        }

        callSource.body().add(methodCall);

        CatchBlock swallowCatch = callSource._catch((TypeReference) ref(Exception.class));

        if (poolObjectClass != null) {
            Block fin = callSource._finally();
            Block poolObjectNotNull = fin._if(Op.ne(poolObject, ExpressionFactory._null()))._then();
            poolObjectNotNull.add(object.invoke("getLifecyleEnabledObjectPool").invoke("returnObject").arg(poolObject));
        }
    }


    private void generateStartMethod(DefinedClass messageSourceClass, FieldVariable thread) {
        Method start = messageSourceClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "start");
        start.javadoc().add("Method to be called when Mule instance gets started.");
        start._throws(ref(MuleException.class));
        Conditional ifNoThread = start.body()._if(Op.eq(thread, ExpressionFactory._null()));
        Invocation newThread = ExpressionFactory._new(ref(Thread.class));
        newThread.arg(ExpressionFactory._this());
        newThread.arg("Receiving Thread");
        ifNoThread._then().assign(thread, newThread);

        start.body().add(thread.invoke("start"));
    }

    private void generateStopMethod(DefinedClass messageSourceClass, FieldVariable thread) {
        Method stop = messageSourceClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "stop");
        stop.javadoc().add("Method to be called when Mule instance gets stopped.");
        stop._throws(ref(MuleException.class));

        stop.body().add(thread.invoke("interrupt"));
    }


    private void generateSourceCallbackMethod(DefinedClass messageSourceClass, ExecutableElement executableElement, FieldVariable messageProcessor, FieldVariable muleContext, FieldVariable flowConstruct) {
        Method process = messageSourceClass.method(Modifier.PUBLIC, ref(Object.class), "process");
        process.javadoc().add("Implements {@link SourceCallback#process(org.mule.api.MuleEvent)}. This message source will be passed on to ");
        process.javadoc().add("the actual pojo's method as a callback mechanism.");
        Variable message = process.param(ref(Object.class), "message");

        DefinedClass dummyInboundEndpointClass = context.getClassForRole(DummyInboundEndpointGenerator.DUMMY_INBOUND_ENDPOINT_ROLE);

        Variable dummyImmutableEndpoint = process.body().decl(dummyInboundEndpointClass, "dummyImmutableEndpoint");
        Invocation newDummyImmutableEndpoint = ExpressionFactory._new(dummyInboundEndpointClass);
        newDummyImmutableEndpoint.arg(muleContext);
        process.body().assign(dummyImmutableEndpoint, newDummyImmutableEndpoint);

        Variable muleMessage = process.body().decl(ref(MuleMessage.class), "muleMessage");
        Invocation newMuleMessage = ExpressionFactory._new(ref(DefaultMuleMessage.class));
        newMuleMessage.arg(message);
        newMuleMessage.arg(muleContext);
        process.body().assign(muleMessage, newMuleMessage);

        Variable muleSession = process.body().decl(ref(MuleSession.class), "muleSession");
        Invocation newMuleSession = ExpressionFactory._new(ref(DefaultMuleSession.class));
        newMuleSession.arg(flowConstruct);
        newMuleSession.arg(muleContext);
        process.body().assign(muleSession, newMuleSession);

        Variable muleEvent = process.body().decl(ref(MuleEvent.class), "muleEvent");
        Invocation newMuleEvent = ExpressionFactory._new(ref(DefaultMuleEvent.class));
        newMuleEvent.arg(muleMessage);
        newMuleEvent.arg(dummyImmutableEndpoint);
        newMuleEvent.arg(muleSession);
        process.body().assign(muleEvent, newMuleEvent);

        TryStatement tryBlock = process.body()._try();
        Variable responseEvent = tryBlock.body().decl(ref(MuleEvent.class), "responseEvent");
        Invocation messageProcess = messageProcessor.invoke("process");
        messageProcess.arg(muleEvent);
        tryBlock.body().assign(responseEvent, messageProcess);
        Conditional ifResponse = tryBlock.body()._if(
                Op.cand(Op.ne(responseEvent, ExpressionFactory._null()),
                        Op.ne(responseEvent.invoke("getMessage"), ExpressionFactory._null()))
        );
        ifResponse._then()._return(responseEvent.invoke("getMessage").invoke("getPayload"));

        CatchBlock muleExceptionBlock = tryBlock._catch(ref(MuleException.class));
        process.body()._return(ExpressionFactory._null());
    }

    private Method generateSetListenerMethod(DefinedClass messageSourceClass, FieldVariable messageProcessor) {
        Method setListener = messageSourceClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "setListener");
        setListener.javadoc().add("Sets the message processor that will \"listen\" the events generated by this message source");
        setListener.javadoc().addParam("listener Message processor");
        Variable listener = setListener.param(ref(MessageProcessor.class), "listener");
        setListener.body().assign(ExpressionFactory._this().ref(messageProcessor), listener);

        return setListener;
    }
}
