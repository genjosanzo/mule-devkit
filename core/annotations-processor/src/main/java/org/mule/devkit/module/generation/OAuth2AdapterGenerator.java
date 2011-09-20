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
import org.mule.api.adapter.OAuth2Adapter;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.api.annotations.oauth.OAuthConsumerKey;
import org.mule.api.annotations.oauth.OAuthConsumerSecret;
import org.mule.api.annotations.oauth.OAuthScope;
import org.mule.devkit.generation.DevkitTypeElement;
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
import org.mule.util.IOUtils;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OAuth2AdapterGenerator extends AbstractOAuthAdapterGenerator {

    @Override
    protected boolean shouldGenerate(DevkitTypeElement typeElement) {
        return typeElement.hasAnnotation(OAuth2.class);
    }

    @Override
    protected void doGenerate(DevkitTypeElement typeElement) throws GenerationException {
        DefinedClass oauthAdapter = getOAuthAdapterClass(typeElement, "OAuth2Adapter", OAuth2Adapter.class);
        OAuth2 oauth2 = typeElement.getAnnotation(OAuth2.class);

        authorizationCodePatternConstant(oauthAdapter, oauth2.verifierRegex());

        accessTokenPatternConstant(oauthAdapter, oauth2);

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
        generateInitialiseMethod(oauthAdapter, messageProcessor, oauth2.callbackPath());

        generateGetAuthorizationUrlMethod(oauthAdapter, typeElement, oauth2);
        generateFetchAccessTokenMethod(oauthAdapter, typeElement, oauth2);
        generateHasTokenExpiredMethod(oauthAdapter, oauth2);
        generateResetMethod(oauthAdapter, oauth2);
    }

    private void accessTokenPatternConstant(DefinedClass oauthAdapter, OAuth2 oauth2) {
        new FieldBuilder(oauthAdapter).type(Pattern.class).name(ACCESS_CODE_PATTERN_FIELD_NAME).staticField().finalField().
                initialValue(ref(Pattern.class).staticInvoke("compile").arg(oauth2.accessTokenRegex())).build();
    }

    private void expirationPatternConstant(DefinedClass oauthAdapter, OAuth2 oauth2) {
        if (!StringUtils.isEmpty(oauth2.expirationRegex())) {
            new FieldBuilder(oauthAdapter).type(Pattern.class).name(EXPIRATION_TIME_PATTERN_FIELD_NAME).staticField().finalField().
                    initialValue(ref(Pattern.class).staticInvoke("compile").arg(oauth2.expirationRegex())).build();
        }
    }

    private void expirationField(DefinedClass oauthAdapter, OAuth2 oauth2) {
        if (!StringUtils.isEmpty(oauth2.expirationRegex())) {
            new FieldBuilder(oauthAdapter).type(Date.class).name(EXPIRATION_FIELD_NAME).setter().build();
        }
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