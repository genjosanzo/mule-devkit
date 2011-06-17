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
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.JExpr;
import org.mule.devkit.model.code.JFieldVar;
import org.mule.devkit.model.code.JInvocation;
import org.mule.devkit.model.code.JMethod;
import org.mule.devkit.model.code.JMod;
import org.mule.devkit.model.code.JPackage;
import org.mule.devkit.model.code.JVar;
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
            JFieldVar muleContext = dummyInboundEndpoint.field(JMod.PRIVATE, ref(MuleContext.class), "muleContext");
            JFieldVar transactionConfig = dummyInboundEndpoint.field(JMod.PRIVATE, ref(TransactionConfig.class), "transactionConfig");

            // add constructor
            JMethod constructor = dummyInboundEndpoint.constructor(JMod.PUBLIC);
            JVar muleContextParam = constructor.param(ref(MuleContext.class), "muleContext");
            constructor.body().assign(JExpr._this().ref(muleContext), muleContextParam);
            JInvocation newTransactionConfig = JExpr._new(ref(MuleTransactionConfig.class));
            constructor.body().assign(JExpr._this().ref(transactionConfig), newTransactionConfig);
            constructor.body().add(newTransactionConfig.invoke("setAction").arg(ref(TransactionConfig.class).staticRef("ACTION_NONE")));

            JMethod getEndpointURI = dummyInboundEndpoint.method(JMod.PUBLIC, EndpointURI.class, "getEndpointURI");
            getEndpointURI.body()._return(JExpr._null());

            JMethod getAddress = dummyInboundEndpoint.method(JMod.PUBLIC, String.class, "getAddress");
            getAddress.body()._return(JExpr._null());

            JMethod getEncoding = dummyInboundEndpoint.method(JMod.PUBLIC, String.class, "getEncoding");
            getEncoding.body()._return(JExpr._null());

            JMethod getConnector = dummyInboundEndpoint.method(JMod.PUBLIC, Connector.class, "getConnector");
            getConnector.body()._return(JExpr._null());

            JMethod getName = dummyInboundEndpoint.method(JMod.PUBLIC, String.class, "getName");
            getName.body()._return(JExpr._null());

            JMethod getTransformers = dummyInboundEndpoint.method(JMod.PUBLIC, List.class, Transformer.class, "getTransformers");
            getTransformers.body()._return(JExpr._null());

            JMethod getResponseTransformers = dummyInboundEndpoint.method(JMod.PUBLIC, List.class, Transformer.class, "getResponseTransformers");
            getResponseTransformers.body()._return(JExpr._null());

            JMethod getProperties = dummyInboundEndpoint.method(JMod.PUBLIC, Map.class, "getProperties");
            getProperties.body()._return(JExpr._null());

            JMethod getProperty = dummyInboundEndpoint.method(JMod.PUBLIC, Object.class, "getProperty");
            getProperty.param(Object.class, "key");
            getProperty.body()._return(JExpr._null());

            JMethod getProtocol = dummyInboundEndpoint.method(JMod.PUBLIC, String.class, "getProtocol");
            getProtocol.body()._return(JExpr._null());

            JMethod isReadOnly = dummyInboundEndpoint.method(JMod.PUBLIC, context.getCodeModel().BOOLEAN, "isReadOnly");
            isReadOnly.body()._return(JExpr.lit(false));

            JMethod getTransactionConfig = dummyInboundEndpoint.method(JMod.PUBLIC, TransactionConfig.class, "getTransactionConfig");
            getTransactionConfig.body()._return(transactionConfig);

            JMethod getFilter = dummyInboundEndpoint.method(JMod.PUBLIC, Filter.class, "getFilter");
            getFilter.body()._return(JExpr._null());

            JMethod isDeleteUnacceptedMessages = dummyInboundEndpoint.method(JMod.PUBLIC, context.getCodeModel().BOOLEAN, "isDeleteUnacceptedMessages");
            isDeleteUnacceptedMessages.body()._return(JExpr.lit(false));

            JMethod getSecurityFilter = dummyInboundEndpoint.method(JMod.PUBLIC, EndpointSecurityFilter.class, "getSecurityFilter");
            getSecurityFilter.body()._return(JExpr._null());

            JMethod getMessageProcessorsFactory = dummyInboundEndpoint.method(JMod.PUBLIC, EndpointMessageProcessorChainFactory.class, "getMessageProcessorsFactory");
            getMessageProcessorsFactory.body()._return(JExpr._null());

            JMethod getMessageProcessors = dummyInboundEndpoint.method(JMod.PUBLIC, List.class, MessageProcessor.class, "getMessageProcessors");
            getMessageProcessors.body()._return(JExpr._null());

            JMethod getResponseMessageProcessors = dummyInboundEndpoint.method(JMod.PUBLIC, List.class, MessageProcessor.class, "getResponseMessageProcessors");
            getResponseMessageProcessors.body()._return(JExpr._null());

            JMethod getExchangePattern = dummyInboundEndpoint.method(JMod.PUBLIC, MessageExchangePattern.class, "getExchangePattern");
            getExchangePattern.body()._return(ref(MessageExchangePattern.class).staticRef("ONE_WAY"));

            JMethod getResponseTimeout = dummyInboundEndpoint.method(JMod.PUBLIC, context.getCodeModel().INT, "getResponseTimeout");
            getResponseTimeout.body()._return(JExpr.lit(0));

            JMethod getInitialState = dummyInboundEndpoint.method(JMod.PUBLIC, String.class, "getInitialState");
            getInitialState.body()._return(JExpr._null());

            JMethod getMuleContext = dummyInboundEndpoint.method(JMod.PUBLIC, MuleContext.class, "getMuleContext");
            getMuleContext.body()._return(muleContext);

            JMethod getRetryPolicyTemplate = dummyInboundEndpoint.method(JMod.PUBLIC, RetryPolicyTemplate.class, "getRetryPolicyTemplate");
            getRetryPolicyTemplate.body()._return(JExpr._null());

            JMethod getEndpointBuilderName = dummyInboundEndpoint.method(JMod.PUBLIC, String.class, "getEndpointBuilderName");
            getEndpointBuilderName.body()._return(JExpr._null());

            JMethod isProtocolSupported = dummyInboundEndpoint.method(JMod.PUBLIC, context.getCodeModel().BOOLEAN, "isProtocolSupported");
            isProtocolSupported.param(String.class, "protocol");
            isProtocolSupported.body()._return(JExpr.lit(false));

            JMethod getMimeType = dummyInboundEndpoint.method(JMod.PUBLIC, String.class, "getMimeType");
            getMimeType.body()._return(JExpr._null());

            JMethod isDisableTransportTransformer = dummyInboundEndpoint.method(JMod.PUBLIC, context.getCodeModel().BOOLEAN, "isDisableTransportTransformer");
            isDisableTransportTransformer.body()._return(JExpr.lit(false));
        }
    }

    private DefinedClass getDummyInboundEndpointClass(ExecutableElement executableElement, GeneratorContext context) {
        TypeElement parentClass = ElementFilter.typesIn(Arrays.asList(executableElement.getEnclosingElement())).get(0);
        String packageName = context.getNameUtils().getPackageName(context.getElementsUtils().getBinaryName(parentClass).toString());
        JPackage pkg = context.getCodeModel()._package(packageName);
        DefinedClass dummyInboundEndpoint = pkg._class("DummyInboundEndpoint");
        dummyInboundEndpoint._implements(ImmutableEndpoint.class);

        context.setClassRole(DUMMY_INBOUND_ENDPOINT_ROLE, dummyInboundEndpoint);

        return dummyInboundEndpoint;
    }

}
