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

package org.mule.devkit.generation.mule;

import org.apache.commons.lang.StringUtils;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.NestedProcessor;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.InvalidateConnectionOn;
import org.mule.api.annotations.Mime;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.oauth.OAuthAccessToken;
import org.mule.api.annotations.oauth.OAuthAccessTokenSecret;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.InvocationHeaders;
import org.mule.api.annotations.param.OutboundHeaders;
import org.mule.api.annotations.param.Payload;
import org.mule.api.callback.HttpCallback;
import org.mule.api.callback.SourceCallback;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.Cast;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.Expression;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.ForEach;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Type;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.mule.expression.MessageHeaderExpressionEvaluator;
import org.mule.expression.MessageHeadersExpressionEvaluator;
import org.mule.expression.MessageHeadersListExpressionEvaluator;
import org.mule.transformer.TransformerTemplate;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class MessageProcessorGenerator extends AbstractMessageGenerator {


    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return (typeElement.hasAnnotation(Module.class) || typeElement.hasAnnotation(Connector.class)) &&
                typeElement.hasMethodsAnnotatedWith(Processor.class);
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {
        for (ExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            generateMessageProcessor(typeElement, executableElement);
        }
    }

    private void generateMessageProcessor(DevKitTypeElement typeElement, ExecutableElement executableElement) {
        // get class
        DefinedClass messageProcessorClass;

        boolean intercepting = executableElement.getAnnotation(Processor.class).intercepting();
        if (intercepting) {
            messageProcessorClass = getInterceptingMessageProcessorClass(executableElement);
        } else {
            messageProcessorClass = getMessageProcessorClass(executableElement);
        }

        context.note("Generating message processor as " + messageProcessorClass.fullName() + " for method " + executableElement.getSimpleName().toString() + " in " + typeElement.getSimpleName().toString());

        // add javadoc
        generateMessageProcessorClassDoc(executableElement, messageProcessorClass);

        // add a field for each argument of the method
        Map<String, AbstractMessageGenerator.FieldVariableElement> fields = generateProcessorFieldForEachParameter(messageProcessorClass, executableElement);

        // add fields for connectivity if required
        ExecutableElement connectMethod = connectMethodForClass(typeElement);
        Map<String, AbstractMessageGenerator.FieldVariableElement> connectFields = null;
        if (connectMethod != null) {
            connectFields = generateProcessorFieldForEachParameter(messageProcessorClass, connectMethod);
        }

        // add standard fields
        FieldVariable logger = generateLoggerField(messageProcessorClass);
        FieldVariable object = generateFieldForModuleObject(messageProcessorClass, typeElement);
        FieldVariable muleContext = generateFieldForMuleContext(messageProcessorClass);
        FieldVariable expressionManager = generateFieldForExpressionManager(messageProcessorClass);
        FieldVariable patternInfo = generateFieldForPatternInfo(messageProcessorClass);
        FieldVariable flowConstruct = generateFieldForFlowConstruct(messageProcessorClass);
        FieldVariable retryCount = generateRetryCountField(messageProcessorClass);
        FieldVariable retryMax = generateRetryMaxField(messageProcessorClass);

        FieldVariable messageProcessorListener = null;
        if (intercepting) {
            messageProcessorListener = generateFieldForMessageProcessorListener(messageProcessorClass);
        }

        // add initialise
        generateInitialiseMethod(messageProcessorClass, fields, typeElement, muleContext, expressionManager, patternInfo, object, retryCount);

        // add start
        generateStartMethod(messageProcessorClass, fields);

        // add stop
        generateStopMethod(messageProcessorClass, fields);

        // add dispose
        generateDiposeMethod(messageProcessorClass, fields);

        // add setmulecontext
        generateSetMuleContextMethod(messageProcessorClass, muleContext, fields);

        // add setflowconstruct
        generateSetFlowConstructMethod(messageProcessorClass, flowConstruct, fields);

        if (intercepting) {
            // add setlistener
            generateSetListenerMethod(messageProcessorClass, messageProcessorListener);

            // add process method
            generateSourceCallbackProcessMethod(messageProcessorClass, messageProcessorListener, muleContext, flowConstruct);
            generateSourceCallbackProcessWithPropertiesMethod(messageProcessorClass, messageProcessorListener, muleContext, flowConstruct);
            generateSourceCallbackProcessMethodWithNoPayload(messageProcessorClass, messageProcessorListener, muleContext, flowConstruct);
        }

        // add setobject
        generateSetModuleObjectMethod(messageProcessorClass, object);

        // add setRetryMax
        generateSetter(messageProcessorClass, retryMax);

        // generate setters for all parameters
        for (String fieldName : fields.keySet()) {
            generateSetter(messageProcessorClass, fields.get(fieldName).getField());
        }

        // generate setters for connectivity fields
        if (connectFields != null) {
            for (String fieldName : connectFields.keySet()) {
                generateSetter(messageProcessorClass, connectFields.get(fieldName).getField());
            }
        }

        // generate evaluate & transform methods and helpers
        generateComputeClassHierarchyMethod(messageProcessorClass);
        generateIsListClassMethod(messageProcessorClass);
        generateIsMapClassMethod(messageProcessorClass);
        generateIsListMethod(messageProcessorClass);
        generateIsMapMethod(messageProcessorClass);
        generateIsAssignableFrom(messageProcessorClass);
        generateEvaluateMethod(messageProcessorClass, patternInfo, expressionManager);
        generateEvaluateAndTransformMethod(messageProcessorClass, muleContext);

        // get pool object if poolable
        if (typeElement.isPoolable()) {
            DefinedClass poolObjectClass = context.getClassForRole(context.getNameUtils().generatePoolObjectRoleKey(typeElement));

            // add process method
            generateProcessMethod(executableElement, messageProcessorClass, fields, connectFields, messageProcessorListener, muleContext, object, poolObjectClass, logger, retryCount, retryMax);
        } else {
            // add process method
            generateProcessMethod(executableElement, messageProcessorClass, fields, connectFields, messageProcessorListener, muleContext, object, logger, retryCount, retryMax);
        }
    }

    private void generateEvaluateAndTransformMethod(DefinedClass messageProcessorClass, FieldVariable muleContext) {
        Method evaluateAndTransform = messageProcessorClass.method(Modifier.PRIVATE, ref(Object.class), "evaluateAndTransform");
        evaluateAndTransform._throws(ref(TransformerException.class));
        Variable muleMessage = evaluateAndTransform.param(ref(MuleMessage.class), "muleMessage");
        Variable expectedType = evaluateAndTransform.param(ref(java.lang.reflect.Type.class), "expectedType");
        Variable expectedMimeType = evaluateAndTransform.param(ref(String.class), "expectedMimeType");
        Variable source = evaluateAndTransform.param(ref(Object.class), "source");

        evaluateAndTransform.body()._if(Op.eq(source, ExpressionFactory._null()))._then()._return(source);

        Variable target = evaluateAndTransform.body().decl(ref(Object.class), "target", ExpressionFactory._null());
        Conditional isList = evaluateAndTransform.body()._if(
                ExpressionFactory.invoke("isList").arg(source.invoke("getClass")));
        Conditional isExpectedList = isList._then()._if(
                ExpressionFactory.invoke("isList").arg(expectedType));
        Variable newList = isExpectedList._then().decl(ref(List.class), "newList", ExpressionFactory._new(ref(ArrayList.class)));
        Variable listParameterizedType = isExpectedList._then().decl(ref(java.lang.reflect.Type.class), "valueType",
                ExpressionFactory.cast(ref(ParameterizedType.class), expectedType).
                        invoke("getActualTypeArguments").component(ExpressionFactory.lit(0)));
        Variable listIterator = isExpectedList._then().decl(ref(ListIterator.class), "iterator",
                ExpressionFactory.cast(ref(List.class), source).
                        invoke("listIterator"));

        Block whileHasNext = isExpectedList._then()._while(listIterator.invoke("hasNext")).body();
        Variable subTarget = whileHasNext.decl(ref(Object.class), "subTarget", listIterator.invoke("next"));
        whileHasNext.add(newList.invoke("add").arg(
                ExpressionFactory.invoke("evaluateAndTransform").arg(muleMessage).arg(listParameterizedType).
                        arg(expectedMimeType).
                        arg(subTarget)
        ));
        isExpectedList._then().assign(target, newList);
        isExpectedList._else().assign(target, source);

        Conditional isMap = isList._elseif(
                ExpressionFactory.invoke("isMap").arg(source.invoke("getClass")));
        Conditional isExpectedMap = isMap._then()._if(
                ExpressionFactory.invoke("isMap").arg(expectedType));

        Block isExpectedMapBlock = isExpectedMap._then();
        Variable keyType = isExpectedMapBlock.decl(ref(java.lang.reflect.Type.class), "keyType",
                ref(Object.class).dotclass());
        Variable valueType = isExpectedMapBlock.decl(ref(java.lang.reflect.Type.class), "valueType",
                ref(Object.class).dotclass());

        Block isGenericMap = isExpectedMapBlock._if(Op._instanceof(expectedType, ref(ParameterizedType.class)))._then();

        isGenericMap.assign(keyType, ExpressionFactory.cast(ref(ParameterizedType.class), expectedType).
                invoke("getActualTypeArguments").component(ExpressionFactory.lit(0)));
        isGenericMap.assign(valueType, ExpressionFactory.cast(ref(ParameterizedType.class), expectedType).
                invoke("getActualTypeArguments").component(ExpressionFactory.lit(1)));

        Variable map = isExpectedMapBlock.decl(ref(Map.class), "map", ExpressionFactory.cast(ref(Map.class), source));

        Variable newMap = isExpectedMapBlock.decl(ref(Map.class), "newMap", ExpressionFactory._new(ref(HashMap.class)));
        ForEach forEach = isExpectedMapBlock.forEach(ref(Object.class), "entryObj", map.invoke("entrySet"));
        Block forEachBlock = forEach.body().block();
        Variable entry = forEachBlock.decl(ref(Map.Entry.class), "entry", ExpressionFactory.cast(ref(Map.Entry.class), forEach.var()));
        Variable newKey = forEachBlock.decl(ref(Object.class), "newKey", ExpressionFactory.invoke("evaluateAndTransform").arg(muleMessage).arg(keyType).arg(expectedMimeType).arg(entry.invoke("getKey")));
        Variable newValue = forEachBlock.decl(ref(Object.class), "newValue", ExpressionFactory.invoke("evaluateAndTransform").arg(muleMessage).arg(valueType).arg(expectedMimeType).arg(entry.invoke("getValue")));
        forEachBlock.invoke(newMap, "put").arg(newKey).arg(newValue);

        isExpectedMapBlock.assign(target, newMap);

        isExpectedMap._else().assign(target, source);

        Block otherwise = isMap._else();
        otherwise.assign(target, ExpressionFactory.invoke("evaluate").arg(muleMessage).arg(source));


        Conditional shouldTransform = evaluateAndTransform.body()._if(Op.cand(
                Op.ne(target, ExpressionFactory._null()),
                Op.not(ExpressionFactory.invoke("isAssignableFrom").arg(expectedType).arg(target.invoke("getClass")))
        ));

        Variable sourceDataType = shouldTransform._then().decl(ref(DataType.class), "sourceDataType",
                ref(DataTypeFactory.class).staticInvoke("create").arg(target.invoke("getClass")));
        Variable targetDataType = shouldTransform._then().decl(ref(DataType.class), "targetDataType", ExpressionFactory._null());
        
        Conditional ifExpectedMimeTypeNotNull = shouldTransform._then()._if(Op.ne(expectedMimeType, ExpressionFactory._null()));
        ifExpectedMimeTypeNotNull._then().assign(targetDataType, ref(DataTypeFactory.class).staticInvoke("create").arg(
                                ExpressionFactory.cast(ref(Class.class), expectedType)).arg(expectedMimeType));

        ifExpectedMimeTypeNotNull._else().assign(targetDataType, ref(DataTypeFactory.class).staticInvoke("create").arg(
                                ExpressionFactory.cast(ref(Class.class), expectedType)));
        
        Variable transformer = shouldTransform._then().decl(ref(Transformer.class), "t",
                muleContext.invoke("getRegistry").invoke("lookupTransformer").arg(sourceDataType).arg(targetDataType));

        shouldTransform._then()._return(transformer.invoke("transform").arg(target));

        shouldTransform._else()._return(target);
    }


    private void generateEvaluateMethod(DefinedClass messageProcessorClass, FieldVariable patternInfo, FieldVariable expressionManager) {
        Method evaluate = messageProcessorClass.method(Modifier.PRIVATE, ref(Object.class), "evaluate");
        Variable muleMessage = evaluate.param(ref(MuleMessage.class), "muleMessage");
        Variable source = evaluate.param(ref(Object.class), "source");

        Block ifString = evaluate.body()._if(Op._instanceof(source, ref(String.class)))._then();
        Variable stringSource = ifString.decl(ref(String.class), "stringSource", ExpressionFactory.cast(ref(String.class), source));
        Conditional isPattern = ifString._if(Op.cand(
                stringSource.invoke("startsWith").arg(patternInfo.invoke("getPrefix")),
                stringSource.invoke("endsWith").arg(patternInfo.invoke("getSuffix"))
        ));

        isPattern._then()._return(expressionManager.invoke("evaluate").arg(stringSource).arg(muleMessage));
        isPattern._else()._return(expressionManager.invoke("parse").arg(stringSource).arg(muleMessage));

        evaluate.body()._return(source);
    }

    private void generateMessageProcessorClassDoc(ExecutableElement executableElement, DefinedClass messageProcessorClass) {
        messageProcessorClass.javadoc().add(messageProcessorClass.name() + " invokes the ");
        messageProcessorClass.javadoc().add("{@link " + ((TypeElement) executableElement.getEnclosingElement()).getQualifiedName().toString() + "#");
        messageProcessorClass.javadoc().add(executableElement.getSimpleName().toString() + "(");
        boolean first = true;
        for (VariableElement variable : executableElement.getParameters()) {
            if (!first) {
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

    private void generateProcessMethod(ExecutableElement executableElement, DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, Map<String, FieldVariableElement> connectionFields, FieldVariable messageProcessorListener, FieldVariable muleContext, FieldVariable object, FieldVariable logger, FieldVariable retryCount, FieldVariable retryMax) {
        generateProcessMethod(executableElement, messageProcessorClass, fields, connectionFields, messageProcessorListener, muleContext, object, null, logger, retryCount, retryMax);
    }

    private void generateProcessMethod(ExecutableElement executableElement, DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, Map<String, FieldVariableElement> connectionFields, FieldVariable messageProcessorListener, FieldVariable muleContext, FieldVariable object, DefinedClass poolObjectClass, FieldVariable logger, FieldVariable retryCount, FieldVariable retryMax) {
        String methodName = executableElement.getSimpleName().toString();
        Type muleEvent = ref(MuleEvent.class);

        Method process = messageProcessorClass.method(Modifier.PUBLIC, muleEvent, "process");
        process.javadoc().add("Invokes the MessageProcessor.");
        process.javadoc().addParam("event MuleEvent to be processed");
        process.javadoc().addThrows(ref(MuleException.class));

        process._throws(MuleException.class);
        Variable event = process.param(muleEvent, "event");
        Variable muleMessage = process.body().decl(ref(MuleMessage.class), "_muleMessage", event.invoke("getMessage"));

        DefinedClass moduleObjectClass = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey((TypeElement) executableElement.getEnclosingElement()));
        Variable moduleObject = process.body().decl(moduleObjectClass, "castedModuleObject", ExpressionFactory._null());
        findConfig(process.body(), muleContext, object, methodName, event, moduleObjectClass, moduleObject);

        Variable poolObject = declarePoolObjectIfClassNotNull(poolObjectClass, process);

        Map<String, Expression> connectionParameters = declareConnectionParametersVariables(executableElement, connectionFields, process);
        Variable connection = addConnectionVariableIfNeeded(executableElement, process);

        ExecutableElement connectMethod = connectForMethod(executableElement);
        ExecutableElement connectionIdentifierMethod = connectionIdentifierForMethod(executableElement);
        TryStatement callProcessor = process.body()._try();

        if (connectMethod != null) {
            for (VariableElement variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();

                Conditional ifNotNull = callProcessor.body()._if(Op.ne(connectionFields.get(fieldName).getField(),
                        ExpressionFactory._null()));

                Type type = ref(connectionFields.get(fieldName).getVariableElement().asType()).boxify();

                Variable transformed = (Variable) connectionParameters.get(fieldName);

                Invocation getGenericType = messageProcessorClass.dotclass().invoke("getDeclaredField").arg(
                        ExpressionFactory.lit(connectionFields.get(fieldName).getFieldType().name())
                ).invoke("getGenericType");
                Invocation evaluateAndTransform = ExpressionFactory.invoke("evaluateAndTransform").arg(muleMessage).arg(getGenericType).arg(ExpressionFactory._null());

                evaluateAndTransform.arg(connectionFields.get(fieldName).getField());

                Cast cast = ExpressionFactory.cast(type, evaluateAndTransform);

                ifNotNull._then().assign(transformed, cast);

                Invocation evaluateAndTransformLocal = ExpressionFactory.invoke("evaluateAndTransform").arg(muleMessage).arg(getGenericType).arg(ExpressionFactory._null());

                evaluateAndTransformLocal.arg(moduleObject.invoke("get" + StringUtils.capitalize(fieldName)));

                Cast castLocal = ExpressionFactory.cast(type, evaluateAndTransformLocal);

                Conditional ifConfigAlsoNull = ifNotNull._else()._if(Op.eq(moduleObject.invoke("get" + StringUtils.capitalize(fieldName)), ExpressionFactory._null()));
                TypeReference coreMessages = ref(CoreMessages.class);
                Invocation failedToInvoke = coreMessages.staticInvoke("failedToCreate");
                if (methodName != null) {
                    failedToInvoke.arg(ExpressionFactory.lit(methodName));
                }
                Invocation messageException = ExpressionFactory._new(ref(MessagingException.class));
                messageException.arg(failedToInvoke);
                if (event != null) {
                    messageException.arg(event);
                }
                messageException.arg(ExpressionFactory._new(ref(RuntimeException.class)).arg("You must provide a " + fieldName + " at the config or the message processor level."));
                ifConfigAlsoNull._then()._throw(messageException);

                ifNotNull._else().assign(transformed, castLocal);

            }
        }

        List<Expression> parameters = new ArrayList<Expression>();
        Variable interceptCallback = null;
        Variable outboundHeadersMap = null;
        for (VariableElement variable : executableElement.getParameters()) {
            String fieldName = variable.getSimpleName().toString();

            if (variable.asType().toString().startsWith(HttpCallback.class.getName())) {
                parameters.add(fields.get(fieldName).getFieldType());
            } else if (variable.asType().toString().startsWith(SourceCallback.class.getName())) {
                parameters.add(ExpressionFactory._this());
            } else if (variable.getAnnotation(OAuthAccessToken.class) != null) {
                continue;
            } else if (variable.getAnnotation(OAuthAccessTokenSecret.class) != null) {
                continue;
            } else if (context.getTypeMirrorUtils().isNestedProcessor(variable.asType())) {
                declareNestedProcessorParameter(fields, muleContext, event, callProcessor, parameters, variable, fieldName);
            } else if (variable.asType().toString().startsWith(MuleMessage.class.getName())) {
                parameters.add(muleMessage);
            } else {
                outboundHeadersMap = declareStandardParameter(messageProcessorClass, fields, muleMessage, callProcessor, parameters, outboundHeadersMap, variable, fieldName);
            }
        }

        if (connectMethod != null) {
            DefinedClass connectionKey = context.getClassForRole(context.getNameUtils().generateConnectionParametersRoleKey((TypeElement) executableElement.getEnclosingElement()));

            Conditional ifDebugEnabled = callProcessor.body()._if(logger.invoke("isDebugEnabled"));
            Variable messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("Attempting to acquire a connection using "));
            for (String field : connectionParameters.keySet()) {
                ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("[" + field + " = ")));
                ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(connectionParameters.get(field)));
                ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("] ")));
            }
            ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));

            Invocation newKey = ExpressionFactory._new(connectionKey);
            Invocation createConnection = moduleObject.invoke("acquireConnection");
            for (VariableElement variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();
                newKey.arg(connectionParameters.get(fieldName));
            }
            createConnection.arg(newKey);
            callProcessor.body().assign(connection, createConnection);

            Conditional ifConnectionIsNull = callProcessor.body()._if(Op.eq(connection, ExpressionFactory._null()));
            TypeReference coreMessages = ref(CoreMessages.class);
            Invocation failedToInvoke = coreMessages.staticInvoke("failedToCreate");
            if (methodName != null) {
                failedToInvoke.arg(ExpressionFactory.lit(methodName));
            }
            Invocation messageException = ExpressionFactory._new(ref(MessagingException.class));
            messageException.arg(failedToInvoke);
            if (event != null) {
                messageException.arg(event);
            }
            messageException.arg(ExpressionFactory._new(ref(RuntimeException.class)).arg("Cannot create connection"));
            ifConnectionIsNull._then()._throw(messageException);

            ifDebugEnabled = ifConnectionIsNull._else()._if(logger.invoke("isDebugEnabled"));
            messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("Connection has been acquired with "));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("[id = ")));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(
                    connection.invoke(connectionIdentifierMethod.getSimpleName().toString())
            ));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("] ")));
            ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));
        }

        Type returnType = ref(executableElement.getReturnType());

        callProcessor.body().add(retryCount.invoke("getAndIncrement"));

        if (connectMethod != null) {
            generateMethodCall(callProcessor.body(), connection, methodName, parameters, event, returnType, poolObject, interceptCallback, messageProcessorListener);
        } else {
            generateMethodCall(callProcessor.body(), moduleObject, methodName, parameters, event, returnType, poolObject, interceptCallback, messageProcessorListener);
        }

        callProcessor.body().add(retryCount.invoke("set").arg(ExpressionFactory.lit(0)));

        for (VariableElement variable : executableElement.getParameters()) {
            OutboundHeaders outboundHeaders = variable.getAnnotation(OutboundHeaders.class);
            if (outboundHeaders != null) {
                Conditional ifNotEmpty = callProcessor.body()._if(Op.cand(Op.ne(outboundHeadersMap, ExpressionFactory._null()),
                        Op.not(outboundHeadersMap.invoke("isEmpty"))));
                ifNotEmpty._then().add(event.invoke("getMessage").invoke("addProperties").arg(outboundHeadersMap)
                        .arg(ref(PropertyScope.class).staticRef("OUTBOUND")));
            }
        }
        
        if( executableElement.getAnnotation(Mime.class) != null ) {
            Cast defaultMuleMessage = ExpressionFactory.cast(ref(DefaultMuleMessage.class), event.invoke("getMessage"));
            Invocation setMimeType = defaultMuleMessage.invoke("setMimeType").arg(
                    ExpressionFactory.lit(executableElement.getAnnotation(Mime.class).value())
            );
            callProcessor.body().add(setMimeType);
        }

        callProcessor.body()._return(event);

        InvalidateConnectionOn invalidateConnectionOn = executableElement.getAnnotation(InvalidateConnectionOn.class);
        if (connectMethod != null &&
                invalidateConnectionOn != null) {

            final String transformerAnnotationName = InvalidateConnectionOn.class.getName();
            DeclaredType exception = null;
            List<? extends AnnotationMirror> annotationMirrors = executableElement.getAnnotationMirrors();
            for (AnnotationMirror annotationMirror : annotationMirrors) {
                if (transformerAnnotationName.equals(annotationMirror.getAnnotationType().toString())) {
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
                        if ("exception".equals(
                                entry.getKey().getSimpleName().toString())) {
                            exception = (DeclaredType) entry.getValue().getValue();
                            break;
                        }
                    }
                }
            }

            CatchBlock catchBlock = callProcessor._catch(ref(exception).boxify());

            Conditional ifDebugEnabled = catchBlock.body()._if(logger.invoke("isDebugEnabled"));
            Variable messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("An exception ("));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ref(exception).boxify().fullName()));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(") has been thrown while executing "));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit(methodName)));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(". Destroying the connection with [id = "));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(
                    connection.invoke(connectionIdentifierMethod.getSimpleName().toString())
            ));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("]."));
            ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));

            TryStatement innerTry = catchBlock.body()._try();

            DefinedClass connectionKey = context.getClassForRole(context.getNameUtils().generateConnectionParametersRoleKey((TypeElement) executableElement.getEnclosingElement()));
            Invocation newKey = ExpressionFactory._new(connectionKey);
            for (VariableElement variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();
                newKey.arg(connectionParameters.get(fieldName));
            }

            Invocation destroySession = moduleObject.invoke("destroyConnection");
            destroySession.arg(newKey);
            destroySession.arg(connection);

            innerTry.body().add(destroySession);
            innerTry.body().assign(connection, ExpressionFactory._null());

            CatchBlock logException = innerTry._catch(ref(Exception.class));
            Variable destroyException = logException.param("e");
            logException.body().add(logger.invoke("error").arg(destroyException.invoke("getMessage")).arg(destroyException));

            Conditional ifRetryMaxNotReached = catchBlock.body()._if(Op.lte(retryCount.invoke("get"), retryMax));
            ifDebugEnabled = ifRetryMaxNotReached._then()._if(logger.invoke("isDebugEnabled"));
            messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("Forcing a retry [time="));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(retryCount));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(" out of  "));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(retryMax));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("]."));
            ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));

            ifRetryMaxNotReached._then()._return(ExpressionFactory.invoke("process").arg(event));

            Variable invalidConnection = catchBlock.param("invalidConnection");
            TypeReference coreMessages = ref(CoreMessages.class);
            Invocation failedToInvoke = coreMessages.staticInvoke("failedToInvoke");
            if (methodName != null) {
                failedToInvoke.arg(ExpressionFactory.lit(methodName));
            }
            Invocation messageException = ExpressionFactory._new(ref(MessagingException.class));
            messageException.arg(failedToInvoke);
            if (event != null) {
                messageException.arg(event);
            }
            messageException.arg(invalidConnection);
            catchBlock.body()._throw(messageException);
        }

        generateThrow("failedToInvoke", MessagingException.class,
                callProcessor._catch(ref(Exception.class)), event, methodName);

        if (poolObjectClass != null) {
            Block fin = callProcessor._finally();
            Block poolObjectNotNull = fin._if(Op.ne(poolObject, ExpressionFactory._null()))._then();
            poolObjectNotNull.add(moduleObject.invoke("getLifecyleEnabledObjectPool").invoke("returnObject").arg(poolObject));
        }

        if (connectMethod != null) {
            Block fin = callProcessor._finally();

            TryStatement tryToReleaseConnection = fin._try();

            Conditional ifConnectionNotNull = tryToReleaseConnection.body()._if(Op.ne(connection, ExpressionFactory._null()));


            Conditional ifDebugEnabled = ifConnectionNotNull._then()._if(logger.invoke("isDebugEnabled"));
            Variable messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("Releasing the connection back into the pool [id="));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(
                    connection.invoke(connectionIdentifierMethod.getSimpleName().toString())
            ));
            ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("]."));
            ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));


            DefinedClass connectionKey = context.getClassForRole(context.getNameUtils().generateConnectionParametersRoleKey((TypeElement) executableElement.getEnclosingElement()));
            Invocation newKey = ExpressionFactory._new(connectionKey);
            for (VariableElement variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();
                newKey.arg(connectionParameters.get(fieldName));
            }

            Invocation returnConnection = moduleObject.invoke("releaseConnection");
            returnConnection.arg(newKey);
            returnConnection.arg(connection);

            ifConnectionNotNull._then().add(returnConnection);

            generateThrow("failedToInvoke", MessagingException.class,
                    tryToReleaseConnection._catch(ref(Exception.class)), event, methodName);
        }

    }

    private Variable declareStandardParameter(DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, Variable muleMessage, TryStatement callProcessor, List<Expression> parameters, Variable outboundHeadersMap, VariableElement variable, String fieldName) {
        InboundHeaders inboundHeaders = variable.getAnnotation(InboundHeaders.class);
        OutboundHeaders outboundHeaders = variable.getAnnotation(OutboundHeaders.class);
        InvocationHeaders invocationHeaders = variable.getAnnotation(InvocationHeaders.class);
        Payload payload = variable.getAnnotation(Payload.class);

        if (outboundHeaders == null) {
            Type type = ref(fields.get(fieldName).getVariableElement().asType()).boxify();
            String name = "transformed" + StringUtils.capitalize(fieldName);
            Invocation getGenericType = messageProcessorClass.dotclass().invoke("getDeclaredField").arg(
                    ExpressionFactory.lit(fields.get(fieldName).getFieldType().name())
            ).invoke("getGenericType");
            Invocation evaluateAndTransform = ExpressionFactory.invoke("evaluateAndTransform").arg(muleMessage).arg(getGenericType);
            
            Mime mime = fields.get(fieldName).getVariableElement().getAnnotation(Mime.class);
            if( mime != null ) {
                evaluateAndTransform.arg(ExpressionFactory.lit(mime.value()));
            } else {
                evaluateAndTransform.arg(ExpressionFactory._null());
            }

            if (inboundHeaders != null) {
                if (context.getTypeMirrorUtils().isArrayOrList(fields.get(fieldName).getVariableElement().asType())) {
                    evaluateAndTransform.arg("#[" + MessageHeadersListExpressionEvaluator.NAME + ":INBOUND:" + inboundHeaders.value() + "]");
                } else if (context.getTypeMirrorUtils().isMap(fields.get(fieldName).getVariableElement().asType())) {
                    evaluateAndTransform.arg("#[" + MessageHeadersExpressionEvaluator.NAME + ":INBOUND:" + inboundHeaders.value() + "]");
                } else {
                    evaluateAndTransform.arg("#[" + MessageHeaderExpressionEvaluator.NAME + ":INBOUND:" + inboundHeaders.value() + "]");
                }
            } else if (invocationHeaders != null) {
                if (context.getTypeMirrorUtils().isArrayOrList(fields.get(fieldName).getVariableElement().asType())) {
                    evaluateAndTransform.arg("#[" + MessageHeadersListExpressionEvaluator.NAME + ":INVOCATION:" + invocationHeaders.value() + "]");
                } else if (context.getTypeMirrorUtils().isMap(fields.get(fieldName).getVariableElement().asType())) {
                    evaluateAndTransform.arg("#[" + MessageHeadersExpressionEvaluator.NAME + ":INVOCATION:" + invocationHeaders.value() + "]");
                } else {
                    evaluateAndTransform.arg("#[" + MessageHeaderExpressionEvaluator.NAME + ":INVOCATION:" + invocationHeaders.value() + "]");
                }
            } else if (payload != null) {
                evaluateAndTransform.arg("#[payload]");
            } else {
                evaluateAndTransform.arg(fields.get(fieldName).getField());
            }

            Cast cast = ExpressionFactory.cast(type, evaluateAndTransform);

            Variable transformed = callProcessor.body().decl(type, name, cast);
            parameters.add(transformed);
        } else {
            Type type = ref(HashMap.class).narrow(ref(String.class), ref(Object.class));
            String name = "transformed" + StringUtils.capitalize(fieldName);

            outboundHeadersMap = callProcessor.body().decl(type, name, ExpressionFactory._new(type));
            parameters.add(outboundHeadersMap);
        }
        return outboundHeadersMap;
    }

    private void declareNestedProcessorParameter(Map<String, FieldVariableElement> fields, FieldVariable muleContext, Variable event, TryStatement callProcessor, List<Expression> parameters, VariableElement variable, String fieldName) {
        DefinedClass callbackClass = context.getClassForRole(NestedProcessorChainGenerator.ROLE);
        DefinedClass stringCallbackClass = context.getClassForRole(NestedProcessorStringGenerator.ROLE);

        boolean isList = context.getTypeMirrorUtils().isArrayOrList(variable.asType());

        if (!isList) {
            Variable transformed = callProcessor.body().decl(ref(NestedProcessor.class), "transformed" + StringUtils.capitalize(fieldName),
                    ExpressionFactory._null());

            Conditional ifMessageProcessor = callProcessor.body()._if(Op.cand(
                    Op.ne(fields.get(fieldName).getField(), ExpressionFactory._null()),
                    Op._instanceof(fields.get(fieldName).getField(), ref(MessageProcessor.class))));

            ifMessageProcessor._then()
                    .assign(transformed,
                            ExpressionFactory._new(callbackClass).arg(event).arg(muleContext).arg(
                                    ExpressionFactory.cast(ref(MessageProcessor.class), fields.get(fieldName).getField())));

            Conditional ifString = ifMessageProcessor._elseif(Op.cand(
                    Op.ne(fields.get(fieldName).getField(), ExpressionFactory._null()),
                    Op._instanceof(fields.get(fieldName).getField(), ref(String.class))));

            ifString._then()
                    .assign(transformed,
                            ExpressionFactory._new(stringCallbackClass).arg(
                                    ExpressionFactory.cast(ref(String.class), fields.get(fieldName).getField())
                            ));

            parameters.add(transformed);
        } else {
            Variable transformed = callProcessor.body().decl(ref(List.class).narrow(NestedProcessor.class), "transformed" + StringUtils.capitalize(fieldName),
                    ExpressionFactory._new(ref(ArrayList.class).narrow(NestedProcessor.class)));

            Conditional ifMessageProcessor = callProcessor.body()._if(Op.cand(
                    Op.ne(fields.get(fieldName).getField(), ExpressionFactory._null()),
                    Op._instanceof(fields.get(fieldName).getField(), ref(List.class))));

            ForEach forEachProcessor = ifMessageProcessor._then().forEach(ref(MessageProcessor.class),
                    "messageProcessor",
                    ExpressionFactory.cast(ref(List.class).narrow(MessageProcessor.class),
                            fields.get(fieldName).getField()));
            forEachProcessor.body().add(transformed.invoke("add").arg(
                    ExpressionFactory._new(callbackClass).arg(event).arg(muleContext).arg(
                            forEachProcessor.var())
            ));

            Conditional ifString = ifMessageProcessor._elseif(Op.cand(
                    Op.ne(fields.get(fieldName).getField(), ExpressionFactory._null()),
                    Op._instanceof(fields.get(fieldName).getField(), ref(String.class))));

            ifString._then()
                    .add(transformed.invoke("add").arg(
                            ExpressionFactory._new(stringCallbackClass).arg(
                                    ExpressionFactory.cast(ref(String.class), fields.get(fieldName).getField())
                            )));

            parameters.add(transformed);
        }
    }

    private Map<String, Expression> declareConnectionParametersVariables(ExecutableElement executableElement, Map<String, FieldVariableElement> connectionFields, Method process) {
        Map<String, Expression> connectionParameters = new HashMap<String, Expression>();
        ExecutableElement connectMethod = connectForMethod(executableElement);
        if (connectMethod != null) {
            for (VariableElement variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();

                Type type = ref(connectionFields.get(fieldName).getVariableElement().asType()).boxify();
                String name = "transformed" + StringUtils.capitalize(fieldName);

                Variable transformed = process.body().decl(type, name, ExpressionFactory._null());
                connectionParameters.put(fieldName, transformed);
            }
        }
        return connectionParameters;
    }

    private Variable addConnectionVariableIfNeeded(ExecutableElement executableElement, Method process) {
        ExecutableElement connectMethod = connectForMethod(executableElement);
        if (connectForMethod(executableElement) != null) {
            DefinedClass connectionClass = context.getClassForRole(context.getNameUtils().generateConnectorObjectRoleKey((TypeElement) connectMethod.getEnclosingElement()));
            return process.body().decl(connectionClass, "connection", ExpressionFactory._null());
        }
        return null;
    }

    private Variable declarePoolObjectIfClassNotNull(DefinedClass poolObjectClass, Method process) {
        if (poolObjectClass != null) {
            return process.body().decl(poolObjectClass, "poolObject", ExpressionFactory._null());
        }
        return null;
    }

    private Variable generateMethodCall(Block body, Variable object, String methodName, List<Expression> parameters, Variable event, Type returnType, Variable poolObject, Variable interceptCallback, FieldVariable messageProcessorListener) {
        Variable resultPayload = null;
        if (returnType != context.getCodeModel().VOID) {
            resultPayload = body.decl(ref(Object.class), "resultPayload");
        }

        Invocation methodCall;
        if (poolObject != null) {
            body.assign(poolObject, ExpressionFactory.cast(poolObject.type(), object.invoke("getLifecyleEnabledObjectPool").invoke("borrowObject")));
            methodCall = poolObject.invoke(methodName);
        } else {
            methodCall = object.invoke(methodName);
        }

        for (Expression parameter : parameters) {
            methodCall.arg(parameter);
        }

        if (returnType != context.getCodeModel().VOID) {
            body.assign(resultPayload, methodCall);
        } else {
            body.add(methodCall);
        }

        Block scope = body;
        if (interceptCallback != null) {
            Conditional shallContinue = body._if(Op.cand(interceptCallback.invoke("getShallContinue"),
                    Op.ne(messageProcessorListener, ExpressionFactory._null())));

            shallContinue._then().assign(event, messageProcessorListener.invoke("process").arg(event));

            scope = shallContinue._else();
        }

        if (returnType != context.getCodeModel().VOID) {
            generatePayloadOverwrite(scope, event, resultPayload);
        }

        return resultPayload;
    }

    private void generatePayloadOverwrite(Block block, Variable event, Variable resultPayload) {
        Invocation applyTransformers = event.invoke("getMessage").invoke("applyTransformers");
        applyTransformers.arg(event);
        Invocation newTransformerTemplate = ExpressionFactory._new(ref(TransformerTemplate.class));

        Variable overwritePayloadCallback = block.decl(ref(TransformerTemplate.OverwitePayloadCallback.class), "overwritePayloadCallback", ExpressionFactory._null());

        Conditional ifPayloadIsNull = block._if(resultPayload.eq(ExpressionFactory._null()));

        Invocation newOverwritePayloadCallback = ExpressionFactory._new(ref(TransformerTemplate.OverwitePayloadCallback.class));
        newOverwritePayloadCallback.arg(resultPayload);
        Invocation newOverwritePayloadCallbackWithNull = ExpressionFactory._new(ref(TransformerTemplate.OverwitePayloadCallback.class));
        newOverwritePayloadCallbackWithNull.arg(ref(NullPayload.class).staticInvoke("getInstance"));
        ifPayloadIsNull._else().assign(overwritePayloadCallback, newOverwritePayloadCallback);
        ifPayloadIsNull._then().assign(overwritePayloadCallback, newOverwritePayloadCallbackWithNull);

        newTransformerTemplate.arg(overwritePayloadCallback);

        Variable transformerList = block.decl(ref(List.class).narrow(Transformer.class), "transformerList");
        block.assign(transformerList, ExpressionFactory._new(ref(ArrayList.class).narrow(Transformer.class)));
        block.add(transformerList.invoke("add").arg(newTransformerTemplate));

        applyTransformers.arg(transformerList);
        block.add(applyTransformers);
    }
}