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
package org.mule.devkit.it;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.construct.Flow;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class OAuthModuleTest extends FunctionalTestCase {

    @Override
    protected String getConfigResources() {
        return "oauth.xml";
    }

    public void testNonProtectedResource() throws Exception {
        MuleEvent responseEvent = runFlow("nonProtectedResource");
        assertEquals(OAuthModule.NON_PROTECTED_RESOURCE, responseEvent.getMessageAsString());
    }

    public void testProtectedResource() throws Exception {
        MuleEvent responseEvent = runFlow("protectedResource");
        String url = verifyUserIsRedirectedToAuthorizationUrl(responseEvent);
        simulateCallbackUponUserAuthorizingConsumer(url);
        responseEvent = runFlow("protectedResource");
        verifiyProtectedResourceWasAccessed(responseEvent);
    }

    private void verifiyProtectedResourceWasAccessed(MuleEvent responseEvent) throws MuleException {
        assertEquals(OAuthModule.PROTECTED_RESOURCE, responseEvent.getMessageAsString());
        assertEquals(1, RequestTokenComponent.timesCalled);
        assertEquals(1, AccessTokenComponent.timesCalled);
    }

    private void simulateCallbackUponUserAuthorizingConsumer(String url) throws IOException {
        String callbackUrl = url.substring(url.indexOf("oauth_callback=") + "oauth_callback=".length()).replaceAll("%3A", ":").replaceAll("%2F", "/");
        callbackUrl += "?oauth_verifier=" + Constants.OAUTH_VERIFIER;
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(callbackUrl).openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.getInputStream();
    }

    private String verifyUserIsRedirectedToAuthorizationUrl(MuleEvent responseEvent) {
        assertEquals("302", responseEvent.getMessage().getOutboundProperty("http.status"));
        String url = responseEvent.getMessage().getOutboundProperty("Location");
        assertNotNull(url);
        assertEquals(1, RequestTokenComponent.timesCalled);
        assertEquals(0, AccessTokenComponent.timesCalled);
        return url;
    }

    private Flow lookupFlowConstruct(String name) {
        return (Flow) AbstractMuleTestCase.muleContext.getRegistry().lookupFlowConstruct(name);
    }

    private MuleEvent runFlow(String flowName) throws Exception {
        Flow flow = lookupFlowConstruct(flowName);
        MuleEvent event = AbstractMuleTestCase.getTestEvent("");
        return flow.process(event);
    }
}