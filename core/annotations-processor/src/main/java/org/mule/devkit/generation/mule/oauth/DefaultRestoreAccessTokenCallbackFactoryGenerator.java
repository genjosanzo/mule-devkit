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
import org.mule.api.processor.MessageProcessor;
import org.mule.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.*;

import javax.lang.model.element.TypeElement;

public class DefaultRestoreAccessTokenCallbackFactoryGenerator extends AbstractMessageGenerator {

    public static final String ROLE = "RestoreAccessTokenCallbackFactoryBean";

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        if (typeElement.hasAnnotation(OAuth.class) || typeElement.hasAnnotation(OAuth2.class)) {
            return true;
        }

        return false;
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) throws GenerationException {
        DefinedClass factory = getDefaultRestoreAccessTokenCallbackFactoryClass(typeElement);

        DefinedClass callback = context.getClassForRole(DefaultRestoreAccessTokenCallbackGenerator.ROLE);
        Method getObjectType = factory.method(Modifier.PUBLIC, ref(Class.class), "getObjectType");
        getObjectType.body()._return(callback.dotclass());
        
        Method getObject = factory.method(Modifier.PUBLIC, ref(Object.class), "getObject");
        getObject._throws(ref(Exception.class));
        Variable callbackVariable = getObject.body().decl(callback, "callback", ExpressionFactory._new(callback));
        getObject.body().add(
                callbackVariable.invoke("setMessageProcessor").arg(
                        ExpressionFactory.cast(ref(MessageProcessor.class),
                                ExpressionFactory._super().invoke("getObject"))));
        
        getObject.body()._return(callbackVariable);
    }
    
    private DefinedClass getDefaultRestoreAccessTokenCallbackFactoryClass(TypeElement type) {
        String callbackClassName = context.getNameUtils().generateClassNameInPackage(type, ".config", "RestoreAccessTokenCallbackFactoryBean");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(callbackClassName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(callbackClassName));
        clazz._extends(ref(MessageProcessorChainFactoryBean.class));

        context.setClassRole(ROLE, clazz);

        return clazz;
    }    
}
