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

package org.mule.devkit.generation.adapter;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.annotations.lifecycle.Start;
import org.mule.api.annotations.lifecycle.Stop;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.DevkitTypeElement;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Variable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class LifecycleAdapterGenerator extends AbstractModuleGenerator {

    @Override
    protected boolean shouldGenerate(DevkitTypeElement typeElement) {
        return true;
    }

    @Override
    protected void doGenerate(DevkitTypeElement typeElement) {
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

        DefinedClass previous = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));

        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(lifecycleAdapterName), previous);

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

    private ExecutableElement getStartElement(DevkitTypeElement typeElement) {
        List<ExecutableElement> startMethods = typeElement.getMethodsAnnotatedWith(Start.class);
        return !startMethods.isEmpty() ? startMethods.get(0) : null;
    }

    private ExecutableElement getStopElement(DevkitTypeElement typeElement) {
        List<ExecutableElement> stopMethods = typeElement.getMethodsAnnotatedWith(Stop.class);
        return !stopMethods.isEmpty() ? stopMethods.get(0) : null;
    }

    private ExecutableElement getPostConstructElement(DevkitTypeElement typeElement) {
        List<ExecutableElement> postConstructMethods = typeElement.getMethodsAnnotatedWith(PostConstruct.class);
        return !postConstructMethods.isEmpty() ? postConstructMethods.get(0) : null;
    }

    private ExecutableElement getPreDestroyElement(DevkitTypeElement typeElement) {
        List<ExecutableElement> preDestroyMethods = typeElement.getMethodsAnnotatedWith(PreDestroy.class);
        return !preDestroyMethods.isEmpty() ? preDestroyMethods.get(0) : null;
    }
}