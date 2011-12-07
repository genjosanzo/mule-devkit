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
import org.mule.devkit.model.studio.GlobalType;
import org.mule.devkit.model.studio.Group;

import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;
import java.util.List;

public class GlobalTransformerTypeBuilder extends GlobalTypeBuilder {

    public GlobalTransformerTypeBuilder(GeneratorContext context, ExecutableElement executableElement, DevKitTypeElement typeElement) {
        super(context, executableElement, typeElement);
    }

    @Override
    public GlobalType build() {
        GlobalType globalTransformer = super.build();
        globalTransformer.setAbstract(true);
        return globalTransformer;
    }

    protected List<AttributeCategory> getAttributeCategories() {
        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(helper.formatCaption(MuleStudioXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION));
        attributeCategory.setDescription(helper.formatDescription(MuleStudioXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_DESCRIPTION));

        Group group = new Group();
        group.setCaption(helper.formatCaption(MuleStudioXmlGenerator.GROUP_DEFAULT_CAPTION));
        group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupName(createNameAttributeType()));

        attributeCategory.getGroup().add(group);

        List<AttributeCategory> attributeCategories = new ArrayList<AttributeCategory>();
        attributeCategories.add(attributeCategory);
        return attributeCategories;
    }

    protected String getDescriptionBasedOnType() {
        return helper.formatDescription(javaDocUtils.getSummary(executableElement));
    }

    protected String getExtendsBasedOnType() {
        return MuleStudioXmlGenerator.URI_PREFIX + typeElement.name() + '/' + getLocalIdBasedOnType();
    }

    protected String getLocalIdBasedOnType() {
        return nameUtils.uncamel(executableElement.getSimpleName().toString());
    }

    protected String getCaptionBasedOnType() {
        return helper.formatCaption(nameUtils.friendlyNameFromCamelCase(executableElement.getSimpleName().toString()));
    }

    protected String getNameDescriptionBasedOnType() {
        return "Identifies the transformer so that other elements can reference it. Required if the transformer is defined at the global level.";
    }
}