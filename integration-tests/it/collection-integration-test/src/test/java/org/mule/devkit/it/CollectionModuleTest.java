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

import java.util.HashMap;
import java.util.Map;

public class CollectionModuleTest extends AbstractModuleTest {

    @Override
    protected String getConfigResources() {
        return "collection.xml";
    }

    public void testList() throws Exception {
        runFlow("flowList", 2);
    }

    public void testConfigStrings() throws Exception {
        runFlow("flowConfigStrings", 2);
    }

    public void testConfigItems() throws Exception {
        runFlow("flowConfigItems", 2);
    }

    public void testDynamicConfigItems() throws Exception {
        runFlow("flowDynamicConfigItems", 3);
    }

    public void testMap() throws Exception {
        runFlow("flowMap", 2);
    }

    public void testConfigMapStrings() throws Exception {
        runFlow("flowConfigMapStrings", 2);
    }

    public void testConfigMapItems() throws Exception {
        runFlow("flowConfigMapItems", "ObjectAObjectB");
    }

    public void testHasFirstName() throws Exception {
        runFlow("flowHasFirstName");
    }

    public void testNested() throws Exception {
        runFlow("flowNested");
    }

    public void testConfigItemsRefList() throws Exception {
        runFlow("flowConfigItemsRefList", 3);
    }

    public void testCountTwoLists() throws Exception {
        runFlow("flowCountTwoLists", 5);
    }

    public void testMapOfObjects() throws Exception {
        runFlowWithPayload("flowMapOfObjects", 2, "SOFT");
    }

    public void testRetrieveKey() throws Exception {
        Map<String, String> a1 = new HashMap<String, String>();
        a1.put("a", "Mule");
        a1.put("b", "Soft");
        Map<String, String> a2 = new HashMap<String, String>();
        a2.put("a", "Soft");
        a2.put("b", "Mule");
        runFlowWithPayload("flowRetrieveKey", "Soft", a1);
        runFlowWithPayload("flowRetrieveKey", "Mule", a2);
    }

}