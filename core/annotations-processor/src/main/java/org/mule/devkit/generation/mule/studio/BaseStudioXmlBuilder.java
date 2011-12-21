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
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.display.VariableDisplay;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.EnumElement;
import org.mule.devkit.model.studio.EnumType;
import org.mule.devkit.model.studio.Group;
import org.mule.devkit.model.studio.ModeElementType;
import org.mule.devkit.model.studio.ModeType;
import org.mule.devkit.model.studio.NestedElementReference;
import org.mule.devkit.model.studio.ObjectFactory;
import org.mule.devkit.utils.JavaDocUtils;
import org.mule.devkit.utils.NameUtils;
import org.mule.devkit.utils.TypeMirrorUtils;
import org.mule.util.StringUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseStudioXmlBuilder {

    public static final String GENERAL_GROUP_NAME = "General";
    public static final String CONNECTION_GROUP_NAME = "Connection";
    public static final String CONNECTION_GROUP_LABEL = "Use these fields to override the credentials defined in the %s connector.";
    protected ObjectFactory objectFactory;
    protected MuleStudioUtils helper;
    protected DevKitTypeElement typeElement;
    protected ExecutableElement executableElement;
    protected NameUtils nameUtils;
    protected JavaDocUtils javaDocUtils;
    protected TypeMirrorUtils typeMirrorUtils;
    protected Types typeUtils;
    protected String moduleName;
    protected GeneratorContext context;


    protected BaseStudioXmlBuilder(GeneratorContext context) {
        this.context = context;
        nameUtils = context.getNameUtils();
        javaDocUtils = context.getJavaDocUtils();
        typeMirrorUtils = context.getTypeMirrorUtils();
        typeUtils = context.getTypeUtils();
        helper = new MuleStudioUtils(context);
        objectFactory = new ObjectFactory();
    }

    protected BaseStudioXmlBuilder(GeneratorContext context, DevKitTypeElement typeElement) {
        this(context);
        this.typeElement = typeElement;
        moduleName = typeElement.name();
    }

    protected Group createGroupWithModeSwitch(List<ExecutableElement> methods) {
        List<ModeElementType> modes = new ArrayList<ModeElementType>();
        for (ExecutableElement method : methods) {
            ModeElementType mode = new ModeElementType();
            String methodName = method.getSimpleName().toString();
            mode.setModeId(MuleStudioEditorXmlGenerator.URI_PREFIX + typeElement.name() + '/' + nameUtils.uncamel(methodName));
            mode.setModeLabel(nameUtils.friendlyNameFromCamelCase(methodName));
            modes.add(mode);
        }

        ModeType modeSwitch = new ModeType();
        modeSwitch.getMode().addAll(modes);
        modeSwitch.setCaption(helper.formatCaption("Operation"));
        modeSwitch.setName(StringUtils.capitalize(moduleName) + " operations to execute");
        modeSwitch.setDescription(helper.formatDescription("Operation"));

        Group group = new Group();
        group.setId(typeElement.name() + "ConnectorGeneric");
        group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupModeSwitch(modeSwitch));
        group.setCaption(helper.formatCaption(MuleStudioEditorXmlGenerator.GROUP_DEFAULT_CAPTION));
        return group;
    }

    protected BaseStudioXmlBuilder(GeneratorContext context, ExecutableElement executableElement, DevKitTypeElement typeElement) {
        this(context, typeElement);
        this.executableElement = executableElement;
    }

    protected List<AttributeCategory> processMethodParameters() {
        return processVariableElements(getParametersSorted());
    }

    protected List<AttributeCategory> processConfigurableFields(Group defaultGroup) {
        List<AttributeCategory> attributeCategories = processVariableElements(getConfigurableFieldsSorted());
        for (AttributeCategory attributeCategory : attributeCategories) {
            if (attributeCategory.getCaption().equals(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION)) {
                attributeCategory.setCaption(helper.getFormattedCaption(typeElement));
                attributeCategory.setDescription(helper.formatDescription(typeElement.name() + " configuration properties"));
                List<Group> groups = attributeCategory.getGroup();
                if (groups.isEmpty()) {
                    groups.add(defaultGroup);
                } else {
                    groups.add(0, defaultGroup);
                }
            }
        }
        if (attributeCategories.isEmpty()) {
            AttributeCategory attributeCategory = new AttributeCategory();
            attributeCategory.setCaption(helper.getFormattedCaption(typeElement));
            attributeCategory.setDescription(helper.formatDescription(typeElement.name() + " configuration properties"));
            attributeCategory.getGroup().add(defaultGroup);
            attributeCategories.add(attributeCategory);
        }
        return attributeCategories;
    }

    private List<AttributeCategory> processVariableElements(List<? extends VariableElement> variableElements) {

        Map<String, Group> groupsByName = new LinkedHashMap<String, Group>();
        Map<String, AttributeCategory> attributeCategoriesByName = new LinkedHashMap<String, AttributeCategory>();
        getOrCreateDefaultAttributeCategory(attributeCategoriesByName);

        processConnectionAttributes(groupsByName, attributeCategoriesByName);

        for (VariableElement parameter : variableElements) {
            JAXBElement<? extends AttributeType> jaxbElement = createJaxbElement(parameter);
            AttributeCategory attributeCategory = getOrCreateAttributeCategory(attributeCategoriesByName, parameter.getAnnotation(VariableDisplay.class));
            Group group = getOrCreateGroup(groupsByName, parameter);
            group.getRegexpOrEncodingOrModeSwitch().add(jaxbElement);

            if (!attributeCategory.getGroup().contains(group)) {
                attributeCategory.getGroup().add(group);
            }
        }

        ArrayList<AttributeCategory> attributeCategories = new ArrayList<AttributeCategory>(attributeCategoriesByName.values());
        for(AttributeCategory attributeCategory : attributeCategories) {
            Collections.sort(attributeCategory.getGroup(), new GroupComparator());
        }
        return attributeCategories;
    }

    protected void processConnectionAttributes(Map<String, Group> groupsByName, Map<String, AttributeCategory> attributeCategoriesByName) {
         // override if necessary
    }

    private AttributeCategory getOrCreateDefaultAttributeCategory(Map<String, AttributeCategory> attributeCategoriesByName) {
        return getOrCreateAttributeCategory(attributeCategoriesByName, null);

    }

    private AttributeCategory getOrCreateAttributeCategory(Map<String, AttributeCategory> attributeCategoriesByName, VariableDisplay display) {
        if (display == null || StringUtils.isBlank(display.tab())) {
            if (!attributeCategoriesByName.containsKey(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION)) {
                AttributeCategory attributeCategoryGeneral = new AttributeCategory();
                attributeCategoryGeneral.setCaption(helper.formatCaption(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION));
                attributeCategoryGeneral.setDescription(helper.formatDescription(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_DESCRIPTION));
                attributeCategoriesByName.put(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION, attributeCategoryGeneral);
            }
            return attributeCategoriesByName.get(MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION);
        } else {
            String attributeCategoryName;
            if (StringUtils.isNotBlank(display.tab())) {
                attributeCategoryName = display.tab();
            } else {
                attributeCategoryName = MuleStudioEditorXmlGenerator.ATTRIBUTE_CATEGORY_DEFAULT_CAPTION;
            }
            if (!attributeCategoriesByName.containsKey(attributeCategoryName)) {
                AttributeCategory attributeCategory = new AttributeCategory();
                attributeCategory.setCaption(helper.formatCaption(attributeCategoryName));
                attributeCategory.setDescription(helper.formatDescription(attributeCategoryName));
                attributeCategoriesByName.put(attributeCategoryName, attributeCategory);
            }
            return attributeCategoriesByName.get(attributeCategoryName);
        }
    }

    private Group getOrCreateGroup(Map<String, Group> groupsByName, VariableElement parameter) {
        VariableDisplay display = parameter.getAnnotation(VariableDisplay.class);
        if (display == null || StringUtils.isBlank(display.inputGroup())) {
            if (!groupsByName.containsKey(GENERAL_GROUP_NAME)) {
                Group groupGeneral = new Group();
                groupGeneral.setCaption(helper.formatCaption(GENERAL_GROUP_NAME));
                groupGeneral.setId(StringUtils.uncapitalize(GENERAL_GROUP_NAME));
                groupsByName.put(GENERAL_GROUP_NAME, groupGeneral);
            }
            return groupsByName.get(GENERAL_GROUP_NAME);
        } else {
            String groupName = display.inputGroup();
            if (!groupsByName.containsKey(groupName)) {
                Group group = new Group();
                group.setCaption(groupName);
                group.setId(StringUtils.uncapitalize(groupName));
                groupsByName.put(groupName, group);
            }
            return groupsByName.get(groupName);
        }
    }

    private JAXBElement<? extends AttributeType> createJaxbElement(VariableElement parameter) {
        JAXBElement<? extends AttributeType> jaxbElement;
        if (typeMirrorUtils.isEnum(parameter)) {
            EnumType enumType = createEnumType(parameter);
            jaxbElement = helper.createJAXBElement(enumType);
        } else if (typeMirrorUtils.isCollection(parameter)) {
            NestedElementReference childElement = createNestedElementReference(executableElement, parameter);
            jaxbElement = objectFactory.createGroupChildElement(childElement);
        } else {
            AttributeType attributeType = createAttributeType(parameter);
            jaxbElement = helper.createJAXBElement(attributeType);
        }
        return jaxbElement;
    }

    private List<? extends VariableElement> getParametersSorted() {
        List<? extends VariableElement> parameters = new ArrayList<VariableElement>(executableElement.getParameters());
        Iterator<? extends VariableElement> iterator = parameters.iterator();
        while (iterator.hasNext()) {
            if (typeMirrorUtils.ignoreParameter(iterator.next())) {
                iterator.remove();
            }
        }
        Collections.sort(parameters, new VariableComparator(context));
        return parameters;
    }

    private AttributeType createAttributeType(VariableElement parameter) {
        AttributeType attributeType = helper.createAttributeTypeIgnoreEnumsAndCollections(parameter);
        if (attributeType != null) {
            helper.setAttributeTypeInfo(parameter, attributeType);
        }
        return attributeType;
    }

    protected List<AttributeType> getConnectionAttributes(DevKitTypeElement typeElement) {
        List<AttributeType> parameters = new ArrayList<AttributeType>();
        ExecutableElement connectMethod = typeElement.getMethodsAnnotatedWith(Connect.class).get(0);
        for (VariableElement connectAttributeType : connectMethod.getParameters()) {
            AttributeType parameter = helper.createAttributeTypeIgnoreEnumsAndCollections(connectAttributeType);
            helper.setAttributeTypeInfo(connectAttributeType, parameter);
            parameter.setRequired(false);
            parameters.add(parameter);
        }
        return parameters;
    }

    private NestedElementReference createNestedElementReference(ExecutableElement executableElement, VariableElement parameter) {
        NestedElementReference childElement = new NestedElementReference();
        String prefix;
        if (executableElement != null) {
            prefix = nameUtils.uncamel(executableElement.getSimpleName().toString());
            childElement.setDescription(helper.formatDescription(javaDocUtils.getParameterSummary(parameter.getSimpleName().toString(), executableElement)));
        } else {
            prefix = "configurable";
            childElement.setDescription(helper.formatDescription(javaDocUtils.getSummary(parameter)));
        }
        childElement.setName(MuleStudioEditorXmlGenerator.URI_PREFIX + moduleName + '/' + prefix + '-' + nameUtils.uncamel(parameter.getSimpleName().toString()));
        childElement.setAllowMultiple(false);
        childElement.setCaption(helper.getFormattedCaption(parameter));
        childElement.setInplace(true);
        return childElement;
    }

    private EnumType createEnumType(VariableElement parameter) {
        EnumType enumType = new EnumType();
        enumType.setSupportsExpressions(true);
        enumType.setAllowsCustom(true);
        helper.setAttributeTypeInfo(parameter, enumType);
        for (Element enumMember : typeUtils.asElement(parameter.asType()).getEnclosedElements()) {
            if (enumMember.getKind() == ElementKind.ENUM_CONSTANT) {
                String enumConstant = enumMember.getSimpleName().toString();
                EnumElement enumElement = new EnumElement();
                enumElement.setValue(enumConstant);
                enumType.getOption().add(enumElement);
            }
        }
        Collections.sort(enumType.getOption(), new EnumElementComparator());
        return enumType;
    }

    private List<VariableElement> getConfigurableFieldsSorted() {
        List<VariableElement> configurableFields = typeElement.getFieldsAnnotatedWith(Configurable.class);
        Collections.sort(configurableFields, new VariableComparator(context));
        return configurableFields;
    }
}