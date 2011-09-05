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

import org.mule.api.MuleEvent;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.NullPayload;

import java.util.Date;

public class DateModuleTest extends FunctionalTestCase {

    @Override
    protected String getConfigResources() {
        return "date.xml";
    }

    public void testSetDate() throws Exception {
        runFlow("setDate", "2011-08-10T23:59:59-00:00");
        Object responsePayload = runFlow("getDate", "some payload");
        assertEquals(new Date(2011 - 1900, 8 - 1, 10, 23, 59, 59), responsePayload);
    }

    public void testSetDateOptional() throws Exception {
        runFlow("setOptionalDate", "some payload");
        Object responsePayload = runFlow("getDate", "some payload");
        assertEquals(NullPayload.class, responsePayload.getClass());
    }

    private SimpleFlowConstruct lookupFlowConstruct(String name) {
        return (SimpleFlowConstruct) AbstractMuleTestCase.muleContext.getRegistry().lookupFlowConstruct(name);
    }

    private Object runFlow(String flowName, String payload) throws Exception {
        SimpleFlowConstruct flow = lookupFlowConstruct(flowName);
        MuleEvent event = AbstractMuleTestCase.getTestEvent(payload);
        return flow.process(event).getMessage().getPayload();
    }
}