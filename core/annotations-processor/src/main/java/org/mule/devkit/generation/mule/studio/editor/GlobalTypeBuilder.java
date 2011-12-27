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

import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.GlobalType;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

public abstract class GlobalTypeBuilder extends BaseStudioXmlBuilder {

    public GlobalTypeBuilder(GeneratorContext context, ExecutableElement executableElement, DevKitTypeElement typeElement) {
        super(context, executableElement, typeElement);
    }

    public GlobalTypeBuilder(GeneratorContext context, DevKitTypeElement typeElement) {
        super(context, typeElement);
    }

    protected abstract List<AttributeCategory> getAttributeCategories();

    protected abstract String getDescriptionBasedOnType();

    protected abstract String getExtendsBasedOnType();

    protected abstract String getLocalIdBasedOnType();

    protected abstract String getCaptionBasedOnType();

    protected abstract String getNameDescriptionBasedOnType();

    protected abstract String getImage();

    protected abstract String getIcon();

    public GlobalType build() {
        GlobalType globalType = new GlobalType();
        globalType.setImage(getImage());
        globalType.setIcon(getIcon());
        globalType.setCaption(getCaptionBasedOnType());
        globalType.setLocalId(getLocalIdBasedOnType());
        globalType.setExtends(getExtendsBasedOnType());
        globalType.setDescription(getDescriptionBasedOnType());

        globalType.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().addAll(getAttributeCategories());

        return globalType;
    }

    protected AttributeType createNameAttributeType() {
        AttributeType name = new AttributeType();
        name.setName("name");
        name.setCaption(helper.formatCaption("Name"));
        name.setDescription(helper.formatDescription(getNameDescriptionBasedOnType()));
        name.setRequired(true);
        return name;
    }
}