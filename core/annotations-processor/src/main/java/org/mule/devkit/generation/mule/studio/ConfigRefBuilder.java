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
import org.mule.devkit.generation.spring.SchemaGenerator;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.Group;
import org.mule.devkit.model.studio.NewType;
import org.mule.devkit.model.studio.PatternType;
import org.mule.util.StringUtils;

import javax.xml.bind.JAXBElement;

public class ConfigRefBuilder extends BaseStudioXmlBuilder {

    public static final String GLOBAL_REF_NAME = SchemaGenerator.ATTRIBUTE_NAME_CONFIG_REF;

    public ConfigRefBuilder(GeneratorContext context, DevKitTypeElement typeElement) {
        super(context, typeElement);
    }

    public JAXBElement<PatternType> build() {
        NewType globalRef = new NewType();
        globalRef.setRequiredType(MuleStudioEditorXmlGenerator.URI_PREFIX + moduleName + '/' + MuleStudioEditorXmlGenerator.GLOBAL_CLOUD_CONNECTOR_LOCAL_ID);
        globalRef.setName(GLOBAL_REF_NAME);
        globalRef.setCaption(helper.formatCaption("config reference"));
        globalRef.setDescription(helper.formatDescription("Specify which configuration to use for this invocation"));

        Group group = new Group();
        group.setId(helper.getGlobalRefId(moduleName));
        group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupGlobalRef(globalRef));
        group.setCaption(helper.formatCaption(MuleStudioEditorXmlGenerator.GROUP_DEFAULT_CAPTION));

        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(helper.formatCaption(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION));
        attributeCategory.setDescription(helper.formatDescription(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_DESCRIPTION));
        attributeCategory.getGroup().add(group);

        PatternType cloudConnector = new PatternType();
        cloudConnector.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().add(attributeCategory);
        cloudConnector.setCaption(helper.formatCaption(helper.getGlobalRefId(moduleName)));
        cloudConnector.setLocalId(helper.getGlobalRefId(moduleName));
        cloudConnector.setDescription(helper.formatDescription("Interact with " + StringUtils.capitalize(moduleName)));
        cloudConnector.setAbstract(true);
        cloudConnector.setIcon(helper.getConnectorIcon(moduleName));
        cloudConnector.setImage(helper.getConnectorImage(moduleName));

        return objectFactory.createNamespaceTypeCloudConnector(cloudConnector);
    }
}