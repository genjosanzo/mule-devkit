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
import org.mule.api.annotations.Source;
import org.mule.api.annotations.rest.RestCall;
import org.mule.api.annotations.rest.RestUriParam;
import org.mule.api.callback.SourceCallback;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class RestValidator implements Validator {

    @Override
    public boolean shouldValidate(DevKitTypeElement typeElement, GeneratorContext context) {
        return typeElement.isModuleOrConnector() && typeElement.hasMethodsAnnotatedWith(RestCall.class);
    }

    @Override
    public void validate(DevKitTypeElement typeElement, GeneratorContext context) throws ValidationException {

        for (ExecutableElement method : typeElement.getMethodsAnnotatedWith(RestCall.class)) {

            if (!method.getModifiers().contains(Modifier.ABSTRACT)) {
                throw new ValidationException(method, "@RestCall can only be applied to abstract methods");
            }

            if( method.getThrownTypes().size() != 1 ) {
                throw new ValidationException(method, "@RestCall abstract method must throw IOException");
            }
        }
        
        for(VariableElement field : typeElement.getFieldsAnnotatedWith(RestUriParam.class)) {
            boolean getterFound = false;
            for (ExecutableElement method : typeElement.getMethods()) {
                if( method.getSimpleName().toString().equals("get" + StringUtils.capitalize(field.getSimpleName().toString()))) {
                    getterFound = true;
                    break;
                }
            }
            if( !getterFound ) {
                throw new ValidationException(field, "Cannot find a getter method for " + field.getSimpleName().toString() + " but its being marked as URI parameter of a REST call.");
            }
        }
    }
}