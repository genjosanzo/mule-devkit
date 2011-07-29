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

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeadersModuleTest extends AbstractModuleTest
{

    @Override
    protected String getConfigResources()
    {
        return "headers.xml";
    }

    public void testProcessHeader() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("foo", "MULESOFT");

        runFlowWithPayloadAndHeaders("processHeader", headers, "MULESOFT", "");
    }

    public void testProcessHeaderOptional() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        //headers.put("faz", "MULESOFT");

        runFlowWithPayloadAndHeaders("processHeaderOptional", headers, "faz not set", "");
    }

    public void testProcessHeaderNotOptional() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("faz", "MULESOFT");

        runFlowWithPayloadAndHeaders("processHeaderOptional", headers, "MULESOFT", "");
    }

    public void testProcessHeaderWithType() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("url", "http://www.mulesoft.com");

        runFlowWithPayloadAndHeaders("processHeaderWithType", headers, new URL("http://www.mulesoft.com"), "");
    }

    public void testProcessHeaders() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("foo", "MULE");
        headers.put("bar", "SOFT");

        Map result = (Map)runFlowWithPayloadAndHeaders("processHeaders", headers, "").getPayload();

        assertEquals(result.get("foo"), "MULE");
        assertEquals(result.get("bar"), "SOFT");
    }

    public void testProcessHeadersAll() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("foo", "MULE");
        headers.put("bar", "SOFT");

        Map result = (Map)runFlowWithPayloadAndHeaders("processHeadersAll", headers, "").getPayload();

        assertEquals(result.get("foo"), "MULE");
        assertEquals(result.get("bar"), "SOFT");
    }

    public void testProcessHeadersWildcard() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("MULE_1", "MULE");
        headers.put("bar", "SOFT");

        Map result = (Map)runFlowWithPayloadAndHeaders("processHeadersWildcard", headers, "").getPayload();

        assertEquals(result.get("MULE_1"), "MULE");
        assertFalse(result.containsKey("bar"));
    }

    public void testProcessHeadersMultiWildcard() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("MULE_1", "MULE");
        headers.put("bar", "SOFT");

        Map result = (Map)runFlowWithPayloadAndHeaders("processHeadersMultiWildcard", headers, "").getPayload();

        assertEquals(result.get("MULE_1"), "MULE");
        assertTrue(result.containsKey("bar"));
    }

    public void testProcessSingleMapHeader() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("foo", "MULE");
        headers.put("bar", "SOFT");

        Map result = (Map)runFlowWithPayloadAndHeaders("processSingleMapHeader", headers, "").getPayload();

        assertEquals(result.get("foo"), "MULE");
    }

    public void testProcessHeadersOptional() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("foo", "MULE");
        headers.put("bar", "SOFT");
        headers.put("baz", "SOFT");

        Map result = (Map)runFlowWithPayloadAndHeaders("processHeadersOptional", headers, "").getPayload();

        assertEquals(result.get("foo"), "MULE");
        assertEquals(result.get("bar"), "SOFT");
        assertEquals(result.get("baz"), "SOFT");
    }

    public void testProcessHeadersAllOptional() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("foo", "MULE");
        headers.put("bar", "SOFT");

        Map result = (Map)runFlowWithPayloadAndHeaders("processHeadersAllOptional", headers, "").getPayload();

        assertEquals(result.get("foo"), "MULE");
        assertEquals(result.get("bar"), "SOFT");
    }


    public void testProcessHeadersList() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("foo", "MULE");
        headers.put("bar", "SOFT");
        headers.put("baz", "SOFT");

        List result = (List)runFlowWithPayloadAndHeaders("processHeadersList", headers, "").getPayload();

        assertEquals(result.get(0), "MULE");
        assertEquals(result.get(1), "SOFT");
        assertEquals(result.get(2), "SOFT");
    }

    public void testProcessHeadersListAll() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("foo", "MULE");
        headers.put("bar", "SOFT");
        headers.put("baz", "SOFT");

        List result = (List)runFlowWithPayloadAndHeaders("processHeadersListAll", headers, "").getPayload();

        assertEquals(result.get(0), "MULE");
        assertEquals(result.get(1), "SOFT");
        assertEquals(result.get(2), "SOFT");
    }

    public void testProcessSingleHeaderList() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("foo", "MULE");
        headers.put("bar", "SOFT");
        headers.put("baz", "SOFT");

        List result = (List)runFlowWithPayloadAndHeaders("processSingleHeaderList", headers, "").getPayload();

        assertEquals(result.get(0), "MULE");
        assertEquals(result.size(), 1);
    }

    public void testProcessHeadersListOptional() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("foo", "MULE");
        headers.put("bar", "SOFT");
        headers.put("baz", "SOFT");

        List result = (List)runFlowWithPayloadAndHeaders("processHeadersListOptional", headers, "").getPayload();

        assertEquals(result.get(0), "MULE");
        assertEquals(result.get(1), "SOFT");
        assertEquals(result.get(2), "SOFT");
    }

    public void testProcessHeadersListAllOptional() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("foo", "MULE");
        headers.put("bar", "SOFT");

        List result = (List)runFlowWithPayloadAndHeaders("processHeadersListAllOptional", headers, "").getPayload();

        assertEquals(result.get(0), "MULE");
        assertEquals(result.get(1), "SOFT");
    }

    public void testProcessHeadersListWildcard() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("MULE_1", "MULE");
        headers.put("bar", "SOFT");

        List result = (List)runFlowWithPayloadAndHeaders("processHeadersListWildcard", headers, "").getPayload();

        assertEquals(result.get(0), "MULE");
    }


    public void testProcessHeadersListMultiWildcard() throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("MULE_1", "MULE");
        headers.put("bar", "SOFT");

        List result = (List)runFlowWithPayloadAndHeaders("processHeadersListMultiWildcard", headers, "").getPayload();

        assertEquals(result.get(0), "SOFT");
        assertEquals(result.get(1), "MULE");
    }
}

