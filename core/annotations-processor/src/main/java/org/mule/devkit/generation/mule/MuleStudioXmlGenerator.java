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

package org.mule.devkit.generation.mule;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang.WordUtils;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.annotations.param.Session;
import org.mule.api.annotations.session.SessionCreate;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.BooleanParameter;
import org.mule.devkit.model.studio.ChildElement;
import org.mule.devkit.model.studio.CloudConnector;
import org.mule.devkit.model.studio.GlobalCloudConnector;
import org.mule.devkit.model.studio.GlobalRef;
import org.mule.devkit.model.studio.Group;
import org.mule.devkit.model.studio.IntegerParameter;
import org.mule.devkit.model.studio.Mode;
import org.mule.devkit.model.studio.ModeSwitch;
import org.mule.devkit.model.studio.Name;
import org.mule.devkit.model.studio.Namespace;
import org.mule.devkit.model.studio.Nested;
import org.mule.devkit.model.studio.Parameter;
import org.mule.devkit.model.studio.StringParameter;
import org.mule.devkit.model.studio.TextParameter;
import org.mule.util.StringUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

public class MuleStudioXmlGenerator extends AbstractMessageGenerator {

    public static final String URI_PREFIX = "http://www.mulesoft.org/schema/mule/";
    private static final String TOOLING_NS = URI_PREFIX + "tooling.attributes";
    private static final String ALIAS_ID_PREFIX = "org.mule.tooling.ui.modules.core.pattern.";
    public static final String GLOBAL_CLOUD_CONNECTOR_LOCAL_ID = "config";

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return context.hasOption("generateStudioXml");
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {
        XStream xStream = context.getStudioModel().getXStream();

        Namespace namespace = new Namespace(xStream);
        String moduleName = typeElement.name();
        namespace.setPrefix(moduleName);
        namespace.setXmlns(TOOLING_NS);
        namespace.setUrl(URI_PREFIX + moduleName);

        List<ExecutableElement> processorMethods = typeElement.getMethodsAnnotatedWith(Processor.class);

        namespace.setGlobalCloudConnector(createGlobalCloudConnector(xStream, moduleName, typeElement));
        namespace.getCloudConnectors().add(createCloudConnectorListingOps(xStream, processorMethods, moduleName));
        namespace.getCloudConnectors().add(createConfigRefAbstractCloudConnector(xStream, moduleName));

        List<String> parsedLocalIds = new ArrayList<String>();
        for (ExecutableElement executableElement : processorMethods) {
            generateCloudConnectorElement(executableElement, namespace, xStream, moduleName, parsedLocalIds, typeElement);
        }
        context.getStudioModel().setNamespace(namespace);
        context.getStudioModel().setModuleName(moduleName);
    }

    private CloudConnector createConfigRefAbstractCloudConnector(XStream xStream, String moduleName) {
        GlobalRef globalRef = new GlobalRef(xStream);
        globalRef.setRequiredType(URI_PREFIX + moduleName + "/" + GLOBAL_CLOUD_CONNECTOR_LOCAL_ID);

        Group group = new Group(xStream);
        group.setId(getGlobalRefId(moduleName));
        group.getParameters().add(globalRef);

        AttributeCategory attributeCategory = new AttributeCategory(xStream);
        attributeCategory.setGroup(group);

        CloudConnector cloudConnector = new CloudConnector(xStream, moduleName);
        cloudConnector.setAttributeCategory(attributeCategory);
        cloudConnector.setCaption(formatCaption(getGlobalRefId(moduleName)));

        cloudConnector.setLocalId(getGlobalRefId(moduleName));
        cloudConnector.setDescription(formatDescription("Interact with " + StringUtils.capitalize(moduleName)));
        cloudConnector.setAbstract("true");

        return cloudConnector;
    }

