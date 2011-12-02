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
import org.mule.devkit.model.studio.ObjectFactory;
import org.mule.devkit.utils.JavaDocUtils;
import org.mule.devkit.utils.NameUtils;

import javax.lang.model.element.ExecutableElement;

public class GlobalTypeBuilder {

    private static final String URI_PREFIX = "http://www.mulesoft.org/schema/mule/";
    private ObjectFactory objectFactory;
    private MuleStudioUtils helper;
    private DevKitTypeElement typeElement;
    private NameUtils nameUtils;
    private JavaDocUtils javaDocUtils;
    private ExecutableElement executableElement;

    public GlobalTypeBuilder(GeneratorContext context, ExecutableElement executableElement, DevKitTypeElement typeElement) {
        this.typeElement = typeElement;
        this.executableElement = executableElement;
        helper = new MuleStudioUtils(context);
        objectFactory = new ObjectFactory();
        nameUtils = context.getNameUtils();
        javaDocUtils = context.getJavaDocUtils();
    }

    public GlobalType build() {
        GlobalType globalType = new GlobalType();
        globalType.setImage(helper.getImage(typeElement.name()));
        globalType.setIcon(helper.getIcon(typeElement.name()));
        globalType.setCaption(nameUtils.uncamel(executableElement.getSimpleName().toString()));
        globalType.setLocalId(nameUtils.uncamel(executableElement.getSimpleName().toString()));
        globalType.setExtends(URI_PREFIX + typeElement.name() + '/' + nameUtils.uncamel(executableElement.getSimpleName().toString()));
        globalType.setDescription(helper.formatDescription(javaDocUtils.getSummary(executableElement)));

        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(helper.formatCaption("General"));
        attributeCategory.setDescription(helper.formatDescription("General properties"));

        globalType.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().add(attributeCategory);

        Group group = new Group();
        group.setCaption(helper.formatCaption("Generic"));
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
        if(isTransformer()) {
            return "Identifies the transformer so that other elements can reference it. Required if the transformer is defined at the global level.";
        } else {
            return "Endpoint name";
        }
    }

    private String getIdBasedOnType() {
        if(isTransformer()) {
            return "abstractTransformerGeneric";
        } else {
            return "abstractEndpointGeneric";
        }
    }

    private boolean isTransformer() {
        return executableElement.getAnnotation(Transformer.class) !=null;
    }
}