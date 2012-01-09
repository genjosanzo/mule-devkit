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

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.oauth.SaveAccessTokenCallback;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Variable;

import javax.lang.model.element.TypeElement;

public class DefaultSaveAccessTokenCallbackGenerator extends AbstractMessageGenerator {

    public static final String ROLE = "DefaultSaveAccessTokenCallback";

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        if (typeElement.hasAnnotation(OAuth.class) || typeElement.hasAnnotation(OAuth2.class)) {
            return true;
        }

        return false;
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) throws GenerationException {
        DefinedClass callbackClass = getDefaultSaveAccessTokenCallbackClass(typeElement);

        FieldVariable messageProcessor = generateFieldForMessageProcessor(callbackClass, "messageProcessor");
        FieldVariable logger = generateLoggerField(callbackClass);
        FieldVariable hasBeenStarted = generateFieldForBoolean(callbackClass, "hasBeenStarted");
        FieldVariable hasBeenInitialized = generateFieldForBoolean(callbackClass, "hasBeenInitialized");

        Method constructor = callbackClass.constructor(Modifier.PUBLIC);
        constructor.body().assign(hasBeenStarted, ExpressionFactory.FALSE);
        constructor.body().assign(hasBeenInitialized, ExpressionFactory.FALSE);

        generateGetter(callbackClass, messageProcessor);
        generateSetter(callbackClass, messageProcessor);

        Method saveAccessTokenMethod = callbackClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "saveAccessToken");
        Variable accessToken = saveAccessTokenMethod.param(ref(String.class), "accessToken");
        Variable accessTokenSecret = saveAccessTokenMethod.param(ref(String.class), "accessTokenSecret");

        Variable event = saveAccessTokenMethod.body().decl(ref(MuleEvent.class), "event", ref(RequestContext.class).staticInvoke("getEvent"));
        Conditional ifAccessTokenNotNull = saveAccessTokenMethod.body()._if(Op.ne(accessToken, ExpressionFactory._null());
        ifAccessTokenNotNull._then().add(
                event.invoke("getMessage").invoke("setInvocationProperty").arg("OAuthAccessToken").arg(accessToken)
        );
        Conditional ifAccessTokenSecretNotNull = saveAccessTokenMethod.body()._if(Op.ne(accessTokenSecret, ExpressionFactory._null());
        ifAccessTokenSecretNotNull._then().add(
                event.invoke("getMessage").invoke("setInvocationProperty").arg("OAuthAccessTokenSecret").arg(accessTokenSecret)
        );

        Conditional ifMuleContextAware = saveAccessTokenMethod.body()._if(Op._instanceof(messageProcessor, ref(MuleContextAware.class)));
        ifMuleContextAware._then().add(
                ExpressionFactory.cast(ref(MuleContextAware.class), messageProcessor).invoke("setMuleContext").arg(
                        ref(RequestContext.class).staticInvoke("getEventContext").invoke("getMuleContext")
                )
        );

        Conditional ifFlowConstructAware = saveAccessTokenMethod.body()._if(Op._instanceof(messageProcessor, ref(FlowConstructAware.class)));
        ifFlowConstructAware._then().add(
                ExpressionFactory.cast(ref(FlowConstructAware.class), messageProcessor).invoke("setFlowConstruct").arg(
                        ref(RequestContext.class).staticInvoke("getEventContext").invoke("getFlowConstruct")
                )
        );

        Conditional ifNotInitialized = saveAccessTokenMethod.body()._if(Op.not(hasBeenInitialized));
        Conditional ifInitialisable = ifNotInitialized._then()._if(Op._instanceof(messageProcessor, ref(Initialisable.class)));
        TryStatement tryToInitialize = ifInitialisable._then()._try();
        tryToInitialize.body().add(
                ExpressionFactory.cast(ref(Initialisable.class), messageProcessor).invoke("initialise")
        );
        CatchBlock catchInitlize = tryToInitialize._catch(ref(Exception.class));
        Variable exception = catchInitlize.param("e");
        catchInitlize.body().add(
                logger.invoke("error").arg(
                        exception.invoke("getMessage")
                ).arg(
                        exception
                )
        );
        ifNotInitialized._then().assign(hasBeenInitialized, ExpressionFactory.TRUE);

        Conditional ifNotStarted = saveAccessTokenMethod.body()._if(Op.not(hasBeenStarted));
        Conditional ifStartable = ifNotStarted._then()._if(Op._instanceof(messageProcessor, ref(Startable.class)));
        TryStatement tryToStart = ifStartable._then()._try();
        tryToStart.body().add(
                ExpressionFactory.cast(ref(Startable.class), messageProcessor).invoke("start")
        );
        CatchBlock catchStart = tryToStart._catch(ref(Exception.class));
        exception = catchStart.param("e");
        catchStart.body().add(
                logger.invoke("error").arg(
                        exception.invoke("getMessage")
                ).arg(
                        exception
                )
        );

        ifNotStarted._then().assign(hasBeenStarted, ExpressionFactory.TRUE);

        TryStatement tryProcess = saveAccessTokenMethod.body()._try();
        tryProcess.body().add(
                messageProcessor.invoke("process").arg(event)
        );
        CatchBlock catchProcess = tryProcess._catch(ref(Exception.class));
        exception = catchProcess.param("e");
        catchProcess.body().add(
                logger.invoke("error").arg(
                        exception.invoke("getMessage")
                ).arg(
                        exception
                )
        );
    }

    private DefinedClass getDefaultSaveAccessTokenCallbackClass(TypeElement type) {
        String callbackClassName = context.getNameUtils().generateClassNameInPackage(type, ".config", "DefaultSaveAccessTokenCallback");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(callbackClassName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(callbackClassName), new Class[]{
                SaveAccessTokenCallback.class});
        context.setClassRole(ROLE, clazz);

        return clazz;
    }
}
