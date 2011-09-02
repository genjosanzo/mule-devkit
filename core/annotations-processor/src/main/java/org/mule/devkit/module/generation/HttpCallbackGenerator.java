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

import org.mule.MessageExchangePattern;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.callback.HttpCallback;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.transport.Connector;
import org.mule.config.spring.factories.AsyncMessageProcessorsFactoryBean;
import org.mule.construct.SimpleFlowConstruct;
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

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class HttpCallbackGenerator extends AbstractModuleGenerator {

    public static final String HTTP_CALLBACK_ROLE = "HttpCallback";
    public static final String PORT_FIELD_NAME = "port";
    public static final String DOMAIN_FIELD_NAME = "domain";
    private static final String CLASS_NAME = "DefaultPortHttpCallback";
    private static final String INBOUND_ENDPOINT_EXCHANGE_PATTERN = "REQUEST_RESPONSE";
    private Method buildUrlMethod;
    private FieldVariable muleContextField;
    private FieldVariable flowConstructVariable;
    private Method createHttpInboundEndpointMethod;
    private FieldVariable urlField;
    private Method createConnectorMethod;
    private Method createFlowRefMessageProcessorMethod;
    private FieldVariable loggerField;
    private FieldVariable callbackFlowField;
    private FieldVariable portField;
    private FieldVariable callbackMessageProcessorField;
    private FieldVariable domainField;
    private FieldVariable localUrlField;

    @Override
    protected boolean shouldGenerate(TypeElement typeElement) {
        List<ExecutableElement> methods = ElementFilter.methodsIn(typeElement.getEnclosedElements());
        for (ExecutableElement method : methods) {
            if (method.getAnnotation(Processor.class) != null) {
                for (VariableElement variable : method.getParameters()) {
                    if (variable.asType().toString().contains(HttpCallback.class.getName())) {
                        return true;
                    }
                }
            }
        }
        return typeElement.getAnnotation(OAuth.class) != null;
    }

    @Override
    protected void doGenerate(TypeElement typeElement) {
        DefinedClass callbackClass = getProcessorCallbackClass(typeElement);
        generateFields(callbackClass);
        generateConstructorArgSimpleFlowConstruct(callbackClass);
        generateConstructorArgMessageProcessor(callbackClass);
        generateBuildUrlMethod(callbackClass);
        generateCreateFlowRefMessageProcessorMethod(callbackClass);
        generateCreateConnectorMethod(callbackClass);
        generateCreateHttpInboundEndpointMethod(callbackClass);
        generateStartMethod(callbackClass);
        generateStopMethod(callbackClass);

        context.setClassRole(HTTP_CALLBACK_ROLE, callbackClass);
    }

    private void generateFields(DefinedClass callbackClass) {
        loggerField = FieldBuilder.newLoggerField(callbackClass);
        portField = new FieldBuilder(callbackClass).
                privateVisibility().
                type(Integer.class).
                name(PORT_FIELD_NAME).
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
                type(SimpleFlowConstruct.class).
                name("callbackFlow").
                javadoc("The flow to be called upon the http callback").
                build();
        flowConstructVariable = new FieldBuilder(callbackClass).
                privateVisibility().
                type(SimpleFlowConstruct.class).
                name("flowConstruct").
                javadoc("The dynamically created flow").
                build();
        callbackMessageProcessorField = new FieldBuilder(callbackClass).
                type(MessageProcessor.class).
                name("callbackMessageProcessor").
                javadoc("The message processor to be called upon the http callback").
                build();
    }

    private void generateConstructorArgSimpleFlowConstruct(DefinedClass callbackClass) {
        Method constructor = callbackClass.constructor(Modifier.PUBLIC);
        Variable callbackFlowArg = constructor.param(ref(SimpleFlowConstruct.class), "callbackFlow");
        Variable muleContextArg = constructor.param(ref(MuleContext.class), "muleContext");
        Variable callbackDomainArg = constructor.param(ref(String.class), "callbackDomain");
        Variable callbackPortArg = constructor.param(ref(Integer.class), "callbackPort");
        constructor.body().assign(ExpressionFactory._this().ref(callbackFlowField), callbackFlowArg);
        constructor.body().assign(ExpressionFactory._this().ref(muleContextField), muleContextArg);
        constructor.body().assign(ExpressionFactory._this().ref(portField), callbackPortArg);
        constructor.body().assign(ExpressionFactory._this().ref(domainField), callbackDomainArg);
    }

    private void generateConstructorArgMessageProcessor(DefinedClass callbackClass) {
        Method constructor = callbackClass.constructor(Modifier.PUBLIC);
        Variable messageProcessorArg = constructor.param(ref(MessageProcessor.class), "callbackMessageProcessor");
        Variable muleContextArg = constructor.param(ref(MuleContext.class), "muleContext");
        Variable callbackDomainArg = constructor.param(ref(String.class), "callbackDomain");
        Variable callbackPortArg = constructor.param(ref(Integer.class), "callbackPort");
        constructor.body().assign(ExpressionFactory._this().ref(callbackMessageProcessorField), messageProcessorArg);
        constructor.body().assign(ExpressionFactory._this().ref(muleContextField), muleContextArg);
        constructor.body().assign(ExpressionFactory._this().ref(portField), callbackPortArg);
        constructor.body().assign(ExpressionFactory._this().ref(domainField), callbackDomainArg);
    }

    private void generateBuildUrlMethod(DefinedClass callbackClass) {
        buildUrlMethod = callbackClass.method(Modifier.PRIVATE, ref(String.class), "buildUrl");
        Block body = buildUrlMethod.body();
        Variable urlBuilder = body.decl(ref(StringBuilder.class), "urlBuilder", ExpressionFactory._new(ref(StringBuilder.class)));
        Block ifDomainNotPrefixed = body._if(Op.not(domainField.invoke("startsWith").arg("http://")))._then();
        ifDomainNotPrefixed.invoke(urlBuilder, "append").arg("http://");
        body.invoke(urlBuilder, "append").arg(domainField);
        body.invoke(urlBuilder, "append").arg(":");
        body.invoke(urlBuilder, "append").arg(portField);
        body.invoke(urlBuilder, "append").arg("/");
        body.invoke(urlBuilder, "append").arg(ref(UUID.class).staticInvoke("randomUUID"));
        body._return(urlBuilder.invoke("toString"));
    }

    private void generateCreateFlowRefMessageProcessorMethod(DefinedClass callbackClass) {
        createMessageProcessorInnerClass(callbackClass);

        createFlowRefMessageProcessorMethod = callbackClass.method(Modifier.PRIVATE, ref(MessageProcessor.class), "createFlowRefMessageProcessor")._throws(ref(MuleException.class));
        Block body = createFlowRefMessageProcessorMethod.body();


        Variable asyncMessageProcessorsFactoryBean = body.decl(ref(AsyncMessageProcessorsFactoryBean.class), "asyncMessageProcessorsFactoryBean", ExpressionFactory._new(ref(AsyncMessageProcessorsFactoryBean.class)));
        body.invoke(asyncMessageProcessorsFactoryBean, "setMuleContext").arg(muleContextField);

        Variable messageProcessor = body.decl(ref(MessageProcessor.class), "messageProcessor");

        Conditional ifCallbackFlowNotNull = body._if(Op.ne(callbackFlowField, ExpressionFactory._null()));
        ifCallbackFlowNotNull._then().assign(messageProcessor, ExpressionFactory._new(callbackClass.listClasses()[0]));
        ifCallbackFlowNotNull._else().assign(messageProcessor, callbackMessageProcessorField);

        body.invoke(asyncMessageProcessorsFactoryBean, "setMessageProcessors").arg(ref(Arrays.class).staticInvoke("asList").arg(messageProcessor));

        TryStatement tryStatement = body._try();
        tryStatement.body()._return(ExpressionFactory.cast(ref(MessageProcessor.class), asyncMessageProcessorsFactoryBean.invoke("getObject")));
        CatchBlock catchBlock = tryStatement._catch(ref(Exception.class));
        catchBlock.body()._throw(ExpressionFactory._new(ref(FlowConstructInvalidException.class)).arg(catchBlock.param("e")));
    }

    private void generateCreateConnectorMethod(DefinedClass callbackClass) {
        createConnectorMethod = callbackClass.method(Modifier.PRIVATE, ref(Connector.class), "createConnector")._throws(ref(MuleException.class));
        Block body = createConnectorMethod.body();
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
        body.assign(ExpressionFactory._this().ref(localUrlField), urlField.invoke("replace").arg(domainField).arg("localhost"));
        Variable dynamicFlowName = body.decl(ref(String.class), "dynamicFlowName", ref(String.class).staticInvoke("format").arg("DynamicFlow-%s").arg(localUrlField));
        body.assign(flowConstructVariable, ExpressionFactory._new(ref(SimpleFlowConstruct.class)).arg(dynamicFlowName).arg(muleContextField));
        body.invoke(flowConstructVariable, "setMessageSource").arg(ExpressionFactory.invoke(createHttpInboundEndpointMethod));
        body.invoke(flowConstructVariable, "setMessageProcessors").arg(ref(Arrays.class).staticInvoke("asList").arg(ExpressionFactory.invoke(createFlowRefMessageProcessorMethod)));
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

    private DefinedClass getProcessorCallbackClass(TypeElement type) {
        String httpCallbackClassName = context.getNameUtils().generateClassNameInPackage(type, ".config.spring", CLASS_NAME);
        Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(httpCallbackClassName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(httpCallbackClassName), new Class[]{HttpCallback.class});

        context.setClassRole(HTTP_CALLBACK_ROLE, clazz);

        return clazz;
    }
}