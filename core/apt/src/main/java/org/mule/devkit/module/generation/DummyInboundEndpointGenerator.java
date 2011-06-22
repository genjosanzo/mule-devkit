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
import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.devkit.annotations.Source;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.generation.GeneratorContext;
import org.mule.devkit.model.code.*;
import org.mule.transaction.MuleTransactionConfig;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DummyInboundEndpointGenerator extends AbstractModuleGenerator {

    public static final String DUMMY_INBOUND_ENDPOINT_ROLE = "DummyInboundEndpoint";

    public void generate(Element type) throws GenerationException {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Source source = executableElement.getAnnotation(Source.class);

            if (source == null)
                continue;

            DefinedClass dummyInboundEndpoint = getDummyInboundEndpointClass(executableElement, context);

            // add internal fields
            FieldVariable muleContext = dummyInboundEndpoint.field(Modifier.PRIVATE, ref(MuleContext.class), "muleContext");
            FieldVariable transactionConfig = dummyInboundEndpoint.field(Modifier.PRIVATE, ref(TransactionConfig.class), "transactionConfig");

            // add constructor
            Method constructor = dummyInboundEndpoint.constructor(Modifier.PUBLIC);
            Variable muleContextParam = constructor.param(ref(MuleContext.class), "muleContext");
            constructor.body().assign(ExpressionFactory._this().ref(muleContext), muleContextParam);
            Invocation newTransactionConfig = ExpressionFactory._new(ref(MuleTransactionConfig.class));
            constructor.body().assign(ExpressionFactory._this().ref(transactionConfig), newTransactionConfig);
            constructor.body().add(newTransactionConfig.invoke("setAction").arg(ref(TransactionConfig.class).staticRef("ACTION_NONE")));

            Method getEndpointURI = dummyInboundEndpoint.method(Modifier.PUBLIC, EndpointURI.class, "getEndpointURI");
            getEndpointURI.body()._return(ExpressionFactory._null());

            Method getAddress = dummyInboundEndpoint.method(Modifier.PUBLIC, String.class, "getAddress");
            getAddress.body()._return(ExpressionFactory._null());

            Method getEncoding = dummyInboundEndpoint.method(Modifier.PUBLIC, String.class, "getEncoding");
            getEncoding.body()._return(ExpressionFactory._null());

            Method getConnector = dummyInboundEndpoint.method(Modifier.PUBLIC, Connector.class, "getConnector");
            getConnector.body()._return(ExpressionFactory._null());

            Method getName = dummyInboundEndpoint.method(Modifier.PUBLIC, String.class, "getName");
            getName.body()._return(ExpressionFactory._null());

            Method getTransformers = dummyInboundEndpoint.method(Modifier.PUBLIC, List.class, Transformer.class, "getTransformers");
            getTransformers.body()._return(ExpressionFactory._null());

            Method getResponseTransformers = dummyInboundEndpoint.method(Modifier.PUBLIC, List.class, Transformer.class, "getResponseTransformers");
            getResponseTransformers.body()._return(ExpressionFactory._null());

            Method getProperties = dummyInboundEndpoint.method(Modifier.PUBLIC, Map.class, "getProperties");
            getProperties.body()._return(ExpressionFactory._null());

            Method getProperty = dummyInboundEndpoint.method(Modifier.PUBLIC, Object.class, "getProperty");
            getProperty.param(Object.class, "key");
            getProperty.body()._return(ExpressionFactory._null());

            Method getProtocol = dummyInboundEndpoint.method(Modifier.PUBLIC, String.class, "getProtocol");
            getProtocol.body()._return(ExpressionFactory._null());

            Method isReadOnly = dummyInboundEndpoint.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, "isReadOnly");
            isReadOnly.body()._return(ExpressionFactory.lit(false));

            Method getTransactionConfig = dummyInboundEndpoint.method(Modifier.PUBLIC, TransactionConfig.class, "getTransactionConfig");
            getTransactionConfig.body()._return(transactionConfig);

            Method getFilter = dummyInboundEndpoint.method(Modifier.PUBLIC, Filter.class, "getFilter");
            getFilter.body()._return(ExpressionFactory._null());

            Method isDeleteUnacceptedMessages = dummyInboundEndpoint.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, "isDeleteUnacceptedMessages");
            isDeleteUnacceptedMessages.body()._return(ExpressionFactory.lit(false));

            Method getSecurityFilter = dummyInboundEndpoint.method(Modifier.PUBLIC, EndpointSecurityFilter.class, "getSecurityFilter");
            getSecurityFilter.body()._return(ExpressionFactory._null());

            Method getMessageProcessorsFactory = dummyInboundEndpoint.method(Modifier.PUBLIC, EndpointMessageProcessorChainFactory.class, "getMessageProcessorsFactory");
            getMessageProcessorsFactory.body()._return(ExpressionFactory._null());

            Method getMessageProcessors = dummyInboundEndpoint.method(Modifier.PUBLIC, List.class, MessageProcessor.class, "getMessageProcessors");
            getMessageProcessors.body()._return(ExpressionFactory._null());

            Method getResponseMessageProcessors = dummyInboundEndpoint.method(Modifier.PUBLIC, List.class, MessageProcessor.class, "getResponseMessageProcessors");
            getResponseMessageProcessors.body()._return(ExpressionFactory._null());

            Method getExchangePattern = dummyInboundEndpoint.method(Modifier.PUBLIC, MessageExchangePattern.class, "getExchangePattern");
            getExchangePattern.body()._return(ref(MessageExchangePattern.class).staticRef("ONE_WAY"));

            Method getResponseTimeout = dummyInboundEndpoint.method(Modifier.PUBLIC, context.getCodeModel().INT, "getResponseTimeout");
            getResponseTimeout.body()._return(ExpressionFactory.lit(0));

            Method getInitialState = dummyInboundEndpoint.method(Modifier.PUBLIC, String.class, "getInitialState");
            getInitialState.body()._return(ExpressionFactory._null());

            Method getMuleContext = dummyInboundEndpoint.method(Modifier.PUBLIC, MuleContext.class, "getMuleContext");
            getMuleContext.body()._return(muleContext);

            Method getRetryPolicyTemplate = dummyInboundEndpoint.method(Modifier.PUBLIC, RetryPolicyTemplate.class, "getRetryPolicyTemplate");
            getRetryPolicyTemplate.body()._return(ExpressionFactory._null());

            Method getEndpointBuilderName = dummyInboundEndpoint.method(Modifier.PUBLIC, String.class, "getEndpointBuilderName");
            getEndpointBuilderName.body()._return(ExpressionFactory._null());

            Method isProtocolSupported = dummyInboundEndpoint.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, "isProtocolSupported");
            isProtocolSupported.param(String.class, "protocol");
            isProtocolSupported.body()._return(ExpressionFactory.lit(false));

            Method getMimeType = dummyInboundEndpoint.method(Modifier.PUBLIC, String.class, "getMimeType");
            getMimeType.body()._return(ExpressionFactory._null());

            Method isDisableTransportTransformer = dummyInboundEndpoint.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, "isDisableTransportTransformer");
            isDisableTransportTransformer.body()._return(ExpressionFactory.lit(false));
        }
    }

    private DefinedClass getDummyInboundEndpointClass(ExecutableElement executableElement, GeneratorContext context) {
        TypeElement parentClass = ElementFilter.typesIn(Arrays.asList(executableElement.getEnclosingElement())).get(0);
        String packageName = context.getNameUtils().getPackageName(context.getElementsUtils().getBinaryName(parentClass).toString());
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(packageName);
        DefinedClass dummyInboundEndpoint = pkg._class("DummyInboundEndpoint");
        dummyInboundEndpoint._implements(ImmutableEndpoint.class);

        context.setClassRole(DUMMY_INBOUND_ENDPOINT_ROLE, dummyInboundEndpoint);

        return dummyInboundEndpoint;
    }

}
