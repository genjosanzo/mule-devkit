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

import org.mule.api.annotations.Connect;
import org.mule.api.annotations.ConnectionIdentifier;
import org.mule.api.annotations.Disconnect;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.ValidateConnection;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

public class ConnectorValidator implements Validator {

    @Override
    public boolean shouldValidate(DevKitTypeElement typeElement, GeneratorContext context) {
        return typeElement.isModuleOrConnector();
    }

    @Override
    public void validate(DevKitTypeElement typeElement, GeneratorContext context) throws ValidationException {

        List<ExecutableElement> connectMethods = typeElement.getMethodsAnnotatedWith(Connect.class);
        List<ExecutableElement> validateConnectionMethods = typeElement.getMethodsAnnotatedWith(ValidateConnection.class);
        List<ExecutableElement> disconnectMethods = typeElement.getMethodsAnnotatedWith(Disconnect.class);
        List<ExecutableElement> connectionIdentifierMethods = typeElement.getMethodsAnnotatedWith(ConnectionIdentifier.class);

        if (typeElement.hasAnnotation(Module.class)) {
            if (!connectMethods.isEmpty()) {
                throw new ValidationException(typeElement, "@Connect methods not allowed for @Module classes, use class level annotation @Connector instead");
            }
            if (!validateConnectionMethods.isEmpty()) {
                throw new ValidationException(typeElement, "@Connect methods not allowed for @Module classes, use class level annotation @Connector instead");
            }
            if (!disconnectMethods.isEmpty()) {
                throw new ValidationException(typeElement, "@Connect methods not allowed for @Module classes, use class level annotation @Connector instead");
            }
            if (!connectionIdentifierMethods.isEmpty()) {
                throw new ValidationException(typeElement, "@Connect methods not allowed for @Module classes, use class level annotation @Connector instead");
            }
            return;
        }

        if (connectMethods.size() > 1) {
            throw new ValidationException(typeElement, "You cannot annotate more than one method with @Connect");
        }
        if (connectMethods.size() < 1) {
            throw new ValidationException(typeElement, "You must provide at least one method with @Connect");
        }

        if (disconnectMethods.size() > 1) {
            throw new ValidationException(typeElement, "You cannot annotate more than one method with @Disconnect");
        }
        if (disconnectMethods.size() < 1) {
            throw new ValidationException(typeElement, "You must provide at least one method with @Disconnect");
        }

        if (validateConnectionMethods.size() > 1) {
            throw new ValidationException(typeElement, "You cannot annotate more than one method with @ValidateConnection");
        }
        if (validateConnectionMethods.size() < 1) {
            throw new ValidationException(typeElement, "You must provide at least one method with @ValidateConnection");
        }

        if (connectionIdentifierMethods.size() > 1) {
            throw new ValidationException(typeElement, "You cannot annotate more than one method with @ConnectionIdentifier");
        }
        if (connectionIdentifierMethods.size() < 1) {
            throw new ValidationException(typeElement, "You must provide at least one method with @ConnectionIdentifier");
        }

        ExecutableElement connectMethod = connectMethods.get(0);
        ExecutableElement disconnectMethod = disconnectMethods.get(0);
        ExecutableElement validateConnectionMethod = validateConnectionMethods.get(0);

        if (connectMethod.getThrownTypes().size() != 1) {
            throw new ValidationException(typeElement, "A @Connect method can only throw a single type of exception. That exception must be ConnectionException.");
        }

        if (!connectMethod.getThrownTypes().get(0).toString().equals("org.mule.api.ConnectionException")) {
            throw new ValidationException(typeElement, "A @Connect method can only throw a single type of exception. That exception must be ConnectionException.");
        }

        if (!connectMethod.getReturnType().toString().equals("void")) {
            throw new ValidationException(typeElement, "A @Connect method cannot return anything.");
        }

        if (!validateConnectionMethod.getReturnType().toString().equals("boolean") &&
                !validateConnectionMethod.getReturnType().toString().equals("java.lang.Boolean")) {
            throw new ValidationException(typeElement, "A @ValidateConnection method must return a boolean.");
        }

        if (!disconnectMethod.getParameters().isEmpty()) {
            throw new ValidationException(typeElement, "The @Disconnect method cannot receive any arguments");
        }

        if (!validateConnectionMethod.getParameters().isEmpty()) {
            throw new ValidationException(typeElement, "The @ValidateConnection method cannot receive any arguments");
        }


        if (!disconnectMethod.getReturnType().toString().equals("void")) {
            throw new ValidationException(typeElement, "A @Disconnect method cannot return anything.");
        }
    }
}