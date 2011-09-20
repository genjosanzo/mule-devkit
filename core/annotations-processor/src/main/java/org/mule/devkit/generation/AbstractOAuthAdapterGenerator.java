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
package org.mule.devkit.generation;

import org.apache.commons.lang.StringUtils;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.callback.HttpCallback;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.MessageFactory;
import org.mule.devkit.generation.callback.HttpCallbackGenerator;
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

import javax.lang.model.element.TypeElement;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractOAuthAdapterGenerator extends AbstractModuleGenerator {

    public static final String VERIFIER_FIELD_NAME = "oauthVerifier";
    public static final String HAS_TOKEN_EXPIRED_METHOD_NAME = "hasTokenExpired";
    public static final String RESET_METHOD_NAME = "reset";
    public static final String OAUTH_ACCESS_TOKEN_FIELD_NAME = "accessToken";
    public static final String OAUTH_ACCESS_TOKEN_SECRET_FIELD_NAME = "accessTokenSecret";
    public static final String GET_AUTHORIZATION_URL_METHOD_NAME = "getAuthorizationUrl";
    public static final String FETCH_ACCESS_TOKEN_METHOD_NAME = "fetchAccessToken";
    public static final String OAUTH_VERIFIER_FIELD_NAME = "oauthVerifier";
    protected static final String REDIRECT_URL_FIELD_NAME = "redirectUrl";
    protected static final String ACCESS_TOKEN_FIELD_NAME = "accessToken";
    protected static final String ENCODING = "UTF-8";
    protected static final String GRANT_TYPE = "authorization_code";
    protected static final String ACCESS_CODE_PATTERN_FIELD_NAME = "ACCESS_CODE_PATTERN";
    protected static final String CALLBACK_FIELD_NAME = "oauthCallback";
    protected static final String AUTH_CODE_PATTERN_FIELD_NAME = "AUTH_CODE_PATTERN";
    protected static final String EXPIRATION_TIME_PATTERN_FIELD_NAME = "EXPIRATION_TIME_PATTERN";
    protected static final String EXPIRATION_FIELD_NAME = "expiration";

    protected DefinedClass getOAuthAdapterClass(TypeElement typeElement, String classSuffix, Class<?> interf) {
        String oauthAdapterName = context.getNameUtils().generateClassName(typeElement, ".config", classSuffix);
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(oauthAdapterName));

        DefinedClass classToExtend = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));

        DefinedClass oauthAdapter = pkg._class(context.getNameUtils().getClassName(oauthAdapterName), classToExtend);
        oauthAdapter._implements(MuleContextAware.class);
        oauthAdapter._implements(Startable.class);
        oauthAdapter._implements(Initialisable.class);
        oauthAdapter._implements(Stoppable.class);
        oauthAdapter._implements(interf);

        context.setClassRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement), oauthAdapter);

        oauthAdapter.javadoc().add("A {@code " + oauthAdapter.name() + "} is a wrapper around ");
        oauthAdapter.javadoc().add(ref(typeElement.asType()));
        oauthAdapter.javadoc().add(" that adds OAuth capabilites to the pojo.");

        return oauthAdapter;
    }

    protected FieldVariable authorizationCodePatternConstant(DefinedClass oauthAdapter, String regex) {
        return new FieldBuilder(oauthAdapter).type(Pattern.class).name(AUTH_CODE_PATTERN_FIELD_NAME).staticField().finalField().
                initialValue(ref(Pattern.class).staticInvoke("compile").arg(regex)).build();
    }

    protected FieldVariable authorizationCodeField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(String.class).name(OAUTH_VERIFIER_FIELD_NAME).getterAndSetter().build();
    }

    protected FieldVariable redirectUrlField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(String.class).name(REDIRECT_URL_FIELD_NAME).getter().build();
    }

    protected FieldVariable accessTokenField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(String.class).name(ACCESS_TOKEN_FIELD_NAME).getterAndSetter().build();
    }

    protected FieldVariable oauthCallbackField(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(HttpCallback.class).name(CALLBACK_FIELD_NAME).build();
    }

    protected void generateStartMethod(DefinedClass oauthAdapter) {
        Method start = oauthAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, Startable.PHASE_NAME);
        start._throws(MuleException.class);
        start.body().invoke(ExpressionFactory._super(), Startable.PHASE_NAME);
        start.body().invoke(oauthAdapter.fields().get(CALLBACK_FIELD_NAME), Startable.PHASE_NAME);
        start.body().assign(oauthAdapter.fields().get(REDIRECT_URL_FIELD_NAME), oauthAdapter.fields().get(CALLBACK_FIELD_NAME).invoke("getUrl"));
    }

    protected void generateStopMethod(DefinedClass oauthAdapter) {
        Method start = oauthAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, Stoppable.PHASE_NAME);
        start._throws(MuleException.class);
        start.body().invoke(ExpressionFactory._super(), (Stoppable.PHASE_NAME));
        start.body().invoke(oauthAdapter.fields().get(CALLBACK_FIELD_NAME), Stoppable.PHASE_NAME);
    }

    protected Method generateInitialiseMethod(DefinedClass oauthAdapter, DefinedClass messageProcessor, String callbackPath) {
        Method initialise = oauthAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, Initialisable.PHASE_NAME);
        if (ref(Initialisable.class).isAssignableFrom(oauthAdapter._extends())) {
            initialise.body().invoke(ExpressionFactory._super(), Initialisable.PHASE_NAME);
        }
        Invocation domain = ExpressionFactory.invoke("get" + StringUtils.capitalize(HttpCallbackGenerator.DOMAIN_FIELD_NAME));
        Invocation localPort = ExpressionFactory.invoke("get" + StringUtils.capitalize(HttpCallbackGenerator.LOCAL_PORT_FIELD_NAME));
        Invocation remotePort = ExpressionFactory.invoke("get" + StringUtils.capitalize(HttpCallbackGenerator.REMOTE_PORT_FIELD_NAME));
        Invocation async = ExpressionFactory.invoke("get" + StringUtils.capitalize(HttpCallbackGenerator.ASYNC_FIELD_NAME));
        FieldVariable callback = oauthAdapter.fields().get(CALLBACK_FIELD_NAME);
        FieldVariable muleContext = oauthAdapter.fields().get(MULE_CONTEXT_FIELD_NAME);
        if (StringUtils.isEmpty(callbackPath)) {
            initialise.body().assign(callback, ExpressionFactory._new(context.getClassForRole(HttpCallbackGenerator.HTTP_CALLBACK_ROLE)).
                    arg(ExpressionFactory._new(messageProcessor)).arg(muleContext).arg(domain).arg(localPort).arg(remotePort).arg(async));
        } else {
            initialise.body().assign(callback, ExpressionFactory._new(context.getClassForRole(HttpCallbackGenerator.HTTP_CALLBACK_ROLE)).
                    arg(ExpressionFactory._new(messageProcessor)).arg(muleContext).arg(domain).arg(localPort).arg(remotePort).arg(callbackPath).arg(async));
        }
        return initialise;
    }

    protected DefinedClass generateMessageProcessorInnerClass(DefinedClass oauthAdapter) throws GenerationException {
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

    protected void muleContextField(DefinedClass oauthAdapter) {
        new FieldBuilder(oauthAdapter).name(MULE_CONTEXT_FIELD_NAME).type(MuleContext.class).setter().build();
    }
}