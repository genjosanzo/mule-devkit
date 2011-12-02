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
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.EndpointType;
import org.mule.devkit.model.studio.EnumElement;
import org.mule.devkit.model.studio.EnumType;
import org.mule.devkit.model.studio.Group;
import org.mule.devkit.model.studio.NestedElementReference;
import org.mule.devkit.model.studio.ObjectFactory;
import org.mule.devkit.utils.JavaDocUtils;
import org.mule.devkit.utils.NameUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EndpointTypeBuilder {

    private static final String URI_PREFIX = "http://www.mulesoft.org/schema/mule/";
    private ObjectFactory objectFactory;
    private GeneratorContext context;
    private MuleStudioUtils helper;
    private DevKitTypeElement typeElement;
    private ExecutableElement executableElement;
    private NameUtils nameUtils;
    private JavaDocUtils javaDocUtils;

    public EndpointTypeBuilder(GeneratorContext context, ExecutableElement executableElement, DevKitTypeElement typeElement) {
        this.context = context;
        this.executableElement = executableElement;
        this.typeElement = typeElement;
        nameUtils = context.getNameUtils();
        javaDocUtils = context.getJavaDocUtils();
        helper = new MuleStudioUtils(context);
        objectFactory = new ObjectFactory();
    }

    public EndpointType build() {
        EndpointType endpoint = new EndpointType();
        endpoint.setLocalId(nameUtils.uncamel(executableElement.getSimpleName().toString()));
        endpoint.setCaption(nameUtils.friendlyNameFromCamelCase(executableElement.getSimpleName().toString()));
        endpoint.setIcon(helper.getIcon(typeElement.name()));
        endpoint.setImage(helper.getIcon(typeElement.name()));
        endpoint.setDescription(helper.formatDescription(javaDocUtils.getSummary(executableElement)));
        endpoint.setSupportsInbound(true);
        endpoint.setSupportsOutbound(false);

        Group group = new Group();
        group.setCaption(helper.formatCaption("General"));
        group.setId("general");

        addMethodParametersToGroup(group);

        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(helper.formatCaption("General"));
        attributeCategory.setDescription(helper.formatDescription("General properties"));
        attributeCategory.getGroup().add(group);

        endpoint.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().add(attributeCategory);

        return endpoint;
    }

    private void addMethodParametersToGroup(Group group) {
        List<? extends VariableElement> parameters = getParametersSorted();
        List<AttributeType> simpleTypeAttributeTypes = getSimpleTypeAttributeTypes(executableElement, parameters);
        for (AttributeType attributeType : simpleTypeAttributeTypes) {
            group.getRegexpOrEncodingOrModeSwitch().add(helper.createJAXBElement(attributeType));
        }

        List<EnumType> enumAttributeTypes = getEnumAttributeTypes(executableElement, parameters);
        for (EnumType enumType : enumAttributeTypes) {
            group.getRegexpOrEncodingOrModeSwitch().add(helper.createJAXBElement(enumType));
        }

        List<NestedElementReference> childElementAttributeTypes = getChildElementsAttributeTypes(executableElement, parameters);
        for (NestedElementReference childElement : childElementAttributeTypes) {
            group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupChildElement(childElement));
        }
    }

    private List<? extends VariableElement> getParametersSorted() {
        List<? extends VariableElement> parameters = new ArrayList<VariableElement>(executableElement.getParameters());
        Collections.sort(parameters, new VariableComparator(context.getTypeMirrorUtils()));
        return parameters;
    }

    private List<AttributeType> getSimpleTypeAttributeTypes(ExecutableElement executableElement, List<? extends VariableElement> parameters) {
        List<AttributeType> attributeTypes = new ArrayList<AttributeType>();
        for (VariableElement parameter : parameters) {
            AttributeType attributeType = helper.createAttributeTypeIgnoreEnumsAndCollections(parameter);
            if (attributeType != null) {
                helper.setAttributeTypeInfo(executableElement, parameter, attributeType);
                attributeTypes.add(attributeType);
            }
        }
        return attributeTypes;
    }

    private List<NestedElementReference> getChildElementsAttributeTypes(ExecutableElement executableElement, List<? extends VariableElement> parameters) {
        List<NestedElementReference> nestedElementReferences = new ArrayList<NestedElementReference>();
        for (VariableElement parameter : parameters) {
            if (context.getTypeMirrorUtils().isCollection(parameter.asType()) && !context.getTypeMirrorUtils().ignoreParameter(parameter)) {
                NestedElementReference childElement = new NestedElementReference();
                childElement.setName(URI_PREFIX + typeElement.name() + "/" + context.getNameUtils().uncamel(executableElement.getSimpleName().toString()) + '-' + context.getNameUtils().uncamel(parameter.getSimpleName().toString()));
                childElement.setAllowMultiple(false);
                childElement.setDescription(helper.formatDescription(context.getJavaDocUtils().getParameterSummary(parameter.getSimpleName().toString(), executableElement)));
                childElement.setCaption(helper.formatCaption(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                childElement.setInplace(true);
                nestedElementReferences.add(childElement);
            }
        }
        return nestedElementReferences;
    }

    private List<EnumType> getEnumAttributeTypes(ExecutableElement executableElement, List<? extends VariableElement> parameters) {
        List<EnumType> enumTypes = new ArrayList<EnumType>();
        for (VariableElement parameter : parameters) {
            if (context.getTypeMirrorUtils().isEnum(parameter.asType()) && !context.getTypeMirrorUtils().ignoreParameter(parameter)) {
                EnumType enumType = new EnumType();
                enumType.setSupportsExpressions(true);
                enumType.setAllowsCustom(true);
                helper.setAttributeTypeInfo(executableElement, parameter, enumType);
                for (Element enumMember : context.getTypeUtils().asElement(parameter.asType()).getEnclosedElements()) {
                    if (enumMember.getKind() == ElementKind.ENUM_CONSTANT) {
                        String enumConstant = enumMember.getSimpleName().toString();
                        EnumElement enumElement = new EnumElement();
                        enumElement.setCaption(helper.formatCaption(context.getJavaDocUtils().getSummary(enumMember)));
                        enumElement.setValue(enumConstant);
                        enumType.getOption().add(enumElement);
                    }
                }
                enumTypes.add(enumType);
            }
        }
        return enumTypes;
    }
}