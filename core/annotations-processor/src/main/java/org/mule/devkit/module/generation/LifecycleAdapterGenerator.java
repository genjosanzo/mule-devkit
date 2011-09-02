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

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.annotations.lifecycle.Start;
import org.mule.api.annotations.lifecycle.Stop;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;

public class LifecycleAdapterGenerator extends AbstractModuleGenerator {

    public void generate(TypeElement typeElement) throws GenerationException {
        DefinedClass lifecycleAdapter = getLifecycleAdapterClass(typeElement);
        lifecycleAdapter.javadoc().add("A <code>" + lifecycleAdapter.name() + "</code> is a wrapper around ");
        lifecycleAdapter.javadoc().add(ref(typeElement.asType()));
        lifecycleAdapter.javadoc().add(" that adds lifecycle methods to the pojo.");

        ExecutableElement startElement = getStartElement(typeElement);
        lifecycleAdapter._implements(Startable.class);
        Method start = generateLifecycleInvocation(lifecycleAdapter, startElement, "start", DefaultMuleException.class, false);
        start._throws(ref(MuleException.class));

        ExecutableElement stopElement = getStopElement(typeElement);
        lifecycleAdapter._implements(Stoppable.class);
        Method stop = generateLifecycleInvocation(lifecycleAdapter, stopElement, "stop", DefaultMuleException.class, false);
        stop._throws(ref(MuleException.class));

        ExecutableElement postConstructElement = getPostConstructElement(typeElement);
        lifecycleAdapter._implements(Initialisable.class);
        generateLifecycleInvocation(lifecycleAdapter, postConstructElement, "initialise", InitialisationException.class, true);

        ExecutableElement preDestroyElement = getPreDestroyElement(typeElement);
        lifecycleAdapter._implements(Disposable.class);
        generateLifecycleInvocation(lifecycleAdapter, preDestroyElement, "dispose", null, false);
    }

    private DefinedClass getLifecycleAdapterClass(TypeElement typeElement) {
        String lifecycleAdapterName = context.getNameUtils().generateClassName(typeElement, ".config", "LifecycleAdapter");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(lifecycleAdapterName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(lifecycleAdapterName), (TypeReference) ref(typeElement.asType()));

        context.setClassRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement), clazz);

        return clazz;
    }

    private Method generateLifecycleInvocation(DefinedClass lifecycleWrapper, ExecutableElement superExecutableElement, String name, Class<?> catchException, boolean addThis) {
        Method lifecycleMethod = lifecycleWrapper.method(Modifier.PUBLIC, context.getCodeModel().VOID, name);

        if (catchException != null &&
                superExecutableElement != null &&
                superExecutableElement.getThrownTypes() != null &&
                superExecutableElement.getThrownTypes().size() > 0) {
            lifecycleMethod._throws(ref(catchException));
        }

        if (superExecutableElement != null) {

            Invocation startInvocation = ExpressionFactory._super().invoke(superExecutableElement.getSimpleName().toString());

            if (superExecutableElement.getThrownTypes().size() > 0) {
                TryStatement tryBlock = lifecycleMethod.body()._try();
                tryBlock.body().add(startInvocation);

                int i = 0;
                for (TypeMirror exception : superExecutableElement.getThrownTypes()) {
                    CatchBlock catchBlock = tryBlock._catch(ref(exception).boxify());
                    Variable catchedException = catchBlock.param("e" + i);

                    Invocation newMuleException = ExpressionFactory._new(ref(catchException));
                    newMuleException.arg(catchedException);

                    if (addThis) {
                        newMuleException.arg(ExpressionFactory._this());
                    }

                    catchBlock.body().add(newMuleException);
                    i++;
                }
            } else {
                lifecycleMethod.body().add(startInvocation);
            }
        }
        return lifecycleMethod;
    }

    private ExecutableElement getStartElement(TypeElement typeElement) {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(typeElement.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Start start = executableElement.getAnnotation(Start.class);

            if (start != null) {
                return executableElement;
            }
        }

        return null;
    }

    private ExecutableElement getStopElement(TypeElement typeElement) {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(typeElement.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Stop stop = executableElement.getAnnotation(Stop.class);

            if (stop != null) {
                return executableElement;
            }
        }

        return null;
    }

    private ExecutableElement getPostConstructElement(TypeElement typeElement) {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(typeElement.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            PostConstruct postConstruct = executableElement.getAnnotation(PostConstruct.class);

            if (postConstruct != null) {
                return executableElement;
            }
        }

        return null;
    }

    private ExecutableElement getPreDestroyElement(TypeElement typeElement) {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(typeElement.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            PreDestroy preDestroy = executableElement.getAnnotation(PreDestroy.class);

            if (preDestroy != null) {
                return executableElement;
            }
        }

        return null;
    }
}