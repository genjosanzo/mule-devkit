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
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.JBlock;
import org.mule.devkit.model.code.JCatchBlock;
import org.mule.devkit.model.code.JClass;
import org.mule.devkit.model.code.JConditional;
import org.mule.devkit.model.code.JExpr;
import org.mule.devkit.model.code.JExpression;
import org.mule.devkit.model.code.JFieldVar;
import org.mule.devkit.model.code.JInvocation;
import org.mule.devkit.model.code.JMethod;
import org.mule.devkit.model.code.JMod;
import org.mule.devkit.model.code.JOp;
import org.mule.devkit.model.code.JPackage;
import org.mule.devkit.model.code.JTryBlock;
import org.mule.devkit.model.code.JVar;
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

    protected JFieldVar generateFieldForPatternInfo(DefinedClass messageProcessorClass) {
        return messageProcessorClass.field(JMod.PRIVATE, ref(TemplateParser.PatternInfo.class), "patternInfo");
    }

    protected JFieldVar generateFieldForExpressionManager(DefinedClass messageProcessorClass) {
        return messageProcessorClass.field(JMod.PRIVATE, ref(ExpressionManager.class), "expressionManager");
    }

    protected JFieldVar generateFieldForMuleContext(DefinedClass messageProcessorClass) {
        return messageProcessorClass.field(JMod.PRIVATE, ref(MuleContext.class), "muleContext");
    }

    protected JFieldVar generateFieldForPojo(DefinedClass messageProcessorClass, Element typeElement) {
        DefinedClass pojo = context.getClassForRole(context.getNameUtils().generateClassName((TypeElement) typeElement, "Pojo"));

        return messageProcessorClass.field(JMod.PRIVATE, pojo, "object");
    }

    protected DefinedClass getBeanDefinitionParserClass(ExecutableElement executableElement) {
        String beanDefinitionParserName = context.getNameUtils().generateClassName(executableElement, "BeanDefinitionParser");
        JPackage pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(beanDefinitionParserName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(beanDefinitionParserName), ChildDefinitionParser.class);

        return clazz;
    }

    protected DefinedClass getMessageProcessorClass(ExecutableElement executableElement) {
        String beanDefinitionParserName = context.getNameUtils().generateClassName(executableElement, "MessageProcessor");
        JPackage pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(beanDefinitionParserName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(beanDefinitionParserName), new Class[]{Initialisable.class, MessageProcessor.class, MuleContextAware.class});

        return clazz;
    }

    protected DefinedClass getMessageSourceClass(ExecutableElement executableElement) {
        String beanDefinitionParserName = context.getNameUtils().generateClassName(executableElement, "MessageSource");
        JPackage pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(beanDefinitionParserName));
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

            if (SchemaTypeConversion.isSupported(variable.asType().toString()) || context.getTypeMirrorUtils().isXmlType(variable.asType())) {
                String fieldName = variable.getSimpleName().toString();
                JFieldVar field = messageProcessorClass.field(JMod.PRIVATE, ref(Object.class), fieldName);
                fields.put(variable.getSimpleName().toString(), new AbstractMessageGenerator.FieldVariableElement(field, variable));
            } else {
                String fieldName = variable.getSimpleName().toString();
                JFieldVar field = messageProcessorClass.field(JMod.PRIVATE, ref(variable.asType()), fieldName);
                fields.put(variable.getSimpleName().toString(), new AbstractMessageGenerator.FieldVariableElement(field, variable));
            }
        }
        return fields;
    }

    protected JMethod generateInitialiseMethod(DefinedClass messageProcessorClass, Element typeElement, JFieldVar muleContext, JFieldVar expressionManager, JFieldVar patternInfo, JFieldVar object) {
        DefinedClass pojoClass = context.getClassForRole(context.getNameUtils().generateClassName((TypeElement) typeElement, "Pojo"));

        JMethod initialise = messageProcessorClass.method(JMod.PUBLIC, context.getCodeModel().VOID, "initialise");
        initialise._throws(InitialisationException.class);

        if (expressionManager != null) {
            initialise.body().assign(expressionManager, muleContext.invoke("getExpressionManager"));
        }
        if (patternInfo != null) {
            initialise.body().assign(patternInfo, ref(TemplateParser.class).staticInvoke("createMuleStyleParser").invoke("getStyle"));
        }

        JConditional ifNoObject = initialise.body()._if(JOp.eq(object, JExpr._null()));
        JTryBlock tryLookUp = ifNoObject._then()._try();
        tryLookUp.body().assign(object, muleContext.invoke("getRegistry").invoke("lookupObject").arg(JExpr.dotclass(pojoClass)));
        JCatchBlock catchBlock = tryLookUp._catch(ref(RegistrationException.class));
        JVar exception = catchBlock.param("e");
        JClass coreMessages = ref(CoreMessages.class);
        JInvocation failedToInvoke = coreMessages.staticInvoke("initialisationFailure");
        failedToInvoke.arg(pojoClass.fullName());
        JInvocation messageException = JExpr._new(ref(InitialisationException.class));
        messageException.arg(failedToInvoke);
        messageException.arg(exception);
        messageException.arg(JExpr._this());
        catchBlock.body()._throw(messageException);

        return initialise;
    }


    protected JMethod generateSetObjectMethod(DefinedClass messageProcessorClass, JFieldVar object) {
        JMethod setObject = messageProcessorClass.method(JMod.PUBLIC, context.getCodeModel().VOID, "setObject");
        JVar objectParam = setObject.param(object.type(), "object");
        setObject.body().assign(JExpr._this().ref(object), objectParam);

        return setObject;
    }


    protected JMethod generateSetMuleContextMethod(DefinedClass messageProcessorClass, JFieldVar muleContext) {
        JMethod setMuleContext = messageProcessorClass.method(JMod.PUBLIC, context.getCodeModel().VOID, "setMuleContext");
        JVar muleContextParam = setMuleContext.param(ref(MuleContext.class), "context");
        setMuleContext.body().assign(JExpr._this().ref(muleContext), muleContextParam);

        return setMuleContext;
    }

    protected void generateExpressionEvaluator(JBlock block, JVar evaluatedField, JFieldVar field, JFieldVar patternInfo, JFieldVar expressionManager, JVar muleMessage) {
        JConditional conditional = block._if(JOp._instanceof(field, ref(String.class)));
        JBlock trueBlock = conditional._then();
        JConditional isPattern = trueBlock._if(JOp.cand(
                JExpr.invoke(JExpr.cast(ref(String.class), field), "startsWith").arg(patternInfo.invoke("getPrefix")),
                JExpr.invoke(JExpr.cast(ref(String.class), field), "endsWith").arg(patternInfo.invoke("getSuffix"))));
        JInvocation evaluate = expressionManager.invoke("evaluate");
        evaluate.arg(JExpr.cast(ref(String.class), field));
        evaluate.arg(muleMessage);
        isPattern._then().assign(evaluatedField, evaluate);
        JInvocation parse = expressionManager.invoke("parse");
        parse.arg(JExpr.cast(ref(String.class), field));
        parse.arg(muleMessage);
        isPattern._else().assign(evaluatedField, parse);
        JBlock falseBlock = conditional._else();
        falseBlock.assign(evaluatedField, field);
    }

    protected void generateTransform(JBlock block, JVar transformedField, JVar evaluatedField, TypeMirror expectedType, JFieldVar muleContext) {
        JInvocation isAssignableFrom = JExpr.dotclass(ref(expectedType).boxify()).invoke("isAssignableFrom").arg(evaluatedField.invoke("getClass"));
        JConditional ifIsAssignableFrom = block._if(JOp.not(isAssignableFrom));
        JBlock isAssignable = ifIsAssignableFrom._then();
        JVar dataTypeSource = isAssignable.decl(ref(DataType.class), "source");
        JVar dataTypeTarget = isAssignable.decl(ref(DataType.class), "target");

        isAssignable.assign(dataTypeSource, ref(DataTypeFactory.class).staticInvoke("create").arg(evaluatedField.invoke("getClass")));
        isAssignable.assign(dataTypeTarget, ref(DataTypeFactory.class).staticInvoke("create").arg(JExpr.dotclass(ref(expectedType).boxify())));

        JVar transformer = isAssignable.decl(ref(Transformer.class), "t");
        JInvocation lookupTransformer = muleContext.invoke("getRegistry").invoke("lookupTransformer");
        lookupTransformer.arg(dataTypeSource);
        lookupTransformer.arg(dataTypeTarget);
        isAssignable.assign(transformer, lookupTransformer);
        isAssignable.assign(transformedField, JExpr.cast(ref(expectedType).boxify(), transformer.invoke("transform").arg(evaluatedField)));

        JBlock notAssignable = ifIsAssignableFrom._else();
        notAssignable.assign(transformedField, JExpr.cast(ref(expectedType).boxify(), evaluatedField));
    }

    protected JMethod generateSetter(DefinedClass messageProcessorClass, JFieldVar field) {
        JMethod setter = messageProcessorClass.method(JMod.PUBLIC, context.getCodeModel().VOID, "set" + StringUtils.capitalize(field.name()));
        JVar value = setter.param(field.type(), "value");
        setter.body().assign(JExpr._this().ref(field), value);

        return setter;
    }


    protected void generateThrow(String bundle, Class<?> clazz, JCatchBlock callProcessorCatch, JExpression event, String methodName) {
        JVar exception = callProcessorCatch.param("e");
        JClass coreMessages = ref(CoreMessages.class);
        JInvocation failedToInvoke = coreMessages.staticInvoke(bundle);
        if (methodName != null) {
            failedToInvoke.arg(JExpr.lit(methodName));
        }
        JInvocation messageException = JExpr._new(ref(clazz));
        messageException.arg(failedToInvoke);
        if (event != null) {
            messageException.arg(event);
        }
        messageException.arg(exception);
        callProcessorCatch.body()._throw(messageException);
    }

    protected class FieldVariableElement {
        private final JFieldVar field;
        private final VariableElement variableElement;

        public FieldVariableElement(JFieldVar field, VariableElement variableElement) {
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

        public JFieldVar getField() {
            return field;
        }

        public VariableElement getVariableElement() {
            return variableElement;
        }
    }


}
