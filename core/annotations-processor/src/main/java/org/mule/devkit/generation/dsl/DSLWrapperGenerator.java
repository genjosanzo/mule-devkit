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

package org.mule.devkit.generation.dsl;

import org.apache.commons.lang.StringUtils;
import org.mule.api.MuleContext;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.annotations.param.Session;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.DevkitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.ClassAlreadyExistsException;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Package;
import org.mule.devkit.model.code.Type;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.LinkedList;
import java.util.List;

public class DSLWrapperGenerator extends AbstractModuleGenerator {

    @Override
    protected boolean shouldGenerate(DevkitTypeElement typeElement) {
        if (getPojoType(typeElement).fullName().endsWith("HttpCallbackAdapter")) {
            return false;
        }
        return true;
    }

    @Override
    protected void doGenerate(DevkitTypeElement typeElement) throws GenerationException {

        final DefinedClass wrapperClass = buildWrapperClass(typeElement);
        final FieldVariable object = generateFieldForPojo(typeElement, wrapperClass);

        initializeWrapper(typeElement, wrapperClass, object);

        for (final ExecutableElement executableElement : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
            final Processor mp = executableElement.getAnnotation(Processor.class);

            if (mp == null)
                continue;

            final List<Parameter> mandatoryParams = new LinkedList<Parameter>();
            final List<FieldVariable> fields = new LinkedList<FieldVariable>();

            final DefinedClass _interface = buildMessageDefinitionInterface(executableElement);
            final DefinedClass _builder = buildMessageBuilderClass(executableElement, _interface);

            generateFieldForPojo(typeElement, _builder);

            handleParameters(executableElement.getParameters(), _interface, _builder, mandatoryParams, fields);

            generateBuilderConstructor(object, _builder, mandatoryParams);

            generateWrapperMethods(wrapperClass, object, executableElement.getSimpleName().toString(), _interface, _builder, multiply(mandatoryParams));

            generateBuildMethod(object, executableElement, _builder, fields);
        }
    }

    private void handleParameters(final List<? extends VariableElement> parameters, final DefinedClass _interface, final DefinedClass _builder, final List<Parameter> mandatoryParams, final List<FieldVariable> fields) {
        for (final VariableElement param : parameters) {

            if (param.getAnnotation(Session.class) != null) {
                continue;
            }

            final String paramName = param.getSimpleName().toString();
            final FieldVariable field = _builder.field(Modifier.PRIVATE, Object.class, paramName);

            fields.add(field);

            final Optional optional = param.getAnnotation(Optional.class);

            if (optional != null) {
                generateOptionalMethod(_interface, paramName, ref(param.asType()), _interface);
                generateOptionalMethod(_interface, paramName, expressionType(), _interface);

                generateOptionalMethod(_builder, paramName, ref(param.asType()), _interface);
                generateOptionalMethod(_builder, paramName, expressionType(), _interface);
            } else {
                mandatoryParams.add(new Parameter(paramName, ref(param.asType()).fullName()));
            }

            final Default defaultInfo = param.getAnnotation(Default.class);

            if (defaultInfo != null) {
                field.init(defaultValue(param.asType(), defaultInfo.value()));
            } else {
                field.init(ExpressionFactory._null());
            }
        }
    }

    private void generateWrapperMethods(final DefinedClass wrapperClass, final FieldVariable object, final String methodName, final Type returnType, final DefinedClass _builder, final Parameter[][] signatures) {
        for (int i = 0; i < signatures.length; i++) {
            final Method methodWrapper = wrapperClass.method(Modifier.PUBLIC, returnType, methodName);
            final Invocation builderInstace = ExpressionFactory._new(_builder);
            builderInstace.arg(object);
            for (int k = 0; k < signatures[i].length; k++) {
                final Variable paramRef = methodWrapper.param(ref(signatures[i][k].getType()), signatures[i][k].getName());
                builderInstace.arg(paramRef);
            }
            methodWrapper.body()._return(builderInstace);
        }
    }

