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
import org.mule.api.MuleException;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Module;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.config.PoolingProfile;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.Variable;
import org.mule.util.pool.DefaultLifecycleEnabledObjectPool;
import org.mule.util.pool.LifecyleEnabledObjectPool;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

public class PoolAdapterGenerator extends AbstractMessageGenerator {
    public void generate(Element element) throws GenerationException {
        Module module = element.getAnnotation(Module.class);
        if (!module.poolable())
            return;

        DefinedClass poolAdapter = getPoolAdapterClass(element);
        poolAdapter.javadoc().add("A <code>" + poolAdapter.name() + "</code> is a wrapper around ");
        poolAdapter.javadoc().add(ref(element.asType()));
        poolAdapter.javadoc().add(" that enables pooling on the POJO.");

        java.util.List<VariableElement> variables = ElementFilter.fieldsIn(element.getEnclosedElements());
        for (VariableElement variable : variables) {
            Configurable configurable = variable.getAnnotation(Configurable.class);

            if (configurable == null)
                continue;

            FieldVariable configField = poolAdapter.field(Modifier.PRIVATE, ref(variable.asType()), variable.getSimpleName().toString());
            generateSetter(poolAdapter, configField);
        }

        FieldVariable muleContext = generateFieldForMuleContext(poolAdapter);
        FieldVariable flowConstruct = generateFieldForFlowConstruct(poolAdapter);
        FieldVariable poolingProfile = poolAdapter.field(Modifier.PROTECTED, ref(PoolingProfile.class), "poolingProfile");
        FieldVariable lifecyleEnabledObjectPool = poolAdapter.field(Modifier.PROTECTED, ref(LifecyleEnabledObjectPool.class), "lifecyleEnabledObjectPool");

        generateSetter(poolAdapter, poolingProfile);
        generateGetter(poolAdapter, poolingProfile);
        generateSetter(poolAdapter, lifecyleEnabledObjectPool);
        generateGetter(poolAdapter, lifecyleEnabledObjectPool);
        generateSetFlowConstructMethod(poolAdapter, flowConstruct);
        generateSetMuleContextMethod(poolAdapter, muleContext);

        generateStartMethod((TypeElement) element, poolAdapter, lifecyleEnabledObjectPool, muleContext, poolingProfile);
        generateStopMethod(poolAdapter, lifecyleEnabledObjectPool);
    }

    private void generateStopMethod(DefinedClass poolAdapter, FieldVariable lifecyleEnabledObjectPool) {
        Method stopMethod = poolAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, "stop");
        stopMethod._throws(MuleException.class);

        Block newBody = stopMethod.body()._if(Op.ne(lifecyleEnabledObjectPool, ExpressionFactory._null()))._then();
        newBody.add(lifecyleEnabledObjectPool.invoke("stop"));
        newBody.add(lifecyleEnabledObjectPool.invoke("close"));
        newBody.assign(lifecyleEnabledObjectPool, ExpressionFactory._null());
    }

    private void generateStartMethod(TypeElement element, DefinedClass poolAdapter, FieldVariable lifecyleEnabledObjectPool, FieldVariable muleContext, FieldVariable poolingProfile) {
        DefinedClass objectFactory = context.getClassForRole(context.getNameUtils().generatePojoFactoryKey(element));

        Method startMethod = poolAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, "start");
        startMethod._throws(MuleException.class);

        Variable objectFactoryField = startMethod.body().decl(objectFactory, "objectFactory", ExpressionFactory._new(objectFactory));
        java.util.List<VariableElement> variables = ElementFilter.fieldsIn(element.getEnclosedElements());
        for (VariableElement variable : variables) {
            Configurable configurable = variable.getAnnotation(Configurable.class);

            if (configurable == null)
                continue;

            startMethod.body().add(objectFactoryField.invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(ExpressionFactory._this().ref(variable.getSimpleName().toString())));
        }

        Invocation defaultLifecycleEnabledObjectPool = ExpressionFactory._new(ref(DefaultLifecycleEnabledObjectPool.class));
        defaultLifecycleEnabledObjectPool.arg(objectFactoryField);
        defaultLifecycleEnabledObjectPool.arg(poolingProfile);
        defaultLifecycleEnabledObjectPool.arg(muleContext);
        startMethod.body().assign(lifecyleEnabledObjectPool, defaultLifecycleEnabledObjectPool);

        startMethod.body().add(lifecyleEnabledObjectPool.invoke("initialise"));
        startMethod.body().add(lifecyleEnabledObjectPool.invoke("start"));
    }

    private DefinedClass getPoolAdapterClass(Element typeElement) {
        String poolAdapterName = context.getNameUtils().generateClassName((TypeElement) typeElement, ".config", "PoolAdapter");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(poolAdapterName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(poolAdapterName));
        clazz._implements(Startable.class);
        clazz._implements(Stoppable.class);
        clazz._implements(MuleContextAware.class);
        clazz._implements(FlowConstructAware.class);
        context.setClassRole(context.getNameUtils().generateModuleObjectRoleKey((TypeElement) typeElement), clazz);

        return clazz;
    }
}