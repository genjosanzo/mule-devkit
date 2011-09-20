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

import org.mule.api.annotations.Module;
import org.mule.api.annotations.param.Session;
import org.mule.api.annotations.session.SessionCreate;
import org.mule.api.annotations.session.SessionDestroy;
import org.mule.devkit.generation.DevkitTypeElement;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class SessionValidator implements Validator {

    @Override
    public void validate(DevkitTypeElement typeElement) throws ValidationException {

        if (!typeElement.hasAnnotation(Module.class)) {
            return;
        }

        List<ExecutableElement> sessionCreateMethods = typeElement.getMethodsAnnotatedWith(SessionCreate.class);
        if (sessionCreateMethods.size() > 1) {
            throw new ValidationException(typeElement, "You cannot annotate more than one method with @SessionCreate");
        }

        List<ExecutableElement> sessionDestroyMethods = typeElement.getMethodsAnnotatedWith(SessionDestroy.class);
        if (sessionDestroyMethods.size() > 1) {
            throw new ValidationException(typeElement, "You cannot annotate more than one method with @SessionDestroy");
        }

        if (sessionCreateMethods.isEmpty() ^ sessionDestroyMethods.isEmpty()) {
            throw new ValidationException(typeElement, "You need have exactly one method annotated with @SessionCreate and one with @SessionDestroy");
        }

        if (sessionCreateMethods.isEmpty()) {
            for (ExecutableElement executableElement : typeElement.getMethods()) {
                for (VariableElement parameter : executableElement.getParameters()) {
                    if (parameter.getAnnotation(Session.class) != null) {
                        throw new ValidationException(parameter, "You cannot annotate a parameter with @Session without specifying a way to create session with @SessionCreate");
                    }
                }
            }
            return; // no @SessionCreate or @SessionDestroy methods present, nor @Session parameters
        }

        ExecutableElement sessionCreateMethod = sessionCreateMethods.get(0);
        ExecutableElement sessionDestroyMethod = sessionDestroyMethods.get(0);

        TypeMirror sessionCreateMethodReturnType = sessionCreateMethod.getReturnType();

        if (sessionDestroyMethod.getParameters().size() != 1) {
            throw new ValidationException(typeElement, "The @SessionDestroy method must receive a single argument");
        }

        if (!sessionDestroyMethod.getParameters().get(0).asType().equals(sessionCreateMethodReturnType)) {
            throw new ValidationException(typeElement, "The argument for @SessionDestroy must be of the same type as the return value of @SessionCreate");
        }


        for (ExecutableElement executableElement : typeElement.getMethods()) {
            for (VariableElement parameter : executableElement.getParameters()) {
                if (parameter.getAnnotation(Session.class) != null) {
                    if (!parameter.asType().equals(sessionCreateMethodReturnType)) {
                        throw new ValidationException(parameter, "The type for any parameter annotated with @Session must be of the same type as the return type of method annotation with @SessionCreate");
                    }
                }
            }
        }
    }
}