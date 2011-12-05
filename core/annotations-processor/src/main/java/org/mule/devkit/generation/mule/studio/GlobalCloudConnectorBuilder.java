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
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.GlobalType;
import org.mule.devkit.model.studio.Group;

import javax.xml.bind.JAXBElement;
import java.util.Collection;

public class GlobalCloudConnectorBuilder extends BaseStudioXmlBuilder {

    public GlobalCloudConnectorBuilder(GeneratorContext context, DevKitTypeElement typeElement) {
        super(context, typeElement);
    }

    public JAXBElement<GlobalType> build() {

        AttributeType nameAttributeType = new AttributeType();
        nameAttributeType.setName("name");
        nameAttributeType.setDescription(helper.formatDescription("Give a name to this configuration so it can be later referenced by config-ref."));
        nameAttributeType.setCaption(helper.formatCaption("Name"));
        nameAttributeType.setRequired(false);

        Group group = new Group();
        group.setId(moduleName + "GenericProperties");
        group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupName(nameAttributeType));
        group.setCaption(helper.formatCaption(MuleStudioXmlGenerator.GROUP_DEFAULT_CAPTION));

        Collection<AttributeCategory> attributeCategories = processConfigurableFields(group);

        GlobalType globalCloudConnector = new GlobalType();
        globalCloudConnector.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().addAll(attributeCategories);
        globalCloudConnector.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(typeElement.name())));
        globalCloudConnector.setLocalId(MuleStudioXmlGenerator.GLOBAL_CLOUD_CONNECTOR_LOCAL_ID);
        globalCloudConnector.setDescription(helper.formatDescription("Global " + nameUtils.friendlyNameFromCamelCase(typeElement.name()) + " configuration information"));
        globalCloudConnector.setExtends(MuleStudioXmlGenerator.URI_PREFIX + typeElement.name() + '/' + helper.getGlobalRefId(typeElement.name()));
        globalCloudConnector.setIcon(helper.getIcon(typeElement.name()));
        globalCloudConnector.setImage(helper.getImage(typeElement.name()));
        return objectFactory.createNamespaceTypeGlobalCloudConnector(globalCloudConnector);
    }
}