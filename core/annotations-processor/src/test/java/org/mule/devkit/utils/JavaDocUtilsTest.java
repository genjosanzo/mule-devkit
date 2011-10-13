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
package org.mule.devkit.utils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class JavaDocUtilsTest {

    @Mock
    private Elements elements;
    @Mock
    private ExecutableElement executableElement;

    @Before
    public void setUpTests() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getTagContentMultiLineSampleTag() throws Exception {
        when(elements.getDocComment(executableElement)).thenReturn("\n" +
                "     Updates the given map of customer address attributes, for the given customer address\n" +
                "     <p/>\n" +
                "     {@sample.xml " +
                "../../../doc/magento-connector.xml.sample\n " +
                "magento:updateCustomerAddress}\n" +
                "     \n" +
                "     @param addressId  the customer address to update\n" +
                "     @param attributes the address attributes to update");
        JavaDocUtils javaDocUtils = new JavaDocUtils(elements);
        String sample = javaDocUtils.getTagContent("sample.xml", executableElement);
        assertEquals("../../../doc/magento-connector.xml.sample magento:updateCustomerAddress", sample);
    }

    @Test
    public void getTagContentSingleLineSampleTag() throws Exception {
        when(elements.getDocComment(executableElement)).thenReturn("\n" +
                "     Updates the given map of customer address attributes, for the given customer address\n" +
                "     <p/>\n" +
                "     {@sample.xml ../../../doc/magento-connector.xml.sample magento:updateCustomerAddress}\n" +
                "     \n" +
                "     @param addressId  the customer address to update\n" +
                "     @param attributes the address attributes to update");
        JavaDocUtils javaDocUtils = new JavaDocUtils(elements);
        String sample = javaDocUtils.getTagContent("sample.xml", executableElement);
        assertEquals("../../../doc/magento-connector.xml.sample magento:updateCustomerAddress", sample);
    }
}