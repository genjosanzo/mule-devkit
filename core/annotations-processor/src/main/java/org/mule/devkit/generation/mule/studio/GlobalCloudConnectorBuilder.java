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
import org.mule.api.annotations.param.Optional;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.GlobalType;
import org.mule.devkit.model.studio.Group;
import org.mule.devkit.model.studio.ObjectFactory;
import org.mule.devkit.utils.JavaDocUtils;
import org.mule.devkit.utils.NameUtils;
import org.mule.devkit.utils.TypeMirrorUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GlobalCloudConnectorBuilder {

    private static final String URI_PREFIX = "http://www.mulesoft.org/schema/mule/";
    private static final String GLOBAL_CLOUD_CONNECTOR_LOCAL_ID = "config";
    private ObjectFactory objectFactory;
    private MuleStudioUtils helper;
    private DevKitTypeElement typeElement;
    private TypeMirrorUtils typeMirrorUtils;
    private NameUtils nameUtils;
    private JavaDocUtils javaDocUtils;

    public GlobalCloudConnectorBuilder(GeneratorContext context, DevKitTypeElement typeElement) {
        this.typeElement = typeElement;
        helper = new MuleStudioUtils(context);
        objectFactory = new ObjectFactory();
        typeMirrorUtils = context.getTypeMirrorUtils();
        nameUtils = context.getNameUtils();
        javaDocUtils = context.getJavaDocUtils();
    }

    public JAXBElement<GlobalType> build() {

        List<AttributeType> fields = new ArrayList<AttributeType>();
        for (VariableElement field : getConfigurableFieldsSorted()) {
            AttributeType parameter = helper.createAttributeType(field);
            if (parameter != null) {
                String parameterName = field.getSimpleName().toString();
                parameter.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(parameterName)));
                parameter.setDescription(helper.formatDescription(javaDocUtils.getSummary(field)));
                parameter.setName(parameterName);
                parameter.setRequired(field.getAnnotation(Optional.class) == null);
                helper.setDefaultValueIfAvailable(field, parameter);
                fields.add(parameter);
            }
        }

        if (typeElement.usesConnectionManager()) {
            addConnectionAttributeTypes(typeElement, fields);
        }

        AttributeType nameAttributeType = new AttributeType();
        nameAttributeType.setName("name");
        nameAttributeType.setDescription(helper.formatDescription("Give a name to this configuration so it can be later referenced by config-ref."));
        nameAttributeType.setCaption(helper.formatCaption("Name"));
        nameAttributeType.setRequired(false);

        Group group = new Group();
        group.setId(typeElement.name() + "GenericProperties");
        group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupName(nameAttributeType));
        group.setCaption(helper.formatCaption("Generic"));

        for (AttributeType attributeType : fields) {
            group.getRegexpOrEncodingOrModeSwitch().add(helper.createJAXBElement(attributeType));
        }

        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(helper.formatCaption(typeElement.name()));
        attributeCategory.setDescription(helper.formatDescription(typeElement.name() + " configuration properties"));
        attributeCategory.getGroup().add(group);

        GlobalType globalCloudConnector = new GlobalType();
        globalCloudConnector.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().add(attributeCategory);
        globalCloudConnector.setCaption(helper.formatCaption(nameUtils.friendlyNameFromCamelCase(typeElement.name())));
        globalCloudConnector.setLocalId(GLOBAL_CLOUD_CONNECTOR_LOCAL_ID);
        globalCloudConnector.setDescription(helper.formatDescription("Global " + nameUtils.friendlyNameFromCamelCase(typeElement.name()) + " configuration information"));
        globalCloudConnector.setExtends(URI_PREFIX + typeElement.name() + '/' + helper.getGlobalRefId(typeElement.name()));
        globalCloudConnector.setIcon(helper.getIcon(typeElement.name()));
        globalCloudConnector.setImage(helper.getImage(typeElement.name()));
        return objectFactory.createNamespaceTypeGlobalCloudConnector(globalCloudConnector);
    }

    private List<VariableElement> getConfigurableFieldsSorted() {
        List<VariableElement> configurableFields = typeElement.getFieldsAnnotatedWith(Configurable.class);
        Collections.sort(configurableFields, new VariableComparator(typeMirrorUtils));
        return configurableFields;
    }

    private void addConnectionAttributeTypes(DevKitTypeElement typeElement, List<AttributeType> parameters) {
        ExecutableElement connectMethod = typeElement.getMethodsAnnotatedWith(Connect.class).get(0);
        for (VariableElement connectAttributeType : connectMethod.getParameters()) {
            AttributeType parameter = helper.createAttributeType(connectAttributeType);
            String parameterName = connectAttributeType.getSimpleName().toString();
            helper.setAttributeTypeInfo(connectMethod, connectAttributeType, parameter, parameterName);
            parameter.setRequired(false);
            parameters.add(parameter);
        }
    }
}