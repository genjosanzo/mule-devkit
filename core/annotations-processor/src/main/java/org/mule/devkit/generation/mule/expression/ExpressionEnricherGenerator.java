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

package org.mule.devkit.generation.mule.expression;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.annotations.ExpressionEnricher;
import org.mule.api.annotations.ExpressionLanguage;
import org.mule.api.annotations.param.CorrelationGroupSize;
import org.mule.api.annotations.param.CorrelationId;
import org.mule.api.annotations.param.CorrelationSequence;
import org.mule.api.annotations.param.ExceptionPayload;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.InvocationHeaders;
import org.mule.api.annotations.param.MessageRootId;
import org.mule.api.annotations.param.MessageUniqueId;
import org.mule.api.annotations.param.OutboundHeaders;
import org.mule.api.annotations.param.Payload;
import org.mule.api.annotations.param.SessionHeaders;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.NamingContants;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.ClassAlreadyExistsException;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.ForEach;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.TypeVariable;
import org.mule.devkit.model.code.Variable;
import org.mule.expression.ExpressionUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExpressionEnricherGenerator extends AbstractMessageGenerator {

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return typeElement.hasAnnotation(ExpressionLanguage.class) && typeElement.getMethodsAnnotatedWith(ExpressionEnricher.class).size() > 0;
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {
        String name = typeElement.getAnnotation(ExpressionLanguage.class).name();

        ExecutableElement executableElement = typeElement.getMethodsAnnotatedWith(ExpressionEnricher.class).get(0);
        TypeReference moduleObject = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));
        DefinedClass enricherClass = getEnricherClass(name, typeElement);

        // build trackmap
        DefinedClass trackMap = null;
        try {
            trackMap = enricherClass._class(Modifier.PRIVATE, "TrackMap");
        } catch (ClassAlreadyExistsException e) {
            trackMap = e.getExistingClass();
        }
        TypeVariable k = trackMap.generify("K");
        TypeVariable v = trackMap.generify("V");
        trackMap._implements(ref(Map.class).narrow(k).narrow(v));

        FieldVariable trackedMap = trackMap.field(Modifier.PRIVATE, ref(Map.class).narrow(k).narrow(v), "trackedMap");
        FieldVariable changedKeys = trackMap.field(Modifier.PRIVATE, ref(Set.class).narrow(k), "changedKeys");

        generateTrackMapConstructor(trackMap, k, v, trackedMap, changedKeys);
        generateTrackMapSize(trackMap, trackedMap);
        generateTrackMapIsEmpty(trackMap, trackedMap);
        generateTrackMapContainsKey(trackMap, trackedMap);
        generateTrackMapContainsValue(trackMap, trackedMap);
        generateTrackMapGet(trackMap, v, trackedMap);
        generateTrackMapPut(trackMap, k, v, trackedMap, changedKeys);
        generateTrackMapRemove(trackMap, v, trackedMap);
        generateTrackMapPutAll(trackMap, k, v, trackedMap);
        generateTrackMapClear(trackMap, trackedMap);
        generateTrackMapKeySet(trackMap, k, trackedMap);
        generateTrackMapChangedKeySet(trackMap, k, changedKeys);
        generateTrackMapValues(trackMap, v, trackedMap);
        generateTrackMapEntrySet(trackMap, k, v, trackedMap);

        context.note("Generating message enricher " + enricherClass.fullName() + " for language at class " + typeElement.getSimpleName().toString());

        FieldVariable module = generateModuleField(moduleObject, enricherClass);

        Method setMuleContext = enricherClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "setMuleContext");
        Variable muleContext = setMuleContext.param(ref(MuleContext.class), "muleContext");
        Conditional ifModuleIsContextAware = setMuleContext.body()._if(Op._instanceof(module, ref(MuleContextAware.class)));
        ifModuleIsContextAware._then().add(ExpressionFactory.cast(ref(MuleContextAware.class), module).invoke("setMuleContext").arg(muleContext));

        Method start = enricherClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "start");
        start._throws(ref(MuleException.class));
        Conditional ifModuleIsStartable = start.body()._if(Op._instanceof(module, ref(Startable.class)));
        ifModuleIsStartable._then().add(ExpressionFactory.cast(ref(Startable.class), module).invoke("start"));

        Method stop = enricherClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "stop");
        stop._throws(ref(MuleException.class));
        Conditional ifModuleIsStoppable = stop.body()._if(Op._instanceof(module, ref(Stoppable.class)));
        ifModuleIsStoppable._then().add(ExpressionFactory.cast(ref(Stoppable.class), module).invoke("stop"));

        Method init = enricherClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "initialise");
        init._throws(ref(InitialisationException.class));
        Conditional ifModuleIsInitialisable = init.body()._if(Op._instanceof(module, ref(Initialisable.class)));
        ifModuleIsInitialisable._then().add(ExpressionFactory.cast(ref(Initialisable.class), module).invoke("initialise"));

        Method dispose = enricherClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "dispose");
        Conditional ifModuleIsDisposable = dispose.body()._if(Op._instanceof(module, ref(Disposable.class)));
        ifModuleIsDisposable._then().add(ExpressionFactory.cast(ref(Disposable.class), module).invoke("dispose"));

        generateConstructor(moduleObject, enricherClass, module);

        generateGetName(name, enricherClass);

        generateSetName(enricherClass);

        generateComputeClassHierarchyMethod(enricherClass);
        generateIsListClassMethod(enricherClass);
        generateIsMapClassMethod(enricherClass);
        generateIsListMethod(enricherClass);
        generateIsMapMethod(enricherClass);
        generateIsAssignableFrom(enricherClass);
        generateTransformMethod(enricherClass);

        Method enrich = enricherClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "enrich");
        Variable expression = enrich.param(ref(String.class), "expression");
        Variable message = enrich.param(ref(MuleMessage.class), "message");
        Variable object = enrich.param(ref(Object.class), "object");

        TryStatement tryStatement = enrich.body()._try();

        Invocation newArray = ExpressionFactory._new(ref(Class.class).array());
        for (VariableElement parameter : executableElement.getParameters()) {
            if( parameter.asType().getKind() == TypeKind.BOOLEAN ||
                    parameter.asType().getKind() == TypeKind.BYTE ||
                    parameter.asType().getKind() == TypeKind.SHORT ||
                    parameter.asType().getKind() == TypeKind.CHAR ||
                    parameter.asType().getKind() == TypeKind.INT ||
                    parameter.asType().getKind() == TypeKind.FLOAT ||
                    parameter.asType().getKind() == TypeKind.LONG ||
                    parameter.asType().getKind() == TypeKind.DOUBLE) {
                newArray.arg(ref(parameter.asType()).boxify().staticRef("TYPE"));
            } else {
                newArray.arg(ref(parameter.asType()).boxify().dotclass());
            }
        }
        Variable parameterClasses = tryStatement.body().decl(ref(Class.class).array(), "parameterClasses", newArray);
        int argCount = 0;
        Invocation getMethod = module.invoke("getClass").invoke("getMethod").arg(executableElement.getSimpleName().toString()).arg(parameterClasses);
        Variable moduleEvaluate = tryStatement.body().decl(ref(java.lang.reflect.Method.class), "evaluateMethod", getMethod);
        List<Variable> types = new ArrayList<Variable>();
        for (VariableElement parameter : executableElement.getParameters()) {
            Variable var = tryStatement.body().decl(ref(Type.class), parameter.getSimpleName().toString() + "Type", moduleEvaluate.invoke("getGenericParameterTypes").component(ExpressionFactory.lit(types.size())));
            types.add(var);
        }

        argCount = 0;
        Variable invocationHeadersVar = null;
        Variable inboundHeadersVar = null;
        Variable outboundHeadersVar = null;
        Variable sessionHeadersVar = null;
        Invocation evaluateInvoke = module.invoke(executableElement.getSimpleName().toString());
        for (VariableElement parameter : executableElement.getParameters()) {
            if (parameter.getAnnotation(Payload.class) != null) {
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(), ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(message.invoke("getPayload"))));
            } else if (parameter.getAnnotation(ExceptionPayload.class) != null) {
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(), ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(message.invoke("getExceptionPayload"))));
            } else if (parameter.getAnnotation(CorrelationId.class) != null) {
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(), ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(message.invoke("getCorrelationId"))));
            } else if (parameter.getAnnotation(CorrelationSequence.class) != null) {
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(), ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(message.invoke("getCorrelationSequence"))));
            } else if (parameter.getAnnotation(CorrelationGroupSize.class) != null) {
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(), ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(message.invoke("getCorrelationGroupSize"))));
            } else if (parameter.getAnnotation(MessageUniqueId.class) != null) {
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(), ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(message.invoke("getUniqueId"))));
            } else if (parameter.getAnnotation(MessageRootId.class) != null) {
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()).boxify(), ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(message.invoke("getMessageRootId"))));
            } else if (parameter.asType().toString().startsWith(MuleMessage.class.getName())) {
                evaluateInvoke.arg(message);
            } else if (parameter.getAnnotation(InboundHeaders.class) != null) {
                InboundHeaders inboundHeaders = parameter.getAnnotation(InboundHeaders.class);
                inboundHeadersVar = tryStatement.body().decl(trackMap.narrow(ref(String.class)).narrow(Object.class), "inboundHeaders", ExpressionFactory._null());
                if (context.getTypeMirrorUtils().isArrayOrList(parameter.asType())) {
                    tryStatement.body().assign(inboundHeadersVar, ExpressionFactory._new(trackMap.narrow(ref(String.class)).narrow(Object.class)).arg(ExpressionFactory.cast(ref(parameter.asType()),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INBOUND:" + inboundHeaders.value()).arg(message).arg(ref(List.class).dotclass())
                            ))));
                } else if (context.getTypeMirrorUtils().isMap(parameter.asType())) {
                    tryStatement.body().assign(inboundHeadersVar, ExpressionFactory._new(trackMap.narrow(ref(String.class)).narrow(Object.class)).arg(ExpressionFactory.cast(ref(parameter.asType()),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INBOUND:" + inboundHeaders.value()).arg(message).arg(ref(Map.class).dotclass())
                            ))));
                } else {
                    tryStatement.body().assign(inboundHeadersVar, ExpressionFactory._new(trackMap.narrow(ref(String.class)).narrow(Object.class)).arg(ExpressionFactory.cast(ref(parameter.asType()),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INBOUND:" + inboundHeaders.value()).arg(message)
                            ))));
                }
                evaluateInvoke.arg(inboundHeadersVar);
            } else if (parameter.getAnnotation(SessionHeaders.class) != null) {
                SessionHeaders sessionHeaders = parameter.getAnnotation(SessionHeaders.class);
                sessionHeadersVar = tryStatement.body().decl(trackMap.narrow(ref(String.class)).narrow(Object.class), "sessionHeaders", ExpressionFactory._null());
                if (context.getTypeMirrorUtils().isArrayOrList(parameter.asType())) {
                    tryStatement.body().assign(sessionHeadersVar, ExpressionFactory._new(trackMap.narrow(ref(String.class)).narrow(Object.class)).arg(ExpressionFactory.cast(ref(parameter.asType()),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("SESSION:" + sessionHeaders.value()).arg(message).arg(ref(List.class).dotclass())
                            ))));
                } else if (context.getTypeMirrorUtils().isMap(parameter.asType())) {
                    tryStatement.body().assign(sessionHeadersVar, ExpressionFactory._new(trackMap.narrow(ref(String.class)).narrow(Object.class)).arg(ExpressionFactory.cast(ref(parameter.asType()),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("SESSION:" + sessionHeaders.value()).arg(message).arg(ref(Map.class).dotclass())
                            ))));
                } else {
                    tryStatement.body().assign(sessionHeadersVar, ExpressionFactory._new(trackMap.narrow(ref(String.class)).narrow(Object.class)).arg(ExpressionFactory.cast(ref(parameter.asType()),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("SESSION:" + sessionHeaders.value()).arg(message)
                            ))));
                }
                evaluateInvoke.arg(sessionHeadersVar);
            } else if (parameter.getAnnotation(OutboundHeaders.class) != null) {
                outboundHeadersVar = tryStatement.body().decl(trackMap.narrow(ref(String.class)).narrow(Object.class), "outboundHeaders", ExpressionFactory._null());
                tryStatement.body().assign(outboundHeadersVar, ExpressionFactory._new(trackMap.narrow(ref(String.class)).narrow(Object.class)).arg(ExpressionFactory.cast(ref(parameter.asType()),
                        ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("OUTBOUND:*").arg(message).arg(ref(Map.class).dotclass())
                        ))));
                evaluateInvoke.arg(outboundHeadersVar);
            } else if (parameter.getAnnotation(InvocationHeaders.class) != null) {
                InvocationHeaders invocationHeaders = parameter.getAnnotation(InvocationHeaders.class);
                invocationHeadersVar = tryStatement.body().decl(trackMap.narrow(ref(String.class)).narrow(Object.class), "invocationHeaders", ExpressionFactory._null());
                if (context.getTypeMirrorUtils().isArrayOrList(parameter.asType())) {
                    tryStatement.body().assign(invocationHeadersVar, ExpressionFactory._new(trackMap.narrow(ref(String.class)).narrow(Object.class)).arg(ExpressionFactory.cast(ref(parameter.asType()),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INVOCATION:" + invocationHeaders.value()).arg(message).arg(ref(List.class).dotclass())
                            ))));
                } else if (context.getTypeMirrorUtils().isMap(parameter.asType())) {
                    tryStatement.body().assign(invocationHeadersVar, ExpressionFactory._new(trackMap.narrow(ref(String.class)).narrow(Object.class)).arg(ExpressionFactory.cast(ref(parameter.asType()),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INVOCATION:" + invocationHeaders.value()).arg(message).arg(ref(Map.class).dotclass())
                            ))));
                } else {
                    tryStatement.body().assign(invocationHeadersVar, ExpressionFactory._new(trackMap.narrow(ref(String.class)).narrow(Object.class)).arg(ExpressionFactory.cast(ref(parameter.asType()),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INVOCATION:" + invocationHeaders.value()).arg(message)
                            ))));
                }
                evaluateInvoke.arg(invocationHeadersVar);
            } else {
                if (parameter.asType().toString().contains("String")) {
                    evaluateInvoke.arg(expression);
                } else if (parameter.asType().toString().contains("Object")) {
                    evaluateInvoke.arg(object);
                }
            }
            argCount++;
        }

        if (ref(executableElement.getReturnType()) != context.getCodeModel().VOID) {
            Variable newPayload = tryStatement.body().decl(ref(Object.class), "newPayload", evaluateInvoke);
            tryStatement.body().add(message.invoke("setPayload").arg(newPayload));
        } else {
            tryStatement.body().add(evaluateInvoke);
        }

        if (inboundHeadersVar != null) {
            ForEach forEach = tryStatement.body().forEach(ref(String.class), "key", inboundHeadersVar.invoke("changedKeySet"));
            forEach.body().add(message.invoke("setProperty").arg(forEach.var()).arg(inboundHeadersVar.invoke("get").arg(forEach.var())).arg(ref(PropertyScope.class).staticRef("INBOUND")));
        }

        if (outboundHeadersVar != null) {
            ForEach forEach = tryStatement.body().forEach(ref(String.class), "key", outboundHeadersVar.invoke("changedKeySet"));
            forEach.body().add(message.invoke("setProperty").arg(forEach.var()).arg(outboundHeadersVar.invoke("get").arg(forEach.var())).arg(ref(PropertyScope.class).staticRef("OUTBOUND")));
        }

        if (sessionHeadersVar != null) {
            ForEach forEach = tryStatement.body().forEach(ref(String.class), "key", sessionHeadersVar.invoke("changedKeySet"));
            forEach.body().add(message.invoke("setProperty").arg(forEach.var()).arg(sessionHeadersVar.invoke("get").arg(forEach.var())).arg(ref(PropertyScope.class).staticRef("SESSION")));
        }

        if (invocationHeadersVar != null) {
            ForEach forEach = tryStatement.body().forEach(ref(String.class), "key", invocationHeadersVar.invoke("changedKeySet"));
            forEach.body().add(message.invoke("setProperty").arg(forEach.var()).arg(invocationHeadersVar.invoke("get").arg(forEach.var())).arg(ref(PropertyScope.class).staticRef("INVOCATION")));
        }

        catchAndRethrowAsRuntimeException(tryStatement, NoSuchMethodException.class);
        catchAndRethrowAsRuntimeException(tryStatement, TransformerException.class);

        context.registerAtBoot(enricherClass);
    }

    private void generateTrackMapEntrySet(DefinedClass trackMap, TypeVariable k, TypeVariable v, FieldVariable trackedMap) {
        Method entrySet = trackMap.method(Modifier.PUBLIC, ref(Set.class).narrow(ref(Map.Entry.class).narrow(k).narrow(v)), "entrySet");
        entrySet.annotate(ref(Override.class));
        entrySet.body()._return(trackedMap.invoke("entrySet"));
    }

    private void generateTrackMapValues(DefinedClass trackMap, TypeVariable v, FieldVariable trackedMap) {
        Method values = trackMap.method(Modifier.PUBLIC, ref(Collection.class).narrow(v), "values");
        values.annotate(ref(Override.class));
        values.body()._return(trackedMap.invoke("values"));
    }

    private void generateTrackMapChangedKeySet(DefinedClass trackMap, TypeVariable k, FieldVariable changedKeys) {
        Method changedkeySet = trackMap.method(Modifier.PUBLIC, ref(Set.class).narrow(k), "changedKeySet");
        changedkeySet.body()._return(ExpressionFactory._this().ref(changedKeys));
    }

    private void generateTrackMapKeySet(DefinedClass trackMap, TypeVariable k, FieldVariable trackedMap) {
        Method keySet = trackMap.method(Modifier.PUBLIC, ref(Set.class).narrow(k), "keySet");
        keySet.annotate(ref(Override.class));
        keySet.body()._return(trackedMap.invoke("keySet"));
    }

    private void generateTrackMapClear(DefinedClass trackMap, FieldVariable trackedMap) {
        Method clear = trackMap.method(Modifier.PUBLIC, context.getCodeModel().VOID, "clear");
        clear.annotate(ref(Override.class));
        clear.body().add(trackedMap.invoke("clear"));
    }

    private void generateTrackMapPutAll(DefinedClass trackMap, TypeVariable k, TypeVariable v, FieldVariable trackedMap) {
        Method putAll = trackMap.method(Modifier.PUBLIC, context.getCodeModel().VOID, "putAll");
        putAll.annotate(ref(Override.class));
        Variable map = putAll.param(ref(Map.class).narrow(k.wildcard()).narrow(v.wildcard()), "map");
        putAll.body().add(trackedMap.invoke("putAll").arg(map));
    }

    private void generateTrackMapRemove(DefinedClass trackMap, TypeVariable v, FieldVariable trackedMap) {
        Method remove = trackMap.method(Modifier.PUBLIC, v, "remove");
        remove.annotate(ref(Override.class));
        Variable o4 = remove.param(ref(Object.class), "o");
        remove.body()._return(trackedMap.invoke("remove").arg(o4));
    }

    private void generateTrackMapPut(DefinedClass trackMap, TypeVariable k, TypeVariable v, FieldVariable trackedMap, FieldVariable changedKeys) {
        Method put = trackMap.method(Modifier.PUBLIC, v, "put");
        put.annotate(ref(Override.class));
        Variable k2 = put.param(k, "k");
        Variable v2 = put.param(v, "v");
        put.body().add(changedKeys.invoke("add").arg(k2));
        put.body()._return(trackedMap.invoke("put").arg(k2).arg(v2));
    }

    private void generateTrackMapGet(DefinedClass trackMap, TypeVariable v, FieldVariable trackedMap) {
        Method get = trackMap.method(Modifier.PUBLIC, v, "get");
        get.annotate(ref(Override.class));
        Variable o3 = get.param(ref(Object.class), "o");
        get.body()._return(trackedMap.invoke("get").arg(o3));
    }

    private void generateTrackMapConstructor(DefinedClass trackMap, TypeVariable k, TypeVariable v, FieldVariable trackedMap, FieldVariable changedKeys) {
        Method constructor = trackMap.constructor(Modifier.PUBLIC);
        Variable innerTrackedMap = constructor.param(ref(Map.class).narrow(k).narrow(v), "trackedMap");
        constructor.body().assign(ExpressionFactory._this().ref(trackedMap), innerTrackedMap);
        constructor.body().assign(ExpressionFactory._this().ref(changedKeys), ExpressionFactory._new(ref(HashSet.class).narrow(k)));
    }

    private void generateTrackMapContainsValue(DefinedClass trackMap, FieldVariable trackedMap) {
        Method containsValue = trackMap.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, "containsValue");
        containsValue.annotate(ref(Override.class));
        Variable o2 = containsValue.param(ref(Object.class), "o");
        containsValue.body()._return(trackedMap.invoke("containsValue").arg(o2));
    }

    private void generateTrackMapContainsKey(DefinedClass trackMap, FieldVariable trackedMap) {
        Method containsKey = trackMap.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, "containsKey");
        containsKey.annotate(ref(Override.class));
        Variable o = containsKey.param(ref(Object.class), "o");
        containsKey.body()._return(trackedMap.invoke("containsKey").arg(o));
    }

    private void generateTrackMapIsEmpty(DefinedClass trackMap, FieldVariable trackedMap) {
        Method isEmpty = trackMap.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, "isEmpty");
        isEmpty.annotate(ref(Override.class));
        isEmpty.body()._return(trackedMap.invoke("isEmpty"));
    }

    private void generateTrackMapSize(DefinedClass trackMap, FieldVariable trackedMap) {
        Method size = trackMap.method(Modifier.PUBLIC, context.getCodeModel().INT, "size");
        size.annotate(ref(Override.class));
        size.body()._return(trackedMap.invoke("size"));
    }

    private void catchAndRethrowAsRuntimeException(TryStatement tryStatement, Class clazz) {
        CatchBlock catchBlock = tryStatement._catch(ref(clazz));
        Variable e = catchBlock.param("e");
        catchBlock.body()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg(e));
    }

    private FieldVariable generateModuleField(TypeReference typeElement, DefinedClass evaluatorClass) {
        return evaluatorClass.field(Modifier.PRIVATE, typeElement, "module", ExpressionFactory._null());
    }

    private void generateConstructor(TypeReference typeRef, DefinedClass evaluatorClass, FieldVariable module) {
        Method constructor = evaluatorClass.constructor(Modifier.PUBLIC);
        constructor.body().assign(module, ExpressionFactory._new(typeRef));
    }

    private void generateGetName(String name, DefinedClass evaluatorClass) {
        Method getName = evaluatorClass.method(Modifier.PUBLIC, ref(String.class), "getName");
        getName.body()._return(ExpressionFactory.lit(name));
    }

    private void generateSetName(DefinedClass evaluatorClass) {
        Method setName = evaluatorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "setName");
        setName.param(ref(String.class), "name");
        setName.body()._throw(ExpressionFactory._new(ref(UnsupportedOperationException.class)));
    }

    private DefinedClass getEnricherClass(String name, Element variableElement) {
        String evaluatorClassName = context.getNameUtils().generateClassNameInPackage(variableElement, context.getNameUtils().camel(name) + NamingContants.EXPRESSION_ENRICHER_CLASS_NAME_SUFFIX);
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(evaluatorClassName) + NamingContants.EXPRESSIONS_NAMESPACE);
        DefinedClass enricherClass = pkg._class(context.getNameUtils().getClassName(evaluatorClassName), new Class<?>[]{org.mule.api.expression.ExpressionEnricher.class});
        enricherClass._implements(ref(MuleContextAware.class));
        enricherClass._implements(ref(Startable.class));
        enricherClass._implements(ref(Stoppable.class));
        enricherClass._implements(ref(Initialisable.class));
        enricherClass._implements(ref(Disposable.class));
        //enricherClass._implements(ref(FlowConstructAware.class));

        return enricherClass;
    }

}