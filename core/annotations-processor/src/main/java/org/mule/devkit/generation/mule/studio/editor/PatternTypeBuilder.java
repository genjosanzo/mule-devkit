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
import org.mule.api.annotations.Transformer;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.Group;
import org.mule.devkit.model.studio.PatternType;
import org.mule.util.StringUtils;

import javax.lang.model.element.ExecutableElement;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PatternTypeBuilder extends BaseStudioXmlBuilder {

    public PatternTypeBuilder(GeneratorContext context, ExecutableElement executableElement, DevKitTypeElement typeElement) {
        super(context, executableElement, typeElement);
    }

    public PatternType build() {
        PatternType patternType = createPatternType();
        if (executableElement.getAnnotation(Processor.class) != null) {
            Collection<AttributeCategory> attributeCategories = processMethodParameters();
            patternType.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().addAll(attributeCategories);
        }
        return patternType;
    }

    private PatternType createPatternType() {
        PatternType cloudConnector = new PatternType();
        cloudConnector.setLocalId(nameUtils.uncamel(executableElement.getSimpleName().toString()));
        cloudConnector.setCaption(helper.getFormattedCaption(executableElement));
        cloudConnector.setAbstract(true);

        if (executableElement.getAnnotation(Processor.class) != null) {
            cloudConnector.setExtends(MuleStudioEditorXmlGenerator.URI_PREFIX + typeElement.name() + '/' + helper.getGlobalRefId(typeElement.name()));
        } else if (executableElement.getAnnotation(Transformer.class) != null) {
            cloudConnector.setExtends(MuleStudioEditorXmlGenerator.URI_PREFIX + typeElement.name() + '/' + AbstractTransformerBuilder.ABSTRACT_TRANSFORMER_LOCAL_ID);
            cloudConnector.setDescription(helper.formatDescription(javaDocUtils.getSummary(executableElement)));
        }

        cloudConnector.setIcon(getIcon());
        cloudConnector.setImage(getImage());
        return cloudConnector;
    }

    private String getImage() {
        if(executableElement.getAnnotation(Transformer.class) != null) {
            return helper.getTransformerImage(typeElement.name());
        } else {
            return helper.getConnectorImage(typeElement.name());
        }
    }

    private String getIcon() {
        if(executableElement.getAnnotation(Transformer.class) != null) {
            return helper.getTransformerIcon(typeElement.name());
        } else {
            return helper.getConnectorIcon(typeElement.name());
        }
    }

    @Override
    protected void processConnectionAttributes(Map<String, Group> groupsByName, Map<String, AttributeCategory> attributeCategoriesByName) {
        if (typeElement.usesConnectionManager()) {
            Group connectionAttributesGroup = new Group();
            connectionAttributesGroup.setCaption(helper.formatCaption(CONNECTION_GROUP_NAME));
            connectionAttributesGroup.setId(StringUtils.uncapitalize(CONNECTION_GROUP_NAME));

            AttributeType label = new AttributeType();
            label.setCaption(String.format(CONNECTION_GROUP_LABEL, helper.getFormattedCaption(typeElement)));

            AttributeType newLine = new AttributeType();
            newLine.setCaption("");

            connectionAttributesGroup.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupLabel(label));
            connectionAttributesGroup.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupLabel(newLine));

            groupsByName.put(CONNECTION_GROUP_NAME, connectionAttributesGroup);

            List<AttributeType> connectionAttributes = getConnectionAttributes(typeElement);
            connectionAttributesGroup.getRegexpOrEncodingOrModeSwitch().addAll(helper.createJAXBElements(connectionAttributes));

            AttributeCategory connectionAttributeCategory = new AttributeCategory();
            connectionAttributeCategory.setCaption(helper.formatCaption(MuleStudioEditorXmlGenerator.CONNECTION_ATTRIBUTE_CATEGORY_CAPTION));
            connectionAttributeCategory.setDescription(helper.formatDescription(MuleStudioEditorXmlGenerator.CONNECTION_ATTRIBUTE_CATEGORY_CAPTION));
            attributeCategoriesByName.put(MuleStudioEditorXmlGenerator.CONNECTION_ATTRIBUTE_CATEGORY_CAPTION, connectionAttributeCategory);
            connectionAttributeCategory.getGroup().add(connectionAttributesGroup);
        }
    }
}