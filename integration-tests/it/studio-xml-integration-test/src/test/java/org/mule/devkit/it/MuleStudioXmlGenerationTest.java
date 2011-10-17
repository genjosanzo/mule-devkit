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

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.examples.RecursiveElementNameAndTextQualifier;
import org.mule.util.IOUtils;

public class MuleStudioXmlGenerationTest extends XMLTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
    }

    public void testGeneratedXmlIsAsExpected() throws Exception {
        String expectedXml = IOUtils.toString(MuleStudioXmlGenerationTest.class.getClassLoader().getResourceAsStream("expected-studio-xml.xml"));
        String actualXml = IOUtils.toString(MuleStudioXmlGenerationTest.class.getClassLoader().getResourceAsStream("META-INF/mymodule-studio.xml"));
        Diff myDiff = new Diff(expectedXml, actualXml);
        myDiff.overrideElementQualifier(new RecursiveElementNameAndTextQualifier());
        assertTrue(new DetailedDiff(myDiff).toString(), myDiff.similar());
    }
}