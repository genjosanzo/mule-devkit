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

import org.apache.commons.lang.StringUtils;
import org.mule.api.Capabilities;
import org.mule.api.MuleException;
import org.mule.api.annotations.Configurable;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.config.PoolingProfile;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.generation.NamingContants;
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

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public class PoolAdapterGenerator extends AbstractMessageGenerator {

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return typeElement.isPoolable();
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) throws GenerationException {
        DefinedClass poolAdapter = getPoolAdapterClass(typeElement);
        poolAdapter.javadoc().add("A <code>" + poolAdapter.name() + "</code> is a wrapper around ");
        poolAdapter.javadoc().add(ref(typeElement.asType()));
        poolAdapter.javadoc().add(" that enables pooling on the POJO.");

        for (VariableElement field : typeElement.getFieldsAnnotatedWith(Configurable.class)) {
            FieldVariable configField = poolAdapter.field(Modifier.PRIVATE, ref(field.asType()), field.getSimpleName().toString());
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

        generateStartMethod(typeElement, poolAdapter, lifecyleEnabledObjectPool, muleContext, poolingProfile);
        generateStopMethod(poolAdapter, lifecyleEnabledObjectPool);
        generateIsCapableOf(typeElement, poolAdapter);
    }

    private void generateStopMethod(DefinedClass poolAdapter, FieldVariable lifecyleEnabledObjectPool) {
        Method stopMethod = poolAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, "stop");
        stopMethod._throws(MuleException.class);

        Block newBody = stopMethod.body()._if(Op.ne(lifecyleEnabledObjectPool, ExpressionFactory._null()))._then();
        newBody.add(lifecyleEnabledObjectPool.invoke("stop"));
        newBody.add(lifecyleEnabledObjectPool.invoke("close"));
        newBody.assign(lifecyleEnabledObjectPool, ExpressionFactory._null());
    }

    private void generateStartMethod(DevKitTypeElement typeElement, DefinedClass poolAdapter, FieldVariable lifecyleEnabledObjectPool, FieldVariable muleContext, FieldVariable poolingProfile) {
        DefinedClass objectFactory = context.getClassForRole(context.getNameUtils().generatePojoFactoryKey(typeElement));

        Method startMethod = poolAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, "start");
        startMethod._throws(MuleException.class);

        Variable objectFactoryField = startMethod.body().decl(objectFactory, "objectFactory", ExpressionFactory._new(objectFactory));
        for (VariableElement field : typeElement.getFieldsAnnotatedWith(Configurable.class)) {
            startMethod.body().add(objectFactoryField.invoke("set" + StringUtils.capitalize(field.getSimpleName().toString())).arg(ExpressionFactory._this().ref(field.getSimpleName().toString())));
        }

        Invocation defaultLifecycleEnabledObjectPool = ExpressionFactory._new(ref(DefaultLifecycleEnabledObjectPool.class));
        defaultLifecycleEnabledObjectPool.arg(objectFactoryField);
        defaultLifecycleEnabledObjectPool.arg(poolingProfile);
        defaultLifecycleEnabledObjectPool.arg(muleContext);
        startMethod.body().assign(lifecyleEnabledObjectPool, defaultLifecycleEnabledObjectPool);

        startMethod.body().add(lifecyleEnabledObjectPool.invoke("initialise"));
        startMethod.body().add(lifecyleEnabledObjectPool.invoke("start"));
    }

    private DefinedClass getPoolAdapterClass(TypeElement typeElement) {
        String poolAdapterName = context.getNameUtils().generateClassName(typeElement, NamingContants.ADAPTERS_NAMESPACE, NamingContants.POOL_ADAPTER_CLASS_NAME_SUFFIX);
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(poolAdapterName));

        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(poolAdapterName));
        clazz._implements(Startable.class);
        clazz._implements(Stoppable.class);
        clazz._implements(MuleContextAware.class);
        clazz._implements(FlowConstructAware.class);
        clazz._implements(Capabilities.class);
        context.setClassRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement), clazz);

        return clazz;
    }
}