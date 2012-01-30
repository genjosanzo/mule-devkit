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
package org.mule.devkit.validation;

import org.apache.commons.lang.StringUtils;
import org.mule.api.MuleContext;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.registry.Registry;
import org.mule.api.security.SecurityManager;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreManager;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.util.queue.QueueManager;

import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.transaction.TransactionManager;

public class InjectValidator implements Validator {
    @Override
    public boolean shouldValidate(DevKitTypeElement typeElement, GeneratorContext context) {
        return typeElement.getFieldsAnnotatedWith(Inject.class).size() > 0;
    }

    @Override
    public void validate(DevKitTypeElement typeElement, GeneratorContext context) throws ValidationException {
        for (VariableElement variable : typeElement.getFieldsAnnotatedWith(Inject.class)) {
            if (!variable.asType().toString().equals(MuleContext.class.getName()) &&
                    !variable.asType().toString().equals(ObjectStoreManager.class.getName()) &&
                    !variable.asType().toString().equals(TransactionManager.class.getName()) &&
                    !variable.asType().toString().equals(QueueManager.class.getName()) &&
                    !variable.asType().toString().equals(MuleConfiguration.class.getName()) &&
                    !variable.asType().toString().equals(LifecycleManager.class.getName()) &&
                    !variable.asType().toString().equals(ClassLoader.class.getName()) &&
                    !variable.asType().toString().equals(ExpressionManager.class.getName()) &&
                    !variable.asType().toString().equals(EndpointFactory.class.getName()) &&
                    !variable.asType().toString().equals(MuleClient.class.getName()) &&
                    !variable.asType().toString().equals(SystemExceptionHandler.class.getName()) &&
                    !variable.asType().toString().equals(SecurityManager.class.getName()) &&
                    !variable.asType().toString().equals(WorkManager.class.getName()) &&
                    !variable.asType().toString().equals(ObjectStore.class.getName()) &&
                    !variable.asType().toString().equals(Registry.class.getName())) {
                throw new ValidationException(variable, "I don't know how to inject the type " + variable.asType().toString() + " in field " + variable.getSimpleName().toString() + ". "
                        + "The only types I know how to inject are: MuleContext, ObjectStoreManager, ObjectStore, TransactionManager, QueueManager, MuleConfiguration, LifecycleManager, ClassLoader,"
                        + "ExpressionManager, EndpointFactory, MuleClient, SystemExceptionHandler, SecurityManager, WorkManager, Registry");
            } else {
                boolean found = false;
                for (ExecutableElement method : typeElement.getMethods()) {
                    if( method.getSimpleName().toString().equals("set" + StringUtils.capitalize(variable.getSimpleName().toString()))) {
                        found = true;
                        break;
                    }
                }
                if( !found ) {
                    throw new ValidationException(variable, "Cannot find a setter method for " + variable.getSimpleName().toString() + " but its being marked as injectable.");
                }

                if( variable.asType().toString().equals(ObjectStore.class.getName()) ) {
                    boolean getterFound = false;
                    for (ExecutableElement method : typeElement.getMethods()) {
                        if( method.getSimpleName().toString().equals("get" + StringUtils.capitalize(variable.getSimpleName().toString()))) {
                            getterFound = true;
                            break;
                        }
                    }
                    if( !getterFound ) {
                        throw new ValidationException(variable, "Cannot find a getter method for " + variable.getSimpleName().toString() + " but its being marked as an injectable Object Store.");
                    }

                }
            }
        }
    }
}
