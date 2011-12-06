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
package org.mule.devkit.it.studio;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.examples.RecursiveElementNameAndTextQualifier;
import org.junit.Test;
import org.mule.util.IOUtils;

import static org.junit.Assert.assertTrue;

public class SourceModuleStudioXmlTest {

    private static final String EXPECTED_STUDIO_XML = "expected-studio-xml.xml";
    private static final String ACTUAL_STUDIO_XML = "META-INF/source-studio.xml";

    @Test
    public void sourceModulesStudioXmlGeneration() throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        String expectedXml = IOUtils.toString(SourceModuleStudioXmlTest.class.getClassLoader().getResourceAsStream(EXPECTED_STUDIO_XML));
        String actualXml = IOUtils.toString(SourceModuleStudioXmlTest.class.getClassLoader().getResourceAsStream(ACTUAL_STUDIO_XML));
        Diff diff = new Diff(expectedXml, actualXml);
        diff.overrideElementQualifier(new RecursiveElementNameAndTextQualifier());
        DetailedDiff detailedDiff = new DetailedDiff(diff);
        assertTrue(detailedDiff.toString(), detailedDiff.similar());
    }
}