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

import org.apache.commons.lang.StringUtils;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.oauth.UnableToAcquireAccessTokenException;
import org.mule.api.oauth.UnableToAcquireRequestTokenException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.generation.adapter.OAuth1AdapterGenerator;
import org.mule.devkit.generation.adapter.OAuth2AdapterGenerator;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Package;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Type;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;

import javax.lang.model.element.TypeElement;

public class AuthorizeMessageProcessorGenerator extends AbstractMessageGenerator {
    public static final String AUTHORIZE_MESSAGE_PROCESSOR_ROLE = "AuthorizeMessageProcessor";
    private static final String HTTP_STATUS_PROPERTY = "http.status";
    private static final String REDIRECT_HTTP_STATUS = "302";
    private static final String LOCATION_PROPERTY = "Location";

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        if (typeElement.hasAnnotation(OAuth.class) || typeElement.hasAnnotation(OAuth2.class)) {
            return true;
        }

        return false;
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) throws GenerationException {
        // get class
        DefinedClass messageProcessorClass;

        messageProcessorClass = getAuthorizeMessageProcessorClass(typeElement);

        // add standard fields
        FieldVariable logger = generateLoggerField(messageProcessorClass);
        FieldVariable object = generateFieldForModuleObject(messageProcessorClass, typeElement);
        FieldVariable muleContext = generateFieldForMuleContext(messageProcessorClass);
        FieldVariable flowConstruct = generateFieldForFlowConstruct(messageProcessorClass);

        // add initialise
        generateInitialiseMethod(messageProcessorClass, null, typeElement, muleContext, null, null, object, null);

        // add start
        generateStartMethod(messageProcessorClass, null);

        // add stop
        generateStopMethod(messageProcessorClass, null);

        // add dispose
        generateDiposeMethod(messageProcessorClass, null);

        // add setmulecontext
        generateSetMuleContextMethod(messageProcessorClass, muleContext, null);

        // add setflowconstruct
        generateSetFlowConstructMethod(messageProcessorClass, flowConstruct, null);

        // add setobject
        generateSetModuleObjectMethod(messageProcessorClass, object);

        // add process method
        Type muleEvent = ref(MuleEvent.class);

        Method process = messageProcessorClass.method(Modifier.PUBLIC, muleEvent, "process");
        process.javadoc().add("Starts the OAuth authorization process");
        process.javadoc().addParam("event MuleEvent to be processed");
        process.javadoc().addThrows(ref(MuleException.class));

        process._throws(MuleException.class);
        Variable event = process.param(muleEvent, "event");
        Variable muleMessage = process.body().decl(ref(MuleMessage.class), "muleMessage", event.invoke("getMessage"));

        DefinedClass moduleObjectClass = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));
        Variable moduleObject = process.body().decl(moduleObjectClass, "castedModuleObject", ExpressionFactory._null());
        findConfig(process.body(), muleContext, object, "authorize", event, moduleObjectClass, moduleObject);

        OAuth2 oauth2 = typeElement.getAnnotation(OAuth2.class);
        if (oauth2 != null && !StringUtils.isEmpty(oauth2.expirationRegex())) {
            Block ifTokenExpired = process.body()._if(moduleObject.invoke(OAuth2AdapterGenerator.HAS_TOKEN_EXPIRED_METHOD_NAME))._then();
            ifTokenExpired.invoke(moduleObject, OAuth2AdapterGenerator.RESET_METHOD_NAME);
        }
        
        TryStatement tryToAuthorize = process.body()._try();

        Invocation oauthVerifier = moduleObject.invoke("get" + StringUtils.capitalize(OAuth1AdapterGenerator.OAUTH_VERIFIER_FIELD_NAME));
        Block ifOauthVerifierIsNull = tryToAuthorize.body()._if(isNull(oauthVerifier))._then();
        Variable authorizationUrl = ifOauthVerifierIsNull.decl(ref(String.class), "authorizationUrl", ExpressionFactory.invoke(moduleObject, OAuth1AdapterGenerator.GET_AUTHORIZATION_URL_METHOD_NAME));
        ifOauthVerifierIsNull.invoke(event.invoke("getMessage"), "setOutboundProperty").arg(HTTP_STATUS_PROPERTY).arg(REDIRECT_HTTP_STATUS);
        ifOauthVerifierIsNull.invoke(event.invoke("getMessage"), "setOutboundProperty").arg(LOCATION_PROPERTY).arg(authorizationUrl);
        ifOauthVerifierIsNull._return(event);
        
        Invocation accessToken = moduleObject.invoke("get" + StringUtils.capitalize(OAuth1AdapterGenerator.OAUTH_ACCESS_TOKEN_FIELD_NAME));
        Block ifAccessTokenIsNull = tryToAuthorize.body()._if(isNull(accessToken))._then();
        ifAccessTokenIsNull.invoke(moduleObject, OAuth1AdapterGenerator.FETCH_ACCESS_TOKEN_METHOD_NAME);
        
        tryToAuthorize.body()._return(event);

        // OAuth 2 does not have a request token
        if( typeElement.hasAnnotation(OAuth.class) ) {
            CatchBlock unableToAcquireRequestTokenException = tryToAuthorize._catch(ref(UnableToAcquireRequestTokenException.class));
            Variable exception = unableToAcquireRequestTokenException.param("e");
            TypeReference coreMessages = ref(CoreMessages.class);
            Invocation failedToInvoke = coreMessages.staticInvoke("failedToInvoke");
            failedToInvoke.arg(ExpressionFactory.lit("authorize"));
            Invocation messageException = ExpressionFactory._new(ref(MessagingException.class));
            messageException.arg(failedToInvoke);
            messageException.arg(event);
            messageException.arg(exception);
            unableToAcquireRequestTokenException.body()._throw(messageException);
        }

        CatchBlock unableToAcquireAccessTokenException = tryToAuthorize._catch(ref(UnableToAcquireAccessTokenException.class));
        Variable exception = unableToAcquireAccessTokenException.param("e2");
        TypeReference coreMessages = ref(CoreMessages.class);
        Invocation failedToInvoke = coreMessages.staticInvoke("failedToInvoke");
        failedToInvoke.arg(ExpressionFactory.lit("authorize"));
        Invocation messageException = ExpressionFactory._new(ref(MessagingException.class));
        messageException.arg(failedToInvoke);
        messageException.arg(event);
        messageException.arg(exception);
        unableToAcquireAccessTokenException.body()._throw(messageException);

    }

    private DefinedClass getAuthorizeMessageProcessorClass(TypeElement type) {
        String httpCallbackClassName = context.getNameUtils().generateClassNameInPackage(type, ".config", "AuthorizeMessageProcessor");
        Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(httpCallbackClassName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(httpCallbackClassName), new Class[]{
                Initialisable.class,
                Startable.class,
                Disposable.class,
                Stoppable.class,
                MessageProcessor.class,
                MuleContextAware.class,
                FlowConstructAware.class});

        context.setClassRole(AUTHORIZE_MESSAGE_PROCESSOR_ROLE, clazz);

        return clazz;
    }

}