    private GlobalCloudConnector createGlobalCloudConnector(XStream xStream, String moduleName, DevKitTypeElement typeElement) {

        List<Parameter> fields = new ArrayList<Parameter>();
        for (VariableElement field : typeElement.getFieldsAnnotatedWith(Configurable.class)) {
            Parameter parameter = getParameter(xStream, field);
            if (parameter == null) {
                continue;
            }
            String parameterName = field.getSimpleName().toString();
            parameter.setCaption(formatCaption(context.getNameUtils().friendlyNameFromCamelCase(parameterName)));
            parameter.setDescription(formatDescription(context.getJavaDocUtils().getSummary(field)));
            parameter.setName(parameterName);
            setOptionalOrRequired(field, parameter);
            setDefaultValueIfAvailable(field, parameter);
            fields.add(parameter);
        }

        if(typeElement.usesSessionManagement()) {
            addSessionParameters(xStream, typeElement, fields);
        }

        Name name = new Name(xStream);
        name.setName("name");
        name.setDescription(formatDescription("Give a name to this configuration so it can be later referenced by config-ref."));
        name.setCaption(formatCaption("Name"));
        name.setRequired("false");

        Group group = new Group(xStream);
        group.setId(moduleName + "GenericProperties");
        group.setName(name);
        group.setParameters(fields);

        AttributeCategory attributeCategory = new AttributeCategory(xStream);
        attributeCategory.setCaption(formatCaption(moduleName));
        attributeCategory.setDescription(formatDescription(moduleName + " configuration properties"));
        attributeCategory.setGroup(group);

        GlobalCloudConnector globalCloudConnector = new GlobalCloudConnector(xStream, moduleName);
        globalCloudConnector.setAttributeCategory(attributeCategory);
        globalCloudConnector.setCaption(formatCaption(context.getNameUtils().friendlyNameFromCamelCase(moduleName)));
        globalCloudConnector.setLocalId(GLOBAL_CLOUD_CONNECTOR_LOCAL_ID);
        globalCloudConnector.setDescription(formatDescription("Global " + context.getNameUtils().friendlyNameFromCamelCase(moduleName) + " configuration information"));
        globalCloudConnector.setExtends(URI_PREFIX + moduleName + '/' + getGlobalRefId(moduleName));
        return globalCloudConnector;
    }

    private CloudConnector createCloudConnectorListingOps(XStream xStream, List<ExecutableElement> processorMethods, String moduleName) {
        List<Mode> modes = new ArrayList<Mode>();
        for (ExecutableElement processorMethod : processorMethods) {
            Mode mode = new Mode(xStream);
            String methodName = processorMethod.getSimpleName().toString();
            mode.setModeId(URI_PREFIX + moduleName + '/' + context.getNameUtils().uncamel(methodName));
            mode.setModeLabel(context.getNameUtils().friendlyNameFromCamelCase(methodName));
            modes.add(mode);
        }

        ModeSwitch modeSwitch = new ModeSwitch(xStream);
        modeSwitch.setModes(modes);
        modeSwitch.setCaption(formatCaption("Operation"));
        modeSwitch.setName(StringUtils.capitalize(moduleName) + " operations to execute");
        modeSwitch.setDescription(formatDescription("Operation"));

        Group group = new Group(xStream);
        group.setId(moduleName + "ConnectorGeneric");
        group.setModeSwitch(modeSwitch);

        AttributeCategory attributeCategory = new AttributeCategory(xStream);
        attributeCategory.setGroup(group);

        CloudConnector cloudConnector = new CloudConnector(xStream, moduleName);
        cloudConnector.setAttributeCategory(attributeCategory);
        cloudConnector.setCaption(formatCaption(moduleName));
        cloudConnector.setLocalId(moduleName + "-connector");
        cloudConnector.setExtends(URI_PREFIX + moduleName + '/' + getGlobalRefId(moduleName));
        cloudConnector.setDescription(formatDescription(moduleName + " Integration"));
        cloudConnector.setAliasId(ALIAS_ID_PREFIX + moduleName);

        return cloudConnector;
    }

