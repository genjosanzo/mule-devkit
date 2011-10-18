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
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.studio.AbstractElementType;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.AttributeType;
import org.mule.devkit.model.studio.Booleantype;
import org.mule.devkit.model.studio.EnumType;
import org.mule.devkit.model.studio.GlobalType;
import org.mule.devkit.model.studio.Group;
import org.mule.devkit.model.studio.IntegerType;
import org.mule.devkit.model.studio.LongType;
import org.mule.devkit.model.studio.ModeElementType;
import org.mule.devkit.model.studio.ModeType;
import org.mule.devkit.model.studio.Module;
import org.mule.devkit.model.studio.NamespaceType;
import org.mule.devkit.model.studio.NestedElementReference;
import org.mule.devkit.model.studio.NestedElementType;
import org.mule.devkit.model.studio.NewType;
import org.mule.devkit.model.studio.ObjectFactory;
import org.mule.devkit.model.studio.PatternType;
import org.mule.devkit.model.studio.StringAttributeType;
import org.mule.devkit.model.studio.TextType;
import org.mule.util.StringUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

public class MuleStudioXmlGenerator extends AbstractMessageGenerator {

    public static final String URI_PREFIX = "http://www.mulesoft.org/schema/mule/";
    public static final String GLOBAL_CLOUD_CONNECTOR_LOCAL_ID = "config";
    private static final String IMAGE = "icons/large/%s-connector-48x32.png";
    private static final String ICON = "icons/small/%s-connector-24x16.png";
    private static final String ALIAS_ID_PREFIX = "org.mule.tooling.ui.modules.core.pattern.";
    private ObjectFactory objectFactory = new ObjectFactory();

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return !context.hasOption("skipStudioXmlGeneration");
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {
        String moduleName = typeElement.name();

        NamespaceType namespace = new NamespaceType();
        namespace.setPrefix(moduleName);
        namespace.setUrl(URI_PREFIX + moduleName);
        namespace.getConnectorOrEndpointOrGlobal().add(createGlobalType(moduleName, typeElement));
        namespace.getConnectorOrEndpointOrGlobal().add(createConnectorTypeListingOps(typeElement, moduleName));
        namespace.getConnectorOrEndpointOrGlobal().add(createConfigRefAbstractConnectorType(moduleName));
        List<String> parsedLocalIds = new ArrayList<String>();
        for (ExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            generateConnectorTypeElement(executableElement, namespace, moduleName, parsedLocalIds, typeElement);
        }

        Module module = new Module();
        module.setNamespace(namespace);

        context.getStudioModel().setModule(module);
        context.getStudioModel().setModuleName(moduleName);
    }

    private JAXBElement<PatternType> createConfigRefAbstractConnectorType(String moduleName) {
        NewType globalRef = new NewType();
        globalRef.setRequiredType(URI_PREFIX + moduleName + "/" + GLOBAL_CLOUD_CONNECTOR_LOCAL_ID);
        globalRef.setName("config-ref");
        globalRef.setCaption(formatCaption("config reference"));
        globalRef.setDescription(formatDescription("Specify which configuration to use for this invocation"));

        Group group = new Group();
        group.setId(getGlobalRefId(moduleName));
        group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupGlobalRef(globalRef));
        group.setCaption(formatCaption("Generic"));

        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(formatCaption("General"));
        attributeCategory.setDescription(formatDescription("General properties"));
        attributeCategory.getGroup().add(group);

        PatternType cloudConnector = new PatternType();
        cloudConnector.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().add(attributeCategory);
        cloudConnector.setCaption(formatCaption(getGlobalRefId(moduleName)));
        cloudConnector.setLocalId(getGlobalRefId(moduleName));
        cloudConnector.setDescription(formatDescription("Interact with " + StringUtils.capitalize(moduleName)));
        cloudConnector.setAbstract(true);
        cloudConnector.setIcon(String.format(ICON, moduleName));
        cloudConnector.setImage(String.format(IMAGE, moduleName));

