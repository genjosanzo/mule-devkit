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

import org.mule.api.annotations.Transformer;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.GlobalType;
import org.mule.devkit.model.studio.Group;

import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;
import java.util.List;

public class GlobalTypeBuilder extends BaseStudioXmlBuilder {

    public GlobalTypeBuilder(GeneratorContext context, ExecutableElement executableElement, DevKitTypeElement typeElement) {
        super(context, executableElement, typeElement);
    }

    public GlobalTypeBuilder(GeneratorContext context, DevKitTypeElement typeElement) {
        super(context, typeElement);
    }

    public GlobalType build() {
        GlobalType globalType = new GlobalType();
        globalType.setImage(helper.getImage(typeElement.name()));
        globalType.setIcon(helper.getIcon(typeElement.name()));
        globalType.setCaption(getCaptionBasedOnType());
        globalType.setLocalId(getLocalIdBasedOnType());
        globalType.setExtends(getExtendsBasedOnType());
        globalType.setDescription(getDescriptionBasedOnType());

        globalType.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().addAll(getAttributeCategories());


        return globalType;
    }

    private List<AttributeCategory> getAttributeCategories() {
        AttributeType name = new AttributeType();
        name.setName("name");
        name.setCaption(helper.formatCaption("Name"));
        name.setDescription(helper.formatDescription(getNameDescriptionBasedOnType()));
        name.setRequired(true);
        if (executableElement != null) {
            AttributeCategory attributeCategory = new AttributeCategory();
            attributeCategory.setCaption(helper.formatCaption(MuleStudioXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION));
            attributeCategory.setDescription(helper.formatDescription(MuleStudioXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_DESCRIPTION));

            Group group = new Group();
            group.setCaption(helper.formatCaption(MuleStudioXmlGenerator.GROUP_DEFAULT_CAPTION));
            group.setId(getIdBasedOnType());

            attributeCategory.getGroup().add(group);


            group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupName(name));

            List<AttributeCategory> attributeCategories = new ArrayList<AttributeCategory>();
            attributeCategories.add(attributeCategory);
            return attributeCategories;
        } else {
            Group group = new Group();
            group.setId(moduleName + "GenericProperties");
            group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupName(name));
            group.setCaption(helper.formatCaption(MuleStudioXmlGenerator.GROUP_DEFAULT_CAPTION));
            return processConfigurableFields(group);
        }
    }

    private String getDescriptionBasedOnType() {
        if (executableElement != null) {
            return helper.formatDescription(javaDocUtils.getSummary(executableElement));
        } else {
            return helper.formatDescription("Global " + nameUtils.friendlyNameFromCamelCase(typeElement.name()) + " configuration information");
        }
    }

    private String getExtendsBasedOnType() {
        if (executableElement != null) {
            return MuleStudioXmlGenerator.URI_PREFIX + typeElement.name() + '/' + getLocalIdBasedOnType();
        } else {
            return MuleStudioXmlGenerator.URI_PREFIX + typeElement.name() + '/' + helper.getGlobalRefId(typeElement.name());
        }
    }

    private String getLocalIdBasedOnType() {
        if (executableElement != null) {
            return nameUtils.uncamel(executableElement.getSimpleName().toString());
        } else {
            return MuleStudioXmlGenerator.GLOBAL_CLOUD_CONNECTOR_LOCAL_ID;
        }
    }

    private String getCaptionBasedOnType() {
        if (executableElement != null) {
            return helper.formatCaption(nameUtils.friendlyNameFromCamelCase(executableElement.getSimpleName().toString()));
        } else {
            return helper.formatCaption(nameUtils.friendlyNameFromCamelCase(typeElement.name()));
        }
    }

    private String getNameDescriptionBasedOnType() {
        if(executableElement != null) {
            if (isTransformer()) {
                return "Identifies the transformer so that other elements can reference it. Required if the transformer is defined at the global level.";
            } else {
                return "Endpoint name";
            }
        } else {
            return helper.formatDescription("Give a name to this configuration so it can be later referenced by config-ref.");
        }
    }

    private String getIdBasedOnType() {
        if (isTransformer()) {
            return "abstractTransformerGeneric";
        } else {
            return "abstractEndpointGeneric";
        }
    }

    private boolean isTransformer() {
        return executableElement.getAnnotation(Transformer.class) != null;
    }
}