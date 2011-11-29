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
package org.mule.devkit.generation.mule.oauth;

import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.api.oauth.RestoreAccessTokenCallback;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Package;

import javax.lang.model.element.TypeElement;

public class DefaultRestoreAccessTokenCallbackGenerator extends AbstractMessageGenerator {

    public static final String ROLE = "DefaultRestoreAccessTokenCallback";

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        if (typeElement.hasAnnotation(OAuth.class) || typeElement.hasAnnotation(OAuth2.class)) {
            return true;
        }

        return false;
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) throws GenerationException {
        DefinedClass callbackClass = getDefaultRestoreAccessTokenCallbackClass(typeElement);

        FieldVariable messageProcessor = generateFieldForMessageProcessor(callbackClass, "messageProcessor");

        generateGetter(callbackClass, messageProcessor);
        generateSetter(callbackClass, messageProcessor);

        Method restoreAccessTokenMethod = callbackClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "restoreAccessToken");
        Method getAccessTokenMethod = callbackClass.method(Modifier.PUBLIC, ref(String.class), "getAccessToken");
        Method getAccessTokenSecretMethod = callbackClass.method(Modifier.PUBLIC, ref(String.class), "getAccessTokenSecret");
    }

    private DefinedClass getDefaultRestoreAccessTokenCallbackClass(TypeElement type) {
        String callbackClassName = context.getNameUtils().generateClassNameInPackage(type, ".config", "DefaultRestoreAccessTokenCallback");
        Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(callbackClassName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(callbackClassName), new Class[]{
                RestoreAccessTokenCallback.class});

        context.setClassRole(ROLE, clazz);

        return clazz;
    }
}
