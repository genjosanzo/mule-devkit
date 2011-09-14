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

public class NestedProcessorModuleTest extends AbstractModuleTest
{

    @Override
    protected String getConfigResources()
    {
        return "callback.xml";
    }

    public void testCallback() throws Exception
    {
        runFlow("callback", "mulesoft");
    }

    public void testCallbackWithString() throws Exception
    {
        runFlow("callbackWithString", "mulesoft");
    }

    public void testCallbackListWithString() throws Exception
    {
        runFlow("callbackListWithString", "mulesoft");
    }

    public void testProcessItems() throws Exception
    {
        runFlowWithPayload("processItems", "payload", "payload");
    }

    public void testProcessOneByOne() throws Exception
    {
        runFlowWithPayload("processItemsOneByOne", 3, "payload");
    }


    public void testCallbackWithPayload() throws Exception
    {
        runFlowWithPayload("callbackWithPayload", "payload", "payload");
    }
}