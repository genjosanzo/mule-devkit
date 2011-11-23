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

import org.junit.Before;
import org.junit.Test;
import org.mule.util.IOUtils;

import static org.junit.Assert.assertTrue;

public class MuleStudioXmlGenerationTest {

    private String actualXml;

    @Before
    public void setUpTests() throws Exception {
        String xml = IOUtils.toString(MuleStudioXmlGenerationTest.class.getClassLoader().getResourceAsStream("META-INF/mymodule-studio.xml"));
        actualXml = normalizeXml(xml);
    }

    @Test
    public void globalCloudConnector() throws Exception {
        runTest("global-cloud-connector.xml");
    }

    @Test
    public void cloudConnectorListingOps() throws Exception {
        runTest("cloud-connector-listing-ops.xml");
    }

    @Test
    public void cloudConnectorGlobalRef() throws Exception {
        runTest("cloud-connector-global-ref.xml");
    }

    @Test
    public void cloudConnectorOperation1() throws Exception {
        runTest("cloud-connector-operation1.xml");
    }

    @Test
    public void cloudConnectorOperation2() throws Exception {
        runTest("cloud-connector-operation2.xml");
    }

    @Test
    public void cloudConnectorOperation3() throws Exception {
        runTest("cloud-connector-operation3.xml");
    }

    @Test
    public void cloudConnectorOperation4() throws Exception {
        runTest("cloud-connector-operation4.xml");
    }

    @Test
    public void cloudConnectorOperation5() throws Exception {
        runTest("cloud-connector-operation5.xml");
    }

    @Test
    public void cloudConnectorOperation6() throws Exception {
        runTest("cloud-connector-operation6.xml");
    }

    @Test
    public void cloudConnectorOperation7() throws Exception {
        runTest("cloud-connector-operation7.xml");
    }

    @Test
    public void cloudConnectorOperation8() throws Exception {
        runTest("cloud-connector-operation8.xml");
    }

    @Test
    public void listOfIntegers() throws Exception {
        String expectedXmlPortion = normalizeXml("<nested image=\"icons/large/mymodule-connector-48x32.png\" localId=\"integers\"\n" +
                "        icon=\"icons/small/mymodule-connector-24x16.png\" description=\"Integers.\" caption=\"Integers\">\n" +
                "    <childElement allowMultiple=\"true\" description=\"Integers.\" caption=\"Integers\"\n" +
                "                  name=\"http://www.mulesoft.org/schema/mule/mymodule/integer\"></childElement>\n" +
                "</nested>\n" +
                "<nested image=\"icons/large/mymodule-connector-48x32.png\" localId=\"integer\"\n" +
                "        icon=\"icons/small/mymodule-connector-24x16.png\" description=\"Integers.\" caption=\"Integers\">\n" +
                "<integer step=\"1\" min=\"0\" description=\"Represents the integers.\" caption=\"Integers\"\n" +
                "         name=\"integer\"></integer>\n" +
                "</nested>");
        assertTrue("expected: " + expectedXmlPortion + " \nactual: " + actualXml, actualXml.contains(expectedXmlPortion));
    }

    @Test
    public void listOfStrings() throws Exception {
        String expectedXmlPortion = normalizeXml("<nested image=\"icons/large/mymodule-connector-48x32.png\" localId=\"strings\"\n" +
                "        icon=\"icons/small/mymodule-connector-24x16.png\" description=\"Strings.\" caption=\"Strings\">\n" +
                "<childElement allowMultiple=\"true\" description=\"Strings.\" caption=\"Strings\"\n" +
                "              name=\"http://www.mulesoft.org/schema/mule/mymodule/string\"></childElement>\n" +
                "</nested>\n" +
                "<nested image=\"icons/large/mymodule-connector-48x32.png\" localId=\"string\"\n" +
                "        icon=\"icons/small/mymodule-connector-24x16.png\" description=\"Strings.\" caption=\"Strings\">\n" +
                "<string description=\"Represents the strings.\" caption=\"Strings\" name=\"string\"></string>\n" +
                "</nested>");
        assertTrue("expected: " + expectedXmlPortion + " \nactual: " + actualXml, actualXml.contains(expectedXmlPortion));
    }

