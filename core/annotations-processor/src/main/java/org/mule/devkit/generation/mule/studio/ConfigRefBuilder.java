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
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.Group;
import org.mule.devkit.model.studio.NewType;
import org.mule.devkit.model.studio.ObjectFactory;
import org.mule.devkit.model.studio.PatternType;
import org.mule.util.StringUtils;

import javax.xml.bind.JAXBElement;

public class ConfigRefBuilder {

    private static final String URI_PREFIX = "http://www.mulesoft.org/schema/mule/";
    private static final String GLOBAL_CLOUD_CONNECTOR_LOCAL_ID = "config";
    private ObjectFactory objectFactory;
    private GeneratorContext context;
    private MuleStudioUtils helper;

    public ConfigRefBuilder(GeneratorContext context) {
        this.context = context;
        helper = new MuleStudioUtils(context);
        objectFactory = new ObjectFactory();
    }

    public JAXBElement<PatternType> build(String moduleName) {
        NewType globalRef = new NewType();
        globalRef.setRequiredType(URI_PREFIX + moduleName + "/" + GLOBAL_CLOUD_CONNECTOR_LOCAL_ID);
        globalRef.setName("config-ref");
        globalRef.setCaption(helper.formatCaption("config reference"));
        globalRef.setDescription(helper.formatDescription("Specify which configuration to use for this invocation"));

        Group group = new Group();
        group.setId(helper.getGlobalRefId(moduleName));
        group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupGlobalRef(globalRef));
        group.setCaption(helper.formatCaption("Generic"));

        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(helper.formatCaption("General"));
        attributeCategory.setDescription(helper.formatDescription("General properties"));
        attributeCategory.getGroup().add(group);

        PatternType cloudConnector = new PatternType();
        cloudConnector.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().add(attributeCategory);
        cloudConnector.setCaption(helper.formatCaption(helper.getGlobalRefId(moduleName)));
        cloudConnector.setLocalId(helper.getGlobalRefId(moduleName));
        cloudConnector.setDescription(helper.formatDescription("Interact with " + StringUtils.capitalize(moduleName)));
        cloudConnector.setAbstract(true);
        cloudConnector.setIcon(helper.getIcon(moduleName));
        cloudConnector.setImage(helper.getImage(moduleName));

        return objectFactory.createNamespaceTypeCloudConnector(cloudConnector);
    }
}