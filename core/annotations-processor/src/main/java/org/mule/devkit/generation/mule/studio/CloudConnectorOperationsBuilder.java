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
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.Group;
import org.mule.devkit.model.studio.ModeElementType;
import org.mule.devkit.model.studio.ModeType;
import org.mule.devkit.model.studio.PatternType;
import org.mule.util.StringUtils;

import javax.lang.model.element.ExecutableElement;
import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CloudConnectorOperationsBuilder extends BaseStudioXmlBuilder {

    private static final String ALIAS_ID_PREFIX = "org.mule.tooling.ui.modules.core.pattern.";
    private static final MethodComparator METHOD_COMPARATOR = new MethodComparator();

    public CloudConnectorOperationsBuilder(GeneratorContext context, DevKitTypeElement typeElement) {
        super(context, typeElement);
    }

    public JAXBElement<PatternType> build() {

        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(helper.formatCaption(MuleStudioXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION));
        attributeCategory.setDescription(helper.formatDescription(MuleStudioXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_DESCRIPTION));
        attributeCategory.getGroup().add(createGroupWithModeSwitch());

        PatternType cloudConnector = new PatternType();
        cloudConnector.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().add(attributeCategory);
        cloudConnector.setCaption(helper.getFormattedCaption(typeElement));
        cloudConnector.setLocalId(typeElement.name() + "-connector");
        cloudConnector.setExtends(MuleStudioXmlGenerator.URI_PREFIX + typeElement.name() + '/' + helper.getGlobalRefId(typeElement.name()));
        cloudConnector.setDescription(helper.getFormattedDescription(typeElement));
        cloudConnector.setAliasId(ALIAS_ID_PREFIX + typeElement.name());
        cloudConnector.setIcon(helper.getIcon(typeElement.name()));
        cloudConnector.setImage(helper.getImage(typeElement.name()));

        return objectFactory.createNamespaceTypeCloudConnector(cloudConnector);
    }

    private Group createGroupWithModeSwitch() {
        List<ModeElementType> modes = new ArrayList<ModeElementType>();
        for (ExecutableElement processorMethod : getProcessorMethodsSorted()) {
            ModeElementType mode = new ModeElementType();
            String methodName = processorMethod.getSimpleName().toString();
            mode.setModeId(MuleStudioXmlGenerator.URI_PREFIX + typeElement.name() + '/' + nameUtils.uncamel(methodName));
            mode.setModeLabel(nameUtils.friendlyNameFromCamelCase(methodName));
            modes.add(mode);
        }

        ModeType modeSwitch = new ModeType();
        modeSwitch.getMode().addAll(modes);
        modeSwitch.setCaption(helper.formatCaption("Operation"));
        modeSwitch.setName(StringUtils.capitalize(moduleName) + " operations to execute");
        modeSwitch.setDescription(helper.formatDescription("Operation"));

        Group group = new Group();
        group.setId(typeElement.name() + "ConnectorGeneric");
        group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupModeSwitch(modeSwitch));
        group.setCaption(helper.formatCaption(MuleStudioXmlGenerator.GROUP_DEFAULT_CAPTION));
        return group;
    }

    private List<ExecutableElement> getProcessorMethodsSorted() {
        List<ExecutableElement> processorMethods = typeElement.getMethodsAnnotatedWith(Processor.class);
        Collections.sort(processorMethods, METHOD_COMPARATOR);
        return processorMethods;
    }
}