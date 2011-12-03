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

import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.studio.EndpointType;
import org.mule.devkit.model.studio.NamespaceType;
import org.mule.devkit.model.studio.ObjectFactory;
import org.mule.devkit.model.studio.PatternType;

import javax.lang.model.element.ExecutableElement;

public class MuleStudioXmlGenerator extends AbstractMessageGenerator {

    public static final String URI_PREFIX = "http://www.mulesoft.org/schema/mule/";
    public static final String GLOBAL_CLOUD_CONNECTOR_LOCAL_ID = "config";
    public static final String ATTRIBUTE_CATEGORY_DEFAULT_CAPTION = "General";
    public static final String ATTRIBUTE_CATEGORY_DEFAULT_DESCRIPTION = "General properties";
    public static final String GROUP_DEFAULT_CAPTION = "Generic";
    private ObjectFactory objectFactory = new ObjectFactory();

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return !context.hasOption("skipStudioXmlGeneration");
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {
        String moduleName = typeElement.name();

        NamespaceType namespace = new NamespaceType();
        namespace.setPrefix(moduleName);
        namespace.setUrl(URI_PREFIX + moduleName);

        namespace.getConnectorOrEndpointOrGlobal().add(new GlobalCloudConnectorBuilder(context, typeElement).build());
        namespace.getConnectorOrEndpointOrGlobal().add(new CloudConnectorOperationsBuilder(context, typeElement).build());
        namespace.getConnectorOrEndpointOrGlobal().add(new ConfigRefBuilder(context).build(moduleName));

        if (typeElement.hasMethodsAnnotatedWith(Transformer.class)) {
            namespace.getConnectorOrEndpointOrGlobal().add(new AbstractTransformerBuilder(context).build());
        }

        for (ExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            PatternType patternType = new PatternTypeBuilder(context, executableElement, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeCloudConnector(patternType));
            namespace.getConnectorOrEndpointOrGlobal().addAll(new NestedsBuilder(context, executableElement, moduleName).build());
        }

        for (ExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Transformer.class)) {
            PatternType patternType = new PatternTypeBuilder(context, executableElement, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeTransformer(patternType));
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalTransformer(new GlobalTypeBuilder(context, executableElement, typeElement).build()));
            namespace.getConnectorOrEndpointOrGlobal().addAll(new NestedsBuilder(context, executableElement, moduleName).build());
        }

        for (ExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Source.class)) {
            EndpointType endpointType = new EndpointTypeBuilder(context, executableElement, typeElement).build();
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createEndpoint(endpointType));
            namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeGlobalEndpoint(new GlobalTypeBuilder(context, executableElement, typeElement).build()));
        }

        context.getStudioModel().setNamespaceType(namespace);
        context.getStudioModel().setModuleName(moduleName);
    }
}