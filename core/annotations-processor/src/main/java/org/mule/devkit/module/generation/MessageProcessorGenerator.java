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
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.callback.HttpCallback;
import org.mule.api.annotations.callback.InterceptCallback;
import org.mule.api.annotations.callback.ProcessorCallback;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.InvocationHeaders;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.Cast;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
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

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class MessageProcessorGenerator extends AbstractMessageGenerator {
    public void generate(Element typeElement) throws GenerationException {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(typeElement.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Processor processor = executableElement.getAnnotation(Processor.class);

            if (processor == null)
                continue;

            generateMessageProcessor(typeElement, executableElement, processor.intercepting());
        }
    }

    private void generateMessageProcessor(Element typeElement, ExecutableElement executableElement, boolean intercepting) {
        // get class
        DefinedClass messageProcessorClass = null;

        if (intercepting) {
            messageProcessorClass = getInterceptingMessageProcessorClass(executableElement);
        } else {
            messageProcessorClass = getMessageProcessorClass(executableElement);
        }

        // add javadoc
        generateMessageProcessorClassDoc(executableElement, messageProcessorClass);

        // add a field for each argument of the method
        Map<String, AbstractMessageGenerator.FieldVariableElement> fields = generateFieldForEachParameter(messageProcessorClass, executableElement);

        // add standard fields
        FieldVariable object = generateFieldForModuleObject(messageProcessorClass, typeElement);
        FieldVariable muleContext = generateFieldForMuleContext(messageProcessorClass);
        FieldVariable expressionManager = generateFieldForExpressionManager(messageProcessorClass);
        FieldVariable patternInfo = generateFieldForPatternInfo(messageProcessorClass);

        FieldVariable messageProcessorListener = null;
        if (intercepting) {
            messageProcessorListener = generateFieldForMessageProcessorListener(messageProcessorClass);
        }

        // add initialise
        generateInitialiseMethod(messageProcessorClass, fields, typeElement, muleContext, expressionManager, patternInfo, object);

        // add start
        generateStartMethod(messageProcessorClass, fields, muleContext);

        // add stop
        generateStopMethod(messageProcessorClass, fields);

        // add dispose
        generateDiposeMethod(messageProcessorClass, fields);

        // add setmulecontext
        generateSetMuleContextMethod(messageProcessorClass, muleContext);

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
            DefinedClass poolObjectClass = context.getClassForRole(context.getNameUtils().generatePoolObjectRoleKey((TypeElement) typeElement));

            // add process method
            generateProcessMethod(executableElement, messageProcessorClass, fields, messageProcessorListener, muleContext, object, expressionManager, patternInfo, poolObjectClass);
        } else {
            // add process method
            generateProcessMethod(executableElement, messageProcessorClass, fields, messageProcessorListener, muleContext, object, expressionManager, patternInfo);
        }
    }

    private void generateStartMethod(DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, FieldVariable muleContext) {
        Method startMethod = messageProcessorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "start");
        startMethod._throws(ref(MuleException.class));

        for (String fieldName : fields.keySet()) {
            FieldVariableElement variableElement = fields.get(fieldName);

            if (variableElement.getVariableElement().asType().toString().contains(ProcessorCallback.class.getName())) {
                Conditional ifStartable = startMethod.body()._if(Op._instanceof(variableElement.getField(), ref(Startable.class)));
                ifStartable._then().add(
                        ExpressionFactory.cast(ref(Startable.class), variableElement.getField()).invoke("start")
                );
            } else if (variableElement.getVariableElement().asType().toString().contains(HttpCallback.class.getName())) {
                startMethod.body().assign(variableElement.getFieldType(), ExpressionFactory._new(context.getClassForRole(HttpCallbackGenerator.HTTP_CALLBACK_ROLE)).arg(fields.get(fieldName).getField()).arg(muleContext));
                startMethod.body().invoke(variableElement.getFieldType(), "start");
            }
        }
    }

    private void generateStopMethod(DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields) {
        Method stopMethod = messageProcessorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "stop");
        stopMethod._throws(ref(MuleException.class));

        for (String fieldName : fields.keySet()) {
            FieldVariableElement variableElement = fields.get(fieldName);

            if (variableElement.getVariableElement().asType().toString().contains(ProcessorCallback.class.getName())) {
                Conditional ifStoppable = stopMethod.body()._if(Op._instanceof(variableElement.getField(), ref(Stoppable.class)));
                ifStoppable._then().add(
                        ExpressionFactory.cast(ref(Stoppable.class), variableElement.getField()).invoke("stop")
                );
            } else if (variableElement.getVariableElement().asType().toString().contains(HttpCallback.class.getName())) {
                stopMethod.body().invoke(variableElement.getFieldType(), "stop");
            }
        }
    }

    private void generateDiposeMethod(DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields) {
        Method diposeMethod = messageProcessorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "dispose");

        for (String fieldName : fields.keySet()) {
            FieldVariableElement variableElement = fields.get(fieldName);

            if (variableElement.getVariableElement().asType().toString().contains(ProcessorCallback.class.getName())) {
                Conditional ifDisposable = diposeMethod.body()._if(Op._instanceof(variableElement.getField(), ref(Disposable.class)));
                ifDisposable._then().add(
                        ExpressionFactory.cast(ref(Disposable.class), variableElement.getField()).invoke("dispose")
                );
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

    private void generateProcessMethod(ExecutableElement executableElement, DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, FieldVariable messageProcessorListener, FieldVariable muleContext, FieldVariable object, FieldVariable expressionManager, FieldVariable patternInfo) {
        generateProcessMethod(executableElement, messageProcessorClass, fields, messageProcessorListener, muleContext, object, expressionManager, patternInfo, null);
    }

    private void generateProcessMethod(ExecutableElement executableElement, DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, FieldVariable messageProcessorListener, FieldVariable muleContext, FieldVariable object, FieldVariable expressionManager, FieldVariable patternInfo, DefinedClass poolObjectClass) {
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

        Variable poolObject = null;
        if (poolObjectClass != null) {
            poolObject = process.body().decl(poolObjectClass, "poolObject", ExpressionFactory._null());
        }

        TryStatement callProcessor = process.body()._try();

        List<Variable> parameters = new ArrayList<Variable>();
        Variable interceptCallback = null;
        for (VariableElement variable : executableElement.getParameters()) {
            String fieldName = variable.getSimpleName().toString();

            if (variable.asType().toString().contains(InterceptCallback.class.getName())) {

                DefinedClass callbackClass = context.getClassForRole(InterceptCallbackGenerator.ROLE);

                interceptCallback = callProcessor.body().decl(callbackClass, "transformed" + StringUtils.capitalize(fieldName),
                        ExpressionFactory._new(callbackClass));

                parameters.add(interceptCallback);

            } else if (variable.asType().toString().contains(HttpCallback.class.getName())) {
                parameters.add(fields.get(fieldName).getFieldType());

            } else if (variable.asType().toString().contains(ProcessorCallback.class.getName())) {

                DefinedClass callbackClass = context.getClassForRole(ProcessorCallbackGenerator.ROLE);

                Variable transformed = callProcessor.body().decl(callbackClass, "transformed" + StringUtils.capitalize(fieldName),
                        ExpressionFactory._null());

                callProcessor.body()._if(Op.ne(fields.get(fieldName).getField(), ExpressionFactory._null()))._then()
                        .assign(transformed,
                                ExpressionFactory._new(callbackClass).arg(event).arg(muleContext).arg(fields.get(fieldName).getField()));

                parameters.add(transformed);

            } else {
                InboundHeaders inboundHeaders = variable.getAnnotation(InboundHeaders.class);
                InvocationHeaders invocationHeaders = variable.getAnnotation(InvocationHeaders.class);

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

        generateThrow("failedToInvoke", MessagingException.class,
                callProcessor._catch((TypeReference) ref(Exception.class)), event, methodName);

        if (poolObjectClass != null) {
            Block fin = callProcessor._finally();
            Block poolObjectNotNull = fin._if(Op.ne(poolObject, ExpressionFactory._null()))._then();
            poolObjectNotNull.add(object.invoke("getLifecyleEnabledObjectPool").invoke("returnObject").arg(poolObject));
        }

    }

    private Variable generateMethodCall(Block body, FieldVariable object, String methodName, List<Variable> parameters, FieldVariable muleContext, Variable event, Type returnType, Variable poolObject, Variable interceptCallback, FieldVariable messageProcessorListener) {
        Variable resultPayload = null;
        if (returnType != context.getCodeModel().VOID) {
            resultPayload = body.decl(ref(Object.class), "resultPayload");
        }

        Invocation methodCall = null;
        if (poolObject != null) {
            body.assign(poolObject, ExpressionFactory.cast(poolObject.type(), object.invoke("getLifecyleEnabledObjectPool").invoke("borrowObject")));
            methodCall = poolObject.invoke(methodName);
        } else {
            methodCall = object.invoke(methodName);
        }

        for (int i = 0; i < parameters.size(); i++) {
            methodCall.arg(parameters.get(i));
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
