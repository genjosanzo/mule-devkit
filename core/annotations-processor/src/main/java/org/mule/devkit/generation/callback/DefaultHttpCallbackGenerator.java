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

package org.mule.devkit.generation.callback;

import org.mule.MessageExchangePattern;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.api.callback.HttpCallback;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.transport.Connector;
import org.mule.config.spring.factories.AsyncMessageProcessorsFactoryBean;
import org.mule.construct.Flow;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.NamingContants;
import org.mule.devkit.model.code.Block;
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
import org.mule.devkit.model.code.builders.FieldBuilder;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.processor.strategy.AsynchronousProcessingStrategy;

import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DefaultHttpCallbackGenerator extends AbstractModuleGenerator {

    public static final String HTTP_CALLBACK_ROLE = "HttpCallback";
    public static final String LOCAL_PORT_FIELD_NAME = "localPort";
    public static final String REMOTE_PORT_FIELD_NAME = "remotePort";
    public static final String DOMAIN_FIELD_NAME = "domain";
    public static final String ASYNC_FIELD_NAME = "async";
    public static final String CONNECTOR_FIELD_NAME = "connector";
    private static final String CLASS_NAME = "DefaultHttpCallback";
    private static final String INBOUND_ENDPOINT_EXCHANGE_PATTERN = "REQUEST_RESPONSE";
    private Method buildUrlMethod;
    private FieldVariable muleContextField;
    private FieldVariable flowConstructVariable;
    private Method createHttpInboundEndpointMethod;
    private FieldVariable urlField;
    private Method createConnectorMethod;
    private Method wrapMessageProcessorInAsyncChain;
    private FieldVariable loggerField;
    private FieldVariable callbackFlowField;
    private FieldVariable localPortField;
    private FieldVariable callbackMessageProcessorField;
    private FieldVariable domainField;
    private FieldVariable localUrlField;
    private FieldVariable callbackPathField;
    private FieldVariable remotePortField;
    private FieldVariable async;
    private FieldVariable connectorField;

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return typeElement.hasAnnotation(OAuth.class) ||
                typeElement.hasAnnotation(OAuth2.class) ||
                typeElement.hasProcessorMethodWithParameter(HttpCallback.class);
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {
        DefinedClass callbackClass = getDefaultHttpCallbackClass(typeElement);

        context.note("Generating HTTP callback implementation as " + callbackClass.fullName());

        generateFields(callbackClass);
        generateConstructorArgSimpleFlowConstruct(callbackClass);
        generateConstructorArgMessageProcessor(callbackClass);
        generateConstructorArgMessageProcessorAndCallbackPath(callbackClass);
        generateConstructorArgSimpleFlowConstructAndConnectorRef(callbackClass);
        generateConstructorArgMessageProcessorAndConnectorRef(callbackClass);
        generateConstructorArgMessageProcessorAndCallbackPathAndConnectorRef(callbackClass);
        generateBuildUrlMethod(callbackClass);
        createMessageProcessorInnerClass(callbackClass);
        generateWrapMessageProcessorInAsyncChain(callbackClass);
        generateCreateConnectorMethod(callbackClass);
        generateCreateHttpInboundEndpointMethod(callbackClass);
        generateStartMethod(callbackClass);
        generateStopMethod(callbackClass);

        context.setClassRole(HTTP_CALLBACK_ROLE, callbackClass);
    }

    private void generateFields(DefinedClass callbackClass) {
        loggerField = FieldBuilder.newLoggerField(callbackClass);
        localPortField = new FieldBuilder(callbackClass).
                privateVisibility().
                type(Integer.class).
                name(LOCAL_PORT_FIELD_NAME).
                build();
        remotePortField = new FieldBuilder(callbackClass).
                privateVisibility().
                type(Integer.class).
                name(REMOTE_PORT_FIELD_NAME).
                javadoc("The port number to be used in the dynamic http inbound endpoint that will receive the callback").
                build();
        domainField = new FieldBuilder(callbackClass).
                type(String.class).
                name(DOMAIN_FIELD_NAME).
                javadoc("The domain to be used in the dynamic http inbound endpoint that will receive the callback").
                build();
        urlField = new FieldBuilder(callbackClass).
                privateVisibility().
                type(String.class).
                name("url").
                javadoc("The dynamically generated url to pass on to the cloud connector. When this url is called the callback flow will be executed").
                getter().
                build();
        localUrlField = new FieldBuilder(callbackClass).
                privateVisibility().
                type(String.class).
                name("localUrl").
                build();
        muleContextField = new FieldBuilder(callbackClass).
                privateVisibility().
                type(MuleContext.class).
                name("muleContext").
                javadoc("Mule Context").
                setter().
                build();
        callbackFlowField = new FieldBuilder(callbackClass).
                privateVisibility().
                type(Flow.class).
                name("callbackFlow").
                javadoc("The flow to be called upon the http callback").
                build();
        flowConstructVariable = new FieldBuilder(callbackClass).
                privateVisibility().
                type(Flow.class).
                name("flowConstruct").
                javadoc("The dynamically created flow").
                build();
        callbackMessageProcessorField = new FieldBuilder(callbackClass).
                type(MessageProcessor.class).
                name("callbackMessageProcessor").
                javadoc("The message processor to be called upon the http callback").
                build();
        callbackPathField = new FieldBuilder(callbackClass).
                type(String.class).
                name("callbackPath").
                javadoc("Optional path to set up the endpoint").
                build();
        async = new FieldBuilder(callbackClass).
                type(Boolean.class).
                name(ASYNC_FIELD_NAME).
                javadoc("Whether the the message processor that invokes the callback flow is asynchronous").
                build();
        connectorField = new FieldBuilder(callbackClass).
                type(Connector.class).
                name(CONNECTOR_FIELD_NAME).
                javadoc("HTTP connector").
                build();
    }

    private void generateConstructorArgSimpleFlowConstructAndConnectorRef(DefinedClass callbackClass) {
        Method constructor = callbackClass.constructor(Modifier.PUBLIC);
        Variable callbackFlowArg = constructor.param(ref(Flow.class), "callbackFlow");
        Variable muleContextArg = constructor.param(ref(MuleContext.class), "muleContext");
        Variable callbackDomainArg = constructor.param(ref(String.class), "callbackDomain");
        Variable localPortArg = constructor.param(ref(Integer.class), "localPort");
        Variable remotePortArg = constructor.param(ref(Integer.class), "remotePort");
        Variable asyncArg = constructor.param(ref(Boolean.class), "async");
        Variable connectorArg = constructor.param(ref(Connector.class), "connector");
        constructor.body().assign(ExpressionFactory._this().ref(callbackFlowField), callbackFlowArg);
        constructor.body().assign(ExpressionFactory._this().ref(muleContextField), muleContextArg);
        constructor.body().assign(ExpressionFactory._this().ref(localPortField), localPortArg);
        constructor.body().assign(ExpressionFactory._this().ref(remotePortField), remotePortArg);
        constructor.body().assign(ExpressionFactory._this().ref(domainField), callbackDomainArg);
        constructor.body().assign(ExpressionFactory._this().ref(async), asyncArg);
        constructor.body().assign(ExpressionFactory._this().ref(connectorField), connectorArg);
    }

    private void generateConstructorArgMessageProcessorAndConnectorRef(DefinedClass callbackClass) {
        Method constructor = callbackClass.constructor(Modifier.PUBLIC);
        Variable messageProcessorArg = constructor.param(ref(MessageProcessor.class), "callbackMessageProcessor");
        Variable muleContextArg = constructor.param(ref(MuleContext.class), "muleContext");
        Variable callbackDomainArg = constructor.param(ref(String.class), "callbackDomain");
        Variable localPortArg = constructor.param(ref(Integer.class), "localPort");
        Variable remotePortArg = constructor.param(ref(Integer.class), "remotePort");
        Variable asyncArg = constructor.param(ref(Boolean.class), "async");
        Variable connectorArg = constructor.param(ref(Connector.class), "connector");
        constructor.body().assign(ExpressionFactory._this().ref(callbackMessageProcessorField), messageProcessorArg);
        constructor.body().assign(ExpressionFactory._this().ref(muleContextField), muleContextArg);
        constructor.body().assign(ExpressionFactory._this().ref(localPortField), localPortArg);
        constructor.body().assign(ExpressionFactory._this().ref(remotePortField), remotePortArg);
        constructor.body().assign(ExpressionFactory._this().ref(domainField), callbackDomainArg);
        constructor.body().assign(ExpressionFactory._this().ref(async), asyncArg);
        constructor.body().assign(ExpressionFactory._this().ref(connectorField), connectorArg);
        
    }

    private void generateConstructorArgMessageProcessorAndCallbackPathAndConnectorRef(DefinedClass callbackClass) {
        Method constructor = callbackClass.constructor(Modifier.PUBLIC);
        Variable messageProcessorArg = constructor.param(ref(MessageProcessor.class), "callbackMessageProcessor");
        Variable muleContextArg = constructor.param(ref(MuleContext.class), "muleContext");
        Variable callbackDomainArg = constructor.param(ref(String.class), "callbackDomain");
        Variable localPortArg = constructor.param(ref(Integer.class), "localPort");
        Variable remotePortArg = constructor.param(ref(Integer.class), "remotePort");
        Variable callbackPathArg = constructor.param(ref(String.class), "callbackPath");
        Variable asyncArg = constructor.param(ref(Boolean.class), "async");
        Variable connectorArg = constructor.param(ref(Connector.class), "connector");
        constructor.body().assign(ExpressionFactory._this().ref(callbackMessageProcessorField), messageProcessorArg);
        constructor.body().assign(ExpressionFactory._this().ref(muleContextField), muleContextArg);
        constructor.body().assign(ExpressionFactory._this().ref(localPortField), localPortArg);
        constructor.body().assign(ExpressionFactory._this().ref(domainField), callbackDomainArg);
        constructor.body().assign(ExpressionFactory._this().ref(remotePortField), remotePortArg);
        constructor.body().assign(ExpressionFactory._this().ref(callbackPathField), callbackPathArg);
        constructor.body().assign(ExpressionFactory._this().ref(async), asyncArg);
        constructor.body().assign(ExpressionFactory._this().ref(connectorField), connectorArg);
    }

    private void generateConstructorArgSimpleFlowConstruct(DefinedClass callbackClass) {
        Method constructor = callbackClass.constructor(Modifier.PUBLIC);
        Variable callbackFlowArg = constructor.param(ref(Flow.class), "callbackFlow");
        Variable muleContextArg = constructor.param(ref(MuleContext.class), "muleContext");
        Variable callbackDomainArg = constructor.param(ref(String.class), "callbackDomain");
        Variable localPortArg = constructor.param(ref(Integer.class), "localPort");
        Variable remotePortArg = constructor.param(ref(Integer.class), "remotePort");
        Variable asyncArg = constructor.param(ref(Boolean.class), "async");
        constructor.body().assign(ExpressionFactory._this().ref(callbackFlowField), callbackFlowArg);
        constructor.body().assign(ExpressionFactory._this().ref(muleContextField), muleContextArg);
        constructor.body().assign(ExpressionFactory._this().ref(localPortField), localPortArg);
        constructor.body().assign(ExpressionFactory._this().ref(remotePortField), remotePortArg);
        constructor.body().assign(ExpressionFactory._this().ref(domainField), callbackDomainArg);
        constructor.body().assign(ExpressionFactory._this().ref(async), asyncArg);
        constructor.body().assign(ExpressionFactory._this().ref(connectorField), ExpressionFactory._null());
    }

    private void generateConstructorArgMessageProcessor(DefinedClass callbackClass) {
        Method constructor = callbackClass.constructor(Modifier.PUBLIC);
        Variable messageProcessorArg = constructor.param(ref(MessageProcessor.class), "callbackMessageProcessor");
        Variable muleContextArg = constructor.param(ref(MuleContext.class), "muleContext");
        Variable callbackDomainArg = constructor.param(ref(String.class), "callbackDomain");
        Variable localPortArg = constructor.param(ref(Integer.class), "localPort");
        Variable remotePortArg = constructor.param(ref(Integer.class), "remotePort");
        Variable asyncArg = constructor.param(ref(Boolean.class), "async");
        constructor.body().assign(ExpressionFactory._this().ref(callbackMessageProcessorField), messageProcessorArg);
        constructor.body().assign(ExpressionFactory._this().ref(muleContextField), muleContextArg);
        constructor.body().assign(ExpressionFactory._this().ref(localPortField), localPortArg);
        constructor.body().assign(ExpressionFactory._this().ref(remotePortField), remotePortArg);
        constructor.body().assign(ExpressionFactory._this().ref(domainField), callbackDomainArg);
        constructor.body().assign(ExpressionFactory._this().ref(async), asyncArg);
        constructor.body().assign(ExpressionFactory._this().ref(connectorField), ExpressionFactory._null());
    }

    private void generateConstructorArgMessageProcessorAndCallbackPath(DefinedClass callbackClass) {
        Method constructor = callbackClass.constructor(Modifier.PUBLIC);
        Variable messageProcessorArg = constructor.param(ref(MessageProcessor.class), "callbackMessageProcessor");
        Variable muleContextArg = constructor.param(ref(MuleContext.class), "muleContext");
        Variable callbackDomainArg = constructor.param(ref(String.class), "callbackDomain");
        Variable localPortArg = constructor.param(ref(Integer.class), "localPort");
        Variable remotePortArg = constructor.param(ref(Integer.class), "remotePort");
        Variable callbackPathArg = constructor.param(ref(String.class), "callbackPath");
        Variable asyncArg = constructor.param(ref(Boolean.class), "async");
        constructor.body().assign(ExpressionFactory._this().ref(callbackMessageProcessorField), messageProcessorArg);
        constructor.body().assign(ExpressionFactory._this().ref(muleContextField), muleContextArg);
        constructor.body().assign(ExpressionFactory._this().ref(localPortField), localPortArg);
        constructor.body().assign(ExpressionFactory._this().ref(domainField), callbackDomainArg);
        constructor.body().assign(ExpressionFactory._this().ref(remotePortField), remotePortArg);
        constructor.body().assign(ExpressionFactory._this().ref(callbackPathField), callbackPathArg);
        constructor.body().assign(ExpressionFactory._this().ref(async), asyncArg);
        constructor.body().assign(ExpressionFactory._this().ref(connectorField), ExpressionFactory._null());
    }

    private void generateBuildUrlMethod(DefinedClass callbackClass) {
        buildUrlMethod = callbackClass.method(Modifier.PRIVATE, ref(String.class), "buildUrl");
        Block body = buildUrlMethod.body();
        Variable urlBuilder = body.decl(ref(StringBuilder.class), "urlBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
        Block ifDomainNotPrefixed = body._if(Op.not(domainField.invoke("contains").arg("://")))._then();
        Conditional ifConnectorNotNull = ifDomainNotPrefixed._if(Op.ne(connectorField, ExpressionFactory._null()));
        ifConnectorNotNull._then().invoke(urlBuilder, "append").arg(Op.plus(connectorField.invoke("getProtocol"), ExpressionFactory.lit("://")));
        ifConnectorNotNull._else().invoke(urlBuilder, "append").arg("http://");
        body.invoke(urlBuilder, "append").arg(domainField);
        body.invoke(urlBuilder, "append").arg(":");
        body.invoke(urlBuilder, "append").arg(remotePortField);
        body.invoke(urlBuilder, "append").arg("/");
        Conditional ifCallbackPathNotNull = body._if(Op.ne(callbackPathField, ExpressionFactory._null()));
        ifCallbackPathNotNull._then().invoke(urlBuilder, "append").arg(callbackPathField);
        ifCallbackPathNotNull._else().invoke(urlBuilder, "append").arg(ref(UUID.class).staticInvoke("randomUUID"));
        body._return(urlBuilder.invoke("toString"));
    }

    private void generateWrapMessageProcessorInAsyncChain(DefinedClass callbackClass) {

        wrapMessageProcessorInAsyncChain = callbackClass.method(Modifier.PRIVATE, ref(MessageProcessor.class), "wrapMessageProcessorInAsyncChain")._throws(ref(MuleException.class));
        Variable messageProcessorParam = wrapMessageProcessorInAsyncChain.param(ref(MessageProcessor.class), "messageProcessor");
        Block body = wrapMessageProcessorInAsyncChain.body();

        Variable asyncMessageProcessorsFactoryBean = body.decl(ref(AsyncMessageProcessorsFactoryBean.class), "asyncMessageProcessorsFactoryBean", ExpressionFactory._new(ref(AsyncMessageProcessorsFactoryBean.class)));
        body.invoke(asyncMessageProcessorsFactoryBean, "setMuleContext").arg(muleContextField);
        body.invoke(asyncMessageProcessorsFactoryBean, "setMessageProcessors").arg(ref(Arrays.class).staticInvoke("asList").arg(messageProcessorParam));
        body.invoke(asyncMessageProcessorsFactoryBean, "setProcessingStrategy").arg(ExpressionFactory._new(ref(AsynchronousProcessingStrategy.class)));

        TryStatement tryStatement = body._try();
        tryStatement.body()._return(ExpressionFactory.cast(ref(MessageProcessor.class), asyncMessageProcessorsFactoryBean.invoke("getObject")));
        CatchBlock catchBlock = tryStatement._catch(ref(Exception.class));
        catchBlock.body()._throw(ExpressionFactory._new(ref(FlowConstructInvalidException.class)).arg(catchBlock.param("e")));
    }

    private void generateCreateConnectorMethod(DefinedClass callbackClass) {
        createConnectorMethod = callbackClass.method(Modifier.PRIVATE, ref(Connector.class), "createConnector")._throws(ref(MuleException.class));
        Block body = createConnectorMethod.body();
        Conditional ifConnectorNotNull = body._if(Op.ne(connectorField, ExpressionFactory._null()));
        ifConnectorNotNull._then()._return(ExpressionFactory._this().ref(connectorField.name()));
        Variable muleRegistryVariable = body.decl(ref(MuleRegistry.class), "muleRegistry", ExpressionFactory.invoke(muleContextField, "getRegistry"));
        Variable httpConnectorVariable = body.decl(ref(Connector.class), "httpConnector", ExpressionFactory.invoke(muleRegistryVariable, "lookupConnector").arg("connector.http.mule.default"));
        Conditional conditional = body._if(Op.ne(httpConnectorVariable, ExpressionFactory._null()));
        conditional._then()._return(httpConnectorVariable);
        conditional._else().block().invoke(loggerField, "error").arg("Could not find connector with name 'connector.http.mule.default'");
        conditional._else().block()._throw(ExpressionFactory._new(ref(DefaultMuleException.class)).arg("Could not find connector with name 'connector.http.mule.default'"));
    }

    private void generateCreateHttpInboundEndpointMethod(DefinedClass callbackClass) {
        createHttpInboundEndpointMethod = callbackClass.method(Modifier.PRIVATE, ref(InboundEndpoint.class), "createHttpInboundEndpoint")._throws(ref(MuleException.class));
        Block body = createHttpInboundEndpointMethod.body();
        Variable inBuilderVariable = body.decl(ref(EndpointURIEndpointBuilder.class), "inBuilder", ExpressionFactory._new(ref(EndpointURIEndpointBuilder.class)).arg(localUrlField).arg(muleContextField));
        body.invoke(inBuilderVariable, "setConnector").arg(ExpressionFactory.invoke(createConnectorMethod));
        body.invoke(inBuilderVariable, "setExchangePattern").arg(ref(MessageExchangePattern.class).staticRef(INBOUND_ENDPOINT_EXCHANGE_PATTERN));
        Variable endpointFactoryVariable = body.decl(ref(EndpointFactory.class), "endpointFactory", ExpressionFactory.invoke(muleContextField, "getEndpointFactory"));
        body._return(ExpressionFactory.invoke(endpointFactoryVariable, "getInboundEndpoint").arg(inBuilderVariable));
    }

    private void generateStartMethod(DefinedClass callbackClass) {
        Method startMethod = callbackClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "start")._throws(ref(MuleException.class));
        Block body = startMethod.body();
        body.assign(ExpressionFactory._this().ref(urlField), ExpressionFactory.invoke(buildUrlMethod));
        body.assign(ExpressionFactory._this().ref(localUrlField), urlField.invoke("replaceFirst").arg(domainField).arg("localhost"));
        body.assign(ExpressionFactory._this().ref(localUrlField), localUrlField.invoke("replaceFirst").arg(ref(String.class).staticInvoke("valueOf").arg(remotePortField)).arg(ref(String.class).staticInvoke("valueOf").arg(localPortField)));
        Variable dynamicFlowName = body.decl(ref(String.class), "dynamicFlowName", ref(String.class).staticInvoke("format").arg("DynamicFlow-%s").arg(localUrlField));
        body.assign(flowConstructVariable, ExpressionFactory._new(ref(Flow.class)).arg(dynamicFlowName).arg(muleContextField));
        body.invoke(flowConstructVariable, "setMessageSource").arg(ExpressionFactory.invoke(createHttpInboundEndpointMethod));

        Variable messageProcessor = body.decl(ref(MessageProcessor.class), "messageProcessor");
        Conditional ifCallbackFlowNotNull = body._if(Op.ne(callbackFlowField, ExpressionFactory._null()));
        ifCallbackFlowNotNull._then().assign(messageProcessor, ExpressionFactory._new(callbackClass.listClasses()[0]));
        ifCallbackFlowNotNull._else().assign(messageProcessor, callbackMessageProcessorField);

        body._if(async)._then().assign(messageProcessor, ExpressionFactory.invoke(wrapMessageProcessorInAsyncChain).arg(messageProcessor));

        Variable mps = body.decl(ref(List.class).narrow(ref(MessageProcessor.class)), "messageProcessors", ExpressionFactory._new(ref(ArrayList.class).narrow(MessageProcessor.class)));
        body.invoke(mps, "add").arg(messageProcessor);

        body.invoke(flowConstructVariable, "setMessageProcessors").arg(mps);

        body.invoke(flowConstructVariable, "initialise");
        body.invoke(flowConstructVariable, "start");
        body.invoke(loggerField, "debug").arg(ref(String.class).staticInvoke("format").arg("Created flow with http inbound endpoint listening at: %s").arg(urlField));
    }

    private void generateStopMethod(DefinedClass callbackClass) {
        Method stopMethod = callbackClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "stop")._throws(ref(MuleException.class));
        Block body = stopMethod.body();
        Block block = body._if(Op.ne(flowConstructVariable, ExpressionFactory._null()))._then();
        block.invoke(flowConstructVariable, "stop");
        block.invoke(flowConstructVariable, "dispose");
        block.invoke(loggerField, "debug").arg("Http callback flow stopped");
    }

    private void createMessageProcessorInnerClass(DefinedClass callbackClass) {
        DefinedClass messageProcessor = callbackClass._class("FlowRefMessageProcessor")._implements(ref(MessageProcessor.class));
        Method processMethod = messageProcessor.method(Modifier.PUBLIC, ref(MuleEvent.class), "process")._throws(ref(MuleException.class));
        processMethod.param(ref(MuleEvent.class), "event");
        processMethod.body()._return(ExpressionFactory.invoke(callbackFlowField, "process").arg(processMethod.params().get(0)));
    }

    private DefinedClass getDefaultHttpCallbackClass(TypeElement type) {
        String httpCallbackClassName = context.getNameUtils().generateClassNameInPackage(type, NamingContants.CONFIG_NAMESPACE, CLASS_NAME);
        Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(httpCallbackClassName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(httpCallbackClassName), new Class[]{HttpCallback.class});

        context.setClassRole(HTTP_CALLBACK_ROLE, clazz);

        return clazz;
    }
}