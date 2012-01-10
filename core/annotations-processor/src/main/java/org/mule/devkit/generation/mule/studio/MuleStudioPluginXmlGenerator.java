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

package org.mule.devkit.generation.mule.studio;

import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.generation.mule.studio.editor.MuleStudioEditorXmlGenerator;
import org.mule.devkit.generation.spring.SchemaGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MuleStudioPluginXmlGenerator extends AbstractMessageGenerator {

    public static final String PLUGIN_XML_FILE_NAME = "plugin.xml";

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return !context.hasOption("skipStudioPluginPackage");
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) throws GenerationException {
        try {

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();

            Element pluginElement = document.createElement("plugin");
            document.appendChild(pluginElement);

            Element extensionElement = document.createElement("extension");
            extensionElement.setAttribute("point", "org.mule.tooling.core.contribution");

            pluginElement.appendChild(extensionElement);

            Element externalContributionElement = document.createElement("externalContribution");
            externalContributionElement.setAttribute("contributionJar", "%JAR_NAME%");
            externalContributionElement.setAttribute("contributionLibPathInMule", "/plugins");
            externalContributionElement.setAttribute("contributionLibs", "%ZIP_NAME%");
            externalContributionElement.setAttribute("contributionSources", "%SOURCES_JAR%");
            externalContributionElement.setAttribute("contributionJavaDocs", "%JAVADOC_JAR%");
            externalContributionElement.setAttribute("contributionNamespace", SchemaGenerator.getNamespace(typeElement));
            externalContributionElement.setAttribute("contributionNamespaceFile", SchemaGenerator.getVersionedLocation(typeElement));
            externalContributionElement.setAttribute("contributionNamespacePrefix", typeElement.name());
            externalContributionElement.setAttribute("contributionType", "cloud-connector");
            externalContributionElement.setAttribute("path", MuleStudioEditorXmlGenerator.EDITOR_XML_FILE_NAME);
            externalContributionElement.setAttribute("version", "%PROJECT_VERSION%");

            extensionElement.appendChild(externalContributionElement);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(context.getCodeModel().getCodeWriter().openBinary(null, PLUGIN_XML_FILE_NAME));
            transformer.transform(source, result);

        } catch (Exception e) {
            throw new GenerationException("Error generating Mule Studio plugin.xml", e);
        }
    }
}