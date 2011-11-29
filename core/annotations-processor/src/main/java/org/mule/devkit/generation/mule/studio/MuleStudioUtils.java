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

import org.apache.commons.lang.WordUtils;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.annotations.param.Payload;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.spring.SchemaGenerator;
import org.mule.devkit.generation.spring.SchemaTypeConversion;
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.Booleantype;
import org.mule.devkit.model.studio.EnumType;
import org.mule.devkit.model.studio.FlowRefType;
import org.mule.devkit.model.studio.IntegerType;
import org.mule.devkit.model.studio.LongType;
import org.mule.devkit.model.studio.NestedElementReference;
import org.mule.devkit.model.studio.ObjectFactory;
import org.mule.devkit.model.studio.StringAttributeType;
import org.mule.devkit.model.studio.TextType;
import org.mule.devkit.model.studio.UrlType;
import org.mule.devkit.utils.JavaDocUtils;
import org.mule.devkit.utils.NameUtils;
import org.mule.devkit.utils.TypeMirrorUtils;
import org.mule.util.StringUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.xml.bind.JAXBElement;

public class MuleStudioUtils {

    private static final String IMAGE = "icons/large/%s-connector-48x32.png";
    private static final String ICON = "icons/small/%s-connector-24x16.png";
    private NameUtils nameUtils;
    private JavaDocUtils javaDocUtils;
    private TypeMirrorUtils typeMirrorUtils;

    public MuleStudioUtils(GeneratorContext context) {
        nameUtils = context.getNameUtils();
        javaDocUtils = context.getJavaDocUtils();
        typeMirrorUtils = context.getTypeMirrorUtils();
    }

    public String formatCaption(String caption) {
        return WordUtils.capitalizeFully(caption);
    }

    public String formatDescription(String description) {
        if (Character.isLowerCase(description.charAt(0))) {
            description = StringUtils.capitalize(description);
        }
        if (!description.endsWith(".")) {
            description += '.';
        }
        return description;
    }

    public String getImage(String moduleName) {
        return String.format(IMAGE, moduleName);
    }

    public String getIcon(String moduleName) {
        return String.format(ICON, moduleName);
    }

    public String getGlobalRefId(String moduleName) {
        return "abstract" + StringUtils.capitalize(moduleName) + "ConnectorGeneric";
    }

    public JAXBElement<? extends AttributeType> createJAXBElement(AttributeType attributeType) {
        ObjectFactory objectFactory = new ObjectFactory();
        if (attributeType instanceof StringAttributeType) {
            return objectFactory.createGroupString((StringAttributeType) attributeType);
        }
        if (attributeType instanceof LongType) {
            return objectFactory.createGroupLong((LongType) attributeType);
        }
        if (attributeType instanceof IntegerType) {
            return objectFactory.createGroupInteger((IntegerType) attributeType);
        }
        if (attributeType instanceof EnumType) {
            return objectFactory.createGroupEnum((EnumType) attributeType);
        }
        if (attributeType instanceof Booleantype) {
            return objectFactory.createGroupBoolean((Booleantype) attributeType);
        }
        if (attributeType instanceof TextType) {
            return objectFactory.createGroupText((TextType) attributeType);
        }
        if (attributeType instanceof FlowRefType) {
            return objectFactory.createGroupFlowRef((FlowRefType) attributeType);
        }
        if (attributeType instanceof UrlType) {
            return objectFactory.createGroupUrl((UrlType) attributeType);
        }
        if (attributeType instanceof NestedElementReference) {
            return objectFactory.createNestedElementTypeChildElement((NestedElementReference) attributeType);
        }
        return null;
    }

    public AttributeType createAttributeTypeIgnoreEnumsAndCollections(Element element) {
        if (SchemaTypeConversion.isSupported(element.asType().toString())) {
            return createAttributeTypeOfSupportedType(element);
        } else if (typeMirrorUtils.isHttpCallback(element)) {
            FlowRefType flowRefType = new FlowRefType();
            flowRefType.setSupportFlow(true);
            flowRefType.setSupportSubflow(true);
            return flowRefType;
        } else if (skipAttributeTypeGeneration(element)) {
            return null;
        } else {
            return new StringAttributeType();
        }
    }

    private boolean skipAttributeTypeGeneration(Element element) {
        return typeMirrorUtils.isCollection(element.asType()) || element.getAnnotation(Payload.class) != null || typeMirrorUtils.isEnum(element.asType());
    }

    private AttributeType createAttributeTypeOfSupportedType(Element element) {
        if (typeMirrorUtils.isString(element) || typeMirrorUtils.isDate(element)) {
            return new StringAttributeType();
        } else if (typeMirrorUtils.isBoolean(element)) {
            return new Booleantype();
        } else if (typeMirrorUtils.isInteger(element)) {
            IntegerType integerType = new IntegerType();
            integerType.setMin(0);
            integerType.setStep(1);
            return integerType;
        } else if (typeMirrorUtils.isLong(element)) {
            LongType longType = new LongType();
            longType.setMin(0);
            longType.setStep(1);
            return longType;
        }
        else if(typeMirrorUtils.isURL(element)) {
             return new UrlType();
        } else {
            throw new RuntimeException("Failed to create Studio XML, type not recognized: type=" + element.asType().toString() + " name=" + element.getSimpleName().toString());
        }
    }

    public void setAttributeTypeInfo(ExecutableElement executableElement, VariableElement variableElement, AttributeType parameter) {
        String parameterName = variableElement.getSimpleName().toString();
        parameter.setCaption(formatCaption(nameUtils.friendlyNameFromCamelCase(parameterName)));
        parameter.setDescription(formatDescription(javaDocUtils.getParameterSummary(parameterName, executableElement)));
        if (parameter instanceof StringAttributeType && !SchemaTypeConversion.isSupported(variableElement.asType().toString())) {
            parameter.setName(parameterName + SchemaGenerator.REF_SUFFIX);
        } else if (parameter instanceof FlowRefType) {
            parameter.setName(nameUtils.uncamel(parameterName) + SchemaGenerator.FLOW_REF_SUFFIX);
        } else {
            parameter.setName(parameterName);
        }
        parameter.setRequired(variableElement.getAnnotation(Optional.class) == null);
        setDefaultValueIfAvailable(variableElement, parameter);
    }

    public void setDefaultValueIfAvailable(VariableElement variableElement, AttributeType parameter) {
        Default annotation = variableElement.getAnnotation(Default.class);
        if (annotation != null) {
            if (parameter instanceof Booleantype) {
                ((Booleantype) parameter).setDefaultValue(Boolean.valueOf(annotation.value()));
            } else if (parameter instanceof IntegerType) {
                ((IntegerType) parameter).setDefaultValue(Integer.valueOf(annotation.value()));
            } else if (parameter instanceof StringAttributeType) {
                ((StringAttributeType) parameter).setDefaultValue(annotation.value());
            } else if (parameter instanceof EnumType) {
                ((EnumType) parameter).setDefaultValue(annotation.value());
            }
        }
    }
}