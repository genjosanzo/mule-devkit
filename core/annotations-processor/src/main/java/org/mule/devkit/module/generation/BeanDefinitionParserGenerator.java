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

package org.mule.devkit.module.generation;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.UnhandledException;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.callback.ProcessorCallback;
import org.mule.api.annotations.callback.SourceCallback;
import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.util.SpringXMLUtils;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.Expression;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.ForEach;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Variable;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
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

    public void generate(Element type) throws GenerationException {
        generateConfigBeanDefinitionParserFor(type);

        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Processor processor = executableElement.getAnnotation(Processor.class);

            if (processor == null)
                continue;

            generateBeanDefinitionParserForProcessor(executableElement);
        }

        for (ExecutableElement executableElement : executableElements) {
            Source source = executableElement.getAnnotation(Source.class);

            if (source == null)
                continue;

            generateBeanDefinitionParserForSource(executableElement);
        }

    }

    private void generateConfigBeanDefinitionParserFor(Element type) {
        DefinedClass beanDefinitionparser = getConfigBeanDefinitionParserClass(type);
        DefinedClass pojo = context.getClassForRole(context.getNameUtils().generatePojoRoleKey((TypeElement) type));

        Method parse = beanDefinitionparser.method(Modifier.PUBLIC, ref(BeanDefinition.class), "parse");
        Variable element = parse.param(ref(org.w3c.dom.Element.class), "element");
        Variable parserContext = parse.param(ref(ParserContext.class), "parserContent");

        Variable builder = parse.body().decl(ref(BeanDefinitionBuilder.class), "builder",
                ref(BeanDefinitionBuilder.class).staticInvoke("rootBeanDefinition").arg(pojo.dotclass().invoke("getName")));

        java.util.List<VariableElement> variables = ElementFilter.fieldsIn(type.getEnclosedElements());
        for (VariableElement variable : variables) {
            Configurable configurable = variable.getAnnotation(Configurable.class);

            if (configurable == null)
                continue;

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

                UpperBlockClosure managedList = generateParseArrayOrList(parse.body(), variable.asType(), listElement, builder, fieldName);

                managedList.getNotRefBlock().add(builder.invoke("addPropertyValue").arg(fieldName).arg(managedList.getManagedCollection()));
            } else if (context.getTypeMirrorUtils().isMap(variable.asType())) {
                Variable listElement = parse.body().decl(ref(org.w3c.dom.Element.class),
                        fieldName + "ListElement",
                        ExpressionFactory._null());

                parse.body().assign(listElement, ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                        .arg(element)
                        .arg(context.getNameUtils().uncamel(fieldName)));

                UpperBlockClosure managedMap = generateParseMap(parse.body(), variable.asType(), listElement, builder, fieldName);

                managedMap.getNotRefBlock().add(builder.invoke("addPropertyValue").arg(fieldName).arg(managedMap.getManagedCollection()));
            } else if (context.getTypeMirrorUtils().isEnum(variable.asType())) {
                generateParseEnum(parse.body(), element, builder, fieldName);
            }
        }

        Variable definition = parse.body().decl(ref(BeanDefinition.class), "definition", builder.invoke("getBeanDefinition"));

        parse.body().add(definition.invoke("setAttribute").arg(
                ref(MuleHierarchicalBeanDefinitionParserDelegate.class).staticRef("MULE_NO_RECURSE")).arg(ref(Boolean.class).staticRef("TRUE")));

        parse.body()._return(definition);

    }

    private void generateBeanDefinitionParserForSource(ExecutableElement executableElement) {
        // get class
        DefinedClass beanDefinitionparser = getBeanDefinitionParserClass(executableElement);
        DefinedClass messageSourceClass = getMessageSourceClass(executableElement);

        generateSourceParseMethod(beanDefinitionparser, messageSourceClass, executableElement);

        generateGenerateChildBeanNameMethod(beanDefinitionparser);
    }

    private void generateGetBeanClass(DefinedClass beanDefinitionparser, Expression expr) {
        Method getBeanClass = beanDefinitionparser.method(Modifier.PROTECTED, ref(Class.class), "getBeanClass");
        Variable element = getBeanClass.param(ref(org.w3c.dom.Element.class), "element");
        getBeanClass.body()._return(expr);
    }

    private void generateBeanDefinitionParserForProcessor(ExecutableElement executableElement) {
        // get class
        DefinedClass beanDefinitionparser = getBeanDefinitionParserClass(executableElement);
        DefinedClass messageProcessorClass = getMessageProcessorClass(executableElement);

        generateProcessorParseMethod(beanDefinitionparser, messageProcessorClass, executableElement);

        generateGenerateChildBeanNameMethod(beanDefinitionparser);
    }

    private void generateProcessorParseMethod(DefinedClass beanDefinitionparser, DefinedClass messageProcessorClass, ExecutableElement executableElement) {
        Method parse = beanDefinitionparser.method(Modifier.PUBLIC, ref(BeanDefinition.class), "parse");
        Variable element = parse.param(ref(org.w3c.dom.Element.class), "element");
        Variable parserContext = parse.param(ref(ParserContext.class), "parserContent");

        Variable definition = generateParseCommon(messageProcessorClass, executableElement, parse, element, parserContext);

        generateAttachMessageProcessor(parse, definition, parserContext);

        parse.body()._return(definition);
    }

    private void generateSourceParseMethod(DefinedClass beanDefinitionparser, DefinedClass messageProcessorClass, ExecutableElement executableElement) {
        Method parse = beanDefinitionparser.method(Modifier.PUBLIC, ref(BeanDefinition.class), "parse");
        Variable element = parse.param(ref(org.w3c.dom.Element.class), "element");
        Variable parserContext = parse.param(ref(ParserContext.class), "parserContent");

        Variable definition = generateParseCommon(messageProcessorClass, executableElement, parse, element, parserContext);

        generateAttachMessageSource(parse, definition, parserContext);

        parse.body()._return(definition);
    }

    private Variable generateParseCommon(DefinedClass messageProcessorClass, ExecutableElement executableElement, Method parse, Variable element, Variable parserContext) {
        Variable builder = parse.body().decl(ref(BeanDefinitionBuilder.class), "builder",
                ref(BeanDefinitionBuilder.class).staticInvoke("rootBeanDefinition").arg(messageProcessorClass.dotclass().invoke("getName")));

        Variable configRef = parse.body().decl(ref(String.class), "configRef", element.invoke("getAttribute").arg("config-ref"));
        Conditional ifConfigRef = parse.body()._if(Op.cand(Op.ne(configRef, ExpressionFactory._null()),
                Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(configRef))));
        ifConfigRef._then().add(builder.invoke("addPropertyValue").arg("pojo").arg(
                ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(configRef))
        );

        for (VariableElement variable : executableElement.getParameters()) {
            if (variable.asType().toString().contains(SourceCallback.class.getName()))
                continue;

            String fieldName = variable.getSimpleName().toString();

            if (variable.asType().toString().contains(ProcessorCallback.class.getName())) {
                generateParseProcessorCallback(parse, element, parserContext, builder, fieldName);
            } else if (SchemaTypeConversion.isSupported(variable.asType().toString())) {
                generateParseSupportedType(parse.body(), element, builder, fieldName);
            } else if (context.getTypeMirrorUtils().isPojo(variable.asType())) {
                Variable pojoElement = parse.body().decl(ref(org.w3c.dom.Element.class),
                        fieldName + "PojoElement",
                        ExpressionFactory._null());

                parse.body().assign(pojoElement, ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                        .arg(element)
                        .arg(context.getNameUtils().uncamel(fieldName)));

                Variable beanDefinitionBuilder = generateParsePojo(variable.asType(), parse.body(), pojoElement);

                parse.body().add(builder.invoke("addPropertyValue").arg(fieldName).arg(beanDefinitionBuilder.invoke("getBeanDefinition")));
            } else if (context.getTypeMirrorUtils().isXmlType(variable.asType())) {
                generateParseXmlType(parse.body(), element, builder, fieldName);
            } else if (context.getTypeMirrorUtils().isArrayOrList(variable.asType())) {
                Variable listElement = parse.body().decl(ref(org.w3c.dom.Element.class),
                        fieldName + "ListElement",
                        ExpressionFactory._null());

                parse.body().assign(listElement, ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                        .arg(element)
                        .arg(context.getNameUtils().uncamel(fieldName)));

                UpperBlockClosure managedList = generateParseArrayOrList(parse.body(), variable.asType(), listElement, builder, fieldName);

                managedList.getNotRefBlock().add(builder.invoke("addPropertyValue").arg(fieldName).arg(managedList.getManagedCollection()));
            } else if (context.getTypeMirrorUtils().isMap(variable.asType())) {
                Variable listElement = parse.body().decl(ref(org.w3c.dom.Element.class),
                        fieldName + "ListElement",
                        ExpressionFactory._null());

                parse.body().assign(listElement, ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                        .arg(element)
                        .arg(context.getNameUtils().uncamel(fieldName)));

                UpperBlockClosure managedMap = generateParseMap(parse.body(), variable.asType(), listElement, builder, fieldName);

                managedMap.getNotRefBlock().add(builder.invoke("addPropertyValue").arg(fieldName).arg(managedMap.getManagedCollection()));
            } else if (context.getTypeMirrorUtils().isEnum(variable.asType())) {
                generateParseEnum(parse.body(), element, builder, fieldName);
            }
        }

        Variable definition = parse.body().decl(ref(BeanDefinition.class), "definition", builder.invoke("getBeanDefinition"));

        parse.body().add(definition.invoke("setAttribute").arg(
                ref(MuleHierarchicalBeanDefinitionParserDelegate.class).staticRef("MULE_NO_RECURSE")).arg(ref(Boolean.class).staticRef("TRUE")));
        return definition;
    }

    private void generateParseProcessorCallback(Method parse, Variable element, Variable parserContext, Variable builder, String fieldName) {
        Variable elements = parse.body().decl(ref(org.w3c.dom.Element.class), fieldName + "Element",
                ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                        .arg(element).arg(context.getNameUtils().uncamel(fieldName)));

        Variable beanDefinitionBuilder = parse.body().decl(ref(BeanDefinitionBuilder.class), fieldName + "BeanDefinitionBuilder",
                ref(BeanDefinitionBuilder.class).staticInvoke("rootBeanDefinition")
                        .arg(ref(MessageProcessorChainFactoryBean.class).dotclass()));

        parse.body().add(beanDefinitionBuilder.invoke("setSource").arg(parserContext.invoke("extractSource")
                .arg(elements)));

        parse.body().add(beanDefinitionBuilder.invoke("setScope")
                .arg(ref(BeanDefinition.class).staticRef("SCOPE_SINGLETON")));

        Variable list = parse.body().decl(ref(List.class), fieldName + "List",
                parserContext.invoke("getDelegate").invoke("parseListElement")
                        .arg(elements).arg(beanDefinitionBuilder.invoke("getBeanDefinition")));

        parse.body().add(builder.invoke("addPropertyValue").arg(fieldName)
                .arg(beanDefinitionBuilder.invoke("getBeanDefinition")));
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
        block.add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                element.invoke("getAttribute").arg(fieldName)
        ));
    }

    private void generateParseEnum(Block block, Variable element, Variable builder, String fieldName) {
        block.add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                element.invoke("getAttribute").arg(fieldName)
        ));
    }

    private UpperBlockClosure generateParseMap(Block body, TypeMirror typeMirror, Variable listElement, Variable builder, String fieldName) {
        DeclaredType variableType = (DeclaredType) typeMirror;
        java.util.List<? extends TypeMirror> variableTypeParameters = variableType.getTypeArguments();

        Variable listChilds = body.decl(ref(List.class).narrow(ref(org.w3c.dom.Element.class)),
                fieldName.replace("-", "") + "ListChilds",
                ExpressionFactory._null());

        Conditional listElementNotNull = body._if(Op.ne(listElement, ExpressionFactory._null()));

        Invocation getElementRef = listElement.invoke("getAttribute").arg("ref");
        Variable ref = listElementNotNull._then().decl(ref(String.class), fieldName.replace("-", "") + "Ref",
                getElementRef);

        Conditional ifRef = listElementNotNull._then()._if(Op.cand(Op.ne(ref, ExpressionFactory._null()),
                Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(ref))));

        ifRef._then().add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(ref)
        ));

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


        if (variableTypeParameters.size() > 1 && context.getTypeMirrorUtils().isPojo(variableTypeParameters.get(1))) {
            Variable pojoBuilder = generateParsePojo(variableTypeParameters.get(1), ifValueRef._else(), forEach.var());

            ifValueRef._else().assign(valueObject, pojoBuilder.invoke("getBeanDefinition"));
        } else if (variableTypeParameters.size() > 1 && context.getTypeMirrorUtils().isArrayOrList(variableTypeParameters.get(1))) {
            UpperBlockClosure subList = generateParseArrayOrList(forEach.body(), variableTypeParameters.get(1), forEach.var(), builder, "inner-" + childName);

            subList.getNotRefBlock().assign(valueObject, subList.getManagedCollection());
        } else if (variableTypeParameters.size() > 1 && context.getTypeMirrorUtils().isMap(variableTypeParameters.get(1))) {
            UpperBlockClosure subMap = generateParseMap(forEach.body(), variableTypeParameters.get(1), forEach.var(), builder, "inner-" + childName);

            subMap.getNotRefBlock().assign(valueObject, subMap.getManagedCollection());
        } else {
            ifValueRef._else().assign(valueObject,
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

    private UpperBlockClosure generateParseArrayOrList(Block body, TypeMirror typeMirror, Variable listElement, Variable builder, String fieldName) {
        DeclaredType variableType = (DeclaredType) typeMirror;
        java.util.List<? extends TypeMirror> variableTypeParameters = variableType.getTypeArguments();

        Variable listChilds = body.decl(ref(List.class).narrow(ref(org.w3c.dom.Element.class)),
                fieldName.replace("-", "") + "ListChilds",
                ExpressionFactory._null());

        Conditional listElementNotNull = body._if(Op.ne(listElement, ExpressionFactory._null()));

        Invocation getElementRef = listElement.invoke("getAttribute").arg("ref");
        Variable ref = listElementNotNull._then().decl(ref(String.class), fieldName.replace("-", "") + "Ref",
                getElementRef);

        Conditional ifRef = listElementNotNull._then()._if(Op.cand(Op.ne(ref, ExpressionFactory._null()),
                Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(ref))));

        ifRef._then().add(builder.invoke("addPropertyValue").arg(fieldName).arg(
                ExpressionFactory._new(ref(RuntimeBeanReference.class)).arg(ref)
        ));

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

        if (variableTypeParameters.size() > 0 && context.getTypeMirrorUtils().isPojo(variableTypeParameters.get(0))) {
            Variable pojoBeanDefinitionBuilder = generateParsePojo(variableTypeParameters.get(0), ifValueRef._else(), forEach.var());

            ifValueRef._else().add(
                    managedList.invoke("add").arg(pojoBeanDefinitionBuilder.invoke("getBeanDefinition")));
        } else if (variableTypeParameters.size() > 0 && context.getTypeMirrorUtils().isArrayOrList(variableTypeParameters.get(0))) {
            UpperBlockClosure subList = generateParseArrayOrList(ifValueRef._else(), variableTypeParameters.get(0), forEach.var(), builder, "inner-" + childName);

            subList.getNotRefBlock().add(
                    managedList.invoke("add").arg(subList.getManagedCollection()));
        } else if (variableTypeParameters.size() > 0 && context.getTypeMirrorUtils().isMap(variableTypeParameters.get(0))) {
            UpperBlockClosure subMap = generateParseMap(ifValueRef._else(), variableTypeParameters.get(0), forEach.var(), builder, "inner-" + childName);

            subMap.getNotRefBlock().add(
                    managedList.invoke("add").arg(subMap.getManagedCollection()));
        } else {
            ifValueRef._else().add(
                    managedList.invoke("add").arg(forEach.var().invoke("getTextContent")));
        }

        return new UpperBlockClosure(managedList, ifRef._else());
    }

    private Variable generateParsePojo(TypeMirror pojoType, Block block, Variable element) {
        DeclaredType declaredType = (DeclaredType) pojoType;
        String baseName = declaredType.asElement().getSimpleName().toString().toLowerCase();
        Variable builder = block.decl(ref(BeanDefinitionBuilder.class), baseName + "BeanDefinitionBuilder",
                ref(BeanDefinitionBuilder.class).staticInvoke("rootBeanDefinition").arg(ref(pojoType).boxify().dotclass()));

        TypeElement typeElement = (TypeElement)declaredType.asElement();
        java.util.List<VariableElement> variables = ElementFilter.fieldsIn(typeElement.getEnclosedElements());
        for (VariableElement variable : variables) {
            String fieldName = variable.getSimpleName().toString();

            if (SchemaTypeConversion.isSupported(variable.asType().toString())) {
                generateParseSupportedType(block, element, builder, fieldName);
            } else if (context.getTypeMirrorUtils().isXmlType(variable.asType())) {
                generateParseXmlType(block, element, builder, fieldName);
            } else if (context.getTypeMirrorUtils().isArrayOrList(variable.asType())) {
                Variable listElement = block.decl(ref(org.w3c.dom.Element.class),
                        fieldName + "ListElement",
                        ExpressionFactory._null());

                block.assign(listElement, ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                        .arg(element)
                        .arg(context.getNameUtils().uncamel(fieldName)));

                UpperBlockClosure managedList = generateParseArrayOrList(block, variable.asType(), listElement, builder, fieldName);

                managedList.getNotRefBlock().add(builder.invoke("addPropertyValue").arg(fieldName).arg(managedList.getManagedCollection()));
            } else if (context.getTypeMirrorUtils().isMap(variable.asType())) {
                Variable listElement = block.decl(ref(org.w3c.dom.Element.class),
                        fieldName + "ListElement",
                        ExpressionFactory._null());

                block.assign(listElement, ref(DomUtils.class).staticInvoke("getChildElementByTagName")
                        .arg(element)
                        .arg(context.getNameUtils().uncamel(fieldName)));

                UpperBlockClosure managedMap = generateParseMap(block, variable.asType(), listElement, builder, fieldName);

                managedMap.getNotRefBlock().add(builder.invoke("addPropertyValue").arg(fieldName).arg(managedMap.getManagedCollection()));
            } else if (context.getTypeMirrorUtils().isEnum(variable.asType())) {
                generateParseEnum(block, element, builder, fieldName);
            }
        }

        return builder;
    }

    private void generateAttachMessageProcessor(Method parse, Variable definition, Variable parserContext) {
        Variable propertyValues = parse.body().decl(ref(MutablePropertyValues.class), "propertyValues",
                parserContext.invoke("getContainingBeanDefinition").invoke("getPropertyValues"));
        Variable messageProcessors = parse.body().decl(ref(PropertyValue.class), "messageProcessors",
                propertyValues.invoke("getPropertyValue").arg("messageProcessors"));
        Conditional noList = parse.body()._if(Op.cor(Op.eq(messageProcessors, ExpressionFactory._null()), Op.eq(messageProcessors.invoke("getValue"),
                ExpressionFactory._null())));
        noList._then().add(propertyValues.invoke("addPropertyValue").arg("messageProcessors").arg(ExpressionFactory._new(ref(ManagedList.class))));
        Variable listMessageProcessors = parse.body().decl(ref(List.class), "listMessageProcessors",
                ExpressionFactory.cast(ref(List.class), propertyValues.invoke("getPropertyValue").arg("messageProcessors").invoke("getValue")));
        parse.body().add(listMessageProcessors.invoke("add").arg(
                definition
        ));
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

    private void generateGetAttributeValue(DefinedClass beanDefinitionparser) {
        Method getAttributeValue = beanDefinitionparser.method(Modifier.PROTECTED, ref(String.class), "getAttributeValue");
        Variable element = getAttributeValue.param(ref(org.w3c.dom.Element.class), "element");
        Variable attributeName = getAttributeValue.param(ref(String.class), "attributeName");

        Invocation getAttribute = element.invoke("getAttribute").arg(attributeName);

        Invocation isEmpty = ref(StringUtils.class).staticInvoke("isEmpty");
        isEmpty.arg(getAttribute);

        Block ifIsEmpty = getAttributeValue.body()._if(isEmpty.not())._then();
        ifIsEmpty._return(getAttribute);

        getAttributeValue.body()._return(ExpressionFactory._null());
    }


    private void generateParseChild(DefinedClass beanDefinitionparser, ExecutableElement executableElement) {
        Method parseChild = beanDefinitionparser.method(Modifier.PROTECTED, context.getCodeModel().VOID, "parseChild");
        Variable element = parseChild.param(ref(org.w3c.dom.Element.class), "element");
        Variable parserContext = parseChild.param(ref(ParserContext.class), "parserContext");
        Variable beanDefinitionBuilder = parseChild.param(ref(BeanDefinitionBuilder.class), "beanDefinitionBuilder");

        generateSetPojoIfConfigRefNotEmpty(parseChild, element, beanDefinitionBuilder);

        for (VariableElement variable : executableElement.getParameters()) {
            if (variable.asType().toString().contains(SourceCallback.class.getName()))
                continue;

            if (SchemaTypeConversion.isSupported(variable.asType().toString()) ||
                    context.getTypeMirrorUtils().isEnum(variable.asType())) {
                parseChild.body().add(generateAddPropertyValue(element, beanDefinitionBuilder, variable));
            }
        }

        Variable assembler = generateBeanAssembler(parseChild, element, beanDefinitionBuilder);
        generatePostProcessCall(parseChild, element, assembler);
    }

    private void generateSetPojoIfConfigRefNotEmpty(Method parseChild, Variable element, Variable beanDefinitionBuilder) {
        Conditional isConfigRefEmpty = parseChild.body()._if(Op.not(generateIsEmptyConfigRef(element)));
        Invocation addPropertyReference = beanDefinitionBuilder.invoke("addPropertyReference");
        addPropertyReference.arg("pojo");
        Invocation getAttributeAlias = generateGetAttributeConfigRef();
        Invocation getAttribute = element.invoke("getAttribute");
        getAttribute.arg(getAttributeAlias);
        addPropertyReference.arg(getAttribute);
        isConfigRefEmpty._then().add(addPropertyReference);
    }

    private Invocation generateIsEmptyConfigRef(Variable element) {
        Invocation getAttributeAlias = generateGetAttributeConfigRef();
        Invocation getAttribute = element.invoke("getAttribute");
        getAttribute.arg(getAttributeAlias);
        Invocation isEmpty = ref(StringUtils.class).staticInvoke("isEmpty");
        isEmpty.arg(getAttribute);
        return isEmpty;
    }

    private Invocation generateGetAttributeConfigRef() {
        Invocation getTargetPropertyConfiguration = ExpressionFactory.invoke("getTargetPropertyConfiguration");
        Invocation getAttributeAlias = getTargetPropertyConfiguration.invoke("getAttributeAlias");
        getAttributeAlias.arg("config-ref");
        return getAttributeAlias;
    }

    private Invocation generateAddPropertyValue(Variable element, Variable beanDefinitionBuilder, VariableElement variable) {
        Invocation getAttributeValue = ExpressionFactory.invoke("getAttributeValue");
        getAttributeValue.arg(element);
        getAttributeValue.arg(ExpressionFactory.lit(variable.getSimpleName().toString()));
        Invocation addPropertyValue = beanDefinitionBuilder.invoke("addPropertyValue");
        addPropertyValue.arg(ExpressionFactory.lit(variable.getSimpleName().toString()));
        addPropertyValue.arg(getAttributeValue);

        return addPropertyValue;
    }

    private Invocation generateAddPropertyRefValue(Variable element, Variable beanDefinitionBuilder, VariableElement variable) {
        Invocation getAttributeValue = ExpressionFactory.invoke("getAttributeValue");
        getAttributeValue.arg(element);
        getAttributeValue.arg(ExpressionFactory.lit(variable.getSimpleName().toString() + "-ref"));
        Invocation addPropertyValue = beanDefinitionBuilder.invoke("addPropertyValue");
        addPropertyValue.arg(ExpressionFactory.lit(variable.getSimpleName().toString()));
        addPropertyValue.arg(getAttributeValue);

        return addPropertyValue;
    }


    private Variable generateBeanAssembler(Method parseChild, Variable element, Variable beanDefinitionBuilder) {
        Variable assembler = parseChild.body().decl(ref(BeanAssembler.class), "assembler");
        Invocation getBeanAssembler = ExpressionFactory.invoke("getBeanAssembler");
        getBeanAssembler.arg(element);
        getBeanAssembler.arg(beanDefinitionBuilder);
        parseChild.body().assign(assembler, getBeanAssembler);
        return assembler;
    }

    private void generatePostProcessCall(Method parseChild, Variable element, Variable assembler) {
        Invocation postProcess = parseChild.body().invoke("postProcess");
        postProcess.arg(ExpressionFactory.invoke("getParserContext"));
        postProcess.arg(assembler);
        postProcess.arg(element);
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
