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

import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.context.MuleContextAware;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.CatchBlock;
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
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.HashSet;
import java.util.Set;

public class EnumTransformerGenerator extends AbstractMessageGenerator {

    public void generate(Element type) throws GenerationException {
        Set<TypeMirror> registeredEnums = new HashSet<TypeMirror>();

        java.util.List<VariableElement> variables = ElementFilter.fieldsIn(type.getEnclosedElements());
        for (VariableElement variable : variables) {
            if (context.getTypeMirrorUtils().isEnum(variable.asType())) {
                if (!registeredEnums.contains(variable.asType())) {
                    registerEnumTransformer(type, variable);
                    registeredEnums.add(variable.asType());
                }
            }
        }

        java.util.List<ExecutableElement> methods = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement method : methods) {
            Processor processor = method.getAnnotation(Processor.class);
            if (processor == null)
                continue;

            for (VariableElement variable : method.getParameters()) {
                if (!context.getTypeMirrorUtils().isEnum(variable.asType()))
                    continue;

                if (!registeredEnums.contains(variable.asType())) {
                    registerEnumTransformer(type, variable);
                    registeredEnums.add(variable.asType());
                }
            }
        }

        for (ExecutableElement method : methods) {
            Source source = method.getAnnotation(Source.class);
            if (source == null)
                continue;

            for (VariableElement variable : method.getParameters()) {
                if (!context.getTypeMirrorUtils().isEnum(variable.asType()))
                    continue;

                if (!registeredEnums.contains(variable.asType())) {
                    registerEnumTransformer(type, variable);
                    registeredEnums.add(variable.asType());
                }
            }
        }
    }

    private void registerEnumTransformer(Element type, VariableElement variableElement) {
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

    private void generateDoTransform(DefinedClass jaxbTransformerClass, VariableElement variableElement) {
        Method doTransform = jaxbTransformerClass.method(Modifier.PROTECTED, ref(Object.class), "doTransform");
        doTransform._throws(TransformerException.class);
        Variable src = doTransform.param(ref(Object.class), "src");
        Variable encoding = doTransform.param(ref(String.class), "encoding");

        Variable result = doTransform.body().decl(ref(variableElement.asType()).boxify(), "result", ExpressionFactory._null());

        Invocation valueOf = ref(Enum.class).staticInvoke("valueOf");
        valueOf.arg(ref(variableElement.asType()).boxify().dotclass());
        valueOf.arg(ExpressionFactory.cast(ref(String.class), src));

        doTransform.body().assign(result, valueOf);

        doTransform.body()._return(result);
    }

    private void generateThrowTransformFailedException(CatchBlock catchBlock, Variable exception, Variable src, TypeReference target) {
        Invocation transformFailedInvoke = ref(CoreMessages.class).staticInvoke("transformFailed");
        transformFailedInvoke.arg(src.invoke("getClass").invoke("getName"));
        transformFailedInvoke.arg(ExpressionFactory.lit(target.fullName()));

        Invocation transformerException = ExpressionFactory._new(ref(TransformerException.class));
        transformerException.arg(transformFailedInvoke);
        transformerException.arg(ExpressionFactory._this());
        transformerException.arg(exception);
        catchBlock.body()._throw(transformerException);
    }

    private void generateConstructor(DefinedClass transformerClass, VariableElement variableElement) {
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

    private DefinedClass getEnumTransformerClass(VariableElement variableElement) {
        javax.lang.model.element.Element enumElement = context.getTypeUtils().asElement(variableElement.asType());
        String transformerClassName = context.getNameUtils().generateClassNameInPackage(variableElement, enumElement.getSimpleName().toString() + "EnumTransformer");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(transformerClassName) + ".config");
        DefinedClass transformer = pkg._class(context.getNameUtils().getClassName(transformerClassName), AbstractTransformer.class, new Class<?>[]{DiscoverableTransformer.class, MuleContextAware.class});

        return transformer;
    }
}

