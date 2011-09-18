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
import org.mule.construct.Flow;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;

public class HttpCallbackProcessorModuleTest extends FunctionalTestCase {

    @Override
    protected MuleContext createMuleContext() throws Exception {
        MuleContext muleContext = super.createMuleContext();
        muleContext.getRegistry().registerObject("connector.http.mule.default", new HttpConnector(muleContext));
        return muleContext;
    }

    @Override
    protected String getConfigResources() {
        return "http-callback.xml";
    }

    @Override
    protected void doSetUp() throws Exception {
        ComponentX.reset();
        ComponentY.reset();
    }

    public void testCallback() throws Exception {
        assertFalse(ComponentX.wasExecuted());
        assertFalse(ComponentY.wasExecuted());
        runFlow("doA");
        Thread.sleep(2000);
        assertTrue(ComponentX.wasExecuted());
        assertTrue(ComponentY.wasExecuted());
    }

    public void testCallbackOptional() throws Exception {
        assertFalse(ComponentX.wasExecuted());
        runFlow("doB");
        Thread.sleep(2000);
        assertFalse(ComponentX.wasExecuted());
    }

    private MuleEvent runFlow(String flowName) throws Exception {
        Flow flow = (Flow) AbstractMuleTestCase.muleContext.getRegistry().lookupFlowConstruct(flowName);
        MuleEvent event = AbstractMuleTestCase.getTestEvent("");
        return flow.process(event);
    }
}