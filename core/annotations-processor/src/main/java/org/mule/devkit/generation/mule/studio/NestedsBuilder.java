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
import org.mule.devkit.model.studio.AbstractElementType;
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.NestedElementReference;
import org.mule.devkit.model.studio.NestedElementType;
import org.mule.devkit.model.studio.ObjectFactory;
import org.mule.devkit.model.studio.StringAttributeType;
import org.mule.devkit.model.studio.TextType;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

public class NestedsBuilder {

    private static final String URI_PREFIX = "http://www.mulesoft.org/schema/mule/";
    private ObjectFactory objectFactory;
    private GeneratorContext context;
    private MuleStudioUtils helper;
    private ExecutableElement executableElement;
    private String moduleName;
    private List<String> parsedLocalIds;

    public NestedsBuilder(GeneratorContext context, ExecutableElement executableElement, String moduleName, List<String> parsedLocalIds) {
        this.context = context;
        this.executableElement = executableElement;
        this.moduleName = moduleName;
        this.parsedLocalIds = parsedLocalIds;
        objectFactory = new ObjectFactory();
        helper = new MuleStudioUtils(context);
    }

    public List<JAXBElement<? extends AbstractElementType>> build() {
        List<JAXBElement<? extends AbstractElementType>> nesteds = new ArrayList<JAXBElement<? extends AbstractElementType>>();
        for (VariableElement parameter : executableElement.getParameters()) {
            String localId = context.getNameUtils().uncamel(parameter.getSimpleName().toString());
            if (isNested(parameter) && !parsedLocalIds.contains(localId)) {

                parsedLocalIds.add(localId);

                NestedElementType nested = new NestedElementType();
                nested.setLocalId(localId);
                nested.setCaption(helper.formatCaption(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                nested.setDescription(helper.formatDescription(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                nested.setIcon(helper.getIcon(moduleName));
                nested.setImage(helper.getImage(moduleName));

                NestedElementReference childElement = new NestedElementReference();
                childElement.setName(URI_PREFIX + moduleName + '/' + context.getNameUtils().singularize(localId));
                childElement.setDescription(helper.formatDescription(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                childElement.setCaption(helper.formatCaption(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                childElement.setAllowMultiple(true);
                nested.getRegexpOrEncodingOrString().add(helper.createJAXBElement(childElement));

                NestedElementType nested1 = new NestedElementType();
                nested1.setCaption(helper.formatCaption(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                nested1.setLocalId(childElement.getName().substring(childElement.getName().lastIndexOf("/") + 1));
                nested1.setDescription(helper.formatDescription(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                nested1.setIcon(helper.getIcon(moduleName));
                nested1.setImage(helper.getImage(moduleName));

                AttributeType key;
                if (((DeclaredType) parameter.asType()).getTypeArguments().isEmpty()) {
                    key = new StringAttributeType();
                } else {
                    TypeMirror typeMirror = ((DeclaredType) parameter.asType()).getTypeArguments().get(0);
                    key = helper.createAttributeType(context.getTypeUtils().asElement(typeMirror));
                }

                if (context.getTypeMirrorUtils().isMap(parameter.asType())) {
                    key.setName("key");
                    key.setDescription(helper.formatDescription("Key."));
                    key.setCaption(helper.formatCaption("Key"));
                } else {
                    key.setName(context.getNameUtils().singularize(localId));
                    key.setCaption(helper.formatCaption(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                    key.setDescription(helper.formatDescription(context.getJavaDocUtils().getParameterSummary(parameter.getSimpleName().toString(), executableElement)));
                }
                nested1.getRegexpOrEncodingOrString().add(helper.createJAXBElement(key));

                if (context.getTypeMirrorUtils().isMap(parameter.asType())) {
                    TextType textAttributeType = new TextType();
                    textAttributeType.setName("value");
                    textAttributeType.setDescription(helper.formatDescription("Value."));
                    textAttributeType.setCaption(helper.formatCaption("Value"));
                    textAttributeType.setIsToElement(true);
                    nested1.getRegexpOrEncodingOrString().add(helper.createJAXBElement(textAttributeType));
                }

                nesteds.add(objectFactory.createNested(nested));
                nesteds.add(objectFactory.createNested(nested1));
            }
        }
        return nesteds;
    }

    private boolean isNested(VariableElement parameter) {
        return context.getTypeMirrorUtils().isMap(parameter.asType()) ||
                context.getTypeMirrorUtils().isArrayOrList(parameter.asType());
    }
}