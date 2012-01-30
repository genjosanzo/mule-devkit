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

package org.mule.devkit.generation.adapter;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.signature.AuthorizationHeaderSigningStrategy;
import oauth.signpost.signature.HmacSha1MessageSigner;
import oauth.signpost.signature.PlainTextMessageSigner;
import oauth.signpost.signature.QueryStringSigningStrategy;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuthConsumerKey;
import org.mule.api.annotations.oauth.OAuthConsumerSecret;
import org.mule.api.annotations.oauth.OAuthMessageSigner;
import org.mule.api.annotations.oauth.OAuthScope;
import org.mule.api.annotations.oauth.OAuthSigningStrategy;
import org.mule.api.oauth.OAuth1Adapter;
import org.mule.api.oauth.UnableToAcquireAccessTokenException;
import org.mule.api.oauth.UnableToAcquireRequestTokenException;
import org.mule.devkit.generation.AbstractOAuthAdapterGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Variable;
import org.mule.devkit.model.code.builders.FieldBuilder;

import javax.lang.model.element.TypeElement;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class OAuth1AdapterGenerator extends AbstractOAuthAdapterGenerator {


    private static final String REQUEST_TOKEN_FIELD_NAME = "requestToken";
    private static final String REQUEST_TOKEN_SECRET_FIELD_NAME = "requestTokenSecret";
    private static final String CONSUMER_FIELD_NAME = "consumer";

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return typeElement.hasAnnotation(OAuth.class);
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) throws GenerationException {
        DefinedClass oauthAdapter = getOAuthAdapterClass(typeElement, "OAuth1Adapter", OAuth1Adapter.class);
        OAuth oauth = typeElement.getAnnotation(OAuth.class);
        authorizationCodePatternConstant(oauthAdapter, oauth.verifierRegex());
        muleContextField(oauthAdapter);

        // logger field
        FieldVariable logger = generateLoggerField(oauthAdapter);

        FieldVariable requestToken = requestTokenField(oauthAdapter);
        FieldVariable requestTokenSecret = requestTokenSecretField(oauthAdapter);
        FieldVariable oauthVerifier = authorizationCodeField(oauthAdapter);
        FieldVariable saveAccessTokenCallback = saveAccessTokenCallbackField(oauthAdapter);
        FieldVariable restoreAccessTokenCallback = restoreAccessTokenCallbackField(oauthAdapter);
        FieldVariable redirectUrl = redirectUrlField(oauthAdapter);
        FieldVariable oauthAccessToken = accessTokenField(oauthAdapter);
        FieldVariable oauthAccessTokenSecret = oauthAccessTokenSecretField(oauthAdapter);
        consumerField(oauthAdapter);
        oauthCallbackField(oauthAdapter);

        DefinedClass messageProcessor = generateMessageProcessorInnerClass(oauthAdapter);

        Method createConsumer = generateCreateConsumerMethod(oauthAdapter, oauth, typeElement);

        generateStartMethod(oauthAdapter);
        generateStopMethod(oauthAdapter);
        generateInitialiseMethod(oauthAdapter, messageProcessor, createConsumer, oauth);

        generateGetAuthorizationUrlMethod(oauthAdapter, requestToken, requestTokenSecret, redirectUrl, typeElement, oauth, logger);
        generateRestoreAccessTokenMethod(oauthAdapter, restoreAccessTokenCallback, logger);
        generateFetchAccessTokenMethod(oauthAdapter, requestToken, requestTokenSecret, saveAccessTokenCallback, oauthVerifier, typeElement, oauth, logger);

        generateHasBeenAuthorizedMethod(oauthAdapter, oauthAccessToken);
        generateOverrides(typeElement, oauthAdapter, oauthAccessToken, oauthAccessTokenSecret);
    }

    private FieldVariable requestTokenField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(String.class).name(REQUEST_TOKEN_FIELD_NAME).build();
    }

    private FieldVariable requestTokenSecretField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(String.class).name(REQUEST_TOKEN_SECRET_FIELD_NAME).build();
    }

    private FieldVariable oauthAccessTokenSecretField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(String.class).name(OAUTH_ACCESS_TOKEN_SECRET_FIELD_NAME).getterAndSetter().build();
    }

    private FieldVariable consumerField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(OAuthConsumer.class).name(CONSUMER_FIELD_NAME).build();
    }

    private Method generateCreateConsumerMethod(DefinedClass oauthAdapter, OAuth oauth, TypeElement typeElement) {
        Method createConsumer = oauthAdapter.method(Modifier.PRIVATE, context.getCodeModel().VOID, "createConsumer");
        Invocation getConsumerKey = ExpressionFactory.invoke(getterMethodForFieldAnnotatedWith(typeElement, OAuthConsumerKey.class));
        Invocation getConsumerSecret = ExpressionFactory.invoke(getterMethodForFieldAnnotatedWith(typeElement, OAuthConsumerSecret.class));
        FieldVariable consumer = oauthAdapter.fields().get(CONSUMER_FIELD_NAME);
        createConsumer.body().assign(consumer, ExpressionFactory._new(ref(DefaultOAuthConsumer.class)).arg(getConsumerKey).arg(getConsumerSecret));
        if (oauth.messageSigner().equals(OAuthMessageSigner.HMAC_SHA1)) {
            createConsumer.body().invoke(consumer, "setMessageSigner").arg(ExpressionFactory._new(ref(HmacSha1MessageSigner.class)));
        } else if (oauth.messageSigner().equals(OAuthMessageSigner.PLAIN_TEXT)) {
            createConsumer.body().invoke(consumer, "setMessageSigner").arg(ExpressionFactory._new(ref(PlainTextMessageSigner.class)));
        }
        if (oauth.signingStrategy().equals(OAuthSigningStrategy.AUTHORIZATION_HEADER)) {
            createConsumer.body().invoke(consumer, "setSigningStrategy").arg(ExpressionFactory._new(ref(AuthorizationHeaderSigningStrategy.class)));
        } else if (oauth.signingStrategy().equals(OAuthSigningStrategy.QUERY_STRING)) {
            createConsumer.body().invoke(consumer, "setSigningStrategy").arg(ExpressionFactory._new(ref(QueryStringSigningStrategy.class)));
        }
        return createConsumer;
    }

    private void generateInitialiseMethod(DefinedClass oauthAdapter, DefinedClass messageProcessor, Method createConsumer, OAuth oauth) {
        Method initialise = generateInitialiseMethod(oauthAdapter, messageProcessor, oauth.callbackPath());
        initialise.body().invoke(createConsumer);
    }

    private void generateGetAuthorizationUrlMethod(DefinedClass oauthAdapter, FieldVariable requestToken, FieldVariable requestTokenSecret, FieldVariable redirectUrl, DevKitTypeElement typeElement, OAuth oauth, FieldVariable logger) {
        Method getAuthorizationUrl = oauthAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, GET_AUTHORIZATION_URL_METHOD_NAME);
        getAuthorizationUrl._throws(ref(UnableToAcquireRequestTokenException.class));
        
        getAuthorizationUrl.type(ref(String.class));
        Variable provider = generateProvider(oauth, getAuthorizationUrl.body(), typeElement);
        Variable authorizationUrl = getAuthorizationUrl.body().decl(ref(String.class), "authorizationUrl");
        TryStatement tryRetrieveRequestToken = getAuthorizationUrl.body()._try();
        FieldVariable consumer = oauthAdapter.fields().get(CONSUMER_FIELD_NAME);
        
        Conditional ifDebugEnabled = tryRetrieveRequestToken.body()._if(logger.invoke("isDebugEnabled"));
        Variable messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("Attempting to acquire a request token "));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("[consumer = ")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(consumer.invoke("getConsumerKey")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("] ")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("[consumerSecret = ")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(consumer.invoke("getConsumerSecret")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("] ")));
        ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));

        tryRetrieveRequestToken.body().assign(authorizationUrl, provider.invoke("retrieveRequestToken").arg(consumer).arg(redirectUrl));
        generateReThrow(tryRetrieveRequestToken, OAuthMessageSignerException.class, UnableToAcquireRequestTokenException.class);
        generateReThrow(tryRetrieveRequestToken, OAuthNotAuthorizedException.class, UnableToAcquireRequestTokenException.class);
        generateReThrow(tryRetrieveRequestToken, OAuthExpectationFailedException.class, UnableToAcquireRequestTokenException.class);
        generateReThrow(tryRetrieveRequestToken, OAuthCommunicationException.class, UnableToAcquireRequestTokenException.class);

        ifDebugEnabled = tryRetrieveRequestToken.body()._if(logger.invoke("isDebugEnabled"));
        messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("Request token acquired "));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("[requestToken = ")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(consumer.invoke("getToken")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("] ")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("[requestTokenSecret = ")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(consumer.invoke("getTokenSecret")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("] ")));
        ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));

        getAuthorizationUrl.body().assign(requestToken, consumer.invoke("getToken"));
        getAuthorizationUrl.body().assign(requestTokenSecret, consumer.invoke("getTokenSecret"));
        getAuthorizationUrl.body()._return(authorizationUrl);
    }
    
    private void generateRestoreAccessTokenMethod(DefinedClass oauthAdapter, FieldVariable restoreAccessTokenCallbackField, FieldVariable logger)
    {
        Method restoreAccessTokenMethod = oauthAdapter.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, "restoreAccessToken");

        Conditional ifRestoreCallbackNotNull = restoreAccessTokenMethod.body()._if(Op.ne(restoreAccessTokenCallbackField, ExpressionFactory._null()));

        Conditional ifDebugEnabled = ifRestoreCallbackNotNull._then()._if(logger.invoke("isDebugEnabled"));
        Variable messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("Attempting to restore access token..."));
        ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));

        TryStatement tryToRestore = ifRestoreCallbackNotNull._then()._try();
        tryToRestore.body().add(restoreAccessTokenCallbackField.invoke("restoreAccessToken"));

        tryToRestore.body().assign(oauthAdapter.fields().get(OAUTH_ACCESS_TOKEN_FIELD_NAME), restoreAccessTokenCallbackField.invoke("getAccessToken"));
        tryToRestore.body().assign(oauthAdapter.fields().get(OAUTH_ACCESS_TOKEN_SECRET_FIELD_NAME), restoreAccessTokenCallbackField.invoke("getAccessTokenSecret"));


        ifDebugEnabled = tryToRestore.body()._if(logger.invoke("isDebugEnabled"));
        messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("Access token and secret has been restored successfully "));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("[accessToken = ")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(restoreAccessTokenCallbackField.invoke("getAccessToken")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("] ")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("[accessTokenSecret = ")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(restoreAccessTokenCallbackField.invoke("getAccessTokenSecret")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("] ")));
        ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));

        tryToRestore.body()._return(ExpressionFactory.TRUE);

        CatchBlock logIfCannotRestore = tryToRestore._catch(ref(Exception.class));
        Variable e = logIfCannotRestore.param("e");
        logIfCannotRestore.body().add(logger.invoke("error").arg("Cannot restore access token, an unexpected error occurred").arg(e));
        ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));

        restoreAccessTokenMethod.body()._return(ExpressionFactory.FALSE);
    }

    private void generateFetchAccessTokenMethod(DefinedClass oauthAdapter, FieldVariable requestToken, FieldVariable requestTokenSecret, FieldVariable saveAccessTokenCallback, FieldVariable oauthVerifier, DevKitTypeElement typeElement, OAuth oauth, FieldVariable logger) {
        Method fetchAccessToken = oauthAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, FETCH_ACCESS_TOKEN_METHOD_NAME);
        fetchAccessToken._throws(ref(UnableToAcquireAccessTokenException.class));

        fetchAccessToken.body().invoke("restoreAccessToken");

        Conditional ifAccessTokenNullOrSecretNull = fetchAccessToken.body()._if(Op.cor(Op.eq(oauthAdapter.fields().get(OAUTH_ACCESS_TOKEN_FIELD_NAME), ExpressionFactory._null()),
                Op.eq(oauthAdapter.fields().get(OAUTH_ACCESS_TOKEN_SECRET_FIELD_NAME), ExpressionFactory._null())));

        Variable provider = generateProvider(oauth, ifAccessTokenNullOrSecretNull._then(), typeElement);
        FieldVariable consumer = oauthAdapter.fields().get(CONSUMER_FIELD_NAME);
        ifAccessTokenNullOrSecretNull._then().invoke(consumer, "setTokenWithSecret").arg(requestToken).arg(requestTokenSecret);
        TryStatement tryRetrieveAccessToken = ifAccessTokenNullOrSecretNull._then()._try();

        Conditional ifDebugEnabled = tryRetrieveAccessToken.body()._if(logger.invoke("isDebugEnabled"));
        Variable messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("Retrieving access token..."));
        ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));

        tryRetrieveAccessToken.body().invoke(provider, "retrieveAccessToken").arg(consumer).arg(oauthVerifier);
        generateReThrow(tryRetrieveAccessToken, OAuthMessageSignerException.class, UnableToAcquireAccessTokenException.class);
        generateReThrow(tryRetrieveAccessToken, OAuthNotAuthorizedException.class, UnableToAcquireAccessTokenException.class);
        generateReThrow(tryRetrieveAccessToken, OAuthExpectationFailedException.class, UnableToAcquireAccessTokenException.class);
        generateReThrow(tryRetrieveAccessToken, OAuthCommunicationException.class, UnableToAcquireAccessTokenException.class);

        ifAccessTokenNullOrSecretNull._then().assign(oauthAdapter.fields().get(OAUTH_ACCESS_TOKEN_FIELD_NAME), consumer.invoke("getToken"));
        ifAccessTokenNullOrSecretNull._then().assign(oauthAdapter.fields().get(OAUTH_ACCESS_TOKEN_SECRET_FIELD_NAME), consumer.invoke("getTokenSecret"));

        ifDebugEnabled = ifAccessTokenNullOrSecretNull._then()._if(logger.invoke("isDebugEnabled"));
        messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("Access token retrieved successfully "));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("[accessToken = ")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(oauthAdapter.fields().get(OAUTH_ACCESS_TOKEN_FIELD_NAME)));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("] ")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("[accessTokenSecret = ")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(oauthAdapter.fields().get(OAUTH_ACCESS_TOKEN_SECRET_FIELD_NAME)));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("] ")));

        Conditional ifSaveCallbackNotNull = ifAccessTokenNullOrSecretNull._then()._if(Op.ne(saveAccessTokenCallback, ExpressionFactory._null()));
        Invocation saveAccessToken = saveAccessTokenCallback.invoke("saveAccessToken").arg(oauthAdapter.fields().get(OAUTH_ACCESS_TOKEN_FIELD_NAME))
                .arg(oauthAdapter.fields().get(OAUTH_ACCESS_TOKEN_SECRET_FIELD_NAME));
        TryStatement tryToSave = ifSaveCallbackNotNull._then()._try();

        ifDebugEnabled = ifSaveCallbackNotNull._then()._if(logger.invoke("isDebugEnabled"));
        messageStringBuilder = ifDebugEnabled._then().decl(ref(StringBuilder.class), "messageStringBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg("Attempting to save access token..."));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("[accessToken = ")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(oauthAdapter.fields().get(OAUTH_ACCESS_TOKEN_FIELD_NAME)));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("] ")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("[accessTokenSecret = ")));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(oauthAdapter.fields().get(OAUTH_ACCESS_TOKEN_SECRET_FIELD_NAME)));
        ifDebugEnabled._then().add(messageStringBuilder.invoke("append").arg(ExpressionFactory.lit("] ")));
        ifDebugEnabled._then().add(logger.invoke("debug").arg(messageStringBuilder.invoke("toString")));

        tryToSave.body().add(saveAccessToken);
        CatchBlock logIfCannotSave = tryToSave._catch(ref(Exception.class));
        Variable e2 = logIfCannotSave.param("e");
        logIfCannotSave.body().add(logger.invoke("error").arg("Cannot save access token, an unexpected error occurred").arg(e2));
    }

    private Variable generateProvider(OAuth oauth, Block block, DevKitTypeElement typeElement) {
        Variable requestTokenUrl = block.decl(ref(String.class), "requestTokenUrl", ExpressionFactory.lit(oauth.requestTokenUrl()));

        if (typeElement.hasFieldAnnotatedWith(OAuthScope.class)) {
            Variable scope = block.decl(ref(String.class), "scope", ExpressionFactory.invoke(getterMethodForFieldAnnotatedWith(typeElement, OAuthScope.class)));
            Block ifScopeNotNull = block._if(Op.ne(scope, ExpressionFactory._null()))._then();
            TryStatement tryToEncodeScopeParam = ifScopeNotNull._try();

            Variable scopeParam = tryToEncodeScopeParam.body().decl(ref(String.class), "scopeParam", ExpressionFactory.lit("?scope=").invoke("concat").arg(ref(URLEncoder.class).staticInvoke("encode").arg(scope).arg("UTF-8")));
            tryToEncodeScopeParam.body().assign(requestTokenUrl, requestTokenUrl.invoke("concat").arg(scopeParam));

            generateReThrow(tryToEncodeScopeParam, UnsupportedEncodingException.class, RuntimeException.class);
        }

        Variable provider = block.decl(ref(OAuthProvider.class), "provider", ExpressionFactory._new(ref(DefaultOAuthProvider.class)).
                arg(requestTokenUrl).arg(oauth.accessTokenUrl()).arg(oauth.authorizationUrl()));
        block.invoke(provider, "setOAuth10a").arg(ExpressionFactory.TRUE);
        return provider;
    }

    private void generateReThrow(TryStatement tryStatement, Class<? extends Exception> exceptionToCatch, Class<? extends Exception> exceptionToThrow) {
        CatchBlock catchBlock = tryStatement._catch(ref(exceptionToCatch));
        Variable caughtException = catchBlock.param("e");
        catchBlock.body()._throw(ExpressionFactory._new(ref(exceptionToThrow)).arg(caughtException));
    }
}