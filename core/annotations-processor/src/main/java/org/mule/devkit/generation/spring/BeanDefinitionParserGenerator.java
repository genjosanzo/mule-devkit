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

package org.mule.devkit.generation.spring;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.UnhandledException;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.api.callback.HttpCallback;
import org.mule.api.callback.SourceCallback;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.config.PoolingProfile;
import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.config.spring.util.SpringXMLUtils;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.adapter.HttpCallbackAdapterGenerator;
import org.mule.devkit.generation.mule.oauth.DefaultRestoreAccessTokenCallbackFactoryGenerator;
import org.mule.devkit.generation.mule.oauth.DefaultSaveAccessTokenCallbackFactoryGenerator;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.ForEach;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.mule.util.TemplateParser;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;

import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.List;

public class BeanDefinitionParserGenerator extends AbstractMessageGenerator {

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return typeElement.hasAnnotation(Module.class) || typeElement.hasAnnotation(Connector.class);
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {
        generateConfigBeanDefinitionParserFor(typeElement);

        for (ExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            generateBeanDefinitionParserForProcessor(executableElement);
        }

        for (ExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Source.class)) {
            generateBeanDefinitionParserForSource(executableElement);
        }
    }

    private void generateConfigBeanDefinitionParserFor(DevKitTypeElement typeElement) {
        DefinedClass beanDefinitionparser = getConfigBeanDefinitionParserClass(typeElement);
        DefinedClass pojo = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));

        context.note("Generating config element definition parser as " + beanDefinitionparser.fullName() + " for class " + typeElement.getSimpleName().toString());

        FieldVariable patternInfo = generateFieldForPatternInfo(beanDefinitionparser);

        Method constructor = beanDefinitionparser.constructor(Modifier.PUBLIC);
        constructor.body().assign(patternInfo, ref(TemplateParser.class).staticInvoke("createMuleStyleParser").invoke("getStyle"));

        Method parse = beanDefinitionparser.method(Modifier.PUBLIC, ref(BeanDefinition.class), "parse");
        Variable element = parse.param(ref(org.w3c.dom.Element.class), "element");
        Variable parserContext = parse.param(ref(ParserContext.class), "parserContent");

        Variable name = parse.body().decl(ref(String.class), "name", element.invoke("getAttribute").arg("name"));
        Conditional ifNotNamed = parse.body()._if(Op.cor(Op.eq(name, ExpressionFactory._null()),
                ref(StringUtils.class).staticInvoke("isBlank").arg(name)));

        ifNotNamed._then().add(element.invoke("setAttribute")
                .arg("name")
                .arg(ref(AutoIdUtils.class).staticInvoke("getUniqueName").arg(element).arg("mule-bean")));

        Variable builder = parse.body().decl(ref(BeanDefinitionBuilder.class), "builder",
                ref(BeanDefinitionBuilder.class).staticInvoke("rootBeanDefinition").arg(pojo.dotclass().invoke("getName")));

        Conditional isInitialisable = parse.body()._if(ref(Initialisable.class).dotclass()
                .invoke("isAssignableFrom").arg(pojo.dotclass()));
        isInitialisable._then().add(builder.invoke("setInitMethodName").arg(ref(Initialisable.class).staticRef("PHASE_NAME")));

        Conditional isDisposable = parse.body()._if(ref(Disposable.class).dotclass()
                .invoke("isAssignableFrom").arg(pojo.dotclass()));
        isDisposable._then().add(builder.invoke("setDestroyMethodName").arg(ref(Disposable.class).staticRef("PHASE_NAME")));

        for (VariableElement variable : typeElement.getFieldsAnnotatedWith(Configurable.class)) {

            String fieldName = variable.getSimpleName().toString();

            if (SchemaTypeConversion.isSupported(variable.asType().toString())) {
                generateParseSupportedType(parse.body(), element, builder, fieldName);
            } else if (context.getTypeMirrorUtils().isXmlType(variable.asType())) {
                generateParseXmlType(parse.body(), element, builder, fieldName);
            } else if (context.getTypeMirrorUtils().isArrayOrList(variable.asType())) {
                Variable listElement = parse.body().decl(ref(org.w3c.dom.Element.class),
                        fieldName + "ListElement",
                        ExpressionFactory._null());

                parse.body().assign(listElement, ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                        .arg(element)
                        .arg(context.getNameUtils().uncamel(fieldName)));

                UpperBlockClosure managedList = generateParseArrayOrList(parse.body(), variable.asType(), listElement, builder, fieldName, patternInfo, parserContext);

                managedList.getNotRefBlock().add(builder.invoke("addPropertyValue").arg(fieldName).arg(managedList.getManagedCollection()));
            } else if (context.getTypeMirrorUtils().isMap(variable.asType())) {
                Variable listElement = parse.body().decl(ref(org.w3c.dom.Element.class),
                        fieldName + "ListElement",
                        ExpressionFactory._null());

                parse.body().assign(listElement, ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                        .arg(element)
                        .arg(context.getNameUtils().uncamel(fieldName)));

                UpperBlockClosure managedMap = generateParseMap(parse.body(), variable.asType(), listElement, builder, fieldName, patternInfo, parserContext);

                managedMap.getNotRefBlock().add(builder.invoke("addPropertyValue").arg(fieldName).arg(managedMap.getManagedCollection()));
            } else if (context.getTypeMirrorUtils().isEnum(variable.asType())) {
                generateParseEnum(parse.body(), element, builder, fieldName);
            } else {
                // not supported use the -ref approach
                Invocation getAttribute = element.invoke("getAttribute").arg(fieldName + "-ref");
                Conditional ifNotNull = parse.body()._if(Op.cand(Op.ne(getAttribute, ExpressionFactory._null()),
                        Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(
                                getAttribute
                        ))));
                ifNotNull._then().add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                        ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(element.invoke("getAttribute").arg(fieldName + "-ref"))
                ));
            }
        }

        for (VariableElement variable : typeElement.getFieldsAnnotatedWith(Inject.class)) {
            if( variable.asType().toString().equals("org.mule.api.store.ObjectStore") ) {
                Invocation getAttribute = element.invoke("getAttribute").arg("objectStore-ref");
                Conditional ifNotNull = parse.body()._if(Op.cand(Op.ne(getAttribute, ExpressionFactory._null()),
                        Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(
                                getAttribute
                        ))));
                ifNotNull._then().add(builder.invoke("addPropertyValue").arg(variable.getSimpleName().toString()).arg(
                        ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(element.invoke("getAttribute").arg("objectStore-ref"))
                ));
            }
        }

        ExecutableElement connect = connectMethodForClass(typeElement);
        if (connect != null) {
            for (VariableElement variable : connect.getParameters()) {
                String fieldName = variable.getSimpleName().toString();

                if (SchemaTypeConversion.isSupported(variable.asType().toString())) {
                    generateParseSupportedType(parse.body(), element, builder, fieldName);
                } else if (context.getTypeMirrorUtils().isArrayOrList(variable.asType())) {
                    Variable listElement = parse.body().decl(ref(org.w3c.dom.Element.class),
                            fieldName + "ListElement",
                            ExpressionFactory._null());

                    parse.body().assign(listElement, ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                            .arg(element)
                            .arg(context.getNameUtils().uncamel(fieldName)));

                    UpperBlockClosure managedList = generateParseArrayOrList(parse.body(), variable.asType(), listElement, builder, fieldName, patternInfo, parserContext);

                    managedList.getNotRefBlock().add(builder.invoke("addPropertyValue").arg(fieldName).arg(managedList.getManagedCollection()));
                } else if (context.getTypeMirrorUtils().isMap(variable.asType())) {
                    Variable listElement = parse.body().decl(ref(org.w3c.dom.Element.class),
                            fieldName + "ListElement",
                            ExpressionFactory._null());

                    parse.body().assign(listElement, ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                            .arg(element)
                            .arg(context.getNameUtils().uncamel(fieldName)));

                    UpperBlockClosure managedMap = generateParseMap(parse.body(), variable.asType(), listElement, builder, fieldName, patternInfo, parserContext);

                    managedMap.getNotRefBlock().add(builder.invoke("addPropertyValue").arg(fieldName).arg(managedMap.getManagedCollection()));
                } else if (context.getTypeMirrorUtils().isEnum(variable.asType())) {
                    generateParseEnum(parse.body(), element, builder, fieldName);
                }
            }
        }

        if (typeElement.hasAnnotation(OAuth.class) || typeElement.hasAnnotation(OAuth2.class)) {
            generateParseHttpCallback(SchemaGenerator.OAUTH_CALLBACK_CONFIG_ELEMENT_NAME, parse, element, builder);

            DefinedClass saveAccessTokenCallbackFactory = context.getClassForRole(DefaultSaveAccessTokenCallbackFactoryGenerator.ROLE);
            DefinedClass restoreAccessTokenCallbackFactory = context.getClassForRole(DefaultRestoreAccessTokenCallbackFactoryGenerator.ROLE);
            generateParseNestedProcessor(parse.body(), element, parserContext, builder, "oauthSaveAccessToken", false, false, false, saveAccessTokenCallbackFactory);
            generateParseNestedProcessor(parse.body(), element, parserContext, builder, "oauthRestoreAccessToken", false, false, false, restoreAccessTokenCallbackFactory);

            generateGenerateChildBeanNameMethod(beanDefinitionparser);
        }
        if (typeElement.hasProcessorMethodWithParameter(HttpCallback.class)) {
            generateParseHttpCallback(SchemaGenerator.HTTP_CALLBACK_CONFIG_ELEMENT_NAME, parse, element, builder);
        }

        if (connect != null) {
            generateParsePoolingProfile("connection-pooling-profile", "connectionPoolingProfile", parse, element, builder);
        }

        if (typeElement.isPoolable()) {
            generateParsePoolingProfile("pooling-profile", "poolingProfile", parse, element, builder);
        }

        Variable definition = parse.body().decl(ref(BeanDefinition.class), "definition", builder.invoke("getBeanDefinition"));

        parse.body().add(definition.invoke("setAttribute").arg(
                ref(MuleHierarchicalBeanDefinitionParserDelegate.class).staticRef("MULE_NO_RECURSE")).arg(ref(Boolean.class).staticRef("TRUE")));

        parse.body()._return(definition);

    }

    private void generateParseHttpCallback(String elementName, Method parse, Variable element, Variable builder) {
        Variable httpCallbackConfigElement = parse.body().decl(ref(org.w3c.dom.Element.class), "httpCallbackConfigElement", ref(DomUtils.class).staticInvoke("getChildElementByTagName").
                arg(element).arg(elementName));
        Block ifHttpCallbackConfigPresent = parse.body()._if(Op.ne(httpCallbackConfigElement, ExpressionFactory._null()))._then();
        generateParseSupportedType(ifHttpCallbackConfigPresent, httpCallbackConfigElement, builder, HttpCallbackAdapterGenerator.DOMAIN_FIELD_NAME);
        generateParseSupportedType(ifHttpCallbackConfigPresent, httpCallbackConfigElement, builder, HttpCallbackAdapterGenerator.LOCAL_PORT_FIELD_NAME);
        generateParseSupportedType(ifHttpCallbackConfigPresent, httpCallbackConfigElement, builder, HttpCallbackAdapterGenerator.REMOTE_PORT_FIELD_NAME);
        generateParseSupportedType(ifHttpCallbackConfigPresent, httpCallbackConfigElement, builder, HttpCallbackAdapterGenerator.ASYNC_FIELD_NAME);

        Invocation getAttribute = httpCallbackConfigElement.invoke("getAttribute").arg(HttpCallbackAdapterGenerator.CONNECTOR_FIELD_NAME + "-ref");
        Conditional ifNotNull = ifHttpCallbackConfigPresent._if(Op.cand(Op.ne(getAttribute, ExpressionFactory._null()),
                Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(
                        getAttribute
                ))));
        ifNotNull._then().add(builder.invoke("addPropertyValue").arg(HttpCallbackAdapterGenerator.CONNECTOR_FIELD_NAME).arg(
                ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(httpCallbackConfigElement.invoke("getAttribute").arg(HttpCallbackAdapterGenerator.CONNECTOR_FIELD_NAME + "-ref"))
        ));
    }

    private void generateParsePoolingProfile(String elementName, String propertyName, Method parse, Variable element, Variable builder) {
        Variable poolingProfileBuilder = parse.body().decl(ref(BeanDefinitionBuilder.class), propertyName + "Builder",
                ref(BeanDefinitionBuilder.class).staticInvoke("rootBeanDefinition").arg(ref(PoolingProfile.class).dotclass().invoke("getName")));

        Variable poolingProfileElement = parse.body().decl(ref(org.w3c.dom.Element.class), propertyName + "Element",
                ref(DomUtils.class).staticInvoke("getChildElementByTagName").arg(element).arg(elementName));

        Conditional ifElementNotNull = parse.body()._if(Op.ne(poolingProfileElement, ExpressionFactory._null()));

        generateParseSupportedType(ifElementNotNull._then(), poolingProfileElement, poolingProfileBuilder, "maxActive");
        generateParseSupportedType(ifElementNotNull._then(), poolingProfileElement, poolingProfileBuilder, "maxIdle");
        generateParseSupportedType(ifElementNotNull._then(), poolingProfileElement, poolingProfileBuilder, "maxWait");

        Invocation getAttribute = poolingProfileElement.invoke("getAttribute").arg("exhaustedAction");
        Conditional ifNotNull = ifElementNotNull._then()._if(Op.cand(Op.ne(getAttribute, ExpressionFactory._null()),
                Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(
                        getAttribute
                ))));
        ifNotNull._then().add(poolingProfileBuilder.invoke("addPropertyValue").arg("exhaustedAction").arg(
                ref(PoolingProfile.class).staticRef("POOL_EXHAUSTED_ACTIONS").invoke("get").arg(poolingProfileElement.invoke("getAttribute").arg("exhaustedAction"))
        ));

        getAttribute = poolingProfileElement.invoke("getAttribute").arg("exhaustedAction");
        ifNotNull = ifElementNotNull._then()._if(Op.cand(Op.ne(getAttribute, ExpressionFactory._null()),
                Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(
                        getAttribute
                ))));
        ifNotNull._then().add(poolingProfileBuilder.invoke("addPropertyValue").arg("initialisationPolicy").arg(
                ref(PoolingProfile.class).staticRef("POOL_INITIALISATION_POLICIES").invoke("get").arg(poolingProfileElement.invoke("getAttribute").arg("initialisationPolicy"))
        ));

        ifElementNotNull._then().add(builder.invoke("addPropertyValue").arg(propertyName).arg(
                poolingProfileBuilder.invoke("getBeanDefinition")
        ));
    }

    private void generateBeanDefinitionParserForSource(ExecutableElement executableElement) {
        // get class
        DefinedClass beanDefinitionparser = getBeanDefinitionParserClass(executableElement);
        DefinedClass messageSourceClass = getMessageSourceClass(executableElement);

        FieldVariable patternInfo = generateFieldForPatternInfo(beanDefinitionparser);

        context.note("Generating bean definition parser as " + beanDefinitionparser.fullName() + " for message source " + messageSourceClass.fullName());

        Method constructor = beanDefinitionparser.constructor(Modifier.PUBLIC);
        constructor.body().assign(patternInfo, ref(TemplateParser.class).staticInvoke("createMuleStyleParser").invoke("getStyle"));

        generateSourceParseMethod(beanDefinitionparser, messageSourceClass, executableElement, patternInfo);

        generateGenerateChildBeanNameMethod(beanDefinitionparser);
    }

    private void generateBeanDefinitionParserForProcessor(ExecutableElement executableElement) {
        DefinedClass beanDefinitionparser = getBeanDefinitionParserClass(executableElement);
        DefinedClass messageProcessorClass;

        if (executableElement.getAnnotation(Processor.class).intercepting()) {
            messageProcessorClass = getInterceptingMessageProcessorClass(executableElement);
        } else {
            messageProcessorClass = getMessageProcessorClass(executableElement);
        }

        context.note("Generating bean definition parser as " + beanDefinitionparser.fullName() + " for message processor " + messageProcessorClass.fullName());

        FieldVariable patternInfo = generateFieldForPatternInfo(beanDefinitionparser);

        Method constructor = beanDefinitionparser.constructor(Modifier.PUBLIC);
        constructor.body().assign(patternInfo, ref(TemplateParser.class).staticInvoke("createMuleStyleParser").invoke("getStyle"));

        generateProcessorParseMethod(beanDefinitionparser, messageProcessorClass, executableElement, patternInfo);

        generateGenerateChildBeanNameMethod(beanDefinitionparser);
    }

    private void generateProcessorParseMethod(DefinedClass beanDefinitionparser, DefinedClass messageProcessorClass, ExecutableElement executableElement, Variable patternInfo) {
        Method parse = beanDefinitionparser.method(Modifier.PUBLIC, ref(BeanDefinition.class), "parse");
        Variable element = parse.param(ref(org.w3c.dom.Element.class), "element");
        Variable parserContext = parse.param(ref(ParserContext.class), "parserContent");

        Variable definition = generateParseCommon(beanDefinitionparser, messageProcessorClass, executableElement, parse, element, patternInfo, parserContext);

        generateAttachMessageProcessor(parse, definition, parserContext);

        parse.body()._return(definition);
    }

    private void generateSourceParseMethod(DefinedClass beanDefinitionparser, DefinedClass messageProcessorClass, ExecutableElement executableElement, Variable patternInfo) {
        Method parse = beanDefinitionparser.method(Modifier.PUBLIC, ref(BeanDefinition.class), "parse");
        Variable element = parse.param(ref(org.w3c.dom.Element.class), "element");
        Variable parserContext = parse.param(ref(ParserContext.class), "parserContent");

        Variable definition = generateParseCommon(beanDefinitionparser, messageProcessorClass, executableElement, parse, element, patternInfo, parserContext);

        generateAttachMessageSource(parse, definition, parserContext);

        parse.body()._return(definition);
    }

    private Variable generateParseCommon(DefinedClass beanDefinitionparser, DefinedClass messageProcessorClass, ExecutableElement executableElement, Method parse, Variable element, Variable patternInfo, Variable parserContext) {
        Variable builder = parse.body().decl(ref(BeanDefinitionBuilder.class), "builder",
                ref(BeanDefinitionBuilder.class).staticInvoke("rootBeanDefinition").arg(messageProcessorClass.dotclass().invoke("getName")));

        Variable configRef = parse.body().decl(ref(String.class), "configRef", element.invoke("getAttribute").arg("config-ref"));
        Conditional ifConfigRef = parse.body()._if(Op.cand(Op.ne(configRef, ExpressionFactory._null()),
                Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(configRef))));
        ifConfigRef._then().add(builder.invoke("addPropertyValue").arg("moduleObject").arg(
                configRef));

        Method getAttributeValue = generateGetAttributeValue(beanDefinitionparser);

        int requiredChildElements = 0;
        for (VariableElement variable : executableElement.getParameters()) {
            if (context.getTypeMirrorUtils().ignoreParameter(variable)) {
                continue;
            }
            if (context.getTypeMirrorUtils().isNestedProcessor(variable.asType())) {
                requiredChildElements++;
            } else if (context.getTypeMirrorUtils().isXmlType(variable.asType())) {
                requiredChildElements++;
            } else if (context.getTypeMirrorUtils().isCollection(variable.asType())) {
                requiredChildElements++;
            }
        }

        for (VariableElement variable : executableElement.getParameters()) {
            if (variable.asType().toString().startsWith(SourceCallback.class.getName())) {
                continue;
            }

            String fieldName = variable.getSimpleName().toString();

            if (context.getTypeMirrorUtils().isNestedProcessor(variable.asType())) {
                boolean isList = context.getTypeMirrorUtils().isArrayOrList(variable.asType());
                if (requiredChildElements == 1) {
                    generateParseNestedProcessor(parse.body(), element, parserContext, builder, fieldName, true, isList, true, ref(MessageProcessorChainFactoryBean.class));
                } else {
                    generateParseNestedProcessor(parse.body(), element, parserContext, builder, fieldName, false, isList, true, ref(MessageProcessorChainFactoryBean.class));
                }
            } else if (SchemaTypeConversion.isSupported(variable.asType().toString())) {
                generateParseSupportedType(parse.body(), element, builder, fieldName);
            } else if (context.getTypeMirrorUtils().isXmlType(variable.asType())) {
                generateParseXmlType(parse.body(), element, builder, fieldName);
            } else if (context.getTypeMirrorUtils().isArrayOrList(variable.asType())) {
                Variable listElement = parse.body().decl(ref(org.w3c.dom.Element.class),
                        fieldName + "ListElement",
                        ExpressionFactory._null());

                parse.body().assign(listElement, ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                        .arg(element)
                        .arg(context.getNameUtils().uncamel(fieldName)));

                UpperBlockClosure managedList = generateParseArrayOrList(parse.body(), variable.asType(), listElement, builder, fieldName, patternInfo, parserContext);

                managedList.getNotRefBlock().add(builder.invoke("addPropertyValue").arg(fieldName).arg(managedList.getManagedCollection()));
            } else if (context.getTypeMirrorUtils().isMap(variable.asType())) {
                Variable listElement = parse.body().decl(ref(org.w3c.dom.Element.class),
                        fieldName + "ListElement",
                        ExpressionFactory._null());

                parse.body().assign(listElement, ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                        .arg(element)
                        .arg(context.getNameUtils().uncamel(fieldName)));

                UpperBlockClosure managedMap = generateParseMap(parse.body(), variable.asType(), listElement, builder, fieldName, patternInfo, parserContext);

                managedMap.getNotRefBlock().add(builder.invoke("addPropertyValue").arg(fieldName).arg(managedMap.getManagedCollection()));
            } else if (context.getTypeMirrorUtils().isEnum(variable.asType())) {
                generateParseEnum(parse.body(), element, builder, fieldName);
            } else if (variable.asType().toString().startsWith(HttpCallback.class.getName())) {
                Variable callbackFlowName = parse.body().decl(ref(String.class), fieldName + "CallbackFlowName", ExpressionFactory.invoke(getAttributeValue).arg(element).arg(context.getNameUtils().uncamel(fieldName) + "-flow-ref"));
                Block block = parse.body()._if(Op.ne(callbackFlowName, ExpressionFactory._null()))._then();
                block.invoke(builder, "addPropertyValue").arg(fieldName + "CallbackFlow").arg(ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(callbackFlowName));
            } else {
                // not supported use the -ref approach
                Invocation getAttribute = element.invoke("getAttribute").arg(fieldName + "-ref");
                Conditional ifNotNull = parse.body()._if(Op.cand(Op.ne(getAttribute, ExpressionFactory._null()),
                        Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(
                                getAttribute
                        ))));
                Conditional ifNotExpression = ifNotNull._then()._if(getAttribute.invoke("startsWith").arg("#"));

                ifNotExpression._else().add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                        Op.plus(Op.plus(ExpressionFactory.lit("#[registry:"),
                                element.invoke("getAttribute").arg(fieldName + "-ref")),
                                ExpressionFactory.lit("]"))
                ));

                ifNotExpression._then().add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                        element.invoke("getAttribute").arg(fieldName + "-ref")
                ));
            }
        }

        ExecutableElement connectMethod = connectForMethod(executableElement);
        if (connectMethod != null) {
            generateParseSupportedType(parse.body(), element, builder, "retryMax");

            for (VariableElement variable : connectMethod.getParameters()) {
                String fieldName = variable.getSimpleName().toString();

                if (SchemaTypeConversion.isSupported(variable.asType().toString())) {
                    generateParseSupportedType(parse.body(), element, builder, fieldName);
                } else if (context.getTypeMirrorUtils().isArrayOrList(variable.asType())) {
                    Variable listElement = parse.body().decl(ref(org.w3c.dom.Element.class),
                            fieldName + "ListElement",
                            ExpressionFactory._null());

                    parse.body().assign(listElement, ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                            .arg(element)
                            .arg(context.getNameUtils().uncamel(fieldName)));

                    UpperBlockClosure managedList = generateParseArrayOrList(parse.body(), variable.asType(), listElement, builder, fieldName, patternInfo, parserContext);

                    managedList.getNotRefBlock().add(builder.invoke("addPropertyValue").arg(fieldName).arg(managedList.getManagedCollection()));
                } else if (context.getTypeMirrorUtils().isMap(variable.asType())) {
                    Variable listElement = parse.body().decl(ref(org.w3c.dom.Element.class),
                            fieldName + "ListElement",
                            ExpressionFactory._null());

                    parse.body().assign(listElement, ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                            .arg(element)
                            .arg(context.getNameUtils().uncamel(fieldName)));

                    UpperBlockClosure managedMap = generateParseMap(parse.body(), variable.asType(), listElement, builder, fieldName, patternInfo, parserContext);

                    managedMap.getNotRefBlock().add(builder.invoke("addPropertyValue").arg(fieldName).arg(managedMap.getManagedCollection()));
                } else if (context.getTypeMirrorUtils().isEnum(variable.asType())) {
                    generateParseEnum(parse.body(), element, builder, fieldName);
                }
            }
        }

        Variable definition = parse.body().decl(ref(BeanDefinition.class), "definition", builder.invoke("getBeanDefinition"));

        parse.body().add(definition.invoke("setAttribute").arg(
                ref(MuleHierarchicalBeanDefinitionParserDelegate.class).staticRef("MULE_NO_RECURSE")).arg(ref(Boolean.class).staticRef("TRUE")));
        return definition;
    }

    private void generateParseNestedProcessor(Block block, Variable element, Variable parserContext, Variable builder, String fieldName, boolean skipElement, boolean isList, boolean allowTextAttribute, TypeReference factoryBean) {

        Variable elements = element;
        if (!skipElement) {
            elements = block.decl(ref(org.w3c.dom.Element.class), fieldName + "Element",
                    ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                            .arg(element).arg(context.getNameUtils().uncamel(fieldName)));
        }

        Conditional ifNotNull = block._if(Op.ne(elements, ExpressionFactory._null()));

        Block beanDefinitionBuidlerBlock = null;
        if (allowTextAttribute) {
            Variable text = ifNotNull._then().decl(ref(String.class), "text", elements.invoke("getAttribute").arg("text"));
            Conditional ifTextElement = ifNotNull._then()._if(Op.cand(Op.ne(text, ExpressionFactory._null()),
                    Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(text))));

            ifTextElement._then().add(builder.invoke("addPropertyValue")
                    .arg(fieldName).arg(text));

            beanDefinitionBuidlerBlock = ifTextElement._else();
        } else {
            beanDefinitionBuidlerBlock = ifNotNull._then();
        }

        Variable beanDefinitionBuilder = beanDefinitionBuidlerBlock.decl(ref(BeanDefinitionBuilder.class), fieldName + "BeanDefinitionBuilder",
                ref(BeanDefinitionBuilder.class).staticInvoke("rootBeanDefinition")
                        .arg(factoryBean.dotclass()));
        Variable beanDefinition = beanDefinitionBuidlerBlock.decl(ref(BeanDefinition.class), fieldName + "BeanDefinition",
                beanDefinitionBuilder.invoke("getBeanDefinition"));

        beanDefinitionBuidlerBlock.add(parserContext.invoke("getRegistry").invoke("registerBeanDefinition")
                .arg(ExpressionFactory.invoke("generateChildBeanName").arg(elements))
                .arg(beanDefinition));

        beanDefinitionBuidlerBlock.add(elements.invoke("setAttribute")
                .arg("name").arg(ExpressionFactory.invoke("generateChildBeanName").arg(elements)));

        beanDefinitionBuidlerBlock.add(beanDefinitionBuilder.invoke("setSource").arg(parserContext.invoke("extractSource")
                .arg(elements)));

        beanDefinitionBuidlerBlock.add(beanDefinitionBuilder.invoke("setScope")
                .arg(ref(BeanDefinition.class).staticRef("SCOPE_SINGLETON")));

        Variable list = beanDefinitionBuidlerBlock.decl(ref(List.class), fieldName + "List",
                parserContext.invoke("getDelegate").invoke("parseListElement")
                        .arg(elements).arg(beanDefinitionBuilder.invoke("getBeanDefinition")));

        beanDefinitionBuidlerBlock.add(parserContext.invoke("getRegistry").invoke("removeBeanDefinition")
                .arg(ExpressionFactory.invoke("generateChildBeanName").arg(elements)));

        if (!isList) {
            beanDefinitionBuidlerBlock.add(builder.invoke("addPropertyValue").arg(fieldName)
                    .arg(beanDefinition));
        } else {
            beanDefinitionBuidlerBlock.add(builder.invoke("addPropertyValue").arg(fieldName)
                    .arg(list));
        }
    }

    private void generateParseXmlType(Block block, Variable element, Variable builder, String fieldName) {
        Variable xmlElement = block.decl(ref(org.w3c.dom.Element.class),
                fieldName + "xmlElement",
                ExpressionFactory._null());

        block.assign(xmlElement, ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                .arg(element)
                .arg(context.getNameUtils().uncamel(fieldName)));

        Conditional xmlElementNotNull = block._if(Op.ne(xmlElement, ExpressionFactory._null()));

        TryStatement tryBlock = xmlElementNotNull._then()._try();

        Variable xmlElementChilds = tryBlock.body().decl(ref(List.class).narrow(org.w3c.dom.Element.class), "xmlElementChilds",
                ref(DomUtils.class).staticInvoke("getChildElements").arg(xmlElement));

        Conditional xmlElementChildsNotEmpty = tryBlock.body()._if(Op.gt(xmlElementChilds.invoke("size"), ExpressionFactory.lit(0)));

        Variable domSource = xmlElementChildsNotEmpty._then().decl(ref(DOMSource.class), "domSource", ExpressionFactory._new(ref(DOMSource.class)).arg(
                xmlElementChilds.invoke("get").arg(ExpressionFactory.lit(0))));
        Variable stringWriter = xmlElementChildsNotEmpty._then().decl(ref(StringWriter.class), "stringWriter", ExpressionFactory._new(ref(StringWriter.class)));
        Variable streamResult = xmlElementChildsNotEmpty._then().decl(ref(StreamResult.class), "result", ExpressionFactory._new(ref(StreamResult.class)).arg(stringWriter));
        Variable tf = xmlElementChildsNotEmpty._then().decl(ref(TransformerFactory.class), "tf", ref(TransformerFactory.class).staticInvoke("newInstance"));
        Variable transformer = xmlElementChildsNotEmpty._then().decl(ref(Transformer.class), "transformer", tf.invoke("newTransformer"));
        Invocation transform = transformer.invoke("transform");
        transform.arg(domSource);
        transform.arg(streamResult);
        xmlElementChildsNotEmpty._then().add(transform);
        xmlElementChildsNotEmpty._then().add(stringWriter.invoke("flush"));

        xmlElementChildsNotEmpty._then().add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                stringWriter.invoke("toString")));

        generateReThrow(tryBlock, TransformerConfigurationException.class);
        generateReThrow(tryBlock, TransformerException.class);
        generateReThrow(tryBlock, TransformerFactoryConfigurationError.class);
    }

    private void generateReThrow(TryStatement tryBlock, Class<?> clazz) {
        CatchBlock catchBlock = tryBlock._catch(ref(clazz).boxify());
        Variable e = catchBlock.param("e");
        catchBlock.body()._throw(ExpressionFactory._new(ref(UnhandledException.class)).arg(e));
    }

    private void generateParseSupportedType(Block block, Variable element, Variable builder, String fieldName) {
        Invocation getAttribute = element.invoke("getAttribute").arg(fieldName);
        Conditional ifNotNull = block._if(Op.cand(Op.ne(getAttribute, ExpressionFactory._null()),
                Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(
                        getAttribute
                ))));
        ifNotNull._then().add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                element.invoke("getAttribute").arg(fieldName)
        ));
    }

    private void generateParseEnum(Block block, Variable element, Variable builder, String fieldName) {
        Invocation hasAttribute = element.invoke("hasAttribute").arg(fieldName);
        Conditional ifNotNull = block._if(hasAttribute);
        ifNotNull._then().add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                element.invoke("getAttribute").arg(fieldName)
        ));
    }

    private UpperBlockClosure generateParseMap(Block body, TypeMirror typeMirror, Variable listElement, Variable builder, String fieldName, Variable patternInfo, Variable parserContext) {
        DeclaredType variableType = (DeclaredType) typeMirror;
        List<? extends TypeMirror> variableTypeParameters = variableType.getTypeArguments();

        Variable listChilds = body.decl(ref(List.class).narrow(ref(org.w3c.dom.Element.class)),
                fieldName.replace("-", "") + "ListChilds",
                ExpressionFactory._null());

        Conditional listElementNotNull = body._if(Op.ne(listElement, ExpressionFactory._null()));

        Invocation getElementRef = listElement.invoke("getAttribute").arg("ref");
        Variable ref = listElementNotNull._then().decl(ref(String.class), fieldName.replace("-", "") + "Ref",
                getElementRef);

        Conditional ifRef = listElementNotNull._then()._if(
                Op.cand(
                        Op.ne(ref, ExpressionFactory._null()),
                        Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(ref))
                )
        );

        Conditional ifRefNotExpresion = ifRef._then()._if(Op.cand(
                Op.not(ref.invoke("startsWith").arg(patternInfo.invoke("getPrefix"))),
                Op.not(ref.invoke("endsWith").arg(patternInfo.invoke("getSuffix")))
        ));

        ifRefNotExpresion._then().add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(ref)
        ));

        ifRefNotExpresion._else().add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                ref));

        Variable managedMap = ifRef._else().decl(ref(ManagedMap.class), fieldName.replace("-", ""),
                ExpressionFactory._new(ref(ManagedMap.class)));

        ifRef._else().assign(listChilds,
                ref(DomUtils.class).staticInvoke("getChildElementsByTagName")
                        .arg(listElement)
                        .arg(context.getNameUtils().uncamel(context.getNameUtils().singularize(fieldName))));

        String childName = context.getNameUtils().uncamel(context.getNameUtils().singularize(fieldName));

        Conditional listChildsNotNull = ifRef._else()._if(Op.ne(listChilds, ExpressionFactory._null()));

        Conditional isListEmpty = listChildsNotNull._then()._if(Op.eq(listChilds.invoke("size"), ExpressionFactory.lit(0)));

        isListEmpty._then().assign(listChilds, ref(DomUtils.class).staticInvoke("getChildElements").arg(
                listElement
        ));

        ForEach forEach = listChildsNotNull._then().forEach(ref(org.w3c.dom.Element.class), fieldName.replace("-", "") + "Child", listChilds);

        Invocation getValueRef = forEach.var().invoke("getAttribute").arg("value-ref");
        Invocation getKeyRef = forEach.var().invoke("getAttribute").arg("key-ref");
        Variable valueRef = forEach.body().decl(ref(String.class), fieldName.replace("-", "") + "ValueRef",
                getValueRef);
        Variable keyRef = forEach.body().decl(ref(String.class), fieldName.replace("-", "") + "KeyRef",
                getKeyRef);

        Variable valueObject = forEach.body().decl(ref(Object.class), "valueObject",
                ExpressionFactory._null());
        Variable keyObject = forEach.body().decl(ref(Object.class), "keyObject",
                ExpressionFactory._null());

        Conditional ifValueRef = forEach.body()._if(Op.cand(Op.ne(valueRef, ExpressionFactory._null()),
                Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(valueRef))));

        ifValueRef._then().assign(valueObject,
                ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(valueRef));

        Block ifNotValueRef = ifValueRef._else();
        if (variableTypeParameters.size() > 1 && context.getTypeMirrorUtils().isArrayOrList(variableTypeParameters.get(1))) {
            UpperBlockClosure subList = generateParseArrayOrList(ifNotValueRef, variableTypeParameters.get(1), forEach.var(), builder, "inner-" + childName, patternInfo, parserContext);

            subList.getNotRefBlock().assign(valueObject, subList.getManagedCollection());
        } else if (variableTypeParameters.size() > 1 && context.getTypeMirrorUtils().isMap(variableTypeParameters.get(1))) {
            UpperBlockClosure subMap = generateParseMap(ifNotValueRef, variableTypeParameters.get(1), forEach.var(), builder, "inner-" + childName, patternInfo, parserContext);

            subMap.getNotRefBlock().assign(valueObject, subMap.getManagedCollection());
        } else {
            ifNotValueRef.assign(valueObject,
                    forEach.var().invoke("getTextContent"));
        }

        Conditional ifKeyRef = forEach.body()._if(Op.cand(Op.ne(keyRef, ExpressionFactory._null()),
                Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(keyRef))));

        ifKeyRef._then().assign(keyObject,
                ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(keyRef));

        ifKeyRef._else().assign(keyObject,
                forEach.var().invoke("getAttribute").arg("key"));

        Conditional noKey = forEach.body()._if(Op.cor(
                Op.eq(keyObject, ExpressionFactory._null()),
                Op.cand(Op._instanceof(keyObject, ref(String.class)),
                        ref(StringUtils.class).staticInvoke("isBlank").arg(
                                ExpressionFactory.cast(ref(String.class), keyObject)
                        ))
        ));

        noKey._then().assign(keyObject, forEach.var().invoke("getTagName"));

        forEach.body().add(managedMap.invoke("put").arg(keyObject).arg(valueObject));

        return new UpperBlockClosure(managedMap, ifRef._else());
    }

    private UpperBlockClosure generateParseArrayOrList(Block body, TypeMirror typeMirror, Variable listElement, Variable builder, String fieldName, Variable patternInfo, Variable parserContext) {
        DeclaredType variableType = (DeclaredType) typeMirror;
        List<? extends TypeMirror> variableTypeParameters = variableType.getTypeArguments();

        Variable listChilds = body.decl(ref(List.class).narrow(ref(org.w3c.dom.Element.class)),
                fieldName.replace("-", "") + "ListChilds",
                ExpressionFactory._null());

        Conditional listElementNotNull = body._if(Op.ne(listElement, ExpressionFactory._null()));

        Invocation getElementRef = listElement.invoke("getAttribute").arg("ref");
        Variable ref = listElementNotNull._then().decl(ref(String.class), fieldName.replace("-", "") + "Ref",
                getElementRef);

        Conditional ifRef = listElementNotNull._then()._if(Op.cand(Op.ne(ref, ExpressionFactory._null()),
                Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(ref))));

        Conditional ifRefNotExpresion = ifRef._then()._if(Op.cand(
                Op.not(ref.invoke("startsWith").arg(patternInfo.invoke("getPrefix"))),
                Op.not(ref.invoke("endsWith").arg(patternInfo.invoke("getSuffix")))
        ));

        ifRefNotExpresion._then().add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(ref)
        ));

        ifRefNotExpresion._else().add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                ref));

        Variable managedList = ifRef._else().decl(ref(ManagedList.class), fieldName.replace("-", ""),
                ExpressionFactory._new(ref(ManagedList.class)));

        String childName = context.getNameUtils().uncamel(context.getNameUtils().singularize(fieldName));

        ifRef._else().assign(listChilds,
                ref(DomUtils.class).staticInvoke("getChildElementsByTagName")
                        .arg(listElement)
                        .arg(childName));

        Conditional listChildsNotNull = ifRef._else()._if(Op.ne(listChilds, ExpressionFactory._null()));

        ForEach forEach = listChildsNotNull._then().forEach(ref(org.w3c.dom.Element.class), fieldName.replace("-", "") + "Child", listChilds);

        Invocation getValueRef = forEach.var().invoke("getAttribute").arg("value-ref");
        Variable valueRef = forEach.body().decl(ref(String.class), "valueRef",
                getValueRef);

        Conditional ifValueRef = forEach.body()._if(Op.cand(Op.ne(valueRef, ExpressionFactory._null()),
                Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(valueRef))));

        ifValueRef._then().add(
                managedList.invoke("add").arg(
                        ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(valueRef)));

        if (!variableTypeParameters.isEmpty() && context.getTypeMirrorUtils().isArrayOrList(variableTypeParameters.get(0))) {
            UpperBlockClosure subList = generateParseArrayOrList(ifValueRef._else(), variableTypeParameters.get(0), forEach.var(), builder, "inner-" + childName, patternInfo, parserContext);

            subList.getNotRefBlock().add(
                    managedList.invoke("add").arg(subList.getManagedCollection()));
        } else if (!variableTypeParameters.isEmpty() && context.getTypeMirrorUtils().isMap(variableTypeParameters.get(0))) {
            UpperBlockClosure subMap = generateParseMap(ifValueRef._else(), variableTypeParameters.get(0), forEach.var(), builder, "inner-" + childName, patternInfo, parserContext);

            subMap.getNotRefBlock().add(
                    managedList.invoke("add").arg(subMap.getManagedCollection()));
        } else {
            ifValueRef._else().add(
                    managedList.invoke("add").arg(forEach.var().invoke("getTextContent")));
        }

        return new UpperBlockClosure(managedList, ifRef._else());
    }


    private void generateAttachMessageSource(Method parse, Variable definition, Variable parserContext) {
        Variable propertyValues = parse.body().decl(ref(MutablePropertyValues.class), "propertyValues",
                parserContext.invoke("getContainingBeanDefinition").invoke("getPropertyValues"));

        parse.body().add(propertyValues.invoke("addPropertyValue").arg("messageSource").arg(
                definition
        ));
    }

    private void generateGenerateChildBeanNameMethod(DefinedClass beanDefinitionparser) {
        Method generateChildBeanName = beanDefinitionparser.method(Modifier.PRIVATE, ref(String.class), "generateChildBeanName");
        Variable element = generateChildBeanName.param(ref(org.w3c.dom.Element.class), "element");

        Variable id = generateChildBeanName.body().decl(ref(String.class), "id", ref(SpringXMLUtils.class).staticInvoke("getNameOrId").arg(element));
        Conditional isBlank = generateChildBeanName.body()._if(ref(StringUtils.class).staticInvoke("isBlank").arg(id));
        Invocation getParentName = ref(SpringXMLUtils.class).staticInvoke("getNameOrId").arg(ExpressionFactory.cast(
                ref(org.w3c.dom.Element.class), element.invoke("getParentNode")
        ));
        Variable parentId = isBlank._then().decl(ref(String.class), "parentId", getParentName);
        isBlank._then()._return(Op.plus(Op.plus(Op.plus(ExpressionFactory.lit("."), parentId), ExpressionFactory.lit(":")), element.invoke("getLocalName")));
        isBlank._else()._return(id);
    }


    private class UpperBlockClosure {
        private Variable managedCollection;
        private Block notRefBlock;

        private UpperBlockClosure(Variable managedCollection, Block notRefBlock) {
            this.managedCollection = managedCollection;
            this.notRefBlock = notRefBlock;
        }

        public Variable getManagedCollection() {
            return managedCollection;
        }

        public void setManagedCollection(Variable managedCollection) {
            this.managedCollection = managedCollection;
        }

        public Block getNotRefBlock() {
            return notRefBlock;
        }

        public void setNotRefBlock(Block notRefBlock) {
            this.notRefBlock = notRefBlock;
        }
    }
}
