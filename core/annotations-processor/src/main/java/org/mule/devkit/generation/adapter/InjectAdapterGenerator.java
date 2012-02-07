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
import org.mule.api.MuleContext;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.registry.Registry;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreManager;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.generation.NamingContants;
import org.mule.devkit.model.code.Cast;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.mule.util.queue.QueueManager;

import javax.inject.Inject;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.transaction.TransactionManager;

public class InjectAdapterGenerator extends AbstractModuleGenerator {

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return typeElement.getFieldsAnnotatedWith(Inject.class).size() > 0;
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) throws GenerationException {
        DefinedClass adapter = getMuleContextAwareAdapter(typeElement);

        Method setMuleContext = adapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, "setMuleContext");
        setMuleContext.annotate(Override.class);
        Variable context = setMuleContext.param(ref(MuleContext.class), "context");

        for (VariableElement variable : typeElement.getFieldsAnnotatedWith(Inject.class)) {
            if (variable.asType().toString().startsWith(MuleContext.class.getName())) {
                setMuleContext.body().add(ExpressionFactory._super().invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(context));
            } else if (variable.asType().toString().startsWith(ObjectStoreManager.class.getName())) {
                Cast getObjectStoreManager = ExpressionFactory.cast(ref(ObjectStoreManager.class), context.invoke("getRegistry").invoke("get").arg(ref(MuleProperties.class).staticRef("OBJECT_STORE_MANAGER")));
                setMuleContext.body().add(ExpressionFactory._super().invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(getObjectStoreManager));
            } else if (variable.asType().toString().startsWith(TransactionManager.class.getName())) {
                Invocation getTransactionManager = context.invoke("getTransactionManager");
                setMuleContext.body().add(ExpressionFactory._super().invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(getTransactionManager));
            } else if (variable.asType().toString().startsWith(QueueManager.class.getName())) {
                Invocation getQueueManager = context.invoke("getQueueManager");
                setMuleContext.body().add(ExpressionFactory._super().invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(getQueueManager));
            } else if (variable.asType().toString().startsWith(ObjectStore.class.getName())) {
                Conditional notNull = setMuleContext.body()._if(Op.eq(ExpressionFactory.invoke("get" + StringUtils.capitalize(variable.getSimpleName().toString())), ExpressionFactory._null()));
                Invocation getObjectStore = context.invoke("getRegistry").invoke("lookupObject").arg(
                        ref(MuleProperties.class).staticRef("OBJECT_STORE_DEFAULT_IN_MEMORY_NAME")
                );
                notNull._then().add(ExpressionFactory._super().invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(ExpressionFactory.cast(ref(ObjectStore.class),getObjectStore)));
            } else if (variable.asType().toString().startsWith(MuleConfiguration.class.getName())) {
                Invocation getConfiguration = context.invoke("getConfiguration");
                setMuleContext.body().add(ExpressionFactory._super().invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(getConfiguration));
            } else if (variable.asType().toString().startsWith(LifecycleManager.class.getName())) {
                Invocation getLifecycleManager = context.invoke("getLifecycleManager");
                setMuleContext.body().add(ExpressionFactory._super().invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(getLifecycleManager));
            } else if (variable.asType().toString().startsWith(ClassLoader.class.getName())) {
                Invocation getExecutionClassLoader = context.invoke("getExecutionClassLoader");
                setMuleContext.body().add(ExpressionFactory._super().invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(getExecutionClassLoader));
            } else if (variable.asType().toString().startsWith(ExpressionManager.class.getName())) {
                Invocation getExpressionManager = context.invoke("getExpressionManager");
                setMuleContext.body().add(ExpressionFactory._super().invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(getExpressionManager));
            } else if (variable.asType().toString().startsWith(EndpointFactory.class.getName())) {
                Invocation getEndpointFactory = context.invoke("getEndpointFactory");
                setMuleContext.body().add(ExpressionFactory._super().invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(getEndpointFactory));
            } else if (variable.asType().toString().startsWith(MuleClient.class.getName())) {
                Invocation getClient = context.invoke("getClient");
                setMuleContext.body().add(ExpressionFactory._super().invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(getClient));
            } else if (variable.asType().toString().startsWith(SystemExceptionHandler.class.getName())) {
                Invocation getExceptionListener = context.invoke("getExceptionListener");
                setMuleContext.body().add(ExpressionFactory._super().invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(getExceptionListener));
            } else if (variable.asType().toString().startsWith(org.mule.api.security.SecurityManager.class.getName())) {
                Invocation getSecurityManager = context.invoke("getSecurityManager");
                setMuleContext.body().add(ExpressionFactory._super().invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(getSecurityManager));
            } else if (variable.asType().toString().startsWith(WorkManager.class.getName())) {
                Invocation getWorkManager = context.invoke("getWorkManager");
                setMuleContext.body().add(ExpressionFactory._super().invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(getWorkManager));
            } else if (variable.asType().toString().startsWith(Registry.class.getName())) {
                Invocation getRegistry = context.invoke("getRegistry");
                setMuleContext.body().add(ExpressionFactory._super().invoke("set" + StringUtils.capitalize(variable.getSimpleName().toString())).arg(getRegistry));
            }
        }
    }

    private DefinedClass getMuleContextAwareAdapter(TypeElement typeElement) {
        String muleContextAwareAdapter = context.getNameUtils().generateClassName(typeElement, NamingContants.ADAPTERS_NAMESPACE, NamingContants.INJECTION_ADAPTER_CLASS_NAME_SUFFIX);
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(muleContextAwareAdapter));

        TypeReference previous = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));
        if (previous == null) {
            previous = (TypeReference) ref(typeElement.asType());
        }

        int modifiers = Modifier.PUBLIC;
        if( typeElement.getModifiers().contains(javax.lang.model.element.Modifier.ABSTRACT) ) {
            modifiers |= Modifier.ABSTRACT;
        }

        DefinedClass clazz = pkg._class(modifiers, context.getNameUtils().getClassName(muleContextAwareAdapter), previous);
        clazz._implements(ref(MuleContextAware.class));

        context.setClassRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement), clazz);

        return clazz;
    }

}
