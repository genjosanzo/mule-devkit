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

package org.mule.devkit.generation.mule.studio.editor;

import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.studio.EndpointType;
import org.mule.devkit.model.studio.GlobalType;
import org.mule.devkit.model.studio.NamespaceType;
import org.mule.devkit.model.studio.ObjectFactory;
import org.mule.devkit.model.studio.PatternType;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

public class MuleStudioEditorXmlGenerator extends AbstractMessageGenerator {

    public static final String EDITOR_XML_FILE_NAME = "editors.xml";
    public static final String URI_PREFIX = "http://www.mulesoft.org/schema/mule/";
    public static final String GLOBAL_CLOUD_CONNECTOR_LOCAL_ID = "config";
    public static final String ATTRIBUTE_CATEGORY_DEFAULT_CAPTION = "General";
    public static final String ATTRIBUTE_CATEGORY_DEFAULT_DESCRIPTION = "General";
    public static final String CONNECTION_ATTRIBUTE_CATEGORY_CAPTION = "Connection";
    public static final String GROUP_DEFAULT_CAPTION = "Generic";
    private ObjectFactory objectFactory = new ObjectFactory();

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return !context.hasOption("skipStudioPluginPackage");
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {
        String moduleName = typeElement.name();

        NamespaceType namespace = new NamespaceType();
        namespace.setPrefix(moduleName);
        namespace.setUrl(URI_PREFIX + moduleName);

        GlobalType globalCloudConnector = new GlobalCloudConnectorTypeBuilder(context, typeElement).build();
        namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalCloudConnector(globalCloudConnector));
        namespace.getConnectorOrEndpointOrGlobal().add(new PatternTypeOperationsBuilder(context, typeElement, PatternTypes.CLOUD_CONNECTOR).build());
        namespace.getConnectorOrEndpointOrGlobal().add(new ConfigRefBuilder(context, typeElement).build());
        namespace.getConnectorOrEndpointOrGlobal().addAll(new NestedsBuilder(context, typeElement).build());

        processProcessorMethods(typeElement, namespace);
        processTransformerMethods(typeElement, namespace);
        processSourceMethods(typeElement, namespace);

        context.getStudioModel().setNamespaceType(namespace);
        context.getStudioModel().setOutputFileName(EDITOR_XML_FILE_NAME);
    }

    private void processProcessorMethods(DevKitTypeElement typeElement, NamespaceType namespace) {
        for (ExecutableElement processorMethod : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            PatternType cloudConnector = new PatternTypeBuilder(context, processorMethod, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeCloudConnector(cloudConnector));
            namespace.getConnectorOrEndpointOrGlobal().addAll(new NestedsBuilder(context, processorMethod, typeElement).build());
        }
    }

    private void processTransformerMethods(DevKitTypeElement typeElement, NamespaceType namespace) {
        List<ExecutableElement> transformerMethods = typeElement.getMethodsAnnotatedWith(Transformer.class);
        if (!transformerMethods.isEmpty()) {
            namespace.getConnectorOrEndpointOrGlobal().add(new PatternTypeOperationsBuilder(context, typeElement, PatternTypes.TRANSFORMER).build());
            namespace.getConnectorOrEndpointOrGlobal().add(new AbstractTransformerBuilder(context, typeElement).build());
            GlobalType globalTransformer = new GlobalTransformerTypeOperationsBuilder(context, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalTransformer(globalTransformer));
        }
        for (ExecutableElement transformerMethod : transformerMethods) {
            PatternType transformer = new PatternTypeBuilder(context, transformerMethod, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeTransformer(transformer));
            GlobalType globalTransformer = new GlobalTransformerTypeBuilder(context, transformerMethod, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalTransformer(globalTransformer));
            namespace.getConnectorOrEndpointOrGlobal().addAll(new NestedsBuilder(context, transformerMethod, typeElement).build());
        }
    }

    private void processSourceMethods(DevKitTypeElement typeElement, NamespaceType namespace) {
        List<ExecutableElement> sourceMethods = typeElement.getMethodsAnnotatedWith(Source.class);
        if (!sourceMethods.isEmpty()) {
            GlobalType abstractGlobalEndpoint = new GlobalEndpointTypeWithNameBuilder(context, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalEndpoint(abstractGlobalEndpoint));
            EndpointType endpointTypeListingOps = new EndpointTypeOperationsBuilder(context, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createEndpoint(endpointTypeListingOps));
            GlobalType globalEndpointListingOps = new GlobalEndpointTypeOperationsBuilder(context, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalEndpoint(globalEndpointListingOps));
        }
        for (ExecutableElement sourceMethod : sourceMethods) {
            EndpointType endpoint = new EndpointTypeBuilder(context, sourceMethod, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createEndpoint(endpoint));
            GlobalType globalEndpoint = new GlobalEndpointTypeBuilder(context, sourceMethod, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalEndpoint(globalEndpoint));
            namespace.getConnectorOrEndpointOrGlobal().addAll(new NestedsBuilder(context, sourceMethod, typeElement).build());
        }
    }
}