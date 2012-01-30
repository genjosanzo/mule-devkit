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
import org.mule.api.oauth.RestoreAccessTokenCallback;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.generation.NamingContants;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.Package;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Variable;

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
        FieldVariable logger = generateLoggerField(callbackClass);
        FieldVariable hasBeenStarted = generateFieldForBoolean(callbackClass, "hasBeenStarted");
        FieldVariable hasBeenInitialized = generateFieldForBoolean(callbackClass, "hasBeenInitialized");
        FieldVariable accessToken = generateFieldForString(callbackClass, "restoredAccessToken");
        FieldVariable accessTokenSecret = generateFieldForString(callbackClass, "restoredAccessTokenSecret");

        Method constructor = callbackClass.constructor(Modifier.PUBLIC);
        constructor.body().assign(hasBeenStarted, ExpressionFactory.FALSE);
        constructor.body().assign(hasBeenInitialized, ExpressionFactory.FALSE);

        generateGetter(callbackClass, messageProcessor);
        generateSetter(callbackClass, messageProcessor);

        Method restoreAccessTokenMethod = callbackClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "restoreAccessToken");
        Variable event = restoreAccessTokenMethod.body().decl(ref(MuleEvent.class), "event", ref(RequestContext.class).staticInvoke("getEvent"));

        Conditional ifMuleContextAware = restoreAccessTokenMethod.body()._if(Op._instanceof(messageProcessor, ref(MuleContextAware.class)));
        ifMuleContextAware._then().add(
                ExpressionFactory.cast(ref(MuleContextAware.class), messageProcessor).invoke("setMuleContext").arg(
                        ref(RequestContext.class).staticInvoke("getEventContext").invoke("getMuleContext")
                )
        );

        Conditional ifFlowConstructAware = restoreAccessTokenMethod.body()._if(Op._instanceof(messageProcessor, ref(FlowConstructAware.class)));
        ifFlowConstructAware._then().add(
                ExpressionFactory.cast(ref(FlowConstructAware.class), messageProcessor).invoke("setFlowConstruct").arg(
                        ref(RequestContext.class).staticInvoke("getEventContext").invoke("getFlowConstruct")
                )
        );

        Conditional ifNotInitialized = restoreAccessTokenMethod.body()._if(Op.not(hasBeenInitialized));
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

        Conditional ifNotStarted = restoreAccessTokenMethod.body()._if(Op.not(hasBeenStarted));
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

        TryStatement tryProcess = restoreAccessTokenMethod.body()._try();
        tryProcess.body().assign(event,
                messageProcessor.invoke("process").arg(event)
        );
        tryProcess.body().assign(accessToken,
                event.invoke("getMessage").invoke("getInvocationProperty").arg("OAuthAccessToken")
        );
        tryProcess.body().assign(accessTokenSecret,
                event.invoke("getMessage").invoke("getInvocationProperty").arg("OAuthAccessTokenSecret")
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

        Method getAccessTokenMethod = callbackClass.method(Modifier.PUBLIC, ref(String.class), "getAccessToken");
        getAccessTokenMethod.body()._return(accessToken);
        Method getAccessTokenSecretMethod = callbackClass.method(Modifier.PUBLIC, ref(String.class), "getAccessTokenSecret");
        getAccessTokenSecretMethod.body()._return(accessTokenSecret);
    }

    private DefinedClass getDefaultRestoreAccessTokenCallbackClass(TypeElement type) {
        String callbackClassName = context.getNameUtils().generateClassNameInPackage(type, NamingContants.CONFIG_NAMESPACE, NamingContants.DEFAULT_RESTORE_ACCESS_TOKEN_CALLBACK_CLASS_NAME);
        Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(callbackClassName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(callbackClassName), new Class[]{
                RestoreAccessTokenCallback.class});

        context.setClassRole(ROLE, clazz);

        return clazz;
    }
}