    @Test
    public void listOfBooleans() throws Exception {
        String expectedXmlPortion = normalizeXml("<nested image=\"icons/large/mymodule-connector-48x32.png\" localId=\"booleans\"\n" +
                "        icon=\"icons/small/mymodule-connector-24x16.png\" description=\"Booleans.\" caption=\"Booleans\">\n" +
                "<childElement allowMultiple=\"true\" description=\"Booleans.\" caption=\"Booleans\"\n" +
                "              name=\"http://www.mulesoft.org/schema/mule/mymodule/boolean\"></childElement>\n" +
                "</nested>\n" +
                "<nested image=\"icons/large/mymodule-connector-48x32.png\" localId=\"boolean\"\n" +
                "        icon=\"icons/small/mymodule-connector-24x16.png\" description=\"Booleans.\" caption=\"Booleans\">\n" +
                "<boolean description=\"Represents the booleans.\" caption=\"Booleans\" name=\"boolean\"></boolean>\n" +
                "</nested>");
        assertTrue("expected: " + expectedXmlPortion + " \nactual: " + actualXml, actualXml.contains(expectedXmlPortion));
    }

    @Test
    public void stringStringMap() throws Exception {
        String expectedXmlPortion = normalizeXml("<nested image=\"icons/large/mymodule-connector-48x32.png\" localId=\"string-string-map\"\n" +
                "        icon=\"icons/small/mymodule-connector-24x16.png\" description=\"String string map.\"\n" +
                "        caption=\"String String Map\">\n" +
                "<childElement allowMultiple=\"true\" description=\"String string map.\" caption=\"String String Map\"\n" +
                "              name=\"http://www.mulesoft.org/schema/mule/mymodule/string-string-map\"></childElement>\n" +
                "</nested>\n" +
                "<nested image=\"icons/large/mymodule-connector-48x32.png\" localId=\"string-string-map\"\n" +
                "        icon=\"icons/small/mymodule-connector-24x16.png\" description=\"String string map.\"\n" +
                "        caption=\"String String Map\">\n" +
                "<string description=\"Key.\" caption=\"Key\" name=\"key\"></string>\n" +
                "<text isToElement=\"true\" description=\"Value.\" caption=\"Value\" name=\"value\"></text>\n" +
                "</nested>");
        assertTrue("expected: " + expectedXmlPortion + " \nactual: " + actualXml, actualXml.contains(expectedXmlPortion));
    }

    @Test
    public void stringObjectMap() throws Exception {
        String expectedXmlPortion = normalizeXml("<nested image=\"icons/large/mymodule-connector-48x32.png\" localId=\"string-object-map\"\n" +
                "        icon=\"icons/small/mymodule-connector-24x16.png\" description=\"String object map.\"\n" +
                "        caption=\"String Object Map\">\n" +
                "<childElement allowMultiple=\"true\" description=\"String object map.\" caption=\"String Object Map\"\n" +
                "              name=\"http://www.mulesoft.org/schema/mule/mymodule/string-object-map\"></childElement>\n" +
                "</nested>\n" +
                "<nested image=\"icons/large/mymodule-connector-48x32.png\" localId=\"string-object-map\"\n" +
                "        icon=\"icons/small/mymodule-connector-24x16.png\" description=\"String object map.\"\n" +
                "        caption=\"String Object Map\">\n" +
                "<string description=\"Key.\" caption=\"Key\" name=\"key\"></string>\n" +
                "<text isToElement=\"true\" description=\"Value.\" caption=\"Value\" name=\"value\"></text>\n" +
                "</nested>");
        assertTrue("expected: " + expectedXmlPortion + " \nactual: " + actualXml, actualXml.contains(expectedXmlPortion));
    }

    private void runTest(String path) {
        String xml = IOUtils.toString(MuleStudioXmlGenerationTest.class.getClassLoader().getResourceAsStream(path));
        String expectedXmlPortion = normalizeXml(xml);
        assertTrue("expected: " + expectedXmlPortion + " \nactual: " + actualXml, actualXml.contains(expectedXmlPortion));
    }

    private String normalizeXml(String xml) {
        return xml.replaceAll("\n|\t", "").replaceAll(">\\s+<", "><").replaceAll("\\s+", " ");
    }
}