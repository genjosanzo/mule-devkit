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
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Disconnect;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;

import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.Map;

public class ConnectorValidator implements Validator {

    @Override
    public boolean shouldValidate(Map<String, String> options) {
        return true;
    }

    @Override
    public void validate(DevKitTypeElement typeElement, GeneratorContext context) throws ValidationException {

        if (!typeElement.hasAnnotation(Connector.class)) {
            return;
        }

        List<ExecutableElement> connectMethods = typeElement.getMethodsAnnotatedWith(Connect.class);
        if (connectMethods.size() > 1) {
            throw new ValidationException(typeElement, "You cannot annotate more than one method with @Connect");
        }
        if (connectMethods.size() < 1) {
            throw new ValidationException(typeElement, "You must provide at least one method with @Connect");
        }

        List<ExecutableElement> disconnectMethods = typeElement.getMethodsAnnotatedWith(Disconnect.class);
        if (disconnectMethods.size() > 1) {
            throw new ValidationException(typeElement, "You cannot annotate more than one method with @Disconnect");
        }
        if (disconnectMethods.size() < 1) {
            throw new ValidationException(typeElement, "You must provide at least one method with @Disconnect");
        }

        if (connectMethods.isEmpty() || disconnectMethods.isEmpty()) {
            throw new ValidationException(typeElement, "You need have exactly one method annotated with @Connect and one with @Disconnect");
        }

        if (typeElement.usesConnectionManager()) {
            ExecutableElement connectMethod = connectMethods.get(0);
            ExecutableElement disconnectMethod = disconnectMethods.get(0);

            if (connectMethod.getThrownTypes().size() != 1) {
                throw new ValidationException(typeElement, "A @Connect method can only throw a single type of exception. That exception must be ConnectionException.");
            }

            if (!connectMethod.getThrownTypes().get(0).toString().equals("org.mule.api.ConnectionException")) {
                throw new ValidationException(typeElement, "A @Connect method can only throw a single type of exception. That exception must be ConnectionException.");
            }

            if (!connectMethod.getReturnType().toString().equals("void")) {
                throw new ValidationException(typeElement, "A @Connect method cannot return anything.");
            }

            if (disconnectMethod.getParameters().size() != 0) {
                throw new ValidationException(typeElement, "The @Disconnect method cannot receive any arguments");
            }

            if (!disconnectMethod.getReturnType().toString().equals("void")) {
                throw new ValidationException(typeElement, "A @Disconnect method cannot return anything.");
            }
        }
    }
}