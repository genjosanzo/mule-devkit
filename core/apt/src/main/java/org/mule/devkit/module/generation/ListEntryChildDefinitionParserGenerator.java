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

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.util.SpringXMLUtils;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.annotations.Source;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.ClassAlreadyExistsException;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Variable;
import org.mule.util.StringUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;

public class ListEntryChildDefinitionParserGenerator extends AbstractMessageGenerator {

    public static final String ROLE = "ListEntryChildDefinitionParser";

    public void generate(Element type) throws GenerationException {
        boolean shouldGenerate = false;

        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Processor processor = executableElement.getAnnotation(Processor.class);
            Source source = executableElement.getAnnotation(Source.class);

            if (processor == null && source == null)
                continue;

            // generate extra parser
            for (VariableElement variable : executableElement.getParameters()) {
                if (context.getTypeMirrorUtils().isArrayOrList(variable.asType())) {
                    shouldGenerate = true;
                }
            }
        }

        if (shouldGenerate) {
            DefinedClass listEntryChildDefinitionParser = getListEntryChildDefinitionParserClass((TypeElement) type);
            generateListEntryChildDefinitionParser(listEntryChildDefinitionParser);
        }

    }

    private void generateListEntryChildDefinitionParser(DefinedClass listEntryBeanDefinitionParser) {

        DefinedClass listEntry = generateEntryClass(listEntryBeanDefinitionParser);

        // generate constructor
        generateConstructor(listEntryBeanDefinitionParser, listEntry);

        generatePostProcessMethod(listEntryBeanDefinitionParser);

        generateParseChildMethod(listEntryBeanDefinitionParser);
    }

    private void generateParseChildMethod(DefinedClass listEntryBeanDefinitionParser) {
        Method parseChild = listEntryBeanDefinitionParser.method(Modifier.PROTECTED, context.getCodeModel().VOID, "parseChild");
        Variable element = parseChild.param(org.w3c.dom.Element.class, "element");
        Variable parserContext = parseChild.param(ParserContext.class, "parserContext");
        Variable builder = parseChild.param(BeanDefinitionBuilder.class, "builder");

        generateSetValueIfValueRefNotEmpty(parseChild, element, builder);

        Invocation sup = ExpressionFactory._super().invoke("parseChild");
        sup.arg(element);
        sup.arg(parserContext);
        sup.arg(builder);
        parseChild.body().add(sup);
    }

    private void generateSetValueIfValueRefNotEmpty(Method parseChild, Variable element, Variable beanDefinitionBuilder) {
        Conditional isConfigRefEmpty = parseChild.body()._if(Op.not(generateIsEmptyValueRef(element)));
        Invocation addPropertyReference = beanDefinitionBuilder.invoke("addPropertyReference");
        addPropertyReference.arg("value");
        Invocation getAttributeAlias = generateGetAttributeValueRef();
        Invocation getAttribute = element.invoke("getAttribute");
        getAttribute.arg(getAttributeAlias);
        addPropertyReference.arg(getAttribute);
        isConfigRefEmpty._then().add(addPropertyReference);
    }

    private Invocation generateIsEmptyValueRef(Variable element) {
        Invocation getAttributeAlias = generateGetAttributeValueRef();
        Invocation getAttribute = element.invoke("getAttribute");
        getAttribute.arg(getAttributeAlias);
        Invocation isEmpty = ref(org.apache.commons.lang.StringUtils.class).staticInvoke("isEmpty");
        isEmpty.arg(getAttribute);
        return isEmpty;
    }

    private Invocation generateGetAttributeValueRef() {
        Invocation getTargetPropertyConfiguration = ExpressionFactory.invoke("getTargetPropertyConfiguration");
        Invocation getAttributeAlias = getTargetPropertyConfiguration.invoke("getAttributeAlias");
        getAttributeAlias.arg("value-ref");
        return getAttributeAlias;
    }

    private void generatePostProcessMethod(DefinedClass listEntryBeanDefinitionParser) {
        Method postProcess = listEntryBeanDefinitionParser.method(Modifier.PROTECTED, context.getCodeModel().VOID, "postProcess");
        Variable parserContext = postProcess.param(ParserContext.class, "parserContext");
        Variable assembler = postProcess.param(BeanAssembler.class, "assembler");
        Variable element = postProcess.param(org.w3c.dom.Element.class, "element");

        Invocation extendBean = assembler.invoke("extendBean");
        extendBean.arg("value");
        extendBean.arg(ref(SpringXMLUtils.class).staticInvoke("getTextChild").arg(element));
        extendBean.arg(ExpressionFactory.FALSE);

        Invocation contains = assembler.invoke("getBean").invoke("getRawBeanDefinition").invoke("getPropertyValues").invoke("contains").arg("value");
        Conditional ifValue = postProcess.body()._if(Op.not(contains));
        TryStatement tryBlock = ifValue._then()._try();

        tryBlock.body().add(extendBean);
        tryBlock._catch(ref(IllegalStateException.class));

        Invocation sup = ExpressionFactory._super().invoke("postProcess");
        sup.arg(parserContext);
        sup.arg(assembler);
        sup.arg(element);
        postProcess.body().add(sup);
    }

    private DefinedClass generateEntryClass(DefinedClass listEntryBeanDefinitionParser) {
        DefinedClass listEntry;

        try {
            listEntry = listEntryBeanDefinitionParser._class(Modifier.STATIC | Modifier.PUBLIC, "ListEntry");
        } catch (ClassAlreadyExistsException e) {
            listEntry = e.getExistingClass();
        }

        FieldVariable value = listEntry.field(Modifier.PRIVATE, Object.class, "value");
        FieldVariable valueRef = listEntry.field(Modifier.PRIVATE, Object.class, "valueRef");

        generateSetMethod(listEntry, value);
        generateGetMethod(listEntry, value);

        generateSetMethod(listEntry, valueRef);
        generateGetMethod(listEntry, valueRef);

        return listEntry;
    }

    private void generateGetMethod(DefinedClass listEntry, FieldVariable value) {
        Method getValue = listEntry.method(Modifier.PUBLIC, value.type(), "get" + StringUtils.capitalize(value.name()));
        getValue.body()._return(ExpressionFactory._this().ref(value.name()));
    }

    private void generateSetMethod(DefinedClass listEntry, FieldVariable value) {
        Method setValue = listEntry.method(Modifier.PUBLIC, context.getCodeModel().VOID, "set" + StringUtils.capitalize(value.name()));
        Variable param = setValue.param(value.type(), value.name());
        setValue.body().assign(ExpressionFactory._this().ref(value.name()), param);
    }

    private void generateConstructor(DefinedClass listEntryBeanDefinitionParser, DefinedClass listEntry) {
        Method constructor = listEntryBeanDefinitionParser.constructor(Modifier.PUBLIC);
        Invocation sup = constructor.body().invoke("super");
        sup.arg("value");
        sup.arg("sourceList");
        sup.arg(ref(ChildListEntryDefinitionParser.ListEntry.class).dotclass());

        constructor.body().invoke("setIgnoredDefault").arg(ExpressionFactory.TRUE);
    }

    private DefinedClass getListEntryChildDefinitionParserClass(TypeElement typeElement) {
        DefinedClass freeForm = context.getClassForRole(FreeFormMapChildDefinitionParserGenerator.ROLE);
        String ListEntryChildDefinitionParserName = context.getNameUtils().generateClassNameInPackage(typeElement, "ListEntryChildDefinitionParser");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(ListEntryChildDefinitionParserName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(ListEntryChildDefinitionParserName), freeForm);

        context.setClassRole(ROLE, clazz);

        return clazz;
    }

}
