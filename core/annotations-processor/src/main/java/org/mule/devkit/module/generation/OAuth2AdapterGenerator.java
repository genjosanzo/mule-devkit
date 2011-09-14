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

package org.mule.devkit.module.generation;

import org.apache.commons.lang.StringUtils;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.annotations.callback.HttpCallback;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.api.annotations.oauth.OAuthConsumerKey;
import org.mule.api.annotations.oauth.OAuthConsumerSecret;
import org.mule.api.annotations.oauth.OAuthScope;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.MessageFactory;
import org.mule.devkit.generation.DevkitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.ClassAlreadyExistsException;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.Package;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Variable;
import org.mule.devkit.model.code.builders.FieldBuilder;
import org.mule.util.IOUtils;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OAuth2AdapterGenerator extends AbstractModuleGenerator {

    public static final String VERIFIER_FIELD_NAME = "oauthVerifier";
    public static final String GET_AUTHORIZATION_URL_METHOD_NAME = "getAuthorizationUrl";
    public static final String HAS_TOKEN_EXPIRED_METHOD_NAME = "hasTokenExpired";
    public static final String RESET_METHOD_NAME = "reset";
    private static final String REDIRECT_URL_FIELD_NAME = "redirectUrl";
    private static final String ACCESS_TOKEN_FIELD_NAME = "accessToken";
    private static final String ENCODING = "UTF-8";
    private static final String GRANT_TYPE = "authorization_code";
    private static final String ACCESS_CODE_PATTERN_FIELD_NAME = "ACCESS_CODE_PATTERN";
    private static final String CALLBACK_FIELD_NAME = "oauthCallback";
    private static final String AUTH_CODE_PATTERN_FIELD_NAME = "AUTH_CODE_PATTERN";
    private static final String EXPIRATION_TIME_PATTERN_FIELD_NAME = "EXPIRATION_TIME_PATTERN";
    private static final String EXPIRATION_FIELD_NAME = "expiration";

    @Override
    protected boolean shouldGenerate(DevkitTypeElement typeElement) {
        return typeElement.hasAnnotation(OAuth2.class);
    }

    @Override
    protected void doGenerate(DevkitTypeElement typeElement) throws GenerationException {
        DefinedClass oauthAdapter = getOAuthAdapterClass(typeElement);
        OAuth2 oauth2 = typeElement.getAnnotation(OAuth2.class);
        oauthAuthorizationCoderPatternConstant(oauthAdapter, oauth2);
        oauthAcessTokenPatternConstant(oauthAdapter, oauth2);
        expirationPatternConstant(oauthAdapter, oauth2);
        muleContextField(oauthAdapter);
        authorizationCodeField(oauthAdapter);
        redirectUrlField(oauthAdapter);
        oauthCallbackField(oauthAdapter);
        accessTokenField(oauthAdapter);
        expirationField(oauthAdapter, typeElement.getAnnotation(OAuth2.class));

        DefinedClass messageProcessor = generateMessageProcessorInnerClass(oauthAdapter);

        generateStartMethod(oauthAdapter);
        generateStopMethod(oauthAdapter);
        generateInitialiseMethod(oauthAdapter, messageProcessor, oauth2);

        generateGetAuthorizationUrlMethod(oauthAdapter, typeElement, oauth2);
        generateFetchAccessTokenMethod(oauthAdapter, typeElement, oauth2);
        generateHasTokenExpiredMethod(oauthAdapter, oauth2);
        generateResetMethod(oauthAdapter, oauth2);
    }

    private DefinedClass getOAuthAdapterClass(DevkitTypeElement typeElement) {
        String lifecycleAdapterName = context.getNameUtils().generateClassName(typeElement, ".config", "OAuth2Adapter");
        Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(lifecycleAdapterName));

        DefinedClass classToExtend = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));

        DefinedClass oauthAdapter = pkg._class(context.getNameUtils().getClassName(lifecycleAdapterName), classToExtend);
        oauthAdapter._implements(MuleContextAware.class);
        oauthAdapter._implements(Startable.class);
        oauthAdapter._implements(Initialisable.class);
        oauthAdapter._implements(Stoppable.class);

        context.setClassRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement), oauthAdapter);

        oauthAdapter.javadoc().add("A {@code " + oauthAdapter.name() + "} is a wrapper around ");
        oauthAdapter.javadoc().add(ref(typeElement.asType()));
        oauthAdapter.javadoc().add(" that adds OAuth capabilites to the pojo.");

        return oauthAdapter;
    }

    private void muleContextField(DefinedClass oauthAdapter) {
        new FieldBuilder(oauthAdapter).type(MuleContext.class).name("muleContext").setter().build();
    }

    private void authorizationCodeField(DefinedClass oauthAdapter) {
        new FieldBuilder(oauthAdapter).type(String.class).name(VERIFIER_FIELD_NAME).getter().build();
    }

    private void redirectUrlField(DefinedClass oauthAdapter) {
        new FieldBuilder(oauthAdapter).type(String.class).name(REDIRECT_URL_FIELD_NAME).getter().build();
    }

    private void oauthAuthorizationCoderPatternConstant(DefinedClass oauthAdapter, OAuth2 oauth2) {
        new FieldBuilder(oauthAdapter).type(Pattern.class).name(AUTH_CODE_PATTERN_FIELD_NAME).staticField().finalField().
                initialValue(ref(Pattern.class).staticInvoke("compile").arg(oauth2.verifierRegex())).build();
    }

    private void oauthAcessTokenPatternConstant(DefinedClass oauthAdapter, OAuth2 oauth2) {
        new FieldBuilder(oauthAdapter).type(Pattern.class).name(ACCESS_CODE_PATTERN_FIELD_NAME).staticField().finalField().
                initialValue(ref(Pattern.class).staticInvoke("compile").arg(oauth2.accessTokenRegex())).build();
    }

    private void expirationPatternConstant(DefinedClass oauthAdapter, OAuth2 oauth2) {
        if (!StringUtils.isEmpty(oauth2.expirationRegex())) {
            new FieldBuilder(oauthAdapter).type(Pattern.class).name(EXPIRATION_TIME_PATTERN_FIELD_NAME).staticField().finalField().
                    initialValue(ref(Pattern.class).staticInvoke("compile").arg(oauth2.expirationRegex())).build();
        }
    }

    private void oauthCallbackField(DefinedClass oauthAdapter) {
        new FieldBuilder(oauthAdapter).type(HttpCallback.class).name(CALLBACK_FIELD_NAME).build();
    }

    private void accessTokenField(DefinedClass oauthAdapter) {
        new FieldBuilder(oauthAdapter).type(String.class).name(ACCESS_TOKEN_FIELD_NAME).getterAndSetter().build();
    }

    private void expirationField(DefinedClass oauthAdapter, OAuth2 oauth2) {
        if (!StringUtils.isEmpty(oauth2.expirationRegex())) {
            new FieldBuilder(oauthAdapter).type(Date.class).name(EXPIRATION_FIELD_NAME).setter().build();
        }
    }

    private void generateStartMethod(DefinedClass oauthAdapter) {
        Method start = oauthAdapter.method(Modifier.PUBLIC, this.context.getCodeModel().VOID, "start");
        start._throws(MuleException.class);
        start.body().invoke(ExpressionFactory._super(), ("start"));
        FieldVariable callbackField = oauthAdapter.fields().get(CALLBACK_FIELD_NAME);
        start.body().invoke(callbackField, "start");
        start.body().assign(oauthAdapter.fields().get(REDIRECT_URL_FIELD_NAME), callbackField.invoke("getUrl"));
    }

    private void generateStopMethod(DefinedClass oauthAdapter) {
        Method start = oauthAdapter.method(Modifier.PUBLIC, this.context.getCodeModel().VOID, "stop");
        start._throws(MuleException.class);
        start.body().invoke(ExpressionFactory._super(), ("stop"));
        start.body().invoke(oauthAdapter.fields().get(CALLBACK_FIELD_NAME), "stop");
    }

    private void generateInitialiseMethod(DefinedClass oauthAdapter, DefinedClass messageProcessor, OAuth2 oauth2) {
        Method initialise = oauthAdapter.method(Modifier.PUBLIC, this.context.getCodeModel().VOID, "initialise");
        if (ref(Initialisable.class).isAssignableFrom(oauthAdapter._extends())) {
            initialise.body().invoke(ExpressionFactory._super(), "initialise");
        }
        Invocation domain = ExpressionFactory.invoke("get" + StringUtils.capitalize(HttpCallbackGenerator.DOMAIN_FIELD_NAME));
        Invocation port = ExpressionFactory.invoke("get" + StringUtils.capitalize(HttpCallbackGenerator.PORT_FIELD_NAME));
        FieldVariable callback = oauthAdapter.fields().get(CALLBACK_FIELD_NAME);
        FieldVariable muleContext = oauthAdapter.fields().get(MULE_CONTEXT_FIELD_NAME);
        if (StringUtils.isEmpty(oauth2.callbackPath())) {
            initialise.body().assign(callback, ExpressionFactory._new(context.getClassForRole(HttpCallbackGenerator.HTTP_CALLBACK_ROLE)).
                    arg(ExpressionFactory._new(messageProcessor)).arg(muleContext).arg(domain).arg(port));
        } else {
            initialise.body().assign(callback, ExpressionFactory._new(context.getClassForRole(HttpCallbackGenerator.HTTP_CALLBACK_ROLE)).
                    arg(ExpressionFactory._new(messageProcessor)).arg(muleContext).arg(domain).arg(port).arg(oauth2.callbackPath()));
        }
    }

    private DefinedClass generateMessageProcessorInnerClass(DefinedClass oauthAdapter) throws GenerationException {
        DefinedClass messageProcessor;
        try {
            messageProcessor = oauthAdapter._class(Modifier.PRIVATE, "OnOAuthCallbackMessageProcessor")._implements(ref(MessageProcessor.class));
        } catch (ClassAlreadyExistsException e) {
            throw new GenerationException(e); // This wont happen
        }

        Method processMethod = messageProcessor.method(Modifier.PUBLIC, ref(MuleEvent.class), "process")._throws(ref(MuleException.class));
        Variable event = processMethod.param(ref(MuleEvent.class), "event");

        TryStatement tryToExtractVerifier = processMethod.body()._try();
        tryToExtractVerifier.body().assign(oauthAdapter.fields().get(VERIFIER_FIELD_NAME), ExpressionFactory.invoke("extractAuthorizationCode").arg(event.invoke("getMessageAsString")));
        CatchBlock catchBlock = tryToExtractVerifier._catch(ref(Exception.class));
        Variable exceptionCaught = catchBlock.param("e");
        catchBlock.body()._throw(ExpressionFactory._new(
                ref(MessagingException.class)).
                arg(ref(MessageFactory.class).staticInvoke("createStaticMessage").arg("Could not extract OAuth verifier")).arg(event).arg(exceptionCaught));

        processMethod.body()._return(event);

        Method extractMethod = messageProcessor.method(Modifier.PRIVATE, ref(String.class), "extractAuthorizationCode")._throws(ref(Exception.class));
        Variable response = extractMethod.param(String.class, "response");
        Variable matcher = extractMethod.body().decl(ref(Matcher.class), "matcher", oauthAdapter.fields().get(AUTH_CODE_PATTERN_FIELD_NAME).invoke("matcher").arg(response));
        Conditional ifVerifierFound = extractMethod.body()._if(Op.cand(matcher.invoke("find"), Op.gte(matcher.invoke("groupCount"), ExpressionFactory.lit(1))));
        Invocation group = matcher.invoke("group").arg(ExpressionFactory.lit(1));
        ifVerifierFound._then()._return(ref(URLDecoder.class).staticInvoke("decode").arg(group).arg(ENCODING));
        ifVerifierFound._else()._throw(ExpressionFactory._new(
                ref(Exception.class)).arg(ref(String.class).staticInvoke("format").arg("OAuth authorization code could not be extracted from: %s").arg(response)));
        return messageProcessor;
    }

    private void generateGetAuthorizationUrlMethod(DefinedClass oauthAdapter, DevkitTypeElement typeElement, OAuth2 oauth2) {
        Method getAuthorizationUrl = oauthAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, GET_AUTHORIZATION_URL_METHOD_NAME);
        getAuthorizationUrl.type(ref(String.class));

        Variable urlBuilder = getAuthorizationUrl.body().decl(ref(StringBuilder.class), "urlBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
        getAuthorizationUrl.body().invoke(urlBuilder, "append").arg(oauth2.authorizationUrl());
        getAuthorizationUrl.body().invoke(urlBuilder, "append").arg("?");
        getAuthorizationUrl.body().invoke(urlBuilder, "append").arg("response_type=code&");
        getAuthorizationUrl.body().invoke(urlBuilder, "append").arg("client_id=");
        getAuthorizationUrl.body().invoke(urlBuilder, "append").arg(ExpressionFactory.invoke(getterMethodForFieldAnnotatedWith(typeElement, OAuthConsumerKey.class)));
        getAuthorizationUrl.body().invoke(urlBuilder, "append").arg("&redirect_uri=");
        getAuthorizationUrl.body().invoke(urlBuilder, "append").arg(oauthAdapter.fields().get(REDIRECT_URL_FIELD_NAME));

        if (typeElement.hasFieldAnnotatedWith(OAuthScope.class)) {
            Variable scope = getAuthorizationUrl.body().decl(ref(String.class), "scope", ExpressionFactory.invoke(getterMethodForFieldAnnotatedWith(typeElement, OAuthScope.class)));
            Block ifScopeNotNull = getAuthorizationUrl.body()._if(Op.ne(scope, ExpressionFactory._null()))._then();
            ifScopeNotNull.invoke(urlBuilder, "append").arg("&scope=");
            ifScopeNotNull.invoke(urlBuilder, "append").arg(scope);
        }

        getAuthorizationUrl.body()._return(urlBuilder.invoke("toString"));
    }

    private void generateFetchAccessTokenMethod(DefinedClass oauthAdapter, DevkitTypeElement typeElement, OAuth2 oauth2) {
        Method fetchAccessToken = oauthAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, "fetchAccessToken");

        TryStatement tryStatement = fetchAccessToken.body()._try();

        Block body = tryStatement.body();
        Variable conn = body.decl(ref(HttpURLConnection.class), "conn",
                ExpressionFactory.cast(ref(HttpURLConnection.class), ExpressionFactory._new(ref(URL.class)).arg(oauth2.accessTokenUrl()).invoke("openConnection")));

        body.invoke(conn, "setRequestMethod").arg("POST");
        body.invoke(conn, "setDoOutput").arg(ExpressionFactory.lit(true));

        Invocation consumerKey = ExpressionFactory.invoke(getterMethodForFieldAnnotatedWith(typeElement, OAuthConsumerKey.class));
        Invocation consumerSecret = ExpressionFactory.invoke(getterMethodForFieldAnnotatedWith(typeElement, OAuthConsumerSecret.class));

        Variable builder = body.decl(ref(StringBuilder.class), "builder", ExpressionFactory._new(ref(StringBuilder.class)));

        body.invoke(builder, "append").arg("code=");
        body.invoke(builder, "append").arg(ref(URLEncoder.class).staticInvoke("encode").arg(oauthAdapter.fields().get(VERIFIER_FIELD_NAME)).arg(ENCODING));

        body.invoke(builder, "append").arg("&client_id=");
        body.invoke(builder, "append").arg(ref(URLEncoder.class).staticInvoke("encode").arg(consumerKey).arg(ENCODING));

        body.invoke(builder, "append").arg("&client_secret=");
        body.invoke(builder, "append").arg(ref(URLEncoder.class).staticInvoke("encode").arg(consumerSecret).arg(ENCODING));

        body.invoke(builder, "append").arg("&grant_type=");
        body.invoke(builder, "append").arg(ref(URLEncoder.class).staticInvoke("encode").arg(GRANT_TYPE).arg(ENCODING));

        body.invoke(builder, "append").arg("&redirect_uri=");
        body.invoke(builder, "append").arg(ref(URLEncoder.class).staticInvoke("encode").arg(oauthAdapter.fields().get(REDIRECT_URL_FIELD_NAME)).arg(ENCODING));

        Variable out = body.decl(ref(OutputStreamWriter.class), "out", ExpressionFactory._new(ref(OutputStreamWriter.class)).arg(conn.invoke("getOutputStream")));
        body.invoke(out, "write").arg(builder.invoke("toString"));
        body.invoke(out, "close");

        Variable response = body.decl(ref(String.class), "response", ref(IOUtils.class).staticInvoke("toString").arg(conn.invoke("getInputStream")));

        Variable matcher = body.decl(ref(Matcher.class), "matcher", oauthAdapter.fields().get(ACCESS_CODE_PATTERN_FIELD_NAME).invoke("matcher").arg(response));
        Conditional ifAccessTokenFound = body._if(Op.cand(matcher.invoke("find"), Op.gte(matcher.invoke("groupCount"), ExpressionFactory.lit(1))));
        Invocation group = matcher.invoke("group").arg(ExpressionFactory.lit(1));
        ifAccessTokenFound._then().assign(oauthAdapter.fields().get(ACCESS_TOKEN_FIELD_NAME), ref(URLDecoder.class).staticInvoke("decode").arg(group).arg(ENCODING));
        ifAccessTokenFound._else()._throw(ExpressionFactory._new(
                ref(Exception.class)).arg(ref(String.class).staticInvoke("format").arg("OAuth access token could not be extracted from: %s").arg(response)));


        if (!StringUtils.isEmpty(oauth2.expirationRegex())) {
            Variable expirationMatcher = body.decl(ref(Matcher.class), "expirationMatcher", oauthAdapter.fields().get(EXPIRATION_TIME_PATTERN_FIELD_NAME).invoke("matcher").arg(response));
            Conditional ifExpirationFound = body._if(Op.cand(expirationMatcher.invoke("find"), Op.gte(expirationMatcher.invoke("groupCount"), ExpressionFactory.lit(1))));
            Variable seconds = ifExpirationFound._then().decl(ref(Long.class), "expirationSecsAhead",
                    ref(Long.class).staticInvoke("parseLong").arg(expirationMatcher.invoke("group").arg(ExpressionFactory.lit(1))));
            ifExpirationFound._then().assign(oauthAdapter.fields().get(EXPIRATION_FIELD_NAME), ExpressionFactory._new(ref(Date.class)).arg(
                    Op.plus(ref(System.class).staticInvoke("currentTimeMillis"), Op.mul(seconds, ExpressionFactory.lit(1000)))));
        }
        generateReThrow(tryStatement, Exception.class, RuntimeException.class);
    }

    private void generateHasTokenExpiredMethod(DefinedClass oauthAdapter, OAuth2 oauth2) {
        Method hasTokenExpired = oauthAdapter.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, HAS_TOKEN_EXPIRED_METHOD_NAME);
        if (!StringUtils.isEmpty(oauth2.expirationRegex())) {
            FieldVariable expirationDate = oauthAdapter.fields().get(EXPIRATION_FIELD_NAME);
            hasTokenExpired.body()._return(Op.cand(
                    Op.ne(expirationDate, ExpressionFactory._null()),
                    expirationDate.invoke("before").arg(ExpressionFactory._new(ref(Date.class)))));
        } else {
            hasTokenExpired.body()._return(ExpressionFactory.FALSE);
        }
    }

    private void generateResetMethod(DefinedClass oauthAdapter, OAuth2 oauth2) {
        Method reset = oauthAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, RESET_METHOD_NAME);
        if (!StringUtils.isEmpty(oauth2.expirationRegex())) {
            reset.body().assign(oauthAdapter.fields().get(EXPIRATION_FIELD_NAME), ExpressionFactory._null());
        }
        reset.body().assign(oauthAdapter.fields().get(VERIFIER_FIELD_NAME), ExpressionFactory._null());
        reset.body().assign(oauthAdapter.fields().get(ACCESS_TOKEN_FIELD_NAME), ExpressionFactory._null());
    }

    private void generateReThrow(TryStatement tryStatement, Class<? extends Exception> exceptionToCatch, Class<? extends Exception> exceptionToThrow) {
        CatchBlock catchBlock = tryStatement._catch(ref(exceptionToCatch));
        Variable caughtException = catchBlock.param("e");
        catchBlock.body()._throw(ExpressionFactory._new(ref(exceptionToThrow)).arg(caughtException));
    }
}