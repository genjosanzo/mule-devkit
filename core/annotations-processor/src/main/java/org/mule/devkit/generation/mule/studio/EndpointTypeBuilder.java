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

import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.EndpointType;

import javax.lang.model.element.ExecutableElement;
import java.util.Collection;

public class EndpointTypeBuilder extends BaseStudioXmlBuilder {

    public EndpointTypeBuilder(GeneratorContext context, ExecutableElement executableElement, DevKitTypeElement typeElement) {
        super(context, executableElement, typeElement);
    }

    public EndpointType build() {
        EndpointType endpoint = new EndpointType();
        endpoint.setLocalId(nameUtils.uncamel(executableElement.getSimpleName().toString()));
        endpoint.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(executableElement.getSimpleName().toString())));
        endpoint.setIcon(helper.getIcon(typeElement.name()));
        endpoint.setImage(helper.getIcon(typeElement.name()));
        endpoint.setDescription(helper.formatDescription(javaDocUtils.getSummary(executableElement)));
        endpoint.setSupportsInbound(true);
        endpoint.setSupportsOutbound(false);
        endpoint.setInboundLocalName(nameUtils.uncamel(executableElement.getSimpleName().toString()));

        Collection<AttributeCategory> attributeCategories = processMethodParameters();
        endpoint.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().addAll(attributeCategories);

        return endpoint;
    }
}