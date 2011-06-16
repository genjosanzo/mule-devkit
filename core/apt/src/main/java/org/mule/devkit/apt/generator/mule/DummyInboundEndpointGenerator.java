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

package org.mule.devkit.apt.generator.mule;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.devkit.annotations.Source;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.AbstractCodeGenerator;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.*;
import org.mule.transaction.MuleTransactionConfig;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.Map;

public class DummyInboundEndpointGenerator extends AbstractCodeGenerator {
    public DummyInboundEndpointGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement type) throws GenerationException {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Source source = executableElement.getAnnotation(Source.class);

            if (source == null)
                continue;

            DefinedClass dummyInboundEndpoint = getDummyInboundEndpointClass(executableElement);

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

            JMethod getEndpointURI = dummyInboundEndpoint.method(JMod.PUBLIC, ref(EndpointURI.class), "getEndpointURI");
            getEndpointURI.body()._return(JExpr._null());

            JMethod getAddress = dummyInboundEndpoint.method(JMod.PUBLIC, ref(String.class), "getAddress");
            getAddress.body()._return(JExpr._null());

            JMethod getEncoding = dummyInboundEndpoint.method(JMod.PUBLIC, ref(String.class), "getEncoding");
            getEncoding.body()._return(JExpr._null());

            JMethod getConnector = dummyInboundEndpoint.method(JMod.PUBLIC, ref(Connector.class), "getConnector");
            getConnector.body()._return(JExpr._null());

            JMethod getName = dummyInboundEndpoint.method(JMod.PUBLIC, ref(String.class), "getName");
            getName.body()._return(JExpr._null());

            JMethod getTransformers = dummyInboundEndpoint.method(JMod.PUBLIC, ref(List.class).narrow(ref(Transformer.class)), "getTransformers");
            getTransformers.body()._return(JExpr._null());

            JMethod getResponseTransformers = dummyInboundEndpoint.method(JMod.PUBLIC, ref(List.class).narrow(ref(Transformer.class)), "getResponseTransformers");
            getResponseTransformers.body()._return(JExpr._null());

            JMethod getProperties = dummyInboundEndpoint.method(JMod.PUBLIC, ref(Map.class), "getProperties");
            getProperties.body()._return(JExpr._null());

            JMethod getProperty = dummyInboundEndpoint.method(JMod.PUBLIC, ref(Object.class), "getProperty");
            getProperty.param(ref(Object.class), "key");
            getProperty.body()._return(JExpr._null());

            JMethod getProtocol = dummyInboundEndpoint.method(JMod.PUBLIC, ref(String.class), "getProtocol");
            getProtocol.body()._return(JExpr._null());

            JMethod isReadOnly = dummyInboundEndpoint.method(JMod.PUBLIC, getContext().getCodeModel().BOOLEAN, "isReadOnly");
            isReadOnly.body()._return(JExpr.lit(false));

            JMethod getTransactionConfig = dummyInboundEndpoint.method(JMod.PUBLIC, ref(TransactionConfig.class), "getTransactionConfig");
            getTransactionConfig.body()._return(transactionConfig);

            JMethod getFilter = dummyInboundEndpoint.method(JMod.PUBLIC, ref(Filter.class), "getFilter");
            getFilter.body()._return(JExpr._null());

            JMethod isDeleteUnacceptedMessages = dummyInboundEndpoint.method(JMod.PUBLIC, getContext().getCodeModel().BOOLEAN, "isDeleteUnacceptedMessages");
            isDeleteUnacceptedMessages.body()._return(JExpr.lit(false));

            JMethod getSecurityFilter = dummyInboundEndpoint.method(JMod.PUBLIC, ref(EndpointSecurityFilter.class), "getSecurityFilter");
            getSecurityFilter.body()._return(JExpr._null());

            JMethod getMessageProcessorsFactory = dummyInboundEndpoint.method(JMod.PUBLIC, ref(EndpointMessageProcessorChainFactory.class), "getMessageProcessorsFactory");
            getMessageProcessorsFactory.body()._return(JExpr._null());

            JMethod getMessageProcessors = dummyInboundEndpoint.method(JMod.PUBLIC, ref(List.class).narrow(MessageProcessor.class), "getMessageProcessors");
            getMessageProcessors.body()._return(JExpr._null());

            JMethod getResponseMessageProcessors = dummyInboundEndpoint.method(JMod.PUBLIC, ref(List.class).narrow(MessageProcessor.class), "getResponseMessageProcessors");
            getResponseMessageProcessors.body()._return(JExpr._null());

            JMethod getExchangePattern = dummyInboundEndpoint.method(JMod.PUBLIC, ref(MessageExchangePattern.class), "getExchangePattern");
            getExchangePattern.body()._return(ref(MessageExchangePattern.class).staticRef("ONE_WAY"));

            JMethod getResponseTimeout = dummyInboundEndpoint.method(JMod.PUBLIC, getContext().getCodeModel().INT, "getResponseTimeout");
            getResponseTimeout.body()._return(JExpr.lit(0));

            JMethod getInitialState = dummyInboundEndpoint.method(JMod.PUBLIC, ref(String.class), "getInitialState");
            getInitialState.body()._return(JExpr._null());

            JMethod getMuleContext = dummyInboundEndpoint.method(JMod.PUBLIC, ref(MuleContext.class), "getMuleContext");
            getMuleContext.body()._return(muleContext);

            JMethod getRetryPolicyTemplate = dummyInboundEndpoint.method(JMod.PUBLIC, ref(RetryPolicyTemplate.class), "getRetryPolicyTemplate");
            getRetryPolicyTemplate.body()._return(JExpr._null());

            JMethod getEndpointBuilderName = dummyInboundEndpoint.method(JMod.PUBLIC, ref(String.class), "getEndpointBuilderName");
            getEndpointBuilderName.body()._return(JExpr._null());

            JMethod isProtocolSupported = dummyInboundEndpoint.method(JMod.PUBLIC, getContext().getCodeModel().BOOLEAN, "isProtocolSupported");
            isProtocolSupported.param(ref(String.class), "protocol");
            isProtocolSupported.body()._return(JExpr.lit(false));

            JMethod getMimeType = dummyInboundEndpoint.method(JMod.PUBLIC, ref(String.class), "getMimeType");
            getMimeType.body()._return(JExpr._null());

            JMethod isDisableTransportTransformer = dummyInboundEndpoint.method(JMod.PUBLIC, getContext().getCodeModel().BOOLEAN, "isDisableTransportTransformer");
            isDisableTransportTransformer.body()._return(JExpr.lit(false));
        }
    }
}
