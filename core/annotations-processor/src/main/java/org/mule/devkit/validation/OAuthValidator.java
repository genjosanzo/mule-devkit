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
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.api.annotations.oauth.OAuthAccessToken;
import org.mule.api.annotations.oauth.OAuthAccessTokenSecret;
import org.mule.api.annotations.oauth.OAuthConsumerKey;
import org.mule.api.annotations.oauth.OAuthConsumerSecret;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.Map;

public class OAuthValidator implements Validator {

    @Override
    public boolean shouldValidate(Map<String, String> options) {
        return true;
    }

    @Override
    public void validate(DevKitTypeElement typeElement, GeneratorContext context) throws ValidationException {

        if (!typeElement.hasAnnotation(Module.class)) {
            return;
        }

        if (typeElement.hasAnnotation(OAuth.class) || typeElement.hasAnnotation(OAuth2.class)) {
            checkClassHasFieldWithAnnotation(typeElement, OAuthConsumerKey.class, "@OAuth class must contain a field annotated with @OAuthConsumerKey");
            checkClassHasFieldWithAnnotation(typeElement, OAuthConsumerSecret.class, "@OAuth class must contain a field annotated with @OAuthConsumerSecret");
        } else {
            if (classHasMethodWithParameterAnnotated(typeElement, OAuthAccessToken.class)) {
                throw new ValidationException(typeElement, "Cannot annotate parameter with @OAuthAccessToken without annotating the class with @OAuth");
            }
            if (classHasMethodWithParameterAnnotated(typeElement, OAuthAccessTokenSecret.class)) {
                throw new ValidationException(typeElement, "Cannot annotate parameter with @OAuthAccessTokenSecret without annotating the class with @OAuth");
            }
        }
    }

    private void checkClassHasFieldWithAnnotation(DevKitTypeElement typeElement, Class<? extends Annotation> annotation, String errorMessage) throws ValidationException {
        if(typeElement.getFieldsAnnotatedWith(annotation).isEmpty()) {
            throw new ValidationException(typeElement, errorMessage);
        }
    }

    private boolean classHasMethodWithParameterAnnotated(DevKitTypeElement typeElement, Class<? extends Annotation> annotation) {
        for (ExecutableElement method : typeElement.getMethods()) {
            for (VariableElement parameter : method.getParameters()) {
                if (parameter.getAnnotation(annotation) != null) {
                    return true;
                }
            }
        }
        return false;
    }
}