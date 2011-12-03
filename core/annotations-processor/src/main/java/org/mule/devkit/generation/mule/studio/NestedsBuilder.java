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
import org.mule.devkit.generation.spring.SchemaGenerator;
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

    private ObjectFactory objectFactory;
    private MuleStudioUtils helper;
    private ExecutableElement executableElement;
    private String moduleName;
    private NameUtils nameUtils;
    private Types typeUtils;
    private TypeMirrorUtils typeMirrorUtils;
    private JavaDocUtils javaDocUtils;

    public NestedsBuilder(GeneratorContext context, ExecutableElement executableElement, String moduleName) {
        this.executableElement = executableElement;
        this.moduleName = moduleName;
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
            String localId = nameUtils.uncamel(executableElement.getSimpleName().toString()) + '-' + nameUtils.uncamel(parameter.getSimpleName().toString());
            if (needToCreateNestedElement(parameter)) {

                NestedElementReference childElement = createChildElement(parameter, localId);
                NestedElementType firstLevelNestedElement = createFirstLevelNestedElement(parameter, localId);
                firstLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(childElement));

                NestedElementType secondLevelNestedElement = null;
                NestedElementType thirdLevelNestedElement = null;
                if (isSimpleList(parameter)) {
                    secondLevelNestedElement = createSecondLevelNestedElement(parameter, childElement);
                    handleSimpleList(parameter, localId, secondLevelNestedElement);
                } else if (isSimpleMap(parameter) || isListOfMaps(parameter)) {
                    secondLevelNestedElement = createSecondLevelNestedElement(parameter, childElement);
                    handleSimpleMap(parameter, secondLevelNestedElement);
                    if (isListOfMaps(parameter)) {
                        childElement.setName(nameUtils.singularize(childElement.getName()));
                        thirdLevelNestedElement = new NestedElementType();
                        thirdLevelNestedElement.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                        thirdLevelNestedElement.setLocalId(nameUtils.singularize(localId));
                        thirdLevelNestedElement.setXmlname(nameUtils.uncamel(nameUtils.singularize(parameter.getSimpleName().toString())));
                        thirdLevelNestedElement.setDescription(helper.formatDescription(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                        thirdLevelNestedElement.setIcon(helper.getIcon(moduleName));
                        thirdLevelNestedElement.setImage(helper.getImage(moduleName));
                        NestedElementReference childElement1 = createChildElement(parameter, SchemaGenerator.INNER_PREFIX + nameUtils.singularize(localId));
                        childElement1.setCaption(nameUtils.singularize(childElement1.getCaption()));
                        childElement1.setDescription(nameUtils.singularize(childElement1.getDescription()));
                        thirdLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(childElement1));
                    }
                }

                nesteds.add(objectFactory.createNested(firstLevelNestedElement));
                nesteds.add(objectFactory.createNested(secondLevelNestedElement));
                if (thirdLevelNestedElement != null) {
                    nesteds.add(objectFactory.createNested(thirdLevelNestedElement));
                }
            }
        }
        return nesteds;
    }

    private void handleSimpleMap(VariableElement parameter, NestedElementType secondLevelNestedElement) {
        AttributeType attributeTypeForMapKey;
        if (((DeclaredType) parameter.asType()).getTypeArguments().isEmpty()) {
            attributeTypeForMapKey = new StringAttributeType();
        } else {
            TypeMirror typeMirror = ((DeclaredType) parameter.asType()).getTypeArguments().get(0);
            attributeTypeForMapKey = helper.createAttributeTypeIgnoreEnumsAndCollections(typeUtils.asElement(typeMirror));
            if (attributeTypeForMapKey == null) { // nested
                attributeTypeForMapKey = new StringAttributeType();
            }
        }

        attributeTypeForMapKey.setName(SchemaGenerator.ATTRIBUTE_NAME_KEY);
        attributeTypeForMapKey.setDescription(helper.formatDescription("Key."));
        attributeTypeForMapKey.setCaption(helper.formatCaption("Key"));
        secondLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(attributeTypeForMapKey));

        TextType attributeTypeForMapValues = new TextType();
        attributeTypeForMapValues.setName("value");
        attributeTypeForMapValues.setDescription(helper.formatDescription("Value."));
        attributeTypeForMapValues.setCaption(helper.formatCaption("Value"));
        attributeTypeForMapValues.setIsToElement(true);
        secondLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(attributeTypeForMapValues));
    }

    private void handleSimpleList(VariableElement parameter, String localId, NestedElementType secondLevelNestedElement) {
        AttributeType attributeTypeForListValues;
        if (((DeclaredType) parameter.asType()).getTypeArguments().isEmpty() || typeMirrorUtils.isString(typeUtils.asElement(((DeclaredType) parameter.asType()).getTypeArguments().get(0)))) {
            TextType textType = new TextType();
            textType.setIsToElement(true);
            attributeTypeForListValues = textType;
        } else {
            TypeMirror typeParameter = ((DeclaredType) parameter.asType()).getTypeArguments().get(0);
            attributeTypeForListValues = helper.createAttributeTypeIgnoreEnumsAndCollections(typeUtils.asElement(typeParameter));
        }
        attributeTypeForListValues.setName(nameUtils.singularize(localId));
        attributeTypeForListValues.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        attributeTypeForListValues.setDescription(helper.formatDescription(javaDocUtils.getParameterSummary(parameter.getSimpleName().toString(), executableElement)));
        secondLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(attributeTypeForListValues));
    }

    private NestedElementType createSecondLevelNestedElement(VariableElement parameter, NestedElementReference childElement) {
        NestedElementType nestedElement = new NestedElementType();
        nestedElement.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        String localIdSuffix = childElement.getName().substring(childElement.getName().lastIndexOf('/') + 1);
        if (isListOfMaps(parameter)) {
            nestedElement.setLocalId(SchemaGenerator.INNER_PREFIX + nameUtils.singularize(localIdSuffix));
            nestedElement.setXmlname(SchemaGenerator.INNER_PREFIX + nameUtils.uncamel(nameUtils.singularize(parameter.getSimpleName().toString())));
        } else {
            nestedElement.setLocalId(localIdSuffix);
            nestedElement.setXmlname(nameUtils.uncamel(nameUtils.singularize(parameter.getSimpleName().toString())));
        }
        nestedElement.setDescription(helper.formatDescription(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        nestedElement.setIcon(helper.getIcon(moduleName));
        nestedElement.setImage(helper.getImage(moduleName));
        return nestedElement;
    }

    private NestedElementType createFirstLevelNestedElement(VariableElement parameter, String localId) {
        NestedElementType nestedElement = new NestedElementType();
        nestedElement.setLocalId(localId);
        nestedElement.setXmlname(nameUtils.uncamel(parameter.getSimpleName().toString()));
        nestedElement.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        nestedElement.setDescription(helper.formatDescription(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        nestedElement.setIcon(helper.getIcon(moduleName));
        nestedElement.setImage(helper.getImage(moduleName));
        return nestedElement;
    }

    private boolean isListOfMaps(VariableElement parameter) {
        List<? extends TypeMirror> typeArguments = ((DeclaredType) parameter.asType()).getTypeArguments();
        return typeMirrorUtils.isArrayOrList(parameter.asType()) && !typeArguments.isEmpty() && typeMirrorUtils.isMap(typeArguments.get(0));
    }

    private boolean isSimpleMap(VariableElement parameter) {
        List<? extends TypeMirror> typeArguments = ((DeclaredType) parameter.asType()).getTypeArguments();
        return typeMirrorUtils.isMap(parameter.asType()) && (typeArguments.isEmpty() || !typeMirrorUtils.isCollection(typeArguments.get(1)));
    }

    private boolean isSimpleList(VariableElement parameter) {
        List<? extends TypeMirror> typeArguments = ((DeclaredType) parameter.asType()).getTypeArguments();
        return typeMirrorUtils.isArrayOrList(parameter.asType()) && (typeArguments.isEmpty() || !typeMirrorUtils.isCollection(typeArguments.get(0)));
    }

    private NestedElementReference createChildElement(VariableElement parameter, String localId) {
        NestedElementReference childElement = new NestedElementReference();
        String parameterFriendlyName = nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString());
        if (isListOfMaps(parameter)) {
            childElement.setName(MuleStudioXmlGenerator.URI_PREFIX + moduleName + '/' + localId);
            childElement.setDescription(helper.formatDescription(nameUtils.singularize(parameterFriendlyName)));
            childElement.setCaption(helper.formatCaption(nameUtils.singularize(parameterFriendlyName)));
        } else {
            childElement.setName(MuleStudioXmlGenerator.URI_PREFIX + moduleName + '/' + nameUtils.singularize(localId));
            childElement.setDescription(helper.formatDescription(parameterFriendlyName));
            childElement.setCaption(helper.formatCaption(parameterFriendlyName));
        }
        childElement.setAllowMultiple(true);
        return childElement;
    }

    private boolean needToCreateNestedElement(VariableElement parameter) {
        return (typeMirrorUtils.isMap(parameter.asType()) ||
                typeMirrorUtils.isArrayOrList(parameter.asType())) && !typeMirrorUtils.ignoreParameter(parameter);
    }
}