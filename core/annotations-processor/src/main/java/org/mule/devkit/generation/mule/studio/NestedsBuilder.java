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
import org.mule.devkit.utils.JavaDocUtils;
import org.mule.devkit.utils.NameUtils;
import org.mule.devkit.utils.TypeMirrorUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

public class NestedsBuilder {

    private static final String URI_PREFIX = "http://www.mulesoft.org/schema/mule/";
    private ObjectFactory objectFactory;
    private MuleStudioUtils helper;
    private ExecutableElement executableElement;
    private String moduleName;
    private List<String> parsedLocalIds;
    private NameUtils nameUtils;
    private Types typeUtils;
    private TypeMirrorUtils typeMirrorUtils;
    private JavaDocUtils javaDocUtils;

    public NestedsBuilder(GeneratorContext context, ExecutableElement executableElement, String moduleName, List<String> parsedLocalIds) {
        this.executableElement = executableElement;
        this.moduleName = moduleName;
        this.parsedLocalIds = parsedLocalIds;
        helper = new MuleStudioUtils(context);
        this.nameUtils = context.getNameUtils();
        this.typeUtils = context.getTypeUtils();
        this.typeMirrorUtils = context.getTypeMirrorUtils();
        javaDocUtils = context.getJavaDocUtils();
        objectFactory = new ObjectFactory();
    }

    public List<JAXBElement<? extends AbstractElementType>> build() {
        List<JAXBElement<? extends AbstractElementType>> nesteds = new ArrayList<JAXBElement<? extends AbstractElementType>>();
        for (VariableElement parameter : executableElement.getParameters()) {
            String localId = nameUtils.uncamel(parameter.getSimpleName().toString());
            if (needToCreateNestedElement(parameter) && !parsedLocalIds.contains(localId)) {

                parsedLocalIds.add(localId);

                NestedElementReference childElement = createChildElement(parameter, localId);
                NestedElementType firstLevelNestedElement = createFirstLevelNestedElement(parameter, localId);
                firstLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(childElement));
                NestedElementType secondLevelNestedElement = createSecondLevelNestedElement(parameter, childElement);

                AttributeType key;
                if (((DeclaredType) parameter.asType()).getTypeArguments().isEmpty()) {
                    key = new StringAttributeType();
                } else {
                    TypeMirror typeMirror = ((DeclaredType) parameter.asType()).getTypeArguments().get(0);
                    key = helper.createAttributeTypeIgnoreEnumsAndCollections(typeUtils.asElement(typeMirror));
                }

                if (typeMirrorUtils.isArrayOrList(parameter.asType())) {
                    key.setName(nameUtils.singularize(localId));
                    key.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                    key.setDescription(helper.formatDescription(javaDocUtils.getParameterSummary(parameter.getSimpleName().toString(), executableElement)));
                    secondLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(key));
                } else { //map
                    key.setName("key");
                    key.setDescription(helper.formatDescription("Key."));
                    key.setCaption(helper.formatCaption("Key"));
                    secondLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(key));

                    TextType textAttributeType = new TextType();
                    textAttributeType.setName("value");
                    textAttributeType.setDescription(helper.formatDescription("Value."));
                    textAttributeType.setCaption(helper.formatCaption("Value"));
                    textAttributeType.setIsToElement(true);
                    secondLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(textAttributeType));
                }

                nesteds.add(objectFactory.createNested(firstLevelNestedElement));
                nesteds.add(objectFactory.createNested(secondLevelNestedElement));
            }
        }
        return nesteds;
    }

    private NestedElementType createSecondLevelNestedElement(VariableElement parameter, NestedElementReference childElement) {
        NestedElementType nested1 = new NestedElementType();
        nested1.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        nested1.setLocalId(childElement.getName().substring(childElement.getName().lastIndexOf("/") + 1));
        nested1.setDescription(helper.formatDescription(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        nested1.setIcon(helper.getIcon(moduleName));
        nested1.setImage(helper.getImage(moduleName));
        return nested1;
    }

    private NestedElementType createFirstLevelNestedElement(VariableElement parameter, String localId) {
        NestedElementType nested = new NestedElementType();
        nested.setLocalId(localId);
        nested.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        nested.setDescription(helper.formatDescription(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        nested.setIcon(helper.getIcon(moduleName));
        nested.setImage(helper.getImage(moduleName));
        return nested;
    }

//    private boolean isListOfMaps(VariableElement parameter) {
//        List<? extends TypeMirror> typeArguments = ((DeclaredType) parameter.asType()).getTypeArguments();
//        return typeMirrorUtils.isArrayOrList(parameter.asType()) && !typeArguments.isEmpty() && typeMirrorUtils.isMap(typeArguments.get(0));
//    }
//
//    private boolean isSimpleMap(VariableElement parameter) {
//        List<? extends TypeMirror> typeArguments = ((DeclaredType) parameter.asType()).getTypeArguments();
//        return typeMirrorUtils.isMap(parameter.asType()) && (typeArguments.isEmpty() || !typeMirrorUtils.isCollection(typeArguments.get(1)));
//    }
//
//    private boolean isSimpleList(VariableElement parameter) {
//        List<? extends TypeMirror> typeArguments = ((DeclaredType) parameter.asType()).getTypeArguments();
//        return typeMirrorUtils.isArrayOrList(parameter.asType()) && (typeArguments.isEmpty() || !typeMirrorUtils.isCollection(typeArguments.get(0)));
//    }

    private NestedElementReference createChildElement(VariableElement parameter, String localId) {
        NestedElementReference childElement = new NestedElementReference();
        childElement.setName(URI_PREFIX + moduleName + '/' + nameUtils.singularize(localId));
        childElement.setDescription(helper.formatDescription(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        childElement.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        childElement.setAllowMultiple(true);
        return childElement;
    }

    private boolean needToCreateNestedElement(VariableElement parameter) {
        return typeMirrorUtils.isMap(parameter.asType()) ||
                typeMirrorUtils.isArrayOrList(parameter.asType());
    }
}