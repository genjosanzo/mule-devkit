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
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.studio.NamespaceType;

import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;
import java.util.List;

public class MuleStudioXmlGenerator extends AbstractMessageGenerator {

    public static final String URI_PREFIX = "http://www.mulesoft.org/schema/mule/";

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

        List<String> parsedLocalIds = new ArrayList<String>();
        for (ExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            namespace.getConnectorOrEndpointOrGlobal().add(new CloudConnectorOperationBuilder(context, executableElement, typeElement).build());
            namespace.getConnectorOrEndpointOrGlobal().addAll(new NestedsBuilder(context, executableElement, moduleName, parsedLocalIds).build());
        }

        context.getStudioModel().setNamespaceType(namespace);
        context.getStudioModel().setModuleName(moduleName);
    }
}