    private void generateBuildMethod(final FieldVariable object, final ExecutableElement executableElement, final DefinedClass _builder, final List<FieldVariable> fields) {
        final Method build = _builder.method(Modifier.PUBLIC, pojoType(executableElement), "build");

        final Variable muleContextParam = build.param(muleContextInterface(), "muleContext");
        final Variable placeholderParam = build.param(propertyPlaceholderInterface(), "placeholder");

        final Variable varMP = build.body().decl(pojoType(executableElement), "$mp").init(ExpressionFactory._new(pojoType(executableElement)));

        build.body().invoke(varMP, "setMuleContext").arg(muleContextParam);
        build.body().invoke(varMP, "setModuleObject").arg(object);

        for (final FieldVariable field : fields) {
            final String setterName = "set" + StringUtils.capitalize(field.name());

            final Conditional fieldCond = build.body()._if(field.ne(ExpressionFactory._null()).cand(field._instanceof(expressionType())));

            fieldCond._then().invoke(varMP, setterName).arg(ExpressionFactory.cast(expressionType(), field).invoke("toString").arg(placeholderParam));
            fieldCond._else().invoke(varMP, setterName).arg(field);
        }

        build.body()._return(varMP);
    }

    private void generateBuilderConstructor(final FieldVariable object, final DefinedClass _builder, final List<Parameter> mandatoryParams) {
        final Method builderConstructor = _builder.constructor(Modifier.PUBLIC);
        final Variable objectParam = builderConstructor.param(object.type(), "object");
        builderConstructor.body().assign(ExpressionFactory.refthis("object"), objectParam);
        for (final Parameter param : mandatoryParams) {
            final Variable constructotParam = builderConstructor.param(Object.class, param.getName());
            builderConstructor.body().assign(ExpressionFactory.refthis(param.getName()), constructotParam);
        }
    }

    private void generateOptionalMethod(final DefinedClass defClazz, final String paramName, final Type type, final Type returnType) {
        final String methodName = "with" + StringUtils.capitalize(paramName);
        final Method method = defClazz.method(Modifier.PUBLIC, returnType, methodName);
        final Variable methodParam = method.param(type, paramName);
        method.javadoc().add("Sets " + paramName);
        method.javadoc().addParam(paramName + " Value to set");

        if (!defClazz.isInterface()) {
            method.body().assign(ExpressionFactory.refthis(paramName), methodParam);
            method.body()._return(ExpressionFactory._this());
        }
    }

    protected FieldVariable generateFieldForPojo(final TypeElement typeElement, final DefinedClass defClazz) {
        final FieldVariable fieldPojo = defClazz.field(Modifier.PRIVATE + Modifier.FINAL, getPojoType(typeElement), "object");
        fieldPojo.javadoc().add("Plain old java object");

        return fieldPojo;
    }