    private void generateCloudConnectorElement(ExecutableElement executableElement, Namespace namespace, XStream xStream, String moduleName, List<String> parsedLocalIds, DevKitTypeElement typeElement) {
        List<Parameter> simpleTypeParameters = getSimpleTypeParameters(executableElement, xStream, typeElement);

        List<ChildElement> childElementParameters = getChildElementsParameters(executableElement, xStream, moduleName);

        namespace.getNesteds().addAll(getNesteds(executableElement, xStream, moduleName, parsedLocalIds));

        Group group = new Group(xStream);
        group.setCaption(formatCaption("General"));
        group.setDescription(formatDescription(context.getJavaDocUtils().getSummary(executableElement).replaceAll("\\n|<p/>", "")));
        group.setId("general");
        group.getParameters().addAll(simpleTypeParameters);
        group.getParameters().addAll(childElementParameters);

        AttributeCategory attributeCategory = new AttributeCategory(xStream);
        attributeCategory.setDescription(formatDescription("General properties"));
        attributeCategory.setGroup(group);

        CloudConnector cloudConnector = new CloudConnector(xStream, moduleName);
        cloudConnector.setLocalId(context.getNameUtils().uncamel(executableElement.getSimpleName().toString()));
        cloudConnector.setCaption(formatCaption(context.getNameUtils().uncamel(executableElement.getSimpleName().toString())));
        cloudConnector.setAttributeCategory(attributeCategory);
        cloudConnector.setAbstract("true");
        cloudConnector.setExtends(URI_PREFIX + moduleName + '/' + getGlobalRefId(moduleName));

        namespace.getCloudConnectors().add(cloudConnector);
    }

