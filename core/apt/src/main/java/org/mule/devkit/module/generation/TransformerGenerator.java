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

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.annotations.Transformer;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.JCatchBlock;
import org.mule.devkit.model.code.JClass;
import org.mule.devkit.model.code.JConditional;
import org.mule.devkit.model.code.JExpr;
import org.mule.devkit.model.code.JFieldVar;
import org.mule.devkit.model.code.JInvocation;
import org.mule.devkit.model.code.JMethod;
import org.mule.devkit.model.code.JMod;
import org.mule.devkit.model.code.JOp;
import org.mule.devkit.model.code.JPackage;
import org.mule.devkit.model.code.JTryBlock;
import org.mule.devkit.model.code.JVar;
import org.mule.transformer.types.DataTypeFactory;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.Map;

public class TransformerGenerator extends AbstractModuleGenerator {

    public void generate(Element type) throws GenerationException {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Transformer transformer = executableElement.getAnnotation(Transformer.class);

            if (transformer == null)
                continue;

            // get class
            DefinedClass transformerClass = getTransformerClass(executableElement);

            // declare object
            JFieldVar object = transformerClass.field(JMod.PRIVATE, ref(executableElement.getEnclosingElement().asType()), "object");
            JFieldVar muleContext = transformerClass.field(JMod.PRIVATE, ref(MuleContext.class), "muleContext");

            // declare weight
            JFieldVar weighting = transformerClass.field(JMod.PRIVATE, context.getCodeModel().INT, "weighting", JOp.plus(ref(DiscoverableTransformer.class).staticRef("DEFAULT_PRIORITY_WEIGHTING"), JExpr.lit(transformer.priorityWeighting())));

            //generate constructor
            generateConstructor(transformerClass, executableElement);

            // generate initialise
            generateInitialiseMethod(transformerClass, ref(executableElement.getEnclosingElement().asType()).boxify(), muleContext, object);

            // add setmulecontext
            generateSetMuleContextMethod(transformerClass, muleContext);

            // add setobject
            generateSetObjectMethod(transformerClass, object);

            // doTransform
            generateDoTransform(transformerClass, executableElement, object);

            // set and get weight
            generateGetPriorityWeighting(transformerClass, weighting);
            generateSetPriorityWeighting(transformerClass, weighting);

            context.registerAtBoot(transformerClass);
        }

    }

    private void generateSetPriorityWeighting(DefinedClass jaxbTransformerClass, JFieldVar weighting) {
        JMethod setPriorityWeighting = jaxbTransformerClass.method(JMod.PUBLIC, context.getCodeModel().VOID, "setPriorityWeighting");
        JVar localWeighting = setPriorityWeighting.param(context.getCodeModel().INT, "weighting");
        setPriorityWeighting.body().assign(JExpr._this().ref(weighting), localWeighting);
    }

    private void generateGetPriorityWeighting(DefinedClass jaxbTransformerClass, JFieldVar weighting) {
        JMethod getPriorityWeighting = jaxbTransformerClass.method(JMod.PUBLIC, context.getCodeModel().INT, "getPriorityWeighting");
        getPriorityWeighting.body()._return(weighting);
    }

    private void generateDoTransform(DefinedClass jaxbTransformerClass, ExecutableElement executableElement, JFieldVar object) {
        JMethod doTransform = jaxbTransformerClass.method(JMod.PROTECTED, ref(Object.class), "doTransform");
        doTransform._throws(TransformerException.class);
        JVar src = doTransform.param(ref(Object.class), "src");
        JVar encoding = doTransform.param(ref(String.class), "encoding");

        JVar result = doTransform.body().decl(ref(executableElement.getReturnType()).boxify(), "result", JExpr._null());

        JTryBlock tryBlock = doTransform.body()._try();

        // do something
        JInvocation invoke = object.invoke(executableElement.getSimpleName().toString());
        invoke.arg(src);
        tryBlock.body().assign(result, invoke);

        JCatchBlock exceptionCatch = tryBlock._catch(ref(Exception.class));
        JVar exception = exceptionCatch.param("exception");

        generateThrowTransformFailedException(exceptionCatch, exception, src, ref(executableElement.getReturnType()).boxify());

        doTransform.body()._return(result);
    }

    private void generateThrowTransformFailedException(JCatchBlock catchBlock, JVar exception, JVar src, JClass target) {
        JInvocation transformFailedInvoke = ref(CoreMessages.class).staticInvoke("transformFailed");
        transformFailedInvoke.arg(src.invoke("getClass").invoke("getName"));
        transformFailedInvoke.arg(JExpr.lit(target.fullName()));

        JInvocation transformerException = JExpr._new(ref(TransformerException.class));
        transformerException.arg(transformFailedInvoke);
        transformerException.arg(JExpr._this());
        transformerException.arg(exception);
        catchBlock.body()._throw(transformerException);
    }

    private void generateConstructor(DefinedClass transformerClass, ExecutableElement executableElement) {
        // generate constructor
        JMethod constructor = transformerClass.constructor(JMod.PUBLIC);

        // register source data type
        registerSourceTypes(constructor, executableElement);

        // register destination data type
        registerDestinationType(constructor, ref(executableElement.getReturnType()).boxify());

        constructor.body().invoke("setName").arg(context.getNameUtils().generateClassName(executableElement, "Transformer"));
    }

    private void registerDestinationType(JMethod constructor, JClass clazz) {
        JInvocation setReturnClass = constructor.body().invoke("setReturnClass");
        setReturnClass.arg(JExpr.dotclass(clazz));
    }

    private void registerSourceTypes(JMethod constructor, ExecutableElement executableElement) {
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

        for (AnnotationValue sourceType : sourceTypes) {
            JInvocation registerSourceType = constructor.body().invoke("registerSourceType");
            registerSourceType.arg(ref(DataTypeFactory.class).staticInvoke("create").arg(ref((TypeMirror) sourceType.getValue()).boxify().dotclass()));
        }
    }

    private DefinedClass getTransformerClass(ExecutableElement executableElement) {
        String transformerClassName = context.getNameUtils().generateClassName(executableElement, "Transformer");
        JPackage pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(transformerClassName));
        DefinedClass transformer = pkg._class(context.getNameUtils().getClassName(transformerClassName), new Class<?>[] {DiscoverableTransformer.class, MuleContextAware.class, Initialisable.class});

        return transformer;
    }

    private JMethod generateSetObjectMethod(DefinedClass messageProcessorClass, JFieldVar object) {
        JMethod setObject = messageProcessorClass.method(JMod.PUBLIC, context.getCodeModel().VOID, "setObject");
        JVar objectParam = setObject.param(object.type(), "object");
        setObject.body().assign(JExpr._this().ref(object), objectParam);

        return setObject;
    }


    private JMethod generateSetMuleContextMethod(DefinedClass messageProcessorClass, JFieldVar muleContext) {
        JMethod setMuleContext = messageProcessorClass.method(JMod.PUBLIC, context.getCodeModel().VOID, "setMuleContext");
        JVar muleContextParam = setMuleContext.param(ref(MuleContext.class), "context");
        setMuleContext.body().assign(JExpr._this().ref(muleContext), muleContextParam);

        return setMuleContext;
    }

    private JMethod generateInitialiseMethod(DefinedClass messageProcessorClass, JClass messageProcessor, JFieldVar muleContext, JFieldVar object) {
        JMethod initialise = messageProcessorClass.method(JMod.PUBLIC, context.getCodeModel().VOID, "initialise");
        initialise._throws(InitialisationException.class);

        JConditional ifNoObject = initialise.body()._if(JOp.eq(object, JExpr._null()));
        JTryBlock tryLookUp = ifNoObject._then()._try();
        tryLookUp.body().assign(object, muleContext.invoke("getRegistry").invoke("lookupObject").arg(JExpr.dotclass(messageProcessor)));
        JCatchBlock catchBlock = tryLookUp._catch(ref(RegistrationException.class));
        JVar exception = catchBlock.param("e");
        JClass coreMessages = ref(CoreMessages.class);
        JInvocation failedToInvoke = coreMessages.staticInvoke("initialisationFailure");
        failedToInvoke.arg(messageProcessor.fullName());
        JInvocation messageException = JExpr._new(ref(InitialisationException.class));
        messageException.arg(failedToInvoke);
        messageException.arg(exception);
        messageException.arg(JExpr._this());
        catchBlock.body()._throw(messageException);

        return initialise;
    }
}
