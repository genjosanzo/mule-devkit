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

public class OAuthValidator implements Validator {

    @Override
    public boolean shouldValidate(DevKitTypeElement typeElement, GeneratorContext context) {
        return typeElement.isModuleOrConnector();
    }

    @Override
    public void validate(DevKitTypeElement typeElement, GeneratorContext context) throws ValidationException {
        if (typeElement.hasAnnotation(OAuth.class)) {
            validateOAuth1Class(typeElement);
        } else if (typeElement.hasAnnotation(OAuth2.class)) {
            validateOAuth2Class(typeElement);
        } else {
            validateNonOAuthClass(typeElement);
        }
    }

    private void validateOAuth1Class(DevKitTypeElement typeElement) throws ValidationException {
        if (!typeElement.hasFieldAnnotatedWith(OAuthConsumerKey.class)) {
            throw new ValidationException(typeElement, "@OAuth class must contain a field annotated with @OAuthConsumerKey");
        }
        if (!typeElement.hasFieldAnnotatedWith(OAuthConsumerSecret.class)) {
            throw new ValidationException(typeElement, "@OAuth class must contain a field annotated with @OAuthConsumerSecret");
        }
        if (!classHasMethodWithParameterAnnotated(typeElement, OAuthAccessToken.class)) {
            throw new ValidationException(typeElement, "@OAuth class must have at least one method parameter annotated with @OAuthAccessToken");
        }
    }

    private void validateOAuth2Class(DevKitTypeElement typeElement) throws ValidationException {
        if (!typeElement.hasFieldAnnotatedWith(OAuthConsumerKey.class)) {
            throw new ValidationException(typeElement, "@OAuth2 class must contain a field annotated with @OAuthConsumerKey");
        }
        if (!typeElement.hasFieldAnnotatedWith(OAuthConsumerSecret.class)) {
            throw new ValidationException(typeElement, "@OAuth2 class must contain a field annotated with @OAuthConsumerSecret");
        }
        if (!classHasMethodWithParameterAnnotated(typeElement, OAuthAccessToken.class)) {
            throw new ValidationException(typeElement, "@OAuth2 class must have at least one method parameter annotated with @OAuthAccessToken");
        }
        if(classHasMethodWithParameterAnnotated(typeElement, OAuthAccessTokenSecret.class)) {
            throw new ValidationException(typeElement, "@OAuth2 class cannot have method parameters annotated with @OAuthAccessTokenSecret");
        }
    }

    private void validateNonOAuthClass(DevKitTypeElement typeElement) throws ValidationException {
        if (classHasMethodWithParameterAnnotated(typeElement, OAuthAccessToken.class)) {
            throw new ValidationException(typeElement, "Cannot annotate parameter with @OAuthAccessToken without annotating the class with @OAuth or @OAuth2");
        }
        if (classHasMethodWithParameterAnnotated(typeElement, OAuthAccessTokenSecret.class)) {
            throw new ValidationException(typeElement, "Cannot annotate parameter with @OAuthAccessTokenSecret without annotating the class with @OAuth");
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