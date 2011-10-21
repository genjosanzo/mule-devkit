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

import org.mule.api.annotations.Processor;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.InvocationHeaders;
import org.mule.api.annotations.param.OutboundHeaders;
import org.mule.api.annotations.param.Payload;
import org.mule.api.callback.InterceptCallback;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import java.util.List;

public class ProcessorValidator implements Validator {

    @Override
    public boolean shouldValidate(DevKitTypeElement typeElement, GeneratorContext context) {
        return typeElement.isModuleOrConnector();
    }

    @Override
    public void validate(DevKitTypeElement typeElement, GeneratorContext context) throws ValidationException {

        for (ExecutableElement method : typeElement.getMethodsAnnotatedWith(Processor.class)) {

            if (method.getModifiers().contains(Modifier.STATIC)) {
                throw new ValidationException(method, "@Processor cannot be applied to a static method");
            }

            if (!method.getTypeParameters().isEmpty()) {
                throw new ValidationException(method, "@Processor cannot be applied to a generic method");
            }

            if (!method.getModifiers().contains(Modifier.PUBLIC)) {
                throw new ValidationException(method, "@Processor cannot be applied to a non-public method");
            }

            validateIntercepting(method);

            for (VariableElement parameter : method.getParameters()) {
                int count = 0;
                if (parameter.getAnnotation(InboundHeaders.class) != null) {
                    count++;
                }
                if (parameter.getAnnotation(OutboundHeaders.class) != null) {
                    count++;
                }
                if (parameter.getAnnotation(InvocationHeaders.class) != null) {
                    count++;
                }
                if (parameter.getAnnotation(Payload.class) != null) {
                    count++;
                }

                if (count > 1) {
                    throw new ValidationException(parameter, "You cannot have more than one of InboundHeader, InvocationHeaders or Payload annotation");
                }

                if (parameter.getAnnotation(Payload.class) == null && parameter.asType().getKind() == TypeKind.ARRAY) {
                    throw new ValidationException(parameter, "@Processor parameted cannot be arrays, use List instead");
                }
            }
        }
    }

    private void validateIntercepting(ExecutableElement method) throws ValidationException {
        if (method.getAnnotation(Processor.class).intercepting()) {
            boolean containsInterceptCallback = false;
            List<? extends VariableElement> parameters = method.getParameters();
            for (VariableElement parameter : parameters) {
                if (parameter.asType().toString().contains(InterceptCallback.class.getName())) {
                    containsInterceptCallback = true;
                }
            }

            if (!containsInterceptCallback) {
                throw new ValidationException(method, "An intercepting method method must contain a InterceptCallback as one of its parameters");
            }
        }
    }
}