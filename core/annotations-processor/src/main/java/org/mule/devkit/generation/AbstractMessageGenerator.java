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

package org.mule.devkit.generation;

import org.apache.commons.lang.StringUtils;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.callback.HttpCallback;
import org.mule.api.callback.SourceCallback;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.source.MessageSource;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.construct.Flow;
import org.mule.devkit.generation.callback.DefaultHttpCallbackGenerator;
import org.mule.devkit.model.code.Block;
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
import org.mule.devkit.model.code.Package;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.mule.devkit.model.code.builders.FieldBuilder;
import org.mule.session.DefaultMuleSession;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.TemplateParser;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.BeanDefinitionParser;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractMessageGenerator extends AbstractModuleGenerator {

    protected void generateIsListMethod(DefinedClass messageProcessorClass) {
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

    protected void generateIsMapMethod(DefinedClass messageProcessorClass) {
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

    protected void generateIsListClassMethod(DefinedClass messageProcessorClass) {
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

    protected void generateIsAssignableFrom(DefinedClass messageProcessorClass) {
        Method isAssignableFrom = messageProcessorClass.method(Modifier.PRIVATE, context.getCodeModel().BOOLEAN, "isAssignableFrom");
        Variable expectedType = isAssignableFrom.param(ref(java.lang.reflect.Type.class), "expectedType");
        Variable clazz = isAssignableFrom.param(ref(Class.class), "clazz");

        Block isClass = isAssignableFrom.body()._if(Op._instanceof(expectedType, ref(Class.class)))._then();
        Conditional isPrimitive = isClass._if(ExpressionFactory.cast(ref(Class.class), expectedType).invoke("isPrimitive"));
        isPrimitive._then()._if(Op.cand(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("getName").invoke("equals").arg(ExpressionFactory.lit("boolean")),
                Op.eq(clazz, ref(Boolean.class).dotclass())
        ))._then()._return(ExpressionFactory.TRUE);

        isPrimitive._then()._if(Op.cand(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("getName").invoke("equals").arg(ExpressionFactory.lit("byte")),
                Op.eq(clazz, ref(Byte.class).dotclass())
        ))._then()._return(ExpressionFactory.TRUE);

        isPrimitive._then()._if(Op.cand(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("getName").invoke("equals").arg(ExpressionFactory.lit("short")),
                Op.eq(clazz, ref(Short.class).dotclass())
        ))._then()._return(ExpressionFactory.TRUE);

        isPrimitive._then()._if(Op.cand(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("getName").invoke("equals").arg(ExpressionFactory.lit("char")),
                Op.eq(clazz, ref(Character.class).dotclass())
        ))._then()._return(ExpressionFactory.TRUE);

        isPrimitive._then()._if(Op.cand(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("getName").invoke("equals").arg(ExpressionFactory.lit("int")),
                Op.eq(clazz, ref(Integer.class).dotclass())
        ))._then()._return(ExpressionFactory.TRUE);

        isPrimitive._then()._if(Op.cand(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("getName").invoke("equals").arg(ExpressionFactory.lit("float")),
                Op.eq(clazz, ref(Float.class).dotclass())
        ))._then()._return(ExpressionFactory.TRUE);

        isPrimitive._then()._if(Op.cand(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("getName").invoke("equals").arg(ExpressionFactory.lit("long")),
                Op.eq(clazz, ref(Long.class).dotclass())
        ))._then()._return(ExpressionFactory.TRUE);

        isPrimitive._then()._if(Op.cand(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("getName").invoke("equals").arg(ExpressionFactory.lit("double")),
                Op.eq(clazz, ref(Double.class).dotclass())
        ))._then()._return(ExpressionFactory.TRUE);

        isPrimitive._then()._return(ExpressionFactory.FALSE);

        isPrimitive._else()._return(
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

    protected void generateTransformMethod(DefinedClass messageProcessorClass) {
        Method transform = messageProcessorClass.method(Modifier.PRIVATE, ref(Object.class), "transform");
        transform._throws(ref(TransformerException.class));
        Variable muleMessage = transform.param(ref(MuleMessage.class), "muleMessage");
        Variable expectedType = transform.param(ref(java.lang.reflect.Type.class), "expectedType");
        Variable source = transform.param(ref(Object.class), "source");

        transform.body()._if(Op.eq(source, ExpressionFactory._null()))._then()._return(source);

        Variable target = transform.body().decl(ref(Object.class), "target", ExpressionFactory._null());
        Conditional isList = transform.body()._if(
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
                ExpressionFactory.invoke("transform").arg(muleMessage).arg(listParameterizedType).
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

        //Conditional ifKeysNotOfSameType = isExpectedMapBlock._if(Op.cand(
        //        Op.not(map.invoke("isEmpty")),
        //        Op.not(ExpressionFactory.invoke(keyType.invoke("toString"), "equals").arg(ExpressionFactory.invoke(ExpressionFactory.invoke((ExpressionFactory.invoke(ExpressionFactory.invoke(map.invoke("keySet"), "iterator"), "next"), "getClass", "toString"))))));
        //Block ifKeysNotOfSameTypeThen = ifKeysNotOfSameType._then().block();

        Variable newMap = isExpectedMapBlock.decl(ref(Map.class), "newMap", ExpressionFactory._new(ref(HashMap.class)));
        ForEach forEach = isExpectedMapBlock.forEach(ref(Object.class), "entryObj", map.invoke("entrySet"));
        Block forEachBlock = forEach.body().block();
        Variable entry = forEachBlock.decl(ref(Map.Entry.class), "entry", ExpressionFactory.cast(ref(Map.Entry.class), forEach.var()));
        Variable newKey = forEachBlock.decl(ref(Object.class), "newKey", ExpressionFactory.invoke("transform").arg(muleMessage).arg(keyType).arg(entry.invoke("getKey")));
        Variable newValue = forEachBlock.decl(ref(Object.class), "newValue", ExpressionFactory.invoke("transform").arg(muleMessage).arg(valueType).arg(entry.invoke("getValue")));
        forEachBlock.invoke(newMap, "put").arg(newKey).arg(newValue);

        isExpectedMapBlock.assign(target, newMap);

        //Cast mapCast = ExpressionFactory.cast(ref(Map.class), source);

        //ForEach keyLoop = ifKeysNotOfSameType._else().forEach(ref(Object.class), "key", mapCast.invoke("entrySet"));

        //Cast entryCast = ExpressionFactory.cast(ref(Map.Entry.class), keyLoop.var());

        //Variable value = keyLoop.body().decl(ref(Object.class), "value", mapCast.invoke("get").arg(entryCast.invoke("getKey")));
        //keyLoop.body().add(entryCast.invoke("setValue").arg(
        //        ExpressionFactory.invoke("transform").arg(muleMessage).arg(valueType).arg(value)
        //));

        isExpectedMap._else().assign(target, source);

        isMap._else().assign(target, source);

        Conditional shouldTransform = transform.body()._if(Op.cand(
                Op.ne(target, ExpressionFactory._null()),
                Op.not(ExpressionFactory.invoke("isAssignableFrom").arg(expectedType).arg(target.invoke("getClass")))
        ));

        Variable sourceDataType = shouldTransform._then().decl(ref(DataType.class), "sourceDataType",
                ref(DataTypeFactory.class).staticInvoke("create").arg(target.invoke("getClass")));

        Conditional ifParameterizedType = shouldTransform._then()._if(Op._instanceof(expectedType, ref(ParameterizedType.class)));
        ifParameterizedType._then().assign(expectedType, ExpressionFactory.cast(ref(ParameterizedType.class), expectedType).invoke("getRawType"));

        Variable targetDataType = shouldTransform._then().decl(ref(DataType.class), "targetDataType",
                ref(DataTypeFactory.class).staticInvoke("create").arg(
                        ExpressionFactory.cast(ref(Class.class), expectedType)));

        Variable transformer = shouldTransform._then().decl(ref(Transformer.class), "t",
                muleMessage.invoke("getMuleContext").invoke("getRegistry").invoke("lookupTransformer").arg(sourceDataType).arg(targetDataType));

        shouldTransform._then()._return(transformer.invoke("transform").arg(target));

        shouldTransform._else()._return(target);
    }


    protected void generateIsMapClassMethod(DefinedClass messageProcessorClass) {
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

    protected void generateComputeClassHierarchyMethod(DefinedClass messageProcessorClass) {
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

    protected FieldVariable generateFieldForPatternInfo(DefinedClass messageProcessorClass) {
        FieldVariable patternInfo = messageProcessorClass.field(Modifier.PRIVATE, ref(TemplateParser.PatternInfo.class), "patternInfo");
        patternInfo.javadoc().add("Mule Pattern Info");
        return patternInfo;
    }

    protected FieldVariable generateFieldForExpressionManager(DefinedClass messageProcessorClass) {
        FieldVariable expressionManager = messageProcessorClass.field(Modifier.PRIVATE, ref(ExpressionManager.class), "expressionManager");
        expressionManager.javadoc().add("Mule Expression Manager");
        return expressionManager;
    }

    protected FieldVariable generateFieldForMessageProcessor(DefinedClass messageProcessorClass, String name) {
        FieldVariable expressionManager = messageProcessorClass.field(Modifier.PRIVATE, ref(MessageProcessor.class), name);
        expressionManager.javadoc().add("Message Processor");
        return expressionManager;
    }

    protected FieldVariable generateFieldForBoolean(DefinedClass messageProcessorClass, String name) {
        FieldVariable expressionManager = messageProcessorClass.field(Modifier.PRIVATE, context.getCodeModel().BOOLEAN, name);
        return expressionManager;
    }

    protected FieldVariable generateFieldForString(DefinedClass messageProcessorClass, String name) {
        FieldVariable expressionManager = messageProcessorClass.field(Modifier.PRIVATE, ref(String.class), name);
        return expressionManager;
    }

    protected FieldVariable generateFieldForModuleObject(DefinedClass messageProcessorClass, TypeElement typeElement) {
        FieldVariable field = messageProcessorClass.field(Modifier.PRIVATE, ref(Object.class), "moduleObject");
        field.javadoc().add("Module object");

        return field;
    }

    protected FieldVariable generateFieldForMessageProcessorListener(DefinedClass messageSourceClass) {
        FieldVariable messageProcessor = messageSourceClass.field(Modifier.PRIVATE, ref(MessageProcessor.class), "messageProcessor");
        messageProcessor.javadoc().add("Message processor that will get called for processing incoming events");
        return messageProcessor;
    }

    protected DefinedClass getBeanDefinitionParserClass(ExecutableElement executableElement) {
        String beanDefinitionParserName = context.getNameUtils().generateClassName(executableElement, NamingContants.DEFINITION_PARSER_CLASS_NAME_SUFFIX);
        Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(beanDefinitionParserName) + NamingContants.CONFIG_NAMESPACE);
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(beanDefinitionParserName), new Class[]{BeanDefinitionParser.class});

        return clazz;
    }

    protected DefinedClass getConfigBeanDefinitionParserClass(TypeElement typeElement) {
        String poolAdapterName = context.getNameUtils().generateClassName(typeElement, NamingContants.CONFIG_NAMESPACE, NamingContants.CONFIG_DEFINITION_PARSER_CLASS_NAME_SUFFIX);
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(poolAdapterName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(poolAdapterName), new Class[]{BeanDefinitionParser.class});

        context.setClassRole(context.getNameUtils().generateConfigDefParserRoleKey(typeElement), clazz);

        return clazz;
    }

    protected DefinedClass getMessageProcessorClass(ExecutableElement executableElement) {
        String className = context.getNameUtils().generateClassName(executableElement, NamingContants.MESSAGE_PROCESSOR_CLASS_NAME_SUFFIX);
        return getMessageProcessorClass(className,
                context.getNameUtils().getPackageName(className) + NamingContants.MESSAGE_PROCESSOR_NAMESPACE);
    }

    protected DefinedClass getMessageProcessorClass(String className, String packageName) {
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(packageName);
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(className), new Class[]{
                Initialisable.class,
                Startable.class,
                Disposable.class,
                Stoppable.class,
                MessageProcessor.class,
                MuleContextAware.class,
                FlowConstructAware.class});

        return clazz;
    }

    protected DefinedClass getInterceptingMessageProcessorClass(ExecutableElement executableElement) {
        String beanDefinitionParserName = context.getNameUtils().generateClassName(executableElement, NamingContants.MESSAGE_PROCESSOR_CLASS_NAME_SUFFIX);
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(beanDefinitionParserName) + NamingContants.MESSAGE_PROCESSOR_NAMESPACE);
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(beanDefinitionParserName), new Class[]{
                Initialisable.class,
                Startable.class,
                Disposable.class,
                Stoppable.class,
                InterceptingMessageProcessor.class,
                MuleContextAware.class,
                FlowConstructAware.class,
                SourceCallback.class});

        return clazz;
    }


    protected DefinedClass getMessageSourceClass(ExecutableElement executableElement) {
        String beanDefinitionParserName = context.getNameUtils().generateClassName(executableElement, NamingContants.MESSAGE_SOURCE_CLASS_NAME_SUFFIX);
        Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(beanDefinitionParserName) + NamingContants.MESSAGE_SOURCE_NAMESPACE);
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(beanDefinitionParserName), new Class[]{
                MuleContextAware.class,
                Startable.class,
                Stoppable.class,
                Runnable.class,
                Initialisable.class,
                MessageSource.class,
                SourceCallback.class,
                FlowConstructAware.class});

        return clazz;
    }

    protected Map<String, FieldVariableElement> generateProcessorFieldForEachParameter(DefinedClass messageProcessorClass, ExecutableElement processorMethod) {
        return generateProcessorFieldForEachParameter(messageProcessorClass, processorMethod, null);
    }

    protected Map<String, FieldVariableElement> generateProcessorFieldForEachParameter(DefinedClass messageProcessorClass, ExecutableElement processorMethod, Class annotatedWith) {
        Map<String, AbstractMessageGenerator.FieldVariableElement> fields = new HashMap<String, FieldVariableElement>();
        for (VariableElement variable : processorMethod.getParameters()) {
            if (variable.asType().toString().startsWith(SourceCallback.class.getName())) {
                continue;
            }

            if (annotatedWith != null && variable.getAnnotation(annotatedWith) == null) {
                continue;
            }

            String fieldName = variable.getSimpleName().toString();

            FieldVariable field;
            FieldVariable fieldType;
            if (context.getTypeMirrorUtils().isNestedProcessor(variable.asType())) {
                field = new FieldBuilder(messageProcessorClass).
                        privateVisibility().
                        type(Object.class).
                        name(fieldName).
                        build();
                fieldType = new FieldBuilder(messageProcessorClass).
                        privateVisibility().
                        type(ref(variable.asType())).
                        name("_" + fieldName + "Type").
                        build();
            } else if (variable.asType().toString().startsWith(HttpCallback.class.getName())) {
                // for each parameter of type HttpCallback we need two fields: one that will hold a reference to the flow
                // that is going to be executed upon the callback and the other one to hold the HttpCallback object itself
                field = new FieldBuilder(messageProcessorClass).
                        type(Flow.class).
                        name(fieldName + "CallbackFlow").
                        javadoc("The flow to be invoked when the http callback is received").build();
                fieldType = new FieldBuilder(messageProcessorClass).
                        type(HttpCallback.class).
                        name(fieldName).
                        javadoc("An HttpCallback instance responsible for linking the APIs http callback with the flow {@link " + messageProcessorClass.fullName() + "#" + fieldName + "CallbackFlow").build();
            } else {
                field = new FieldBuilder(messageProcessorClass).
                        privateVisibility().
                        type(Object.class).
                        name(fieldName).
                        build();
                fieldType = new FieldBuilder(messageProcessorClass).
                        privateVisibility().
                        type(ref(variable.asType())).
                        name("_" + fieldName + "Type").
                        build();
            }
            fields.put(variable.getSimpleName().toString(), new AbstractMessageGenerator.FieldVariableElement(field, fieldType, variable));
        }
        return fields;
    }

    protected Map<String, FieldVariableElement> generateStandardFieldForEachParameter(DefinedClass messageProcessorClass, ExecutableElement processorMethod) {
        return generateStandardFieldForEachParameter(messageProcessorClass, processorMethod, null);
    }

    protected Map<String, FieldVariableElement> generateStandardFieldForEachParameter(DefinedClass messageProcessorClass, ExecutableElement processorMethod, Class annotatedWith) {
        Map<String, AbstractMessageGenerator.FieldVariableElement> fields = new HashMap<String, FieldVariableElement>();
        for (VariableElement variable : processorMethod.getParameters()) {
            if (annotatedWith != null && variable.getAnnotation(annotatedWith) == null) {
                continue;
            }

            String fieldName = variable.getSimpleName().toString();

            FieldVariable field = null;
            FieldVariable fieldType = null;
            field = new FieldBuilder(messageProcessorClass).
                    privateVisibility().
                    type(ref(variable.asType())).
                    name(fieldName).
                    build();
            field.javadoc().add(context.getJavaDocUtils().getParameterSummary(variable.getSimpleName().toString(), variable));
            fields.put(variable.getSimpleName().toString(), new AbstractMessageGenerator.FieldVariableElement(field, fieldType, variable));
        }
        return fields;
    }

    protected Method generateInitialiseMethod(DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, TypeElement typeElement, FieldVariable muleContext, FieldVariable expressionManager, FieldVariable patternInfo, FieldVariable object, FieldVariable retryCount, boolean shouldAutoCreate) {
        DefinedClass pojoClass = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));

        Method initialise = messageProcessorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "initialise");
        initialise.javadoc().add("Obtains the expression manager from the Mule context and initialises the connector. If a target object ");
        initialise.javadoc().add(" has not been set already it will search the Mule registry for a default one.");
        initialise.javadoc().addThrows(ref(InitialisationException.class));
        initialise._throws(InitialisationException.class);

        if (retryCount != null) {
            initialise.body().assign(retryCount, ExpressionFactory._new(ref(AtomicInteger.class)));
        }

        if (expressionManager != null) {
            initialise.body().assign(expressionManager, muleContext.invoke("getExpressionManager"));
        }
        if (patternInfo != null) {
            initialise.body().assign(patternInfo, ref(TemplateParser.class).staticInvoke("createMuleStyleParser").invoke("getStyle"));
        }

        if (object != null) {
            Conditional ifNoObject = initialise.body()._if(Op.eq(object, ExpressionFactory._null()));
            TryStatement tryLookUp = ifNoObject._then()._try();
            tryLookUp.body().assign(object, muleContext.invoke("getRegistry").invoke("lookupObject").arg(ExpressionFactory.dotclass(pojoClass)));
            Conditional ifObjectNoFound = tryLookUp.body()._if(Op.eq(object, ExpressionFactory._null()));
            if(shouldAutoCreate) {
                ifObjectNoFound._then().assign(object, ExpressionFactory._new(pojoClass));
                ifObjectNoFound._then().add(muleContext.invoke("getRegistry").invoke("registerObject").arg(pojoClass.dotclass().invoke("getName")).arg(object));
            } else {
                ifObjectNoFound._then()._throw(ExpressionFactory._new(ref(InitialisationException.class)).
                                arg(ref(MessageFactory.class).staticInvoke("createStaticMessage").
                                        arg("Cannot find object")).arg(ExpressionFactory._this()));
            }
            CatchBlock catchBlock = tryLookUp._catch(ref(RegistrationException.class));
            Variable exception = catchBlock.param("e");
            TypeReference coreMessages = ref(CoreMessages.class);
            Invocation failedToInvoke = coreMessages.staticInvoke("initialisationFailure");
            failedToInvoke.arg(pojoClass.fullName());
            Invocation messageException = ExpressionFactory._new(ref(InitialisationException.class));
            messageException.arg(failedToInvoke);
            messageException.arg(exception);
            messageException.arg(ExpressionFactory._this());
            catchBlock.body()._throw(messageException);
        }

        Conditional ifObjectIsString = initialise.body()._if(Op._instanceof(object, ref(String.class)));
        ifObjectIsString._then().assign(object, muleContext.invoke("getRegistry").invoke("lookupObject").arg(ExpressionFactory.cast(ref(String.class), object)));
        ifObjectIsString._then()._if(Op.eq(object, ExpressionFactory._null()))._then().
                    _throw(ExpressionFactory._new(ref(InitialisationException.class)).
                            arg(ref(MessageFactory.class).staticInvoke("createStaticMessage").
                                    arg("Cannot find object by config name")).arg(ExpressionFactory._this()));

        if (fields != null) {
            for (String fieldName : fields.keySet()) {
                FieldVariableElement variableElement = fields.get(fieldName);

                if (context.getTypeMirrorUtils().isNestedProcessor(variableElement.getVariableElement().asType())) {
                    boolean isList = context.getTypeMirrorUtils().isArrayOrList(variableElement.getVariableElement().asType());

                    if (!isList) {
                        Conditional ifInitialisable = initialise.body()._if(Op._instanceof(variableElement.getField(), ref(Initialisable.class)));
                        ifInitialisable._then().add(
                                ExpressionFactory.cast(ref(Initialisable.class), variableElement.getField()).invoke("initialise")
                        );
                    } else {
                        Conditional ifIsList = initialise.body()._if(Op._instanceof(variableElement.getField(), ref(List.class)));
                        ForEach forEachProcessor = ifIsList._then().forEach(ref(MessageProcessor.class), "messageProcessor", ExpressionFactory.cast(ref(List.class).narrow(MessageProcessor.class), fields.get(fieldName).getField()));
                        Conditional ifInitialisable = forEachProcessor.body()._if(Op._instanceof(forEachProcessor.var(), ref(Initialisable.class)));
                        ifInitialisable._then().add(
                                ExpressionFactory.cast(ref(Initialisable.class), forEachProcessor.var()).invoke("initialise")
                        );
                    }
                } else if (variableElement.getVariableElement().asType().toString().startsWith(HttpCallback.class.getName())) {
                    FieldVariable callbackFlowName = fields.get(fieldName).getField();
                    Block ifCallbackFlowNameIsNull = initialise.body()._if(Op.ne(callbackFlowName, ExpressionFactory._null()))._then();
                    Variable castedModuleObject = ifCallbackFlowNameIsNull.decl(pojoClass, "castedModuleObject", ExpressionFactory.cast(pojoClass, object));
                    Invocation domain = castedModuleObject.invoke("get" + StringUtils.capitalize(DefaultHttpCallbackGenerator.DOMAIN_FIELD_NAME));
                    Invocation localPort = castedModuleObject.invoke("get" + StringUtils.capitalize(DefaultHttpCallbackGenerator.LOCAL_PORT_FIELD_NAME));
                    Invocation remotePort = castedModuleObject.invoke("get" + StringUtils.capitalize(DefaultHttpCallbackGenerator.REMOTE_PORT_FIELD_NAME));
                    Invocation async = castedModuleObject.invoke("get" + StringUtils.capitalize(DefaultHttpCallbackGenerator.ASYNC_FIELD_NAME));
                    ifCallbackFlowNameIsNull.assign(variableElement.getFieldType(), ExpressionFactory._new(context.getClassForRole(DefaultHttpCallbackGenerator.HTTP_CALLBACK_ROLE)).
                            arg(callbackFlowName).arg(muleContext).arg(domain).arg(localPort).arg(remotePort).arg(async));
                }
            }
        }

        return initialise;
    }

    protected Method generateSetMuleContextMethod(DefinedClass clazz, FieldVariable muleContext) {
        return generateSetMuleContextMethod(clazz, muleContext, null);
    }

    protected Method generateSetMuleContextMethod(DefinedClass clazz, FieldVariable muleContext, Map<String, FieldVariableElement> fields) {
        Method setMuleContext = clazz.method(Modifier.PUBLIC, context.getCodeModel().VOID, "setMuleContext");
        setMuleContext.javadoc().add("Set the Mule context");
        setMuleContext.javadoc().addParam("context Mule context to set");
        Variable muleContextParam = setMuleContext.param(ref(MuleContext.class), "context");
        setMuleContext.body().assign(ExpressionFactory._this().ref(muleContext), muleContextParam);

        if (fields != null) {
            for (String fieldName : fields.keySet()) {
                FieldVariableElement variableElement = fields.get(fieldName);

                if (context.getTypeMirrorUtils().isNestedProcessor(variableElement.getVariableElement().asType())) {
                    boolean isList = context.getTypeMirrorUtils().isArrayOrList(variableElement.getVariableElement().asType());

                    if (!isList) {
                        Conditional ifMuleContextAware = setMuleContext.body()._if(Op._instanceof(variableElement.getField(), ref(MuleContextAware.class)));
                        ifMuleContextAware._then().add(
                                ExpressionFactory.cast(ref(MuleContextAware.class), variableElement.getField()).invoke("setMuleContext").arg(muleContextParam)
                        );
                    } else {
                        Conditional ifIsList = setMuleContext.body()._if(Op._instanceof(variableElement.getField(), ref(List.class)));
                        ForEach forEachProcessor = ifIsList._then().forEach(ref(MessageProcessor.class), "messageProcessor", ExpressionFactory.cast(ref(List.class).narrow(MessageProcessor.class), fields.get(fieldName).getField()));
                        Conditional ifMuleContextAware = forEachProcessor.body()._if(Op._instanceof(forEachProcessor.var(), ref(MuleContextAware.class)));
                        ifMuleContextAware._then().add(
                                ExpressionFactory.cast(ref(MuleContextAware.class), forEachProcessor.var()).invoke("setMuleContext").arg(muleContextParam)
                        );
                    }
                }
            }
        }

        return setMuleContext;
    }

    protected Method generateSetFlowConstructMethod(DefinedClass messageSourceClass, FieldVariable flowConstruct) {
        return generateSetFlowConstructMethod(messageSourceClass, flowConstruct, null);
    }

    protected Method generateSetFlowConstructMethod(DefinedClass messageSourceClass, FieldVariable flowConstruct, Map<String, FieldVariableElement> fields) {
        Method setFlowConstruct = messageSourceClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "setFlowConstruct");
        setFlowConstruct.javadoc().add("Sets flow construct");
        setFlowConstruct.javadoc().addParam("flowConstruct Flow construct to set");
        Variable newFlowConstruct = setFlowConstruct.param(ref(FlowConstruct.class), "flowConstruct");
        setFlowConstruct.body().assign(ExpressionFactory._this().ref(flowConstruct), newFlowConstruct);

        if (fields != null) {
            for (String fieldName : fields.keySet()) {
                FieldVariableElement variableElement = fields.get(fieldName);

                if (context.getTypeMirrorUtils().isNestedProcessor(variableElement.getVariableElement().asType())) {
                    boolean isList = context.getTypeMirrorUtils().isArrayOrList(variableElement.getVariableElement().asType());

                    if (!isList) {
                        Conditional ifMuleContextAware = setFlowConstruct.body()._if(Op._instanceof(variableElement.getField(), ref(FlowConstructAware.class)));
                        ifMuleContextAware._then().add(
                                ExpressionFactory.cast(ref(FlowConstructAware.class), variableElement.getField()).invoke("setFlowConstruct").arg(newFlowConstruct)
                        );
                    } else {
                        Conditional ifIsList = setFlowConstruct.body()._if(Op._instanceof(variableElement.getField(), ref(List.class)));
                        ForEach forEachProcessor = ifIsList._then().forEach(ref(MessageProcessor.class), "messageProcessor", ExpressionFactory.cast(ref(List.class).narrow(MessageProcessor.class), fields.get(fieldName).getField()));
                        Conditional ifMuleContextAware = forEachProcessor.body()._if(Op._instanceof(forEachProcessor.var(), ref(FlowConstructAware.class)));
                        ifMuleContextAware._then().add(
                                ExpressionFactory.cast(ref(FlowConstructAware.class), forEachProcessor.var()).invoke("setFlowConstruct").arg(newFlowConstruct)
                        );

                    }
                }
            }
        }

        return setFlowConstruct;
    }


    protected void findConfig(Block block, FieldVariable muleContext, FieldVariable object, String methodName, Variable event, DefinedClass moduleObjectClass, Variable moduleObject) {
        Conditional ifObjectIsString = block._if(Op._instanceof(object, ref(String.class)));
        ifObjectIsString._else().assign(moduleObject, ExpressionFactory.cast(moduleObjectClass, object));
        ifObjectIsString._then().assign(moduleObject, ExpressionFactory.cast(moduleObjectClass, muleContext.invoke("getRegistry").invoke("lookupObject").arg(ExpressionFactory.cast(ref(String.class), object))));

        TypeReference coreMessages = ref(CoreMessages.class);
        Invocation failedToInvoke = coreMessages.staticInvoke("failedToCreate");
        if (methodName != null) {
            failedToInvoke.arg(ExpressionFactory.lit(methodName));
        }
        Invocation messageException = ExpressionFactory._new(ref(MessagingException.class));
        messageException.arg(failedToInvoke);
        if (event != null) {
            messageException.arg(event);
        } else {
            messageException.arg(ExpressionFactory.cast(ref(MuleEvent.class), ExpressionFactory._null()));
        }
        messageException.arg(ExpressionFactory._new(ref(RuntimeException.class)).arg("Cannot find the configuration specified by the config-ref attribute."));

        ifObjectIsString._then()._if(Op.eq(moduleObject, ExpressionFactory._null()))._then()._throw(messageException);
    }

    protected FieldVariable generateFieldForFlowConstruct(DefinedClass messageSourceClass) {
        FieldVariable flowConstruct = messageSourceClass.field(Modifier.PRIVATE, ref(FlowConstruct.class), "flowConstruct");
        flowConstruct.javadoc().add("Flow construct");
        return flowConstruct;
    }

    protected FieldVariable generateRetryCountField(DefinedClass messageSourceClass) {
        FieldVariable retryCount = messageSourceClass.field(Modifier.PRIVATE, ref(AtomicInteger.class), "retryCount");
        retryCount.javadoc().add("Variable used to track how many retries we have attempted on this message processor");
        return retryCount;
    }

    protected FieldVariable generateRetryMaxField(DefinedClass messageSourceClass) {
        FieldVariable retryMax = messageSourceClass.field(Modifier.PRIVATE, context.getCodeModel().INT, "retryMax");
        retryMax.javadoc().add("Maximum number of retries that can be attempted.");
        return retryMax;
    }


    protected Method generateSetModuleObjectMethod(DefinedClass messageProcessorClass, FieldVariable object) {
        Method setObject = messageProcessorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "setModuleObject");
        setObject.javadoc().add("Sets the instance of the object under which the processor will execute");
        setObject.javadoc().addParam("moduleObject Instace of the module");
        Variable objectParam = setObject.param(object.type(), "moduleObject");
        setObject.body().assign(ExpressionFactory._this().ref(object), objectParam);

        return setObject;
    }

    protected void generateTransform(Block block, Variable transformedField, Variable evaluatedField, TypeMirror expectedType, FieldVariable muleContext) {
        Invocation isAssignableFrom = ExpressionFactory.dotclass(ref(expectedType).boxify()).invoke("isAssignableFrom").arg(evaluatedField.invoke("getClass"));
        Conditional ifIsAssignableFrom = block._if(Op.not(isAssignableFrom));
        Block isAssignable = ifIsAssignableFrom._then();
        Variable dataTypeSource = isAssignable.decl(ref(DataType.class), "source");
        Variable dataTypeTarget = isAssignable.decl(ref(DataType.class), "target");

        isAssignable.assign(dataTypeSource, ref(DataTypeFactory.class).staticInvoke("create").arg(evaluatedField.invoke("getClass")));
        isAssignable.assign(dataTypeTarget, ref(DataTypeFactory.class).staticInvoke("create").arg(ExpressionFactory.dotclass(ref(expectedType).boxify())));

        Variable transformer = isAssignable.decl(ref(Transformer.class), "t");
        Invocation lookupTransformer = muleContext.invoke("getRegistry").invoke("lookupTransformer");
        lookupTransformer.arg(dataTypeSource);
        lookupTransformer.arg(dataTypeTarget);
        isAssignable.assign(transformer, lookupTransformer);
        isAssignable.assign(transformedField, ExpressionFactory.cast(ref(expectedType).boxify(), transformer.invoke("transform").arg(evaluatedField)));

        Block notAssignable = ifIsAssignableFrom._else();
        notAssignable.assign(transformedField, ExpressionFactory.cast(ref(expectedType).boxify(), evaluatedField));
    }


    protected Method generateSetListenerMethod(DefinedClass messageSourceClass, FieldVariable messageProcessor) {
        Method setListener = messageSourceClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "setListener");
        setListener.javadoc().add("Sets the message processor that will \"listen\" the events generated by this message source");
        setListener.javadoc().addParam("listener Message processor");
        Variable listener = setListener.param(ref(MessageProcessor.class), "listener");
        setListener.body().assign(ExpressionFactory._this().ref(messageProcessor), listener);

        return setListener;
    }

    protected void generateThrow(String bundle, Class<?> clazz, CatchBlock callProcessorCatch, Expression event, String methodName) {
        Variable exception = callProcessorCatch.param("e");
        TypeReference coreMessages = ref(CoreMessages.class);
        Invocation failedToInvoke = coreMessages.staticInvoke(bundle);
        if (methodName != null) {
            failedToInvoke.arg(ExpressionFactory.lit(methodName));
        }
        Invocation messageException = ExpressionFactory._new(ref(clazz));
        messageException.arg(failedToInvoke);
        if (event != null) {
            messageException.arg(event);
        }
        messageException.arg(exception);
        callProcessorCatch.body()._throw(messageException);
    }

    protected class FieldVariableElement {
        private final FieldVariable field;
        private final FieldVariable fieldType;
        private final VariableElement variableElement;

        public FieldVariableElement(FieldVariable field, FieldVariable fieldType, VariableElement variableElement) {
            this.field = field;
            this.fieldType = fieldType;
            this.variableElement = variableElement;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((field == null) ? 0 : field.hashCode());
            result = prime * result + ((fieldType == null) ? 0 : fieldType.hashCode());
            result = prime * result + ((variableElement == null) ? 0 : variableElement.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            FieldVariableElement other = (FieldVariableElement) obj;
            if (field == null) {
                if (other.field != null) {
                    return false;
                }
            } else if (!field.equals(other.field)) {
                return false;
            }
            if (fieldType == null) {
                if (other.fieldType != null) {
                    return false;
                }
            } else if (!fieldType.equals(other.fieldType)) {
                return false;
            }
            if (variableElement == null) {
                if (other.variableElement != null) {
                    return false;
                }
            } else if (!variableElement.equals(other.variableElement)) {
                return false;
            }
            return true;
        }

        public FieldVariable getField() {
            return field;
        }

        public FieldVariable getFieldType() {
            return fieldType;
        }

        public VariableElement getVariableElement() {
            return variableElement;
        }
    }

    protected void generateSourceCallbackProcessMethod(DefinedClass messageSourceClass, FieldVariable messageProcessor, FieldVariable muleContext, FieldVariable flowConstruct) {
        Method process = messageSourceClass.method(Modifier.PUBLIC, ref(Object.class), "process");
        process.javadoc().add("Implements {@link SourceCallback#process(org.mule.api.MuleEvent)}. This message source will be passed on to ");
        process.javadoc().add("the actual pojo's method as a callback mechanism.");
        process._throws(ref(Exception.class));
        Variable message = process.param(ref(Object.class), "message");

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
        newMuleEvent.arg(ref(MessageExchangePattern.class).staticRef("ONE_WAY"));
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

        CatchBlock catchException = tryBlock._catch(ref(Exception.class));
        Variable exception = catchException.param("e");
        catchException.body()._throw(exception);

        process.body()._return(ExpressionFactory._null());
    }

    protected void generateSourceCallbackProcessMethodWithNoPayload(DefinedClass messageSourceClass, FieldVariable messageProcessor, FieldVariable muleContext, FieldVariable flowConstruct) {
        Method process = messageSourceClass.method(Modifier.PUBLIC, ref(Object.class), "process");
        process.javadoc().add("Implements {@link SourceCallback#process()}. This message source will be passed on to ");
        process.javadoc().add("the actual pojo's method as a callback mechanism.");
        process._throws(ref(Exception.class));

        TryStatement tryBlock = process.body()._try();
        Variable responseEvent = tryBlock.body().decl(ref(MuleEvent.class), "responseEvent");
        Invocation messageProcess = messageProcessor.invoke("process");
        messageProcess.arg(ref(RequestContext.class).staticInvoke("getEvent"));
        tryBlock.body().assign(responseEvent, messageProcess);
        Conditional ifResponse = tryBlock.body()._if(
                Op.cand(Op.ne(responseEvent, ExpressionFactory._null()),
                        Op.ne(responseEvent.invoke("getMessage"), ExpressionFactory._null()))
        );
        ifResponse._then()._return(responseEvent.invoke("getMessage").invoke("getPayload"));

        CatchBlock catchException = tryBlock._catch(ref(Exception.class));
        Variable exception = catchException.param("e");
        catchException.body()._throw(exception);
        
        process.body()._return(ExpressionFactory._null());
    }


    protected void generateSourceCallbackProcessWithPropertiesMethod(DefinedClass messageSourceClass, FieldVariable messageProcessor, FieldVariable muleContext, FieldVariable flowConstruct) {
        Method process = messageSourceClass.method(Modifier.PUBLIC, ref(Object.class), "process");
        process.javadoc().add("Implements {@link SourceCallback#process(org.mule.api.MuleEvent)}. This message source will be passed on to ");
        process.javadoc().add("the actual pojo's method as a callback mechanism.");
        process._throws(ref(Exception.class));
        Variable message = process.param(ref(Object.class), "message");
        Variable properties = process.param(ref(Map.class).narrow(String.class).narrow(Object.class), "properties");

        Variable muleMessage = process.body().decl(ref(MuleMessage.class), "muleMessage");
        Invocation newMuleMessage = ExpressionFactory._new(ref(DefaultMuleMessage.class));
        newMuleMessage.arg(message);
        newMuleMessage.arg(properties);
        newMuleMessage.arg(ExpressionFactory._null());
        newMuleMessage.arg(ExpressionFactory._null());
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
        newMuleEvent.arg(ref(MessageExchangePattern.class).staticRef("ONE_WAY"));
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

        CatchBlock catchException = tryBlock._catch(ref(Exception.class));
        Variable exception = catchException.param("e");
        catchException.body()._throw(exception);

        process.body()._return(ExpressionFactory._null());
    }


    protected void generateStartMethod(DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields) {
        Method startMethod = messageProcessorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "start");
        startMethod._throws(ref(MuleException.class));

        if (fields != null) {
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
                } else if (variableElement.getVariableElement().asType().toString().startsWith(HttpCallback.class.getName())) {
                    startMethod.body()._if(Op.ne(variableElement.getFieldType(), ExpressionFactory._null()))._then().invoke(variableElement.getFieldType(), "start");
                }
            }
        }
    }

    protected void generateStopMethod(DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields) {
        Method stopMethod = messageProcessorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "stop");
        stopMethod._throws(ref(MuleException.class));

        if (fields != null) {
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
                } else if (variableElement.getVariableElement().asType().toString().startsWith(HttpCallback.class.getName())) {
                    stopMethod.body()._if(Op.ne(variableElement.getFieldType(), ExpressionFactory._null()))._then().invoke(variableElement.getFieldType(), "stop");
                }
            }
        }
    }

    protected void generateDiposeMethod(DefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields) {
        Method diposeMethod = messageProcessorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "dispose");

        if (fields != null) {
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
    }


    protected Method generateGetAttributeValue(DefinedClass beanDefinitionparser) {
        Method getAttributeValue = beanDefinitionparser.method(Modifier.PROTECTED, ref(String.class), "getAttributeValue");
        Variable element = getAttributeValue.param(ref(org.w3c.dom.Element.class), "element");
        Variable attributeName = getAttributeValue.param(ref(String.class), "attributeName");

        Invocation getAttribute = element.invoke("getAttribute").arg(attributeName);

        Invocation isEmpty = ref(StringUtils.class).staticInvoke("isEmpty");
        isEmpty.arg(getAttribute);

        Block ifIsEmpty = getAttributeValue.body()._if(isEmpty.not())._then();
        ifIsEmpty._return(getAttribute);

        getAttributeValue.body()._return(ExpressionFactory._null());
        return getAttributeValue;
    }


    protected void generateAttachMessageProcessor(Method parse, Variable definition, Variable parserContext) {
        Variable propertyValues = parse.body().decl(ref(MutablePropertyValues.class), "propertyValues",
                parserContext.invoke("getContainingBeanDefinition").invoke("getPropertyValues"));

        Conditional ifIsPoll = parse.body()._if(parserContext.invoke("getContainingBeanDefinition").invoke("getBeanClassName")
                .invoke("equals").arg("org.mule.config.spring.factories.PollingMessageSourceFactoryBean"));

        ifIsPoll._then().add(propertyValues.invoke("addPropertyValue").arg("messageProcessor").arg(definition));

        Conditional ifIsEnricher = ifIsPoll._else()._if(parserContext.invoke("getContainingBeanDefinition").invoke("getBeanClassName")
                .invoke("equals").arg("org.mule.enricher.MessageEnricher"));

        ifIsEnricher._then().add(propertyValues.invoke("addPropertyValue").arg("enrichmentMessageProcessor").arg(definition));

        Variable messageProcessors = ifIsEnricher._else().decl(ref(PropertyValue.class), "messageProcessors",
                propertyValues.invoke("getPropertyValue").arg("messageProcessors"));
        Conditional noList = ifIsEnricher._else()._if(Op.cor(Op.eq(messageProcessors, ExpressionFactory._null()), Op.eq(messageProcessors.invoke("getValue"),
                ExpressionFactory._null())));
        noList._then().add(propertyValues.invoke("addPropertyValue").arg("messageProcessors").arg(ExpressionFactory._new(ref(ManagedList.class))));
        Variable listMessageProcessors = ifIsEnricher._else().decl(ref(List.class), "listMessageProcessors",
                ExpressionFactory.cast(ref(List.class), propertyValues.invoke("getPropertyValue").arg("messageProcessors").invoke("getValue")));
        ifIsEnricher._else().add(listMessageProcessors.invoke("add").arg(
                definition
        ));
    }
}