        return objectFactory.createNamespaceTypeCloudConnector(cloudConnector);
    }

    private JAXBElement<GlobalType> createGlobalType(String moduleName, DevKitTypeElement typeElement) {

        List<AttributeType> fields = new ArrayList<AttributeType>();
        for (VariableElement field : typeElement.getFieldsAnnotatedWith(Configurable.class)) {
            AttributeType parameter = createAttributeType(field);
            if (parameter != null) {
                String parameterName = field.getSimpleName().toString();
                parameter.setCaption(formatCaption(context.getNameUtils().friendlyNameFromCamelCase(parameterName)));
                parameter.setDescription(formatDescription(context.getJavaDocUtils().getSummary(field)));
                parameter.setName(parameterName);
                setOptionalOrRequired(field, parameter);
                setDefaultValueIfAvailable(field, parameter);
                fields.add(parameter);
            }
        }

        if (typeElement.usesConnectionManager()) {
            addConnectionAttributeTypes(typeElement, fields);
        }

        AttributeType nameAttributeType = new AttributeType();
        nameAttributeType.setName("name");
        nameAttributeType.setDescription(formatDescription("Give a name to this configuration so it can be later referenced by config-ref."));
        nameAttributeType.setCaption(formatCaption("Name"));
        nameAttributeType.setRequired(false);

        Group group = new Group();
        group.setId(moduleName + "GenericProperties");
        group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupName(nameAttributeType));
        group.setCaption(formatCaption("Generic"));

        for (AttributeType attributeType : fields) {
            group.getRegexpOrEncodingOrModeSwitch().add(createJAXBElement(attributeType));
        }

        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(formatCaption(moduleName));
        attributeCategory.setDescription(formatDescription(moduleName + " configuration properties"));
        attributeCategory.getGroup().add(group);

        GlobalType globalCloudConnector = new GlobalType();
        globalCloudConnector.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().add(attributeCategory);
        globalCloudConnector.setCaption(formatCaption(context.getNameUtils().friendlyNameFromCamelCase(moduleName)));
        globalCloudConnector.setLocalId(GLOBAL_CLOUD_CONNECTOR_LOCAL_ID);
        globalCloudConnector.setDescription(formatDescription("Global " + context.getNameUtils().friendlyNameFromCamelCase(moduleName) + " configuration information"));
        globalCloudConnector.setExtends(URI_PREFIX + moduleName + '/' + getGlobalRefId(moduleName));
        globalCloudConnector.setIcon(String.format(ICON, moduleName));
        globalCloudConnector.setImage(String.format(IMAGE, moduleName));
        return objectFactory.createNamespaceTypeGlobalCloudConnector(globalCloudConnector);
    }

    private JAXBElement<? extends AttributeType> createJAXBElement(AttributeType attributeType) {
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
        if (attributeType instanceof NestedElementReference) {
            return objectFactory.createNestedElementTypeChildElement((NestedElementReference) attributeType);
        }
        return null;
    }

    private JAXBElement<PatternType> createConnectorTypeListingOps(DevKitTypeElement typeElement, String moduleName) {
        List<ModeElementType> modes = new ArrayList<ModeElementType>();
        for (ExecutableElement processorMethod : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            ModeElementType mode = new ModeElementType();
            String methodName = processorMethod.getSimpleName().toString();
            mode.setModeId(URI_PREFIX + moduleName + '/' + context.getNameUtils().uncamel(methodName));
            mode.setModeLabel(context.getNameUtils().friendlyNameFromCamelCase(methodName));
            modes.add(mode);
        }

        ModeType modeSwitch = new ModeType();
        modeSwitch.getMode().addAll(modes);
        modeSwitch.setCaption(formatCaption("Operation"));
        modeSwitch.setName(StringUtils.capitalize(moduleName) + " operations to execute");
        modeSwitch.setDescription(formatDescription("Operation"));

        Group group = new Group();
        group.setId(moduleName + "ConnectorGeneric");
        group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupModeSwitch(modeSwitch));
        group.setCaption(formatCaption("Generic"));

        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(formatCaption("General"));
        attributeCategory.setDescription(formatDescription("General properties"));
        attributeCategory.getGroup().add(group);

        PatternType cloudConnector = new PatternType();
        cloudConnector.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().add(attributeCategory);
        cloudConnector.setCaption(formatCaption(moduleName));
        cloudConnector.setLocalId(moduleName + "-connector");
        cloudConnector.setExtends(URI_PREFIX + moduleName + '/' + getGlobalRefId(moduleName));
        cloudConnector.setDescription(formatDescription(moduleName + " Integration"));
        cloudConnector.setAliasId(ALIAS_ID_PREFIX + moduleName);
        cloudConnector.setIcon(String.format(ICON, moduleName));
        cloudConnector.setImage(String.format(IMAGE, moduleName));

        return objectFactory.createNamespaceTypeCloudConnector(cloudConnector);
    }

    private void generateConnectorTypeElement(ExecutableElement executableElement, NamespaceType namespace, String moduleName, List<String> parsedLocalIds, DevKitTypeElement typeElement) {
        List<AttributeType> simpleTypeAttributeTypes = getSimpleTypeAttributeTypes(executableElement, typeElement);

        List<NestedElementReference> childElementAttributeTypes = getChildElementsAttributeTypes(executableElement, moduleName);

        namespace.getConnectorOrEndpointOrGlobal().addAll(getNesteds(executableElement, moduleName, parsedLocalIds));

        Group group = new Group();
        group.setCaption(formatCaption("General"));
        group.setDescription(formatDescription(context.getJavaDocUtils().getSummary(executableElement).replaceAll("\\n|<p/>", "")));
        group.setId("general");

        for (AttributeType attributeType : simpleTypeAttributeTypes) {
            group.getRegexpOrEncodingOrModeSwitch().add(createJAXBElement(attributeType));
        }

        for (NestedElementReference childElement : childElementAttributeTypes) {
            group.getRegexpOrEncodingOrModeSwitch().add(objectFactory.createGroupChildElement(childElement));
        }

        AttributeCategory attributeCategory = new AttributeCategory();
        attributeCategory.setCaption(formatCaption("General"));
        attributeCategory.setDescription(formatDescription("General properties"));
        attributeCategory.getGroup().add(group);

        PatternType cloudConnector = createCloudConnectorElemenet(executableElement, moduleName, attributeCategory);

        namespace.getConnectorOrEndpointOrGlobal().add(objectFactory.createNamespaceTypeCloudConnector(cloudConnector));
    }

    private PatternType createCloudConnectorElemenet(ExecutableElement executableElement, String moduleName, AttributeCategory attributeCategory) {
        PatternType cloudConnector = new PatternType();
        cloudConnector.setLocalId(context.getNameUtils().uncamel(executableElement.getSimpleName().toString()));
        cloudConnector.setCaption(formatCaption(context.getNameUtils().uncamel(executableElement.getSimpleName().toString())));
        cloudConnector.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().add(attributeCategory);
        cloudConnector.setAbstract(true);
        cloudConnector.setExtends(URI_PREFIX + moduleName + '/' + getGlobalRefId(moduleName));
        cloudConnector.setIcon(String.format(ICON, moduleName));
        cloudConnector.setImage(String.format(IMAGE, moduleName));
        return cloudConnector;
    }

    private List<JAXBElement<? extends AbstractElementType>> getNesteds(ExecutableElement executableElement, String moduleName, List<String> parsedLocalIds) {
        List<JAXBElement<? extends AbstractElementType>> nesteds = new ArrayList<JAXBElement<? extends AbstractElementType>>();
        for (VariableElement parameter : executableElement.getParameters()) {
            String localId = context.getNameUtils().uncamel(parameter.getSimpleName().toString());
            if (isNested(parameter) && !parsedLocalIds.contains(localId)) {

                parsedLocalIds.add(localId);

                NestedElementType nested = new NestedElementType();
                nested.setLocalId(localId);
                nested.setCaption(formatCaption(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                nested.setDescription(formatDescription(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                nested.setIcon(String.format(ICON, moduleName));
                nested.setImage(String.format(IMAGE, moduleName));

                NestedElementReference childElement = new NestedElementReference();
                childElement.setName(URI_PREFIX + moduleName + '/' + context.getNameUtils().singularize(localId));
                childElement.setDescription(formatDescription(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                childElement.setCaption(formatCaption(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                childElement.setAllowMultiple(true);
                nested.getRegexpOrEncodingOrString().add(createJAXBElement(childElement));

                NestedElementType nested1 = new NestedElementType();
                nested1.setCaption(formatCaption(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                nested1.setLocalId(childElement.getName().substring(childElement.getName().lastIndexOf("/") + 1));
                nested1.setDescription(formatDescription(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                nested1.setIcon(String.format(ICON, moduleName));
                nested1.setImage(String.format(IMAGE, moduleName));

                AttributeType key;
                if (((DeclaredType) parameter.asType()).getTypeArguments().isEmpty()) {
                    key = new StringAttributeType();
                } else {
                    TypeMirror typeMirror = ((DeclaredType) parameter.asType()).getTypeArguments().get(0);
                    key = createAttributeType(context.getTypeUtils().asElement(typeMirror));
                }

                if (context.getTypeMirrorUtils().isMap(parameter.asType())) {
                    key.setName("key");
                    key.setDescription(formatDescription("Key."));
                    key.setCaption(formatCaption("Key"));
                } else {
                    key.setName(context.getNameUtils().singularize(localId));
                    key.setCaption(formatCaption(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                    key.setDescription(formatDescription(context.getJavaDocUtils().getParameterSummary(parameter.getSimpleName().toString(), executableElement)));
                }
                nested1.getRegexpOrEncodingOrString().add(createJAXBElement(key));

                if (context.getTypeMirrorUtils().isMap(parameter.asType())) {
                    TextType textAttributeType = new TextType();
                    textAttributeType.setName("value");
                    textAttributeType.setDescription(formatDescription("Value."));
                    textAttributeType.setCaption(formatCaption("Value"));
                    textAttributeType.setIsToElement(true);
                    nested1.getRegexpOrEncodingOrString().add(createJAXBElement(textAttributeType));
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

    private List<AttributeType> getSimpleTypeAttributeTypes(ExecutableElement executableElement, DevKitTypeElement typeElement) {
        List<AttributeType> parameters = new ArrayList<AttributeType>();
        for (VariableElement variableElement : executableElement.getParameters()) {
            AttributeType parameter = createAttributeType(variableElement);
            String parameterName = variableElement.getSimpleName().toString();
            if (parameter != null) {
                setAttributeTypeInfo(executableElement, parameters, variableElement, parameter, parameterName);
            }
        }
        if(typeElement.usesConnectionManager()) {
            addConnectionAttributeTypes(typeElement, parameters);
        }
        return parameters;
    }

    private void addConnectionAttributeTypes(DevKitTypeElement typeElement, List<AttributeType> parameters) {
        ExecutableElement connectMethod = typeElement.getMethodsAnnotatedWith(Connect.class).get(0);
        for (VariableElement connectAttributeType : connectMethod.getParameters()) {
            AttributeType parameter = createAttributeType(connectAttributeType);
            String parameterName = connectAttributeType.getSimpleName().toString();
            setAttributeTypeInfo(connectMethod, parameters, connectAttributeType, parameter, parameterName);
            parameter.setRequired(false);
        }
    }

    private void setAttributeTypeInfo(ExecutableElement executableElement, List<AttributeType> parameters, VariableElement variableElement, AttributeType parameter, String parameterName) {
        parameter.setCaption(formatCaption(context.getNameUtils().friendlyNameFromCamelCase(parameterName)));
        parameter.setDescription(formatDescription(context.getJavaDocUtils().getParameterSummary(parameterName, executableElement)));
        parameter.setName(parameterName);
        setOptionalOrRequired(variableElement, parameter);
        setDefaultValueIfAvailable(variableElement, parameter);
        parameters.add(parameter);
    }

    private List<NestedElementReference> getChildElementsAttributeTypes(ExecutableElement executableElement, String moduleName) {
        List<NestedElementReference> parameters = new ArrayList<NestedElementReference>();
        for (VariableElement variableElement : executableElement.getParameters()) {
            if (context.getTypeMirrorUtils().isCollection(variableElement.asType())) {
                NestedElementReference childElement = new NestedElementReference();
                childElement.setName(URI_PREFIX + moduleName + "/" + context.getNameUtils().uncamel(variableElement.getSimpleName().toString()));
                childElement.setDescription(formatDescription(context.getJavaDocUtils().getParameterSummary(variableElement.getSimpleName().toString(), executableElement)));
                childElement.setCaption(formatCaption(context.getNameUtils().friendlyNameFromCamelCase(variableElement.getSimpleName().toString())));
                if (isNestedCollection(variableElement)) {
                    childElement.setAllowMultiple(true);
                } else {
                    childElement.setAllowMultiple(false);
                }
                childElement.setInplace(true);
                parameters.add(childElement);
            }
        }
        return parameters;
    }

    private boolean isNestedCollection(VariableElement variableElement) {
        DeclaredType declaredType = (DeclaredType) variableElement.asType();
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        for (TypeMirror typeArgument : typeArguments) {
            if (context.getTypeMirrorUtils().isCollection(typeArgument)) {
                return true;
            }
        }
        return false;
    }

    private void setDefaultValueIfAvailable(VariableElement variableElement, AttributeType parameter) {
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

    private void setOptionalOrRequired(VariableElement variableElement, AttributeType parameter) {
        if (variableElement.getAnnotation(Optional.class) != null) {
            parameter.setRequired(false);
        } else {
            parameter.setRequired(true);
        }
    }

    private AttributeType createAttributeType(Element variableElement) {
        String parameterClassName = variableElement.asType().toString();
        if (parameterClassName.equals(String.class.getName())) {
            return new StringAttributeType();
        } else if (parameterClassName.equals("boolean") || parameterClassName.equals(Boolean.class.getName())) {
            return new Booleantype();
        } else if (parameterClassName.equals("int") || parameterClassName.equals(Integer.class.getName())) {
            IntegerType integerType = new IntegerType();
            integerType.setMin(0);
            integerType.setStep(1);
            return integerType;
        }
        return null;
    }

    private String getGlobalRefId(String moduleName) {
        return "abstract" + StringUtils.capitalize(moduleName) + "ConnectorGeneric";
    }

    private String formatCaption(String caption) {
        return WordUtils.capitalizeFully(caption);
    }

    private String formatDescription(String description) {
        if (Character.isLowerCase(description.charAt(0))) {
            description = StringUtils.capitalize(description);
        }
        if (!description.endsWith(".")) {
            description += '.';
        }
        return description;
    }
}