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
import org.mule.api.annotations.Module;
import org.mule.api.annotations.NestedProcessor;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.callback.HttpCallback;
import org.mule.api.annotations.callback.InterceptCallback;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.api.annotations.oauth.OAuthAccessToken;
import org.mule.api.annotations.oauth.OAuthAccessTokenSecret;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.InvocationHeaders;
import org.mule.api.annotations.param.Payload;
import org.mule.api.annotations.param.Session;
import org.mule.api.annotations.session.InvalidateSessionOn;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.generation.DevkitTypeElement;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.Cast;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.Expression;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.ForEach;
import org.mule.devkit.model.code.ForLoop;
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
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class MessageProcessorGenerator extends AbstractMessageGenerator {

    private static final String HTTP_STATUS_PROPERTY = "http.status";
    private static final String REDIRECT_HTTP_STATUS = "302";
    private static final String LOCATION_PROPERTY = "Location";

    @Override
    protected boolean shouldGenerate(DevkitTypeElement typeElement) {
        return true;
    }

    @Override
    protected void doGenerate(DevkitTypeElement typeElement) {
        for (ExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            generateMessageProcessor(typeElement, executableElement);
        }
    }

    private void generateMessageProcessor(DevkitTypeElement typeElement, ExecutableElement executableElement) {
        // get class
        DefinedClass messageProcessorClass;

        boolean intercepting = executableElement.getAnnotation(Processor.class).intercepting();
        if (intercepting) {
            messageProcessorClass = getInterceptingMessageProcessorClass(executableElement);
        } else {
            messageProcessorClass = getMessageProcessorClass(executableElement);
        }

        // add javadoc
        generateMessageProcessorClassDoc(executableElement, messageProcessorClass);

        // add a field for each argument of the method
        Map<String, AbstractMessageGenerator.FieldVariableElement> fields = generateProcessorFieldForEachParameter(messageProcessorClass, executableElement);

        // add fields for session if required
        ExecutableElement sessionCreate = createSessionForClass(typeElement);
        Map<String, AbstractMessageGenerator.FieldVariableElement> sessionFields = null;
        if (sessionCreate != null) {
            sessionFields = generateProcessorFieldForEachParameter(messageProcessorClass, sessionCreate);
        }

        // add standard fields
        FieldVariable object = generateFieldForModuleObject(messageProcessorClass, typeElement);
        FieldVariable muleContext = generateFieldForMuleContext(messageProcessorClass);
        FieldVariable expressionManager = generateFieldForExpressionManager(messageProcessorClass);
        FieldVariable patternInfo = generateFieldForPatternInfo(messageProcessorClass);
        FieldVariable flowConstruct = generateFieldForFlowConstruct(messageProcessorClass);

        FieldVariable messageProcessorListener = null;
        if (intercepting) {
            messageProcessorListener = generateFieldForMessageProcessorListener(messageProcessorClass);
        }

        // add initialise
        generateInitialiseMethod(messageProcessorClass, fields, typeElement, muleContext, expressionManager, patternInfo, object);

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
        }

        // add setobject
        generateSetModuleObjectMethod(messageProcessorClass, object);

        // generate setters for all parameters
        for (String fieldName : fields.keySet()) {
            generateSetter(messageProcessorClass, fields.get(fieldName).getField());
        }

        // generate setters for session fields
        if (sessionFields != null) {
            for (String fieldName : sessionFields.keySet()) {
                generateSetter(messageProcessorClass, sessionFields.get(fieldName).getField());
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
        if (typeElement.getAnnotation(Module.class).poolable()) {
            DefinedClass poolObjectClass = context.getClassForRole(context.getNameUtils().generatePoolObjectRoleKey(typeElement));

            // add process method
            generateProcessMethod(executableElement, messageProcessorClass, fields, sessionFields, messageProcessorListener, muleContext, object, expressionManager, patternInfo, poolObjectClass);
        } else {
            // add process method
            generateProcessMethod(executableElement, messageProcessorClass, fields, sessionFields, messageProcessorListener, muleContext, object, expressionManager, patternInfo);
        }
    }

    private void generateStartMethod(DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields) {
        Method startMethod = messageProcessorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "start");
        startMethod._throws(ref(MuleException.class));

        for (String fieldName : fields.keySet()) {
            FieldVariableElement variableElement = fields.get(fieldName);

            if (context.getTypeMirrorUtils().isNestedProcessor(variableElement.getVariableElement().asType())) {
                boolean isList = context.getTypeMirrorUtils().isArrayOrList(variableElement.getVariableElement().asType());

                if (!isList) {
                    Conditional ifStartable = startMethod.body()._if(Op._instanceof(variableElement.getField(), ref(Startable.class)));
                    ifStartable._then().add(
                            ExpressionFactory.cast(ref(Startable.class), variableElement.getField()).invoke("start")
                    );
                } else {
                    Conditional ifIsList = startMethod.body()._if(Op._instanceof(variableElement.getField(), ref(List.class)));
                    ForEach forEachProcessor = ifIsList._then().forEach(ref(MessageProcessor.class), "messageProcessor", ExpressionFactory.cast(ref(List.class).narrow(MessageProcessor.class), fields.get(fieldName).getField()));
                    Conditional ifStartable = forEachProcessor.body()._if(Op._instanceof(forEachProcessor.var(), ref(Startable.class)));
                    ifStartable._then().add(
                            ExpressionFactory.cast(ref(Startable.class), forEachProcessor.var()).invoke("start")
                    );
                }
            } else if (variableElement.getVariableElement().asType().toString().contains(HttpCallback.class.getName())) {
                startMethod.body()._if(Op.ne(variableElement.getFieldType(), ExpressionFactory._null()))._then().invoke(variableElement.getFieldType(), "start");
            }
        }
    }

    private void generateStopMethod(DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields) {
        Method stopMethod = messageProcessorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "stop");
        stopMethod._throws(ref(MuleException.class));

        for (String fieldName : fields.keySet()) {
            FieldVariableElement variableElement = fields.get(fieldName);

            if (context.getTypeMirrorUtils().isNestedProcessor(variableElement.getVariableElement().asType())) {
                boolean isList = context.getTypeMirrorUtils().isArrayOrList(variableElement.getVariableElement().asType());

                if (!isList) {
                    Conditional ifStoppable = stopMethod.body()._if(Op._instanceof(variableElement.getField(), ref(Stoppable.class)));
                    ifStoppable._then().add(
                            ExpressionFactory.cast(ref(Stoppable.class), variableElement.getField()).invoke("stop")
                    );
                } else {
                    Conditional ifIsList = stopMethod.body()._if(Op._instanceof(variableElement.getField(), ref(List.class)));
                    ForEach forEachProcessor = ifIsList._then().forEach(ref(MessageProcessor.class), "messageProcessor",
                            ExpressionFactory.cast(ref(List.class).narrow(MessageProcessor.class), fields.get(fieldName).getField()));
                    Conditional ifStoppable = forEachProcessor.body()._if(Op._instanceof(forEachProcessor.var(), ref(Stoppable.class)));
                    ifStoppable._then().add(
                            ExpressionFactory.cast(ref(Stoppable.class), forEachProcessor.var()).invoke("stop")
                    );
                }
            } else if (variableElement.getVariableElement().asType().toString().contains(HttpCallback.class.getName())) {
                stopMethod.body()._if(Op.ne(variableElement.getFieldType(), ExpressionFactory._null()))._then().invoke(variableElement.getFieldType(), "stop");
            }
        }
    }

    private void generateDiposeMethod(DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields) {
        Method diposeMethod = messageProcessorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "dispose");

        for (String fieldName : fields.keySet()) {
            FieldVariableElement variableElement = fields.get(fieldName);

            if (context.getTypeMirrorUtils().isNestedProcessor(variableElement.getVariableElement().asType())) {
                boolean isList = context.getTypeMirrorUtils().isArrayOrList(variableElement.getVariableElement().asType());

                if (!isList) {
                    Conditional ifDisposable = diposeMethod.body()._if(Op._instanceof(variableElement.getField(), ref(Disposable.class)));
                    ifDisposable._then().add(
                            ExpressionFactory.cast(ref(Disposable.class), variableElement.getField()).invoke("dispose")
                    );
                } else {
                    Conditional ifIsList = diposeMethod.body()._if(Op._instanceof(variableElement.getField(), ref(List.class)));
                    ForEach forEachProcessor = ifIsList._then().forEach(ref(MessageProcessor.class), "messageProcessor", ExpressionFactory.cast(ref(List.class).narrow(MessageProcessor.class), fields.get(fieldName).getField()));
                    Conditional ifDisposable = forEachProcessor.body()._if(Op._instanceof(forEachProcessor.var(), ref(Disposable.class)));
                    ifDisposable._then().add(
                            ExpressionFactory.cast(ref(Disposable.class), forEachProcessor.var()).invoke("dispose")
                    );
                }
            }
        }
    }

    private void generateEvaluateAndTransformMethod(DefinedClass messageProcessorClass, FieldVariable muleContext) {
        Method evaluateAndTransform = messageProcessorClass.method(Modifier.PRIVATE, ref(Object.class), "evaluateAndTransform");
        evaluateAndTransform._throws(ref(TransformerException.class));
        Variable muleMessage = evaluateAndTransform.param(ref(MuleMessage.class), "muleMessage");
        Variable expectedType = evaluateAndTransform.param(ref(java.lang.reflect.Type.class), "expectedType");
        Variable source = evaluateAndTransform.param(ref(Object.class), "source");

        evaluateAndTransform.body()._if(Op.eq(source, ExpressionFactory._null()))._then()._return(source);

        Variable target = evaluateAndTransform.body().decl(ref(Object.class), "target", ExpressionFactory._null());
        Conditional isList = evaluateAndTransform.body()._if(
                ExpressionFactory.invoke("isList").arg(source.invoke("getClass")));
        Conditional isExpectedList = isList._then()._if(
                ExpressionFactory.invoke("isList").arg(expectedType));
        Variable listParameterizedType = isExpectedList._then().decl(ref(java.lang.reflect.Type.class), "valueType",
                ExpressionFactory.cast(ref(ParameterizedType.class), expectedType).
                        invoke("getActualTypeArguments").component(ExpressionFactory.lit(0)));
        Variable listIterator = isExpectedList._then().decl(ref(ListIterator.class), "iterator",
                ExpressionFactory.cast(ref(List.class), source).
                        invoke("listIterator"));

        Block whileHasNext = isExpectedList._then()._while(listIterator.invoke("hasNext")).body();
        Variable subTarget = whileHasNext.decl(ref(Object.class), "subTarget", listIterator.invoke("next"));
        whileHasNext.add(listIterator.invoke("set").arg(
                ExpressionFactory.invoke("evaluateAndTransform").arg(muleMessage).arg(listParameterizedType).
                        arg(subTarget)
        ));
        isList._then().assign(target, source);

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

        Conditional ifKeysNotOfSameType = isExpectedMapBlock._if(Op.cand(
                Op.not(map.invoke("isEmpty")),
                Op.not(ExpressionFactory.invoke(expectedType, "equals").arg(ExpressionFactory.invoke(ExpressionFactory.invoke(ExpressionFactory.invoke(map.invoke("keySet"), "iterator"), "next"), "getClass")))));
        Block ifKeysNotOfSameTypeThen = ifKeysNotOfSameType._then().block();

        Variable newMap = ifKeysNotOfSameTypeThen.decl(ref(Map.class), "newMap", ExpressionFactory._new(ref(HashMap.class)));
        ForEach forEach = ifKeysNotOfSameTypeThen.forEach(ref(Object.class), "entryObj", map.invoke("entrySet"));
        Block forEachBlock = forEach.body().block();
        Variable entry = forEachBlock.decl(ref(Map.Entry.class), "entry", ExpressionFactory.cast(ref(Map.Entry.class), forEach.var()));
        Variable newKey = forEachBlock.decl(ref(Object.class), "newKey", ExpressionFactory.invoke("evaluateAndTransform").arg(muleMessage).arg(keyType).arg(entry.invoke("getKey")));
        Variable newValue = forEachBlock.decl(ref(Object.class), "newValue", ExpressionFactory.invoke("evaluateAndTransform").arg(muleMessage).arg(valueType).arg(entry.invoke("getValue")));
        forEachBlock.invoke(newMap, "put").arg(newKey).arg(newValue);

        ifKeysNotOfSameTypeThen.assign(source, newMap);

        Cast mapCast = ExpressionFactory.cast(ref(Map.class), source);

        ForEach keyLoop = ifKeysNotOfSameType._else().forEach(ref(Object.class), "key", mapCast.invoke("entrySet"));

        Cast entryCast = ExpressionFactory.cast(ref(Map.Entry.class), keyLoop.var());

        Variable value = keyLoop.body().decl(ref(Object.class), "value", mapCast.invoke("get").arg(entryCast.invoke("getKey")));
        keyLoop.body().add(entryCast.invoke("setValue").arg(
                ExpressionFactory.invoke("evaluateAndTransform").arg(muleMessage).arg(valueType).arg(value)
        ));

        isMap._then().assign(target, source);

        Block otherwise = isMap._else();
        otherwise.assign(target, ExpressionFactory.invoke("evaluate").arg(muleMessage).arg(source));


        Conditional shouldTransform = evaluateAndTransform.body()._if(Op.cand(
                Op.ne(target, ExpressionFactory._null()),
                Op.not(ExpressionFactory.invoke("isAssignableFrom").arg(expectedType).arg(target.invoke("getClass")))
        ));

        Variable sourceDataType = shouldTransform._then().decl(ref(DataType.class), "sourceDataType",
                ref(DataTypeFactory.class).staticInvoke("create").arg(target.invoke("getClass")));
        Variable targetDataType = shouldTransform._then().decl(ref(DataType.class), "targetDataType",
                ref(DataTypeFactory.class).staticInvoke("create").arg(
                        ExpressionFactory.cast(ref(Class.class), expectedType)));

        Variable transformer = shouldTransform._then().decl(ref(Transformer.class), "t",
                muleContext.invoke("getRegistry").invoke("lookupTransformer").arg(sourceDataType).arg(targetDataType));

        shouldTransform._then()._return(transformer.invoke("transform").arg(target));

        shouldTransform._else()._return(target);
    }

    private void generateIsAssignableFrom(DefinedClass messageProcessorClass) {
        Method isAssignableFrom = messageProcessorClass.method(Modifier.PRIVATE, context.getCodeModel().BOOLEAN, "isAssignableFrom");
        Variable expectedType = isAssignableFrom.param(ref(java.lang.reflect.Type.class), "expectedType");
        Variable clazz = isAssignableFrom.param(ref(Class.class), "clazz");

        Block isClass = isAssignableFrom.body()._if(Op._instanceof(expectedType, ref(Class.class)))._then();
        isClass._return(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("isAssignableFrom").arg(clazz)
        );

        Block isParameterizedType = isAssignableFrom.body()._if(
                Op._instanceof(expectedType, ref(ParameterizedType.class)))._then();
        isParameterizedType._return(
                ExpressionFactory.invoke("isAssignableFrom").arg(
                        ExpressionFactory.cast(ref(ParameterizedType.class), expectedType).invoke("getRawType")
                ).arg(
                        clazz
                )
        );

        Block isWildcardType = isAssignableFrom.body()._if(
                Op._instanceof(expectedType, ref(WildcardType.class)))._then();
        Variable upperBounds = isWildcardType.decl(ref(java.lang.reflect.Type.class).array(), "upperBounds",
                ExpressionFactory.cast(ref(WildcardType.class), expectedType).invoke("getUpperBounds"));
        Block ifHasUpperBounds = isWildcardType._if(Op.ne(upperBounds.ref("length"), ExpressionFactory.lit(0)))._then();
        ifHasUpperBounds._return(
                ExpressionFactory.invoke("isAssignableFrom").arg(
                        upperBounds.component(ExpressionFactory.lit(0))).arg(clazz));

        isAssignableFrom.body()._return(ExpressionFactory.FALSE);
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

    private void generateIsListMethod(DefinedClass messageProcessorClass) {
        Method isList = messageProcessorClass.method(Modifier.PRIVATE, context.getCodeModel().BOOLEAN, "isList");
        Variable type = isList.param(ref(java.lang.reflect.Type.class), "type");

        Conditional isClass = isList.body()._if(Op.cand(Op._instanceof(type, ref(Class.class)),
                ExpressionFactory.invoke("isListClass").arg(ExpressionFactory.cast(ref(Class.class), type))));
        isClass._then()._return(ExpressionFactory.TRUE);

        Conditional isParameterizedType = isList.body()._if(Op._instanceof(type, ref(ParameterizedType.class)));
        isParameterizedType._then()._return(
                ExpressionFactory.invoke("isList").arg(
                        ExpressionFactory.cast(ref(ParameterizedType.class), type).invoke("getRawType")
                )
        );

        Conditional isWildcardType = isList.body()._if(Op._instanceof(type, ref(WildcardType.class)));
        Variable upperBounds = isWildcardType._then().decl(ref(java.lang.reflect.Type.class).array(), "upperBounds",
                ExpressionFactory.cast(ref(WildcardType.class), type).invoke("getUpperBounds"));
        isWildcardType._then()._return(Op.cand(
                Op.ne(upperBounds.ref("length"), ExpressionFactory.lit(0)),
                ExpressionFactory.invoke("isList").arg(upperBounds.component(ExpressionFactory.lit(0)))
        ));

        isList.body()._return(ExpressionFactory.FALSE);
    }

    private void generateIsMapMethod(DefinedClass messageProcessorClass) {
        Method isMap = messageProcessorClass.method(Modifier.PRIVATE, context.getCodeModel().BOOLEAN, "isMap");
        Variable type = isMap.param(ref(java.lang.reflect.Type.class), "type");

        Conditional isClass = isMap.body()._if(Op.cand(Op._instanceof(type, ref(Class.class)),
                ExpressionFactory.invoke("isMapClass").arg(ExpressionFactory.cast(ref(Class.class), type))));
        isClass._then()._return(ExpressionFactory.TRUE);

        Conditional isParameterizedType = isMap.body()._if(Op._instanceof(type, ref(ParameterizedType.class)));
        isParameterizedType._then()._return(
                ExpressionFactory.invoke("isMap").arg(
                        ExpressionFactory.cast(ref(ParameterizedType.class), type).invoke("getRawType")
                )
        );

        Conditional isWildcardType = isMap.body()._if(Op._instanceof(type, ref(WildcardType.class)));
        Variable upperBounds = isWildcardType._then().decl(ref(java.lang.reflect.Type.class).array(), "upperBounds",
                ExpressionFactory.cast(ref(WildcardType.class), type).invoke("getUpperBounds"));
        isWildcardType._then()._return(Op.cand(
                Op.ne(upperBounds.ref("length"), ExpressionFactory.lit(0)),
                ExpressionFactory.invoke("isMap").arg(upperBounds.component(ExpressionFactory.lit(0)))
        ));

        isMap.body()._return(ExpressionFactory.FALSE);
    }

    private void generateIsListClassMethod(DefinedClass messageProcessorClass) {
        Method isListClass = messageProcessorClass.method(Modifier.PRIVATE, context.getCodeModel().BOOLEAN, "isListClass");
        isListClass.javadoc().add("Checks whether the specified class parameter is an instance of ");
        isListClass.javadoc().add(ref(List.class));
        isListClass.javadoc().addParam("clazz <code>Class</code> to check.");
        isListClass.javadoc().addReturn("<code>true</code> is <code>clazz</code> is instance of a collection class, <code>false</code> otherwise.");

        Variable clazz = isListClass.param(ref(Class.class), "clazz");
        Variable classes = isListClass.body().decl(ref(List.class).narrow(ref(Class.class)), "classes", ExpressionFactory._new(ref(ArrayList.class).narrow(ref(Class.class))));
        isListClass.body().invoke("computeClassHierarchy").arg(clazz).arg(classes);

        isListClass.body()._return(classes.invoke("contains").arg(ref(List.class).dotclass()));
    }

    private void generateIsMapClassMethod(DefinedClass messageProcessorClass) {
        Method isMapClass = messageProcessorClass.method(Modifier.PRIVATE, context.getCodeModel().BOOLEAN, "isMapClass");
        isMapClass.javadoc().add("Checks whether the specified class parameter is an instance of ");
        isMapClass.javadoc().add(ref(Map.class));
        isMapClass.javadoc().addParam("clazz <code>Class</code> to check.");
        isMapClass.javadoc().addReturn("<code>true</code> is <code>clazz</code> is instance of a collection class, <code>false</code> otherwise.");

        Variable clazz = isMapClass.param(ref(Class.class), "clazz");
        Variable classes = isMapClass.body().decl(ref(List.class).narrow(ref(Class.class)), "classes", ExpressionFactory._new(ref(ArrayList.class).narrow(ref(Class.class))));
        isMapClass.body().invoke("computeClassHierarchy").arg(clazz).arg(classes);

        isMapClass.body()._return(classes.invoke("contains").arg(ref(Map.class).dotclass()));
    }

    private void generateComputeClassHierarchyMethod(DefinedClass messageProcessorClass) {
        Method computeClassHierarchy = messageProcessorClass.method(Modifier.PRIVATE, context.getCodeModel().VOID, "computeClassHierarchy");
        computeClassHierarchy.javadoc().add("Get all superclasses and interfaces recursively.");
        computeClassHierarchy.javadoc().addParam("clazz   The class to start the search with.");
        computeClassHierarchy.javadoc().addParam("classes List of classes to which to add all found super classes and interfaces.");
        Variable clazz = computeClassHierarchy.param(Class.class, "clazz");
        Variable classes = computeClassHierarchy.param(List.class, "classes");

        ForLoop iterateClasses = computeClassHierarchy.body()._for();
        Variable current = iterateClasses.init(ref(Class.class), "current", clazz);
        iterateClasses.test(Op.ne(current, ExpressionFactory._null()));
        iterateClasses.update(current.assign(current.invoke("getSuperclass")));

        Block ifContains = iterateClasses.body()._if(classes.invoke("contains").arg(current))._then();
        ifContains._return();

        iterateClasses.body().add(classes.invoke("add").arg(current));

        ForEach iterateInterfaces = iterateClasses.body().forEach(ref(Class.class), "currentInterface", current.invoke("getInterfaces"));
        iterateInterfaces.body().invoke("computeClassHierarchy").arg(iterateInterfaces.var()).arg(classes);
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

    private void generateProcessMethod(ExecutableElement executableElement, DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, Map<String, FieldVariableElement> sessionFields, FieldVariable messageProcessorListener, FieldVariable muleContext, FieldVariable object, FieldVariable expressionManager, FieldVariable patternInfo) {
        generateProcessMethod(executableElement, messageProcessorClass, fields, sessionFields, messageProcessorListener, muleContext, object, expressionManager, patternInfo, null);
    }

    private void generateProcessMethod(ExecutableElement executableElement, DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, Map<String, FieldVariableElement> sessionFields, FieldVariable messageProcessorListener, FieldVariable muleContext, FieldVariable object, FieldVariable expressionManager, FieldVariable patternInfo, DefinedClass poolObjectClass) {
        String methodName = executableElement.getSimpleName().toString();
        Type muleEvent = ref(MuleEvent.class);

        Method process = messageProcessorClass.method(Modifier.PUBLIC, muleEvent, "process");
        process.javadoc().add("Invokes the MessageProcessor.");
        process.javadoc().addParam("event MuleEvent to be processed");
        process.javadoc().addThrows(ref(MuleException.class));

        process._throws(MuleException.class);
        Variable event = process.param(muleEvent, "event");
        Variable muleMessage = process.body().decl(ref(MuleMessage.class), "muleMessage", event.invoke("getMessage"));

        Variable poolObject = null;
        if (poolObjectClass != null) {
            poolObject = process.body().decl(poolObjectClass, "poolObject", ExpressionFactory._null());
        }

        if (executableElement.getEnclosingElement().getAnnotation(OAuth.class) != null ||
                executableElement.getEnclosingElement().getAnnotation(OAuth2.class) != null) {
            for (VariableElement variable : executableElement.getParameters()) {
                if (variable.getAnnotation(OAuthAccessToken.class) != null || variable.getAnnotation(OAuthAccessTokenSecret.class) != null) {
                    addOauth(process, event, object, executableElement);
                    break;
                }
            }
        }

        // add session field declarations
        Map<String, Expression> sessionParameters = new HashMap<String, Expression>();
        ExecutableElement createSession = createSessionForMethod(executableElement);
        Variable session = null;
        if (createSession != null) {
            session = process.body().decl(ref(createSession.getReturnType()), "session", ExpressionFactory._null());

            for (VariableElement variable : createSession.getParameters()) {
                String fieldName = variable.getSimpleName().toString();

                Type type = ref(sessionFields.get(fieldName).getVariableElement().asType()).boxify();
                String name = "transformed" + StringUtils.capitalize(fieldName);

                Variable transformed = process.body().decl(type, name, ExpressionFactory._null());
                sessionParameters.put(fieldName, transformed);
            }
        }

        TryStatement callProcessor = process.body()._try();

        if (createSession != null) {
            for (VariableElement variable : createSession.getParameters()) {
                String fieldName = variable.getSimpleName().toString();

                Conditional ifNotNull = callProcessor.body()._if(Op.ne(sessionFields.get(fieldName).getField(),
                        ExpressionFactory._null()));

                Type type = ref(sessionFields.get(fieldName).getVariableElement().asType()).boxify();
                String name = "transformed" + StringUtils.capitalize(fieldName);

                Variable transformed = (Variable) sessionParameters.get(fieldName);

                Invocation getGenericType = messageProcessorClass.dotclass().invoke("getDeclaredField").arg(
                        ExpressionFactory.lit(sessionFields.get(fieldName).getFieldType().name())
                ).invoke("getGenericType");
                Invocation evaluateAndTransform = ExpressionFactory.invoke("evaluateAndTransform").arg(muleMessage).arg(getGenericType);

                evaluateAndTransform.arg(sessionFields.get(fieldName).getField());

                Cast cast = ExpressionFactory.cast(type, evaluateAndTransform);

                ifNotNull._then().assign(transformed, cast);

                Invocation evaluateAndTransformLocal = ExpressionFactory.invoke("evaluateAndTransform").arg(muleMessage).arg(getGenericType);

                evaluateAndTransformLocal.arg(object.invoke("get" + StringUtils.capitalize(fieldName)));

                Cast castLocal = ExpressionFactory.cast(type, evaluateAndTransformLocal);

                ifNotNull._else().assign(transformed, castLocal);

            }
        }

        List<Expression> parameters = new ArrayList<Expression>();
        Variable interceptCallback = null;
        for (VariableElement variable : executableElement.getParameters()) {
            String fieldName = variable.getSimpleName().toString();

            if (variable.asType().toString().contains(InterceptCallback.class.getName())) {

                DefinedClass callbackClass = context.getClassForRole(InterceptCallbackGenerator.ROLE);

                interceptCallback = callProcessor.body().decl(callbackClass, "transformed" + StringUtils.capitalize(fieldName),
                        ExpressionFactory._new(callbackClass));

                parameters.add(interceptCallback);
            } else if (variable.getAnnotation(Session.class) != null) {
                if (createSession != null) {
                    Invocation createSessionInvoke = object.invoke("borrowSession");
                    for (String field : sessionParameters.keySet()) {
                        createSessionInvoke.arg(sessionParameters.get(field));
                    }

                    callProcessor.body().assign(session, createSessionInvoke);

                    parameters.add(session);
                } else {
                    parameters.add(ExpressionFactory._null());
                }
            } else if (variable.asType().toString().contains(HttpCallback.class.getName())) {
                parameters.add(fields.get(fieldName).getFieldType());
            } else if (variable.getAnnotation(OAuthAccessToken.class) != null) {
                Invocation getAccessToken = object.invoke("get" + StringUtils.capitalize(OAuthAdapterGenerator.OAUTH_ACCESS_TOKEN_FIELD_NAME));
                Variable accessToken = callProcessor.body().decl(ref(String.class), "accessToken", getAccessToken);
                parameters.add(accessToken);
            } else if (variable.getAnnotation(OAuthAccessTokenSecret.class) != null) {
                Invocation getAccessToken = object.invoke("get" + StringUtils.capitalize(OAuthAdapterGenerator.OAUTH_ACCESS_TOKEN_SECRET_FIELD_NAME));
                Variable accessTokenSecret = callProcessor.body().decl(ref(String.class), "accessTokenSecret", getAccessToken);
                parameters.add(accessTokenSecret);
            } else if (context.getTypeMirrorUtils().isNestedProcessor(variable.asType())) {
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

            } else {
                InboundHeaders inboundHeaders = variable.getAnnotation(InboundHeaders.class);
                InvocationHeaders invocationHeaders = variable.getAnnotation(InvocationHeaders.class);
                Payload payload = variable.getAnnotation(Payload.class);

                Type type = ref(fields.get(fieldName).getVariableElement().asType()).boxify();
                String name = "transformed" + StringUtils.capitalize(fieldName);
                Invocation getGenericType = messageProcessorClass.dotclass().invoke("getDeclaredField").arg(
                        ExpressionFactory.lit(fields.get(fieldName).getFieldType().name())
                ).invoke("getGenericType");
                Invocation evaluateAndTransform = ExpressionFactory.invoke("evaluateAndTransform").arg(muleMessage).arg(getGenericType);

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
            }
        }

        Type returnType = ref(executableElement.getReturnType());

        generateMethodCall(callProcessor.body(), object, methodName, parameters, muleContext, event, returnType, poolObject, interceptCallback, messageProcessorListener);

        callProcessor.body()._return(event);

        InvalidateSessionOn invalidateSessionOn = executableElement.getAnnotation(InvalidateSessionOn.class);
        if (createSession != null &&
                invalidateSessionOn != null) {

            final String transformerAnnotationName = InvalidateSessionOn.class.getName();
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

            TryStatement innerTry = catchBlock.body()._try();

            Invocation destroySession = object.invoke("destroySession");
            for (String field : sessionParameters.keySet()) {
                destroySession.arg(sessionParameters.get(field));
            }
            destroySession.arg(session);

            innerTry.body().add(destroySession);
            innerTry.body().assign(session, ExpressionFactory._null());

            generateThrow("failedToInvoke", MessagingException.class,
                    innerTry._catch(ref(Exception.class)), event, methodName);

            Variable invalidSession = catchBlock.param("invalidSession");
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
            messageException.arg(invalidSession);
            catchBlock.body()._throw(messageException);
        }

        generateThrow("failedToInvoke", MessagingException.class,
                callProcessor._catch(ref(Exception.class)), event, methodName);

        if (poolObjectClass != null) {
            Block fin = callProcessor._finally();
            Block poolObjectNotNull = fin._if(Op.ne(poolObject, ExpressionFactory._null()))._then();
            poolObjectNotNull.add(object.invoke("getLifecyleEnabledObjectPool").invoke("returnObject").arg(poolObject));
        }

        if (createSession != null) {
            Block fin = callProcessor._finally();
            Block sessionNotNull = fin._if(Op.ne(session, ExpressionFactory._null()))._then();

            TryStatement tryToReleaseSession = sessionNotNull._try();

            Invocation releaseSession = object.invoke("returnSession");
            for (String field : sessionParameters.keySet()) {
                releaseSession.arg(sessionParameters.get(field));
            }
            releaseSession.arg(session);

            tryToReleaseSession.body().add(releaseSession);

            generateThrow("failedToInvoke", MessagingException.class,
                    tryToReleaseSession._catch(ref(Exception.class)), event, methodName);
        }

    }

    private void addOauth(Method process, Variable event, FieldVariable object, ExecutableElement executableElement) {
        OAuth2 oauth2 = executableElement.getEnclosingElement().getAnnotation(OAuth2.class);
        if (oauth2 != null && !StringUtils.isEmpty(oauth2.expirationRegex())) {
            Block ifTokenExpired = process.body()._if(object.invoke(OAuth2AdapterGenerator.HAS_TOKEN_EXPIRED_METHOD_NAME))._then();
            ifTokenExpired.invoke(object, OAuth2AdapterGenerator.RESET_METHOD_NAME);
        }

        Invocation oauthVerifier = object.invoke("get" + StringUtils.capitalize(OAuthAdapterGenerator.OAUTH_VERIFIER_FIELD_NAME));
        Block ifOauthVerifierIsNull = process.body()._if(isNull(oauthVerifier))._then();
        Variable authorizationUrl = ifOauthVerifierIsNull.decl(ref(String.class), "authorizationUrl", ExpressionFactory.invoke(object, OAuthAdapterGenerator.GET_AUTHORIZATION_URL_METHOD_NAME));
        ifOauthVerifierIsNull.invoke(event.invoke("getMessage"), "setOutboundProperty").arg(HTTP_STATUS_PROPERTY).arg(REDIRECT_HTTP_STATUS);
        ifOauthVerifierIsNull.invoke(event.invoke("getMessage"), "setOutboundProperty").arg(LOCATION_PROPERTY).arg(authorizationUrl);
        ifOauthVerifierIsNull._return(event);

        Invocation accessToken = object.invoke("get" + StringUtils.capitalize(OAuthAdapterGenerator.OAUTH_ACCESS_TOKEN_FIELD_NAME));
        Block ifAccessTokenIsNull = process.body()._if(isNull(accessToken))._then();
        ifAccessTokenIsNull.invoke(object, OAuthAdapterGenerator.FETCH_ACCESS_TOKEN_METHOD_NAME);
    }

    private Variable generateMethodCall(Block body, FieldVariable object, String methodName, List<Expression> parameters, FieldVariable muleContext, Variable event, Type returnType, Variable poolObject, Variable interceptCallback, FieldVariable messageProcessorListener) {
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
            Conditional ifPayloadIsNull = scope._if(resultPayload.eq(ExpressionFactory._null()));
            ifPayloadIsNull._then().assign(event, generateNullPayload(muleContext, event));
            generatePayloadOverwrite(ifPayloadIsNull._else(), event, resultPayload);
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