    private List<Nested> getNesteds(ExecutableElement executableElement, XStream xStream, String moduleName, List<String> parsedLocalIds) {
        List<Nested> nesteds = new ArrayList<Nested>();
        for (VariableElement parameter : executableElement.getParameters()) {
            String localId = context.getNameUtils().uncamel(parameter.getSimpleName().toString());
            if (isNested(parameter) && !parsedLocalIds.contains(localId)) {

                parsedLocalIds.add(localId);

                Nested nested = new Nested(xStream, moduleName);
                nested.setLocalId(localId);
                nested.setCaption(formatCaption(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                nested.setDescription(formatDescription(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));

                ChildElement childElement = new ChildElement(xStream);
                childElement.setName(URI_PREFIX + moduleName + '/' + context.getNameUtils().singularize(localId));
                childElement.setDescription(formatDescription(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                childElement.setCaption(formatCaption(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                childElement.setAllowMultiple("true");
                nested.setChildElement(childElement);

                Nested nested1 = new Nested(xStream, moduleName);
                nested1.setCaption(formatCaption(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));
                nested1.setLocalId(childElement.getName().substring(childElement.getName().lastIndexOf("/") + 1));
                nested1.setDescription(formatDescription(context.getNameUtils().friendlyNameFromCamelCase(parameter.getSimpleName().toString())));

                Parameter key;
                if (((DeclaredType) parameter.asType()).getTypeArguments().isEmpty()) {
                    key = new StringParameter(xStream);
                } else {
                    TypeMirror typeMirror = ((DeclaredType) parameter.asType()).getTypeArguments().get(0);
                    String className = typeMirror.toString();
                    if (className.equals(String.class.getName())) {
                        key = new StringParameter(xStream);
                    } else if (className.equals(Integer.class.getName()) || className.equals("int")) {
                        key = new IntegerParameter(xStream);
                    } else {
                        key = new StringParameter(xStream);
                    }
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
                nested1.getParameters().add(key);

                if (context.getTypeMirrorUtils().isMap(parameter.asType())) {
                    TextParameter textParameter = new TextParameter(xStream);
                    textParameter.setName("value");
                    textParameter.setDescription(formatDescription("Value."));
                    textParameter.setCaption(formatCaption("Value"));
                    textParameter.setToElement("true");
                    nested1.getParameters().add(textParameter);
                }

                nesteds.add(nested);
                nesteds.add(nested1);
            }
        }
        return nesteds;
    }

    private boolean isNested(VariableElement parameter) {
        return context.getTypeMirrorUtils().isMap(parameter.asType()) ||
                context.getTypeMirrorUtils().isArrayOrList(parameter.asType());
    }

    private List<Parameter> getSimpleTypeParameters(ExecutableElement executableElement, XStream xStream, DevKitTypeElement typeElement) {
        List<Parameter> parameters = new ArrayList<Parameter>();
        for (VariableElement variableElement : executableElement.getParameters()) {
            Parameter parameter = getParameter(xStream, variableElement);
            String parameterName = variableElement.getSimpleName().toString();
            if (parameter != null) {
                setParameterInfo(executableElement, parameters, variableElement, parameter, parameterName);
            } else if (variableElement.getAnnotation(Session.class) != null) {
                addSessionParameters(xStream, typeElement, parameters);
            }
        }
        return parameters;
    }

    private void addSessionParameters(XStream xStream, DevKitTypeElement typeElement, List<Parameter> parameters) {
        ExecutableElement sessionCreateMethod = typeElement.getMethodsAnnotatedWith(SessionCreate.class).get(0);
        for (VariableElement sessionCreateParameter : sessionCreateMethod.getParameters()) {
            Parameter parameter = getParameter(xStream, sessionCreateParameter);
            String parameterName = sessionCreateParameter.getSimpleName().toString();
            setParameterInfo(sessionCreateMethod, parameters, sessionCreateParameter, parameter, parameterName);
            parameter.setRequired("false");
        }
    }

    private void setParameterInfo(ExecutableElement executableElement, List<Parameter> parameters, VariableElement variableElement, Parameter parameter, String parameterName) {
        parameter.setCaption(formatCaption(context.getNameUtils().friendlyNameFromCamelCase(parameterName)));
        parameter.setDescription(formatDescription(context.getJavaDocUtils().getParameterSummary(parameterName, executableElement)));
        parameter.setName(parameterName);
        setOptionalOrRequired(variableElement, parameter);
        setDefaultValueIfAvailable(variableElement, parameter);
        parameters.add(parameter);
    }

    private List<ChildElement> getChildElementsParameters(ExecutableElement executableElement, XStream xStream, String moduleName) {
        List<ChildElement> parameters = new ArrayList<ChildElement>();
        for (VariableElement variableElement : executableElement.getParameters()) {
            if (context.getTypeMirrorUtils().isCollection(variableElement.asType())) {
                ChildElement childElement = new ChildElement(xStream);
                childElement.setName(URI_PREFIX + moduleName + "/" + context.getNameUtils().uncamel(variableElement.getSimpleName().toString()));
                childElement.setDescription(formatDescription(context.getJavaDocUtils().getParameterSummary(variableElement.getSimpleName().toString(), executableElement)));
                childElement.setCaption(formatCaption(context.getNameUtils().friendlyNameFromCamelCase(variableElement.getSimpleName().toString())));
                if (isNestedCollection(variableElement)) {
                    childElement.setAllowMultiple("true");
                } else {
                    childElement.setAllowMultiple("false");
                }
                childElement.setInplace("true");
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

    private void setDefaultValueIfAvailable(VariableElement variableElement, Parameter parameter) {
        if (variableElement.getAnnotation(Default.class) != null) {
            parameter.setDefaultValue(variableElement.getAnnotation(Default.class).value());
        }
    }

    private void setOptionalOrRequired(VariableElement variableElement, Parameter parameter) {
        if (variableElement.getAnnotation(Optional.class) != null) {
            parameter.setRequired("false");
        } else {
            parameter.setRequired("true");
        }
    }

    private Parameter getParameter(XStream xStream, VariableElement variableElement) {
        String parameterClassName = variableElement.asType().toString();
        if (parameterClassName.equals(String.class.getName())) {
            return new StringParameter(xStream);
        } else if (parameterClassName.equals("boolean") || parameterClassName.equals(Boolean.class.getName())) {
            return new BooleanParameter(xStream);
        } else if (parameterClassName.equals("int") || parameterClassName.equals(Integer.class.getName())) {
            return new IntegerParameter(xStream);
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
        if(Character.isLowerCase(description.charAt(0))) {
            description = StringUtils.capitalize(description);
        }
        if(!description.endsWith(".")) {
           description += '.';
        }
        return description;
    }
}