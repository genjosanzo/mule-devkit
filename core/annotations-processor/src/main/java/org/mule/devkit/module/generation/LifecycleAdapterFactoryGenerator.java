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
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Module;
import org.mule.api.lifecycle.InitialisationCallback;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.object.ObjectFactory;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Variable;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

public class LifecycleAdapterFactoryGenerator extends AbstractModuleGenerator {
    public void generate(Element element) throws GenerationException {
        Module module = element.getAnnotation(Module.class);
        if (!module.poolable())
            return;

        DefinedClass lifecycleAdapterFactory = getLifecycleAdapterFactoryClass(element);
        lifecycleAdapterFactory.javadoc().add("A <code>" + lifecycleAdapterFactory.name() + "</code> is an implementation  ");
        lifecycleAdapterFactory.javadoc().add(" of {@link ObjectFactory} interface for ");
        lifecycleAdapterFactory.javadoc().add(ref(element.asType()));

        DefinedClass poolObjectClass = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey((TypeElement) element));
        context.setClassRole(context.getNameUtils().generatePoolObjectRoleKey((TypeElement) element), poolObjectClass);

        generateFields(element, lifecycleAdapterFactory);

        generateInitialiseMethod(lifecycleAdapterFactory);
        generateDisposeMethod(lifecycleAdapterFactory);
        generateGetInstanceMethod(element, lifecycleAdapterFactory, poolObjectClass);
        generateGetObjectClassMethod(lifecycleAdapterFactory, poolObjectClass);
        generateAddObjectInitialisationCallback(lifecycleAdapterFactory);
        generateIsSingleton(lifecycleAdapterFactory);
        generateIsAutoWireObject(lifecycleAdapterFactory);
        generateIsExternallyManagedLifecycle(lifecycleAdapterFactory);
    }

    private void generateIsExternallyManagedLifecycle(DefinedClass lifecycleAdapterFactory) {
        Method isExternallyManagedLifecycle = lifecycleAdapterFactory.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, "isExternallyManagedLifecycle");
        isExternallyManagedLifecycle.body()._return(ExpressionFactory.FALSE);
    }

    private void generateIsAutoWireObject(DefinedClass lifecycleAdapterFactory) {
        Method isAutoWireObject = lifecycleAdapterFactory.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, "isAutoWireObject");
        isAutoWireObject.body()._return(ExpressionFactory.FALSE);
    }

    private void generateIsSingleton(DefinedClass lifecycleAdapterFactory) {
        Method isSingleton = lifecycleAdapterFactory.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, "isSingleton");
        isSingleton.body()._return(ExpressionFactory.FALSE);
    }

    private void generateAddObjectInitialisationCallback(DefinedClass lifecycleAdapterFactory) {
        Method addObjectInitialisationCallback = lifecycleAdapterFactory.method(Modifier.PUBLIC, context.getCodeModel().VOID, "addObjectInitialisationCallback");
        addObjectInitialisationCallback.param(ref(InitialisationCallback.class), "callback");
        addObjectInitialisationCallback.body()._throw(ExpressionFactory._new(ref(UnsupportedOperationException.class)));
    }

    private void generateGetObjectClassMethod(DefinedClass lifecycleAdapterFactory, DefinedClass poolObjectClass) {
        Method getObjectClass = lifecycleAdapterFactory.method(Modifier.PUBLIC, ref(Class.class), "getObjectClass");
        getObjectClass.body()._return(poolObjectClass.dotclass());
    }

    private void generateGetInstanceMethod(Element element, DefinedClass lifecycleAdapterFactory, DefinedClass poolObjectClass) {
        Method getInstance = lifecycleAdapterFactory.method(Modifier.PUBLIC, ref(Object.class), "getInstance");
        getInstance.param(ref(MuleContext.class), "muleContext");

        Variable object = getInstance.body().decl(poolObjectClass, "object", ExpressionFactory._new(poolObjectClass));
        java.util.List<VariableElement> variables = ElementFilter.fieldsIn(element.getEnclosedElements());
        for (VariableElement variable : variables) {
            Configurable configurable = variable.getAnnotation(Configurable.class);

            if (configurable == null)
                continue;

            getInstance.body().add(object.invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(ExpressionFactory._this().ref(variable.getSimpleName().toString())));
        }

        getInstance.body()._return(object);
    }

    private void generateDisposeMethod(DefinedClass lifecycleAdapterFactory) {
        Method dispose = lifecycleAdapterFactory.method(Modifier.PUBLIC, context.getCodeModel().VOID, "dispose");
    }

    private void generateInitialiseMethod(DefinedClass lifecycleAdapterFactory) {
        Method initialise = lifecycleAdapterFactory.method(Modifier.PUBLIC, context.getCodeModel().VOID, "initialise");
        initialise._throws(ref(InitialisationException.class));
    }

    private void generateFields(Element element, DefinedClass lifecycleAdapterFactory) {
        java.util.List<VariableElement> variables = ElementFilter.fieldsIn(element.getEnclosedElements());
        for (VariableElement variable : variables) {
            Configurable configurable = variable.getAnnotation(Configurable.class);

            if (configurable == null)
                continue;

            FieldVariable configField = lifecycleAdapterFactory.field(Modifier.PRIVATE, ref(variable.asType()), variable.getSimpleName().toString());
            generateSetter(lifecycleAdapterFactory, configField);
        }
    }

    private DefinedClass getLifecycleAdapterFactoryClass(Element typeElement) {
        String lifecycleAdapterName = context.getNameUtils().generateClassName((TypeElement) typeElement, ".config", "LifecycleAdapterFactory");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(lifecycleAdapterName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(lifecycleAdapterName));
        clazz._implements(ref(ObjectFactory.class));

        context.setClassRole(context.getNameUtils().generatePojoFactoryKey((TypeElement) typeElement), clazz);

        return clazz;
    }
}