    protected Type getPojoType(final TypeElement typeElement) {
        return context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));
    }


    private void initializeWrapper(final TypeElement typeElement, final DefinedClass wrapperClass, FieldVariable object) {
        final Method constructor = wrapperClass.constructor(Modifier.PUBLIC);
        constructor.body().assign(object, ExpressionFactory._new(getPojoType(typeElement)));

        final List<VariableElement> variableElements = ElementFilter.fieldsIn(typeElement.getEnclosedElements());
        for (final VariableElement variableElement : variableElements) {
            final Configurable configInfo = variableElement.getAnnotation(Configurable.class);

            if (configInfo == null)
                continue;

            final String fieldName = variableElement.getSimpleName().toString();

            generateWrapperSetter(wrapperClass, variableElement, object);

            final Default defaultInfo = variableElement.getAnnotation(Default.class);

            if (defaultInfo == null)
                continue;

            final Invocation methdoCall = ExpressionFactory.invoke(object, "set" + StringUtils.capitalize(fieldName));
            methdoCall.arg(defaultValue(variableElement.asType(), defaultInfo.value()));
            constructor.body().add(methdoCall);
        }
    }


    protected DefinedClass buildMessageDefinitionInterface(ExecutableElement executableElement) {
        final String typeName = context.getNameUtils().generateClassName(executableElement, "MessageProcessorDefinition");
        final Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(typeName) + ".dsl");

        try {
            return pkg._interface(context.getNameUtils().getClassName(typeName))._implements(messageProcessorDefinitionInterface());
        } catch (ClassAlreadyExistsException e) {
            throw new RuntimeException(e);
        }
    }

    protected DefinedClass buildWrapperClass(TypeElement typeElement) {
        final String typeName = context.getNameUtils().generateClassName(typeElement, ".dsl", "");
        final Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(typeName));
        final DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(typeName));

        return clazz;
    }

    protected DefinedClass buildMessageBuilderClass(final ExecutableElement executableElement, DefinedClass interfaceRef) {
        final String typeName = context.getNameUtils().generateClassName(executableElement, "MessageProcessorBuilder");
        final Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(typeName) + ".dsl.internal");

        return pkg._class(context.getNameUtils().getClassName(typeName))
                ._implements(builderInterface().narrow(pojoType(executableElement)))
                ._implements(interfaceRef);
    }

    protected Method generateWrapperSetter(DefinedClass clazz, VariableElement field, FieldVariable object) {
        final String setterName = "set" + StringUtils.capitalize(field.getSimpleName().toString());

        final Method setter = clazz.method(Modifier.PUBLIC, context.getCodeModel().VOID, setterName);
        setter.javadoc().add("Sets " + field.getSimpleName().toString());
        setter.javadoc().addParam("value Value to set");

        final Variable value = setter.param(ref(field.asType()), "value");
        setter.body().add(ExpressionFactory._this().ref(object).invoke(setterName).arg(value));

        return setter;
    }

    protected Invocation defaultValue(final TypeMirror type, final String init) {
        final Invocation invocation = ref("org.mule.config.dsl.util.DefaultValueConverter").boxify().staticInvoke("valueOf");
        invocation.arg(ExpressionFactory.dotclass(ref(type).boxify()));
        invocation.arg(ExpressionFactory.lit(init));

        return invocation;
    }

    private TypeReference muleContextInterface() {
        return ref(MuleContext.class);
    }

    private Type propertyPlaceholderInterface() {
        return ref("org.mule.config.dsl.PropertyPlaceholder");
    }

    private TypeReference messageProcessorDefinitionInterface() {
        return ref("org.mule.config.dsl.MessageProcessorDefinition").boxify();
    }

    private TypeReference builderInterface() {
        return ref("org.mule.config.dsl.internal.Builder").boxify();
    }

    private TypeReference expressionType() {
        return ref(expressionTypeName()).boxify();
    }

    private String expressionTypeName() {
        return "org.mule.config.dsl.ExpressionEvaluatorDefinition";
    }

    private TypeReference pojoType(final ExecutableElement executableElement) {
        return ref(pojoTypeName(executableElement)).boxify();
    }

    private String pojoTypeName(final ExecutableElement executableElement) {
        return context.getNameUtils().generateClassName(executableElement, ".config", "MessageProcessor");
    }

    private Parameter[][] multiply(List<Parameter> params) {
        Parameter[][] result = new Parameter[1][params.size()];

        for (int i = 0; i < params.size(); i++) {
            result[0][i] = new Parameter(params.get(i).getName());
        }

        for (Parameter param : params) {
            result = multiply(result, param, params.size());
        }

        return result;
    }

    private Parameter[][] multiply(Parameter[][] generate, Parameter placeholder, int length) {
        final String[] types = new String[]{expressionTypeName(), placeholder.getType()};

        Parameter[][] results = new Parameter[types.length * generate.length][length];
        int n = 0;
        for (int i = 0; i < types.length; i++) {
            for (int j = 0; j < generate.length; j++) {
                for (int k = 0; k < length; k++) {
                    results[n][k] = define(generate[j][k], placeholder, types[i]);
                }
                n++;
            }
        }
        return results;
    }

    private Parameter define(Parameter result, Parameter placeholder, String type) {
        if (!placeholder.getName().equals(result.getName())) {
            return new Parameter(result);
        }

        if (result.hasType()) {
            return new Parameter(result);
        } else {
            return new Parameter(result.getName(), type);
        }
    }

    private static class Parameter {

        private final String name;
        private final String type;

        public Parameter(String name) {
            this.name = name;
            this.type = null;
        }

        public Parameter(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public Parameter(Parameter base) {
            this(base.name, base.type);
        }

        public boolean hasType() {
            return this.type != null;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }
}
