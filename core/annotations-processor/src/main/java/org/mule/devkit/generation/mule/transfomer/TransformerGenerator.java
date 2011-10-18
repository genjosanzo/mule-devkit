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

import org.mule.api.annotations.Transformer;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.DefinedClass;
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
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map;

public class TransformerGenerator extends AbstractMessageGenerator {

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return true;
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) throws GenerationException {
        for (ExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Transformer.class)) {

            // get class
            DefinedClass transformerClass = getTransformerClass(executableElement);

            // declare weight
            Transformer transformer = executableElement.getAnnotation(Transformer.class);
            FieldVariable weighting = transformerClass.field(Modifier.PRIVATE, context.getCodeModel().INT, "weighting", Op.plus(ref(DiscoverableTransformer.class).staticRef("DEFAULT_PRIORITY_WEIGHTING"), ExpressionFactory.lit(transformer.priorityWeighting())));

            //generate constructor
            generateConstructor(transformerClass, executableElement);

            // doTransform
            generateDoTransform(transformerClass, executableElement);

            // set and get weight
            generateGetPriorityWeighting(transformerClass, weighting);
            generateSetPriorityWeighting(transformerClass, weighting);

            context.registerAtBoot(transformerClass);
        }

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

    private void generateDoTransform(DefinedClass transformerClass, ExecutableElement executableElement) {
        Method doTransform = transformerClass.method(Modifier.PROTECTED, ref(Object.class), "doTransform");
        doTransform._throws(TransformerException.class);
        Variable src = doTransform.param(ref(Object.class), "src");
        doTransform.param(ref(String.class), "encoding");

        Variable result = doTransform.body().decl(ref(executableElement.getReturnType()).boxify(), "result", ExpressionFactory._null());

        TryStatement tryBlock = doTransform.body()._try();

        // do something
        Invocation invoke = ref(executableElement.getEnclosingElement().asType()).boxify().staticInvoke(executableElement.getSimpleName().toString());

        TypeMirror expectedType = executableElement.getParameters().get(0).asType();

        invoke.arg(ExpressionFactory.cast(ref(expectedType), src));
        tryBlock.body().assign(result, invoke);

        CatchBlock exceptionCatch = tryBlock._catch(ref(Exception.class));
        Variable exception = exceptionCatch.param("exception");

        generateThrowTransformFailedException(exceptionCatch, exception, src, ref(executableElement.getReturnType()).boxify());

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

    private void generateConstructor(DefinedClass transformerClass, ExecutableElement executableElement) {
        // generate constructor
        Method constructor = transformerClass.constructor(Modifier.PUBLIC);

        // register source data type
        registerSourceTypes(constructor, executableElement);

        // register destination data type
        registerDestinationType(constructor, ref(executableElement.getReturnType()).boxify());

        constructor.body().invoke("setName").arg(context.getNameUtils().generateClassName(executableElement, "Transformer"));
    }

    private void registerDestinationType(Method constructor, TypeReference clazz) {
        Invocation setReturnClass = constructor.body().invoke("setReturnClass");
        setReturnClass.arg(ExpressionFactory.dotclass(clazz));
    }

    private void registerSourceTypes(Method constructor, ExecutableElement executableElement) {
        final String transformerAnnotationName = Transformer.class.getName();
        List<? extends AnnotationValue> sourceTypes = null;
        List<? extends AnnotationMirror> annotationMirrors = executableElement.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            if (transformerAnnotationName.equals(annotationMirror.getAnnotationType().toString())) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
                    if ("sourceTypes".equals(
                            entry.getKey().getSimpleName().toString())) {
                        sourceTypes = (List<? extends AnnotationValue>) entry.getValue().getValue();
                        break;
                    }
                }
            }
        }

        Invocation registerSourceType = constructor.body().invoke("registerSourceType");
        registerSourceType.arg(ref(DataTypeFactory.class).staticInvoke("create").arg(ref(executableElement.getParameters().get(0).asType()).boxify().dotclass()));

        if (sourceTypes != null) {
            for (AnnotationValue sourceType : sourceTypes) {
                registerSourceType = constructor.body().invoke("registerSourceType");
                registerSourceType.arg(ref(DataTypeFactory.class).staticInvoke("create").arg(ref((TypeMirror) sourceType.getValue()).boxify().dotclass()));
            }
        }
    }

    public DefinedClass getTransformerClass(ExecutableElement executableElement) {
        String transformerClassName = context.getNameUtils().generateClassName(executableElement, "Transformer");
        Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(transformerClassName) + ".config");
        DefinedClass transformer = pkg._class(context.getNameUtils().getClassName(transformerClassName), AbstractTransformer.class, new Class<?>[]{DiscoverableTransformer.class});

        return transformer;
    }
}