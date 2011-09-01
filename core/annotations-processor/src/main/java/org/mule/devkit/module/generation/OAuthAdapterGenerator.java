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

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.signature.AuthorizationHeaderSigningStrategy;
import oauth.signpost.signature.HmacSha1MessageSigner;
import oauth.signpost.signature.PlainTextMessageSigner;
import oauth.signpost.signature.QueryStringSigningStrategy;
import org.apache.commons.lang.StringUtils;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.annotations.callback.HttpCallback;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuthConsumerKey;
import org.mule.api.annotations.oauth.OAuthConsumerSecret;
import org.mule.api.annotations.oauth.OAuthMessageSigner;
import org.mule.api.annotations.oauth.OAuthScope;
import org.mule.api.annotations.oauth.OAuthSigningStrategy;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.MessageFactory;
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
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Variable;
import org.mule.devkit.model.code.builders.FieldBuilder;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OAuthAdapterGenerator extends AbstractModuleGenerator {

    public static final String OAUTH_VERIFIER_FIELD_NAME = "oauthVerifier";
    public static final String OAUTH_ACCESS_TOKEN_FIELD_NAME = "accessToken";
    public static final String OAUTH_ACCESS_TOKEN_SECRET_FIELD_NAME = "accessTokenSecret";
    public static final String GET_AUTHORIZATION_URL_METHOD_NAME = "getAuthorizationUrl";
    public static final String FETCH_ACCESS_TOKEN_METHOD_NAME = "fetchAccessToken";
    private static final String REQUEST_TOKEN_FIELD_NAME = "requestToken";
    private static final String REQUEST_TOKEN_SECRET_FIELD_NAME = "requestTokenSecret";
    private static final String REDIRECT_URL_FIELD_NAME = "redirectUrl";
    private static final String CONSUMER_FIELD_NAME = "consumer";

    public void generate(Element element) throws GenerationException {
        OAuth oauth = element.getAnnotation(OAuth.class);
        if (oauth == null) {
            return;
        }
        DefinedClass oauthAdapter = getOAuthAdapterClass((TypeElement) element);
        FieldVariable pattern = oauthVerifierPatterConstant(oauthAdapter, oauth);
        FieldVariable muleContext = muleContextField(oauthAdapter);
        FieldVariable requestToken = requestTokenField(oauthAdapter);
        FieldVariable requestTokenSecret = requestTokenSecretField(oauthAdapter);
        FieldVariable oauthVerifier = oauthVerifierField(oauthAdapter);
        FieldVariable redirectUrl = redirectUrlField(oauthAdapter);
        FieldVariable accessToken = oauthAccessTokenField(oauthAdapter);
        FieldVariable accessTokenSecret = oauthAccessTokenSecretField(oauthAdapter);
        FieldVariable consumer = consumerField(oauthAdapter);
        FieldVariable oauthCallback = oauthCallbackField(oauthAdapter);

        DefinedClass messageProcessor = generateMessageProcessorInnerClass(oauthAdapter, oauthVerifier, pattern);
        TypeElement typeElement = (TypeElement) element;

        Method createConsumer = generateCreateConsumerMethod(oauthAdapter, consumer, oauth, typeElement);

        generateStartMethod(oauthAdapter, oauthCallback, redirectUrl);
        generateStopMethod(oauthAdapter, oauthCallback);
        generateInitialiseMethod(oauthAdapter, oauthCallback, messageProcessor, muleContext, createConsumer);

        generateGetAuthorizationUrlMethod(oauthAdapter, requestToken, requestTokenSecret, redirectUrl, consumer, typeElement, oauth);
        generateFetchAccessTokenMethod(oauthAdapter, requestToken, requestTokenSecret, oauthVerifier, consumer, typeElement, oauth);
    }

    private DefinedClass getOAuthAdapterClass(TypeElement typeElement) {
        String oauthAdapterName = context.getNameUtils().generateClassName(typeElement, ".config", "OAuthAdapter");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(oauthAdapterName));

        DefinedClass classToExtend = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));

        DefinedClass oauthAdapter = pkg._class(context.getNameUtils().getClassName(oauthAdapterName), classToExtend);
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

    private FieldVariable muleContextField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(MuleContext.class).name("muleContext").setter().build();
    }

    private FieldVariable requestTokenField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(String.class).name(REQUEST_TOKEN_FIELD_NAME).build();
    }

    private FieldVariable requestTokenSecretField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(String.class).name(REQUEST_TOKEN_SECRET_FIELD_NAME).build();
    }

    private FieldVariable oauthVerifierField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(String.class).name(OAUTH_VERIFIER_FIELD_NAME).getterAndSetter().build();
    }

    private FieldVariable redirectUrlField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(String.class).name(REDIRECT_URL_FIELD_NAME).getter().build();
    }

    private FieldVariable oauthAccessTokenField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(String.class).name(OAUTH_ACCESS_TOKEN_FIELD_NAME).getterAndSetter().build();
    }

    private FieldVariable oauthAccessTokenSecretField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(String.class).name(OAUTH_ACCESS_TOKEN_SECRET_FIELD_NAME).getterAndSetter().build();
    }

    private FieldVariable consumerField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(OAuthConsumer.class).name(CONSUMER_FIELD_NAME).build();
    }

    private FieldVariable oauthVerifierPatterConstant(DefinedClass oauthAdapter, OAuth oauth) {
        return new FieldBuilder(oauthAdapter).type(Pattern.class).name("OAUTH_VERIFIER_PATTERN").staticField().finalField().
                initialValue(ref(Pattern.class).staticInvoke("compile").arg(oauth.verifierRegex())).build();
    }

    private FieldVariable oauthCallbackField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(HttpCallback.class).name("oauthCallback").build();
    }

    private Method generateCreateConsumerMethod(DefinedClass oauthAdapter, FieldVariable consumer, OAuth oauth, TypeElement typeElement) {
        Method createConsumer = oauthAdapter.method(Modifier.PRIVATE, context.getCodeModel().VOID, "createConsumer");
        Invocation getConsumerKey = ExpressionFactory.invoke(getterMethodForFieldAnnotatedWith(typeElement, OAuthConsumerKey.class));
        Invocation getConsumerSecret = ExpressionFactory.invoke(getterMethodForFieldAnnotatedWith(typeElement, OAuthConsumerSecret.class));
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

    private void generateStartMethod(DefinedClass oauthAdapter, FieldVariable oauthCallback, FieldVariable redirectUrl) {
        Method start = oauthAdapter.method(Modifier.PUBLIC, this.context.getCodeModel().VOID, "start");
        start._throws(MuleException.class);
        start.body().invoke(ExpressionFactory._super(), ("start"));
        start.body().invoke(oauthCallback, "start");
        start.body().assign(redirectUrl, oauthCallback.invoke("getUrl"));
    }

    private void generateStopMethod(DefinedClass oauthAdapter, FieldVariable oauthCallback) {
        Method start = oauthAdapter.method(Modifier.PUBLIC, this.context.getCodeModel().VOID, "stop");
        start._throws(MuleException.class);
        start.body().invoke(ExpressionFactory._super(), ("stop"));
        start.body().invoke(oauthCallback, "stop");
    }

    private void generateInitialiseMethod(DefinedClass oauthAdapter, FieldVariable oauthCallback, DefinedClass messageProcessor, FieldVariable muleContext, Method createConsumer) {
        Method initialise = oauthAdapter.method(Modifier.PUBLIC, this.context.getCodeModel().VOID, "initialise");
        if(ref(Initialisable.class).isAssignableFrom(oauthAdapter._extends())) {
            initialise.body().invoke(ExpressionFactory._super(), "initialise");
        }
        Invocation domain = ExpressionFactory.invoke("get" + StringUtils.capitalize(HttpCallbackGenerator.DOMAIN_FIELD_NAME));
        Invocation port = ExpressionFactory.invoke("get" + StringUtils.capitalize(HttpCallbackGenerator.PORT_FIELD_NAME));
        initialise.body().assign(oauthCallback, ExpressionFactory._new(this.context.getClassForRole(HttpCallbackGenerator.HTTP_CALLBACK_ROLE)).
                arg(ExpressionFactory._new(messageProcessor)).arg(muleContext).arg(domain).arg(port));
        initialise.body().invoke(createConsumer);
    }

    private DefinedClass generateMessageProcessorInnerClass(DefinedClass oauthAdapter, FieldVariable oauthVerifier, FieldVariable pattern) throws GenerationException {
        DefinedClass messageProcessor;
        try {
            messageProcessor = oauthAdapter._class(Modifier.PRIVATE, "OnOAuthCallbackMessageProcessor")._implements(ref(MessageProcessor.class));
        } catch (ClassAlreadyExistsException e) {
            throw new GenerationException(e); // This wont happen
        }

        Method processMethod = messageProcessor.method(Modifier.PUBLIC, ref(MuleEvent.class), "process")._throws(ref(MuleException.class));
        Variable event = processMethod.param(ref(MuleEvent.class), "event");

        TryStatement tryToExtractVerifier = processMethod.body()._try();
        tryToExtractVerifier.body().assign(oauthVerifier, ExpressionFactory.invoke("extractVerifier").arg(event.invoke("getMessageAsString")));
        CatchBlock catchBlock = tryToExtractVerifier._catch(ref(Exception.class));
        Variable exceptionCaught = catchBlock.param("e");
        catchBlock.body()._throw(ExpressionFactory._new(
                ref(MessagingException.class)).
                arg(ref(MessageFactory.class).staticInvoke("createStaticMessage").arg("Could not extract OAuth verifier")).arg(event).arg(exceptionCaught));

        processMethod.body()._return(event);

        Method extractMethod = messageProcessor.method(Modifier.PRIVATE, ref(String.class), "extractVerifier")._throws(ref(Exception.class));
        Variable response = extractMethod.param(String.class, "response");
        Variable matcher = extractMethod.body().decl(ref(Matcher.class), "matcher", pattern.invoke("matcher").arg(response));
        Conditional ifVerifierFound = extractMethod.body()._if(Op.cand(matcher.invoke("find"), Op.gte(matcher.invoke("groupCount"), ExpressionFactory.lit(1))));
        ifVerifierFound._then()._return(ref(URLDecoder.class).staticInvoke("decode").arg(matcher.invoke("group").arg(ExpressionFactory.lit(1))).arg("UTF-8"));
        ifVerifierFound._else()._throw(ExpressionFactory._new(
                ref(Exception.class)).arg(ref(String.class).staticInvoke("format").arg("OAuth verifier could not be extracted from: %s").arg(response)));
        return messageProcessor;
    }

    private void generateGetAuthorizationUrlMethod(DefinedClass oauthAdapter, FieldVariable requestToken, FieldVariable requestTokenSecret, FieldVariable redirectUrl, FieldVariable consumer, TypeElement typeElement, OAuth oauth) {
        Method getAuthorizationUrl = oauthAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, GET_AUTHORIZATION_URL_METHOD_NAME);
        getAuthorizationUrl.type(ref(String.class));
        Variable provider = generateProvider(oauth, getAuthorizationUrl.body(), typeElement);
        Variable authorizationUrl = getAuthorizationUrl.body().decl(ref(String.class), "authorizationUrl");
        TryStatement tryRetrieveRequestToken = getAuthorizationUrl.body()._try();
        tryRetrieveRequestToken.body().assign(authorizationUrl, provider.invoke("retrieveRequestToken").arg(consumer).arg(redirectUrl));
        generateReThrow(tryRetrieveRequestToken, Exception.class, RuntimeException.class);
        getAuthorizationUrl.body().assign(requestToken, consumer.invoke("getToken"));
        getAuthorizationUrl.body().assign(requestTokenSecret, consumer.invoke("getTokenSecret"));
        getAuthorizationUrl.body()._return(authorizationUrl);
    }

    private void generateFetchAccessTokenMethod(DefinedClass oauthAdapter, FieldVariable requestToken, FieldVariable requestTokenSecret, FieldVariable oauthVerifier, FieldVariable consumer, TypeElement typeElement, OAuth oauth) {
        Method fetchAccessToken = oauthAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, FETCH_ACCESS_TOKEN_METHOD_NAME);
        Variable provider = generateProvider(oauth, fetchAccessToken.body(), typeElement);
        fetchAccessToken.body().invoke(consumer, "setTokenWithSecret").arg(requestToken).arg(requestTokenSecret);
        TryStatement tryRetrieveAccessToken = fetchAccessToken.body()._try();
        tryRetrieveAccessToken.body().invoke(provider, "retrieveAccessToken").arg(consumer).arg(oauthVerifier);
        generateReThrow(tryRetrieveAccessToken, Exception.class, RuntimeException.class);
        fetchAccessToken.body().assign(oauthAdapter.fields().get(OAUTH_ACCESS_TOKEN_FIELD_NAME), consumer.invoke("getToken"));
        fetchAccessToken.body().assign(oauthAdapter.fields().get(OAUTH_ACCESS_TOKEN_SECRET_FIELD_NAME), consumer.invoke("getTokenSecret"));
    }

    private Variable generateProvider(OAuth oauth, Block block, TypeElement typeElement) {
        Variable requestTokenUrl = block.decl(ref(String.class), "requestTokenUrl", ExpressionFactory.lit(oauth.requestTokenUrl()));

        if (classHasFieldAnnotatedWith(typeElement, OAuthScope.class)) {
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

    private boolean classHasFieldAnnotatedWith(TypeElement typeElement, Class<? extends Annotation> annotation) {
        List<VariableElement> fields = ElementFilter.fieldsIn(typeElement.getEnclosedElements());
        for (VariableElement field : fields) {
            if (field.getAnnotation(annotation) != null) {
                return true;
            }
        }
        return false;
    }
}