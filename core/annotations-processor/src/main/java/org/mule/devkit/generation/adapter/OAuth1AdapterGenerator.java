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
import oauth.signpost.signature.AuthorizationHeaderSigningStrategy;
import oauth.signpost.signature.HmacSha1MessageSigner;
import oauth.signpost.signature.PlainTextMessageSigner;
import oauth.signpost.signature.QueryStringSigningStrategy;
import org.mule.api.adapter.OAuth1Adapter;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuthConsumerKey;
import org.mule.api.annotations.oauth.OAuthConsumerSecret;
import org.mule.api.annotations.oauth.OAuthMessageSigner;
import org.mule.api.annotations.oauth.OAuthScope;
import org.mule.api.annotations.oauth.OAuthSigningStrategy;
import org.mule.devkit.generation.AbstractOAuthAdapterGenerator;
import org.mule.devkit.generation.DevkitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.CatchBlock;
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
    protected boolean shouldGenerate(DevkitTypeElement typeElement) {
        return typeElement.hasAnnotation(OAuth.class);
    }

    @Override
    protected void doGenerate(DevkitTypeElement typeElement) throws GenerationException {
        DefinedClass oauthAdapter = getOAuthAdapterClass(typeElement, "OAuth1Adapter", OAuth1Adapter.class);
        OAuth oauth = typeElement.getAnnotation(OAuth.class);
        authorizationCodePatternConstant(oauthAdapter, oauth.verifierRegex());
        muleContextField(oauthAdapter);
        FieldVariable requestToken = requestTokenField(oauthAdapter);
        FieldVariable requestTokenSecret = requestTokenSecretField(oauthAdapter);
        FieldVariable oauthVerifier = authorizationCodeField(oauthAdapter);
        FieldVariable redirectUrl = redirectUrlField(oauthAdapter);
        accessTokenField(oauthAdapter);
        oauthAccessTokenSecretField(oauthAdapter);
        consumerField(oauthAdapter);
        oauthCallbackField(oauthAdapter);

        DefinedClass messageProcessor = generateMessageProcessorInnerClass(oauthAdapter);

        Method createConsumer = generateCreateConsumerMethod(oauthAdapter, oauth, typeElement);

        generateStartMethod(oauthAdapter);
        generateStopMethod(oauthAdapter);
        generateInitialiseMethod(oauthAdapter, messageProcessor, createConsumer, oauth);

        generateGetAuthorizationUrlMethod(oauthAdapter, requestToken, requestTokenSecret, redirectUrl, typeElement, oauth);
        generateFetchAccessTokenMethod(oauthAdapter, requestToken, requestTokenSecret, oauthVerifier, typeElement, oauth);
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

    private void generateGetAuthorizationUrlMethod(DefinedClass oauthAdapter, FieldVariable requestToken, FieldVariable requestTokenSecret, FieldVariable redirectUrl, DevkitTypeElement typeElement, OAuth oauth) {
        Method getAuthorizationUrl = oauthAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, GET_AUTHORIZATION_URL_METHOD_NAME);
        getAuthorizationUrl.type(ref(String.class));
        Variable provider = generateProvider(oauth, getAuthorizationUrl.body(), typeElement);
        Variable authorizationUrl = getAuthorizationUrl.body().decl(ref(String.class), "authorizationUrl");
        TryStatement tryRetrieveRequestToken = getAuthorizationUrl.body()._try();
        FieldVariable consumer = oauthAdapter.fields().get(CONSUMER_FIELD_NAME);
        tryRetrieveRequestToken.body().assign(authorizationUrl, provider.invoke("retrieveRequestToken").arg(consumer).arg(redirectUrl));
        generateReThrow(tryRetrieveRequestToken, Exception.class, RuntimeException.class);
        getAuthorizationUrl.body().assign(requestToken, consumer.invoke("getToken"));
        getAuthorizationUrl.body().assign(requestTokenSecret, consumer.invoke("getTokenSecret"));
        getAuthorizationUrl.body()._return(authorizationUrl);
    }

    private void generateFetchAccessTokenMethod(DefinedClass oauthAdapter, FieldVariable requestToken, FieldVariable requestTokenSecret, FieldVariable oauthVerifier, DevkitTypeElement typeElement, OAuth oauth) {
        Method fetchAccessToken = oauthAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, FETCH_ACCESS_TOKEN_METHOD_NAME);
        Variable provider = generateProvider(oauth, fetchAccessToken.body(), typeElement);
        FieldVariable consumer = oauthAdapter.fields().get(CONSUMER_FIELD_NAME);
        fetchAccessToken.body().invoke(consumer, "setTokenWithSecret").arg(requestToken).arg(requestTokenSecret);
        TryStatement tryRetrieveAccessToken = fetchAccessToken.body()._try();
        tryRetrieveAccessToken.body().invoke(provider, "retrieveAccessToken").arg(consumer).arg(oauthVerifier);
        generateReThrow(tryRetrieveAccessToken, Exception.class, RuntimeException.class);
        fetchAccessToken.body().assign(oauthAdapter.fields().get(OAUTH_ACCESS_TOKEN_FIELD_NAME), consumer.invoke("getToken"));
        fetchAccessToken.body().assign(oauthAdapter.fields().get(OAUTH_ACCESS_TOKEN_SECRET_FIELD_NAME), consumer.invoke("getTokenSecret"));
    }

    private Variable generateProvider(OAuth oauth, Block block, DevkitTypeElement typeElement) {
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