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
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.lifecycle.Start;
import org.mule.api.annotations.lifecycle.Stop;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.config.MuleManifest;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.NamingContants;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.ForLoop;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class LifecycleAdapterGenerator extends AbstractModuleGenerator {

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return typeElement.hasAnnotation(Module.class) || typeElement.hasAnnotation(Connector.class);
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {
        DefinedClass lifecycleAdapter = getLifecycleAdapterClass(typeElement);
        lifecycleAdapter.javadoc().add("A <code>" + lifecycleAdapter.name() + "</code> is a wrapper around ");
        lifecycleAdapter.javadoc().add(ref(typeElement.asType()));
        lifecycleAdapter.javadoc().add(" that adds lifecycle methods to the pojo.");

        ExecutableElement startElement = getStartElement(typeElement);
        lifecycleAdapter._implements(Startable.class);
        Method start = generateLifecycleInvocation(lifecycleAdapter, typeElement, startElement, "start", DefaultMuleException.class, false);
        start._throws(ref(MuleException.class));

        ExecutableElement stopElement = getStopElement(typeElement);
        lifecycleAdapter._implements(Stoppable.class);
        Method stop = generateLifecycleInvocation(lifecycleAdapter, typeElement, stopElement, "stop", DefaultMuleException.class, false);
        stop._throws(ref(MuleException.class));

        ExecutableElement postConstructElement = getPostConstructElement(typeElement);
        lifecycleAdapter._implements(Initialisable.class);
        generateLifecycleInvocation(lifecycleAdapter, typeElement, postConstructElement, "initialise", InitialisationException.class, true);

        ExecutableElement preDestroyElement = getPreDestroyElement(typeElement);
        lifecycleAdapter._implements(Disposable.class);
        generateLifecycleInvocation(lifecycleAdapter, typeElement, preDestroyElement, "dispose", null, false);
    }

    private DefinedClass getLifecycleAdapterClass(TypeElement typeElement) {
        String lifecycleAdapterName = context.getNameUtils().generateClassName(typeElement, NamingContants.ADAPTERS_NAMESPACE, NamingContants.LIFECYCLE_ADAPTER_CLASS_NAME_SUFFIX);
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(lifecycleAdapterName));

        DefinedClass previous = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));

        int modifiers = Modifier.PUBLIC;
        if( typeElement.getModifiers().contains(javax.lang.model.element.Modifier.ABSTRACT) ) {
            modifiers |= Modifier.ABSTRACT;
        }

        DefinedClass clazz = pkg._class(modifiers, context.getNameUtils().getClassName(lifecycleAdapterName), previous);

        context.setClassRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement), clazz);

        return clazz;
    }

    private Method generateLifecycleInvocation(DefinedClass lifecycleWrapper, DevKitTypeElement typeElement, ExecutableElement superExecutableElement, String name, Class<?> catchException, boolean addThis) {
        Method lifecycleMethod = lifecycleWrapper.method(Modifier.PUBLIC, context.getCodeModel().VOID, name);

        if (name.equals("initialise")) {
            Variable log = lifecycleMethod.body().decl(ref(Logger.class), "log", ref(LoggerFactory.class).staticInvoke("getLogger").arg(lifecycleWrapper.dotclass()));
            Variable runtimeVersion = lifecycleMethod.body().decl(ref(String.class), "runtimeVersion", ref(MuleManifest.class).staticInvoke("getProductVersion"));
            Conditional ifUnkownVersion = lifecycleMethod.body()._if(runtimeVersion.invoke("equals").arg("Unknown"));
            ifUnkownVersion._then().add(log.invoke("warn").arg(ExpressionFactory.lit("Unknown Mule runtime version. This module may not work properly!")));
            Block ifKnownVersion = ifUnkownVersion._else();

            Variable expectedMinVersion = ifKnownVersion.decl(ref(String[].class), "expectedMinVersion", ExpressionFactory.lit(typeElement.minMuleVersion()).invoke("split").arg("\\."));

            Block ifKnownVersionContainsDash = ifKnownVersion._if(ExpressionFactory.invoke(runtimeVersion, "contains").arg("-"))._then();
            ifKnownVersionContainsDash.assign(runtimeVersion, runtimeVersion.invoke("split").arg("-").component(ExpressionFactory.lit(0)));

            Variable currentRuntimeVersion = ifKnownVersion.decl(ref(String[].class), "currentRuntimeVersion", runtimeVersion.invoke("split").arg("\\."));

            ForLoop forEachVersionComponent = ifKnownVersion._for();
            Variable i = forEachVersionComponent.init(context.getCodeModel().INT, "i", ExpressionFactory.lit(0));
            forEachVersionComponent.test(Op.lt(i, expectedMinVersion.ref("length")));
            forEachVersionComponent.update(Op.incr(i));

            TryStatement tryToParseMuleVersion = forEachVersionComponent.body()._try();
            tryToParseMuleVersion.body()._if(Op.lt(
                    ref(Integer.class).staticInvoke("parseInt").arg(currentRuntimeVersion.component(i)),
                    ref(Integer.class).staticInvoke("parseInt").arg(expectedMinVersion.component(i))))._then()
                    ._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("This module is only valid for Mule " + typeElement.minMuleVersion()));
            CatchBlock catchBlock = tryToParseMuleVersion._catch(ref(NumberFormatException.class));
            catchBlock.param("nfe");
            catchBlock.body().invoke(log, "warn").arg("Error parsing Mule version, cannot validate current Mule version");
        }

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

    private ExecutableElement getStartElement(DevKitTypeElement typeElement) {
        List<ExecutableElement> startMethods = typeElement.getMethodsAnnotatedWith(Start.class);
        return !startMethods.isEmpty() ? startMethods.get(0) : null;
    }

    private ExecutableElement getStopElement(DevKitTypeElement typeElement) {
        List<ExecutableElement> stopMethods = typeElement.getMethodsAnnotatedWith(Stop.class);
        return !stopMethods.isEmpty() ? stopMethods.get(0) : null;
    }

    private ExecutableElement getPostConstructElement(DevKitTypeElement typeElement) {
        List<ExecutableElement> postConstructMethods = typeElement.getMethodsAnnotatedWith(PostConstruct.class);
        return !postConstructMethods.isEmpty() ? postConstructMethods.get(0) : null;
    }

    private ExecutableElement getPreDestroyElement(DevKitTypeElement typeElement) {
        List<ExecutableElement> preDestroyMethods = typeElement.getMethodsAnnotatedWith(PreDestroy.class);
        return !preDestroyMethods.isEmpty() ? preDestroyMethods.get(0) : null;
    }
}