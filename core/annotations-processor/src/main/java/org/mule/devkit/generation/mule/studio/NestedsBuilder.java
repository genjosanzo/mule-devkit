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

import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.spring.SchemaGenerator;
import org.mule.devkit.model.studio.AbstractElementType;
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.NestedElementReference;
import org.mule.devkit.model.studio.NestedElementType;
import org.mule.devkit.model.studio.StringAttributeType;
import org.mule.devkit.model.studio.TextType;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

public class NestedsBuilder extends BaseStudioXmlBuilder {

    public NestedsBuilder(GeneratorContext context, ExecutableElement executableElement, DevKitTypeElement typeElement) {
        super(context, executableElement, typeElement);
    }

    public NestedsBuilder(GeneratorContext context, DevKitTypeElement typeElement) {
        super(context, typeElement);
    }

    public List<JAXBElement<? extends AbstractElementType>> build() {
        List<JAXBElement<? extends AbstractElementType>> nesteds = new ArrayList<JAXBElement<? extends AbstractElementType>>();
        for (VariableElement variableElement : getVariableElements()) {
            if (needToCreateNestedElement(variableElement)) {

                String localId = helper.getLocalId(executableElement, variableElement);
                NestedElementReference childElement = createChildElement(variableElement, localId);
                NestedElementType firstLevelNestedElement = createFirstLevelNestedElement(variableElement, localId);
                firstLevelNestedElement.getRegexpOrEncodingOrString().add(helper.createJAXBElement(childElement));

                NestedElementType secondLevelNestedElement = null;
                NestedElementType thirdLevelNestedElement = null;
                if (isSimpleList(variableElement)) {
                    secondLevelNestedElement = createSecondLevelNestedElement(variableElement, childElement);
                    handleSimpleList(variableElement, localId, secondLevelNestedElement);
                } else if (isSimpleMap(variableElement) || isListOfMaps(variableElement)) {
                    secondLevelNestedElement = createSecondLevelNestedElement(variableElement, childElement);
                    handleSimpleMap(variableElement, secondLevelNestedElement);
                    if (isListOfMaps(variableElement)) {
                        childElement.setName(nameUtils.singularize(childElement.getName()));
                        thirdLevelNestedElement = new NestedElementType();
                        thirdLevelNestedElement.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(variableElement.getSimpleName().toString())));
                        thirdLevelNestedElement.setLocalId(nameUtils.singularize(localId));
                        thirdLevelNestedElement.setXmlname(nameUtils.uncamel(nameUtils.singularize(variableElement.getSimpleName().toString())));
                        thirdLevelNestedElement.setDescription(helper.formatDescription(nameUtils.friendlyNameFromCamelCase(variableElement.getSimpleName().toString())));
                        thirdLevelNestedElement.setIcon(getIcon());
                        thirdLevelNestedElement.setImage(getImage());
                        NestedElementReference childElement1 = createChildElement(variableElement, SchemaGenerator.INNER_PREFIX + nameUtils.singularize(localId));
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

    private String getImage() {
        if(executableElement != null) {
            if(executableElement.getAnnotation(Processor.class) != null) {
                return helper.getConnectorImage(moduleName);
            }
            if(executableElement.getAnnotation(Source.class) != null) {
                return helper.getEndpointImage(moduleName);
            }
            if(executableElement.getAnnotation(Transformer.class) != null) {
                return helper.getTransformerImage(moduleName);
            }
        }
        return helper.getConnectorImage(moduleName);
    }

    private String getIcon() {
        if(executableElement != null) {
            if(executableElement.getAnnotation(Processor.class) != null) {
                return helper.getConnectorIcon(moduleName);
            }
            if(executableElement.getAnnotation(Source.class) != null) {
                return helper.getEndpointIcon(moduleName);
            }
            if(executableElement.getAnnotation(Transformer.class) != null) {
                return helper.getTransformerIcon(moduleName);
            }
        }
        return helper.getConnectorIcon(moduleName);
    }

    private List<? extends VariableElement> getVariableElements() {
        if (executableElement != null) {
            return executableElement.getParameters();
        } else {
            return typeElement.getFieldsAnnotatedWith(Configurable.class);
        }
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
        // TODO: temporarily commented out and added follwing 3 lines until Studio supports the isToElement attribute for other attributes types different than text
//        if (((DeclaredType) parameter.asType()).getTypeArguments().isEmpty() || typeMirrorUtils.isString(typeUtils.asElement(((DeclaredType) parameter.asType()).getTypeArguments().get(0)))) {
//            TextType textType = new TextType();
//            textType.setIsToElement(true);
//            attributeTypeForListValues = textType;
//        } else {
//            TypeMirror typeParameter = ((DeclaredType) parameter.asType()).getTypeArguments().get(0);
//            attributeTypeForListValues = helper.createAttributeTypeIgnoreEnumsAndCollections(typeUtils.asElement(typeParameter));
//        }
        TextType textType = new TextType();
        textType.setIsToElement(true);
        attributeTypeForListValues = textType;

        attributeTypeForListValues.setName(nameUtils.singularize(localId));
        attributeTypeForListValues.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        if (executableElement != null) {
            attributeTypeForListValues.setDescription(helper.formatDescription(javaDocUtils.getParameterSummary(parameter.getSimpleName().toString(), executableElement)));
        } else {
            attributeTypeForListValues.setDescription(helper.formatDescription(javaDocUtils.getSummary(parameter)));
        }
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
        nestedElement.setIcon(getIcon());
        nestedElement.setImage(getImage());
        return nestedElement;
    }

    private NestedElementType createFirstLevelNestedElement(VariableElement parameter, String localId) {
        NestedElementType nestedElement = new NestedElementType();
        nestedElement.setLocalId(localId);
        nestedElement.setXmlname(nameUtils.uncamel(parameter.getSimpleName().toString()));
        nestedElement.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        nestedElement.setDescription(helper.formatDescription(nameUtils.friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
        nestedElement.setIcon(getIcon());
        nestedElement.setImage(getImage());
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
            String singularizedLocalId = nameUtils.singularize(localId);
            if (localId.equals(singularizedLocalId)) {
                singularizedLocalId += "-each";
            }
            childElement.setName(MuleStudioXmlGenerator.URI_PREFIX + moduleName + '/' + singularizedLocalId);
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