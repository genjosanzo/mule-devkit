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

public class SessionModuleTest extends AbstractModuleTest {

    @Override
    protected String getConfigResources() {
        return "session.xml";
    }

    public void testVerifySessionWithPassword() throws Exception {
        try {
            runFlow("testInvalidateSessionOnException");
        } catch (Exception e) {
        }

        Map<String, String> map = new HashMap<String, String>();
        map.put("password", "ion2");
        runFlowWithPayload("testSessionWithPassword", "ion2", map);
    }

    public void testVerifySession() throws Exception {
        runFlow("testSession", true);
    }

    public void testStream() throws Exception {
        runFlow("testStream", true);
    }

    public void testVerifySessionWithCredentials() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("username", "ion2");
        map.put("password", "ion3");
        runFlowWithPayload("testSessionWithCredentials", true, map);
    }

    public void testVerifySessionWithUsername() throws Exception {
        try {
            runFlow("testInvalidateSessionOnException");
        } catch (Exception e) {
        }

        Map<String, String> map = new HashMap<String, String>();
        map.put("username", "ion2");
        runFlowWithPayload("testSessionWithUsername", "ion2", map);
    }

    public void testVerifySameSession() throws Exception {
        Integer sessionIdA = runFlow("testGetSessionId");
        Integer sessionIdB = runFlow("testGetSessionId");

        assertEquals(sessionIdA, sessionIdB);
    }

    public void testVerifyDifferentSession() throws Exception {
        Integer sessionIdA = runFlow("testGetSessionId");
        try {
            runFlow("testInvalidateSessionOnException");
        } catch (Exception e) {
        }
        Integer sessionIdB = runFlow("testGetSessionId");

        assertNotSame(sessionIdA, sessionIdB);
    }

}