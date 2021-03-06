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

package org.mule.devkit.generation.mule.transfomer;

import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.context.MuleContextAware;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.NamingContants;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public class EnumTransformerGenerator extends AbstractMessageGenerator {

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return typeElement.hasAnnotation(Module.class) || typeElement.hasAnnotation(Connector.class);
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {

        for (VariableElement field : typeElement.getFields()) {
            if (context.getTypeMirrorUtils().isEnum(field.asType())) {
                if (!context.isEnumRegistered(field.asType())) {
                    registerEnumTransformer(field);
                    context.registerEnum(field.asType());
                }
            }
        }

        for (ExecutableElement method : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            for (VariableElement variable : method.getParameters()) {
                if (context.getTypeMirrorUtils().isEnum(variable.asType()) && !context.isEnumRegistered(variable.asType())) {
                    registerEnumTransformer(variable);
                    context.registerEnum(variable.asType());
                } else if (context.getTypeMirrorUtils().isCollection(variable.asType())) {
                    DeclaredType variableType = (DeclaredType) variable.asType();
                    for (TypeMirror variableTypeParameter : variableType.getTypeArguments()) {
                        if (context.getTypeMirrorUtils().isEnum(variableTypeParameter) && !context.isEnumRegistered(variableTypeParameter)) {
                            Element enumElement = context.getTypeUtils().asElement(variableTypeParameter);
                            registerEnumTransformer(enumElement);
                            context.registerEnum(variableTypeParameter);
                        }
                    }
                }
            }
        }

        for (ExecutableElement method : typeElement.getMethodsAnnotatedWith(Source.class)) {
            for (VariableElement variable : method.getParameters()) {
                if (!context.getTypeMirrorUtils().isEnum(variable.asType())) {
                    continue;
                }

                if (!context.isEnumRegistered(variable.asType())) {
                    registerEnumTransformer(variable);
                    context.registerEnum(variable.asType());
                }
            }
        }
    }

    private void registerEnumTransformer(Element variableElement) {
        // get class
        DefinedClass transformerClass = getEnumTransformerClass(variableElement);

        // declare object
        FieldVariable muleContext = generateFieldForMuleContext(transformerClass);

        // declare weight
        FieldVariable weighting = transformerClass.field(Modifier.PRIVATE, context.getCodeModel().INT, "weighting", ref(DiscoverableTransformer.class).staticRef("DEFAULT_PRIORITY_WEIGHTING"));

        //generate constructor
        generateConstructor(transformerClass, variableElement);

        // add setmulecontext
        generateSetMuleContextMethod(transformerClass, muleContext);

        // doTransform
        generateDoTransform(transformerClass, variableElement);

        // set and get weight
        generateGetPriorityWeighting(transformerClass, weighting);
        generateSetPriorityWeighting(transformerClass, weighting);

        context.registerAtBoot(transformerClass);
    }

    private void generateSetPriorityWeighting(DefinedClass jaxbTransformerClass, FieldVariable weighting) {
        Method setPriorityWeighting = jaxbTransformerClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "setPriorityWeighting");
        Variable localWeighting = setPriorityWeighting.param(context.getCodeModel().INT, "weighting");
        setPriorityWeighting.body().assign(ExpressionFactory._this().ref(weighting), localWeighting);
    }

    private void generateGetPriorityWeighting(DefinedClass jaxbTransformerClass, FieldVariable weighting) {
        Method getPriorityWeighting = jaxbTransformerClass.method(Modifier.PUBLIC, context.getCodeModel().INT, "getPriorityWeighting");
        getPriorityWeighting.body()._return(weighting);
    }

    private void generateDoTransform(DefinedClass jaxbTransformerClass, Element variableElement) {
        Method doTransform = jaxbTransformerClass.method(Modifier.PROTECTED, ref(Object.class), "doTransform");
        doTransform._throws(TransformerException.class);
        Variable src = doTransform.param(ref(Object.class), "src");
        doTransform.param(ref(String.class), "encoding");

        Variable result = doTransform.body().decl(ref(variableElement.asType()).boxify(), "result", ExpressionFactory._null());

        Invocation valueOf = ref(Enum.class).staticInvoke("valueOf");
        valueOf.arg(ref(variableElement.asType()).boxify().dotclass());
        valueOf.arg(ExpressionFactory.cast(ref(String.class), src));

        doTransform.body().assign(result, valueOf);

        doTransform.body()._return(result);
    }

    private void generateConstructor(DefinedClass transformerClass, Element variableElement) {
        // generate constructor
        Method constructor = transformerClass.constructor(Modifier.PUBLIC);

        // register source data type
        registerSourceTypes(constructor);

        // register destination data type
        registerDestinationType(constructor, ref(variableElement.asType()).boxify());

        constructor.body().invoke("setName").arg(transformerClass.name());
    }

    private void registerDestinationType(Method constructor, TypeReference clazz) {
        Invocation setReturnClass = constructor.body().invoke("setReturnClass");
        setReturnClass.arg(ExpressionFactory.dotclass(clazz));
    }

    private void registerSourceTypes(Method constructor) {
        Invocation registerSourceType = constructor.body().invoke("registerSourceType");
        registerSourceType.arg(ref(DataTypeFactory.class).staticInvoke("create").arg(ref(String.class).boxify().dotclass()));
    }

    private DefinedClass getEnumTransformerClass(Element variableElement) {
        javax.lang.model.element.Element enumElement = context.getTypeUtils().asElement(variableElement.asType());
        String transformerClassName = context.getNameUtils().generateClassNameInPackage(variableElement, enumElement.getSimpleName().toString() + NamingContants.ENUM_TRANSFORMER_CLASS_NAME_SUFFIX);
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(transformerClassName) + NamingContants.TRANSFORMERS_NAMESPACE);
        DefinedClass transformer = pkg._class(context.getNameUtils().getClassName(transformerClassName), AbstractTransformer.class, new Class<?>[]{DiscoverableTransformer.class, MuleContextAware.class});

        return transformer;
    }
}

