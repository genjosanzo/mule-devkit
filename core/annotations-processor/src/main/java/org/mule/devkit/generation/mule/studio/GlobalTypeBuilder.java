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

public class GlobalTypeBuilder extends BaseStudioXmlBuilder {

    public GlobalTypeBuilder(GeneratorContext context, ExecutableElement executableElement, DevKitTypeElement typeElement) {
        super(context, executableElement, typeElement);
    }

    public GlobalType build() {
        GlobalType globalType = new GlobalType();
        globalType.setImage(helper.getImage(typeElement.name()));
        globalType.setIcon(helper.getIcon(typeElement.name()));
        globalType.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(executableElement.getSimpleName().toString())));
        globalType.setLocalId(nameUtils.uncamel(executableElement.getSimpleName().toString()));
        globalType.setExtends(MuleStudioXmlGenerator.URI_PREFIX + typeElement.name() + '/' + nameUtils.uncamel(executableElement.getSimpleName().toString()));
        globalType.setDescription(helper.formatDescription(javaDocUtils.getSummary(executableElement)));

        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(helper.formatCaption(MuleStudioXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION));
        attributeCategory.setDescription(helper.formatDescription(MuleStudioXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_DESCRIPTION));

        globalType.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().add(attributeCategory);

        Group group = new Group();
        group.setCaption(helper.formatCaption(MuleStudioXmlGenerator.GROUP_DEFAULT_CAPTION));
        group.setId(getIdBasedOnType());

        attributeCategory.getGroup().add(group);

        AttributeType name = new AttributeType();
        name.setName("name");
        name.setCaption(helper.formatCaption("Name"));
        name.setDescription(helper.formatDescription(getDescriptionBasedOnType()));
        name.setXsdType("substitutableClass");

        group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupName(name));

        return globalType;
    }

    private String getDescriptionBasedOnType() {
        if (isTransformer()) {
            return "Identifies the transformer so that other elements can reference it. Required if the transformer is defined at the global level.";
        } else {
            return "Endpoint name";
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