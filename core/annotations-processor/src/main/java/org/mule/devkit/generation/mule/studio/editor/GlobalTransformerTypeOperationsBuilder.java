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

import org.mule.api.annotations.Transformer;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.studio.AttributeCategory;

import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GlobalTransformerTypeOperationsBuilder extends GlobalTypeBuilder {

    public GlobalTransformerTypeOperationsBuilder(GeneratorContext context, DevKitTypeElement typeElement) {
        super(context, typeElement);
    }

    protected List<AttributeCategory> getAttributeCategories() {
        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(helper.formatCaption(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION));
        attributeCategory.setDescription(helper.formatDescription(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_DESCRIPTION));

        attributeCategory.getGroup().add(createGroupWithModeSwitch(getTransformerMethodsSorted()));

        List<AttributeCategory> attributeCategories = new ArrayList<AttributeCategory>();
        attributeCategories.add(attributeCategory);
        return attributeCategories;
    }

    private List<ExecutableElement> getTransformerMethodsSorted() {
        List<ExecutableElement> transformer = typeElement.getMethodsAnnotatedWith(Transformer.class);
        Collections.sort(transformer, new MethodComparator());
        return transformer;
    }

    protected String getDescriptionBasedOnType() {
        return helper.formatDescription("Global transformer");
    }

    protected String getExtendsBasedOnType() {
        return MuleStudioEditorXmlGenerator.URI_PREFIX + typeElement.name() + '/' + AbstractTransformerBuilder.ABSTRACT_TRANSFORMER_LOCAL_ID;
    }

    protected String getLocalIdBasedOnType() {
        return "global-transformer";
    }

    protected String getCaptionBasedOnType() {
        return helper.getFormattedCaption(typeElement);
    }

    protected String getNameDescriptionBasedOnType() {
        return "Identifies the transformer so that other elements can reference it.";
    }

    @Override
    protected String getImage() {
        return helper.getTransformerImage(typeElement);
    }

    @Override
    protected String getIcon() {
        return helper.getTransformerIcon(typeElement);
    }
}