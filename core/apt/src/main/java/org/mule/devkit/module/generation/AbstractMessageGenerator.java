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
import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.source.MessageSource;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.devkit.annotations.SourceCallback;
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
import org.mule.devkit.model.code.Package;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.TemplateParser;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractMessageGenerator extends AbstractModuleGenerator {

    protected FieldVariable generateFieldForPatternInfo(DefinedClass messageProcessorClass) {
        return messageProcessorClass.field(Modifier.PRIVATE, ref(TemplateParser.PatternInfo.class), "patternInfo");
    }

    protected FieldVariable generateFieldForExpressionManager(DefinedClass messageProcessorClass) {
        return messageProcessorClass.field(Modifier.PRIVATE, ref(ExpressionManager.class), "expressionManager");
    }

    protected FieldVariable generateFieldForMuleContext(DefinedClass messageProcessorClass) {
        return messageProcessorClass.field(Modifier.PRIVATE, ref(MuleContext.class), "muleContext");
    }

    protected FieldVariable generateFieldForPojo(DefinedClass messageProcessorClass, Element typeElement) {
        DefinedClass pojo = context.getClassForRole(context.getNameUtils().generatePojoRoleKey((TypeElement) typeElement));

        return messageProcessorClass.field(Modifier.PRIVATE, pojo, "object");
    }

    protected DefinedClass getBeanDefinitionParserClass(ExecutableElement executableElement) {
        String beanDefinitionParserName = context.getNameUtils().generateClassName(executableElement, "DefinitionParser");
        Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(beanDefinitionParserName) + ".config.spring");
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(beanDefinitionParserName), ChildDefinitionParser.class);

        return clazz;
    }

    protected DefinedClass getMessageProcessorClass(ExecutableElement executableElement) {
        String beanDefinitionParserName = context.getNameUtils().generateClassName(executableElement, "MessageProcessor");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(beanDefinitionParserName) + ".config");
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(beanDefinitionParserName), new Class[]{Initialisable.class, MessageProcessor.class, MuleContextAware.class});

        return clazz;
    }

    protected DefinedClass getMessageSourceClass(ExecutableElement executableElement) {
        String beanDefinitionParserName = context.getNameUtils().generateClassName(executableElement, "MessageSource");
        Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(beanDefinitionParserName) + ".config");
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

    protected Map<String, FieldVariableElement> generateFieldForEachParameter(DefinedClass messageProcessorClass, ExecutableElement executableElement) {
        Map<String, AbstractMessageGenerator.FieldVariableElement> fields = new HashMap<String, FieldVariableElement>();
        for (VariableElement variable : executableElement.getParameters()) {
            if (variable.asType().toString().contains(SourceCallback.class.getName()))
                continue;

            if (SchemaTypeConversion.isSupported(variable.asType().toString()) ||
                    context.getTypeMirrorUtils().isXmlType(variable.asType()) ||
                    context.getTypeMirrorUtils().isEnum(variable.asType())) {
                String fieldName = variable.getSimpleName().toString();
                FieldVariable field = messageProcessorClass.field(Modifier.PRIVATE, ref(Object.class), fieldName);
                fields.put(variable.getSimpleName().toString(), new AbstractMessageGenerator.FieldVariableElement(field, variable));
            } else {
                String fieldName = variable.getSimpleName().toString();
                FieldVariable field = messageProcessorClass.field(Modifier.PRIVATE, ref(variable.asType()), fieldName);
                fields.put(variable.getSimpleName().toString(), new AbstractMessageGenerator.FieldVariableElement(field, variable));
            }
        }
        return fields;
    }

    protected Method generateInitialiseMethod(DefinedClass messageProcessorClass, Element typeElement, FieldVariable muleContext, FieldVariable expressionManager, FieldVariable patternInfo, FieldVariable object) {
        DefinedClass pojoClass = context.getClassForRole(context.getNameUtils().generatePojoRoleKey((TypeElement) typeElement));

        Method initialise = messageProcessorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "initialise");
        initialise._throws(InitialisationException.class);

        if (expressionManager != null) {
            initialise.body().assign(expressionManager, muleContext.invoke("getExpressionManager"));
        }
        if (patternInfo != null) {
            initialise.body().assign(patternInfo, ref(TemplateParser.class).staticInvoke("createMuleStyleParser").invoke("getStyle"));
        }

        Conditional ifNoObject = initialise.body()._if(Op.eq(object, ExpressionFactory._null()));
        TryStatement tryLookUp = ifNoObject._then()._try();
        tryLookUp.body().assign(object, muleContext.invoke("getRegistry").invoke("lookupObject").arg(ExpressionFactory.dotclass(pojoClass)));
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

        return initialise;
    }


    protected Method generateSetObjectMethod(DefinedClass messageProcessorClass, FieldVariable object) {
        Method setObject = messageProcessorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "setObject");
        Variable objectParam = setObject.param(object.type(), "object");
        setObject.body().assign(ExpressionFactory._this().ref(object), objectParam);

        return setObject;
    }


    protected Method generateSetMuleContextMethod(DefinedClass messageProcessorClass, FieldVariable muleContext) {
        Method setMuleContext = messageProcessorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "setMuleContext");
        Variable muleContextParam = setMuleContext.param(ref(MuleContext.class), "context");
        setMuleContext.body().assign(ExpressionFactory._this().ref(muleContext), muleContextParam);

        return setMuleContext;
    }

    protected void generateExpressionEvaluator(Block block, Variable evaluatedField, FieldVariable field, FieldVariable patternInfo, FieldVariable expressionManager, Variable muleMessage) {
        Conditional conditional = block._if(Op._instanceof(field, ref(String.class)));
        Block trueBlock = conditional._then();
        Conditional isPattern = trueBlock._if(Op.cand(
                ExpressionFactory.invoke(ExpressionFactory.cast(ref(String.class), field), "startsWith").arg(patternInfo.invoke("getPrefix")),
                ExpressionFactory.invoke(ExpressionFactory.cast(ref(String.class), field), "endsWith").arg(patternInfo.invoke("getSuffix"))));
        Invocation evaluate = expressionManager.invoke("evaluate");
        evaluate.arg(ExpressionFactory.cast(ref(String.class), field));
        evaluate.arg(muleMessage);
        isPattern._then().assign(evaluatedField, evaluate);
        Invocation parse = expressionManager.invoke("parse");
        parse.arg(ExpressionFactory.cast(ref(String.class), field));
        parse.arg(muleMessage);
        isPattern._else().assign(evaluatedField, parse);
        Block falseBlock = conditional._else();
        falseBlock.assign(evaluatedField, field);
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

    protected Method generateSetter(DefinedClass messageProcessorClass, FieldVariable field) {
        Method setter = messageProcessorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "set" + StringUtils.capitalize(field.name()));
        Variable value = setter.param(field.type(), "value");
        setter.body().assign(ExpressionFactory._this().ref(field), value);

        return setter;
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
        private final VariableElement variableElement;

        public FieldVariableElement(FieldVariable field, VariableElement variableElement) {
            this.field = field;
            this.variableElement = variableElement;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((field == null) ? 0 : field.hashCode());
            result = prime * result + ((variableElement == null) ? 0 : variableElement.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FieldVariableElement other = (FieldVariableElement) obj;
            if (field == null) {
                if (other.field != null)
                    return false;
            } else if (!field.equals(other.field))
                return false;
            if (variableElement == null) {
                if (other.variableElement != null)
                    return false;
            } else if (!variableElement.equals(other.variableElement))
                return false;
            return true;
        }

        public FieldVariable getField() {
            return field;
        }

        public VariableElement getVariableElement() {
            return variableElement;
        }
    }


}
