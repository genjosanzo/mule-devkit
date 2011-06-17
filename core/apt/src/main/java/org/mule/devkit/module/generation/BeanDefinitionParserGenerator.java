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
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.annotations.Source;
import org.mule.devkit.annotations.SourceCallback;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.JBlock;
import org.mule.devkit.model.code.JConditional;
import org.mule.devkit.model.code.JExpr;
import org.mule.devkit.model.code.JExpression;
import org.mule.devkit.model.code.JInvocation;
import org.mule.devkit.model.code.JMethod;
import org.mule.devkit.model.code.JMod;
import org.mule.devkit.model.code.JOp;
import org.mule.devkit.model.code.JPackage;
import org.mule.devkit.model.code.JVar;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;

public class BeanDefinitionParserGenerator extends AbstractMessageGenerator {

    public void generate(Element type) throws GenerationException {
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

    private void generateBeanDefinitionParserForSource(ExecutableElement executableElement) {
        // get class
        DefinedClass beanDefinitionparser = getBeanDefinitionParserClass(executableElement);
        DefinedClass messageSourceClass = getMessageSourceClass(executableElement);

        // add constructor
        JMethod constructor = beanDefinitionparser.constructor(JMod.PUBLIC);
        constructor.body().invoke("super").arg(JExpr.lit("messageSource")).arg(JExpr.dotclass(messageSourceClass));

        // add getBeanClass
        generateGetBeanClass(beanDefinitionparser, JExpr.dotclass(messageSourceClass));

        // add parseChild
        generateParseChild(beanDefinitionparser, executableElement);

        // add getAttributeValue
        generateGetAttributeValue(beanDefinitionparser);

    }

    private void generateGetBeanClass(DefinedClass beanDefinitionparser, JExpression expr) {
        JMethod getBeanClass = beanDefinitionparser.method(JMod.PROTECTED, ref(Class.class), "getBeanClass");
        JVar element = getBeanClass.param(ref(org.w3c.dom.Element.class), "element");
        getBeanClass.body()._return(expr);
    }

    private void generateBeanDefinitionParserForProcessor(ExecutableElement executableElement) {
        // get class
        DefinedClass beanDefinitionparser = getBeanDefinitionParserClass(executableElement);
        DefinedClass messageProcessorClass = getMessageProcessorClass(executableElement);

        // add constructor
        JMethod constructor = beanDefinitionparser.constructor(JMod.PUBLIC);
        constructor.body().invoke("super").arg(JExpr.lit("messageProcessor")).arg(JExpr.dotclass(messageProcessorClass));

        // add getBeanClass
        generateGetBeanClass(beanDefinitionparser, JExpr.dotclass(messageProcessorClass));

        // add parseChild
        generateParseChild(beanDefinitionparser, executableElement);

        // add getAttributeValue
        generateGetAttributeValue(beanDefinitionparser);

    }

    private void generateGetAttributeValue(DefinedClass beanDefinitionparser) {
        JMethod getAttributeValue = beanDefinitionparser.method(JMod.PROTECTED, ref(String.class), "getAttributeValue");
        JVar element = getAttributeValue.param(ref(Element.class), "element");
        JVar attributeName = getAttributeValue.param(ref(String.class), "attributeName");

        JInvocation getAttribute = element.invoke("getAttribute").arg(attributeName);

        JInvocation isEmpty = ref(StringUtils.class).staticInvoke("isEmpty");
        isEmpty.arg(getAttribute);

        JBlock ifIsEmpty = getAttributeValue.body()._if(isEmpty.not())._then();
        ifIsEmpty._return(getAttribute);

        getAttributeValue.body()._return(JExpr._null());
    }


    private void generateParseChild(DefinedClass beanDefinitionparser, ExecutableElement executableElement) {
        JMethod parseChild = beanDefinitionparser.method(JMod.PROTECTED, context.getCodeModel().VOID, "parseChild");
        JVar element = parseChild.param(ref(Element.class), "element");
        JVar parserContext = parseChild.param(ref(ParserContext.class), "parserContext");
        JVar beanDefinitionBuilder = parseChild.param(ref(BeanDefinitionBuilder.class), "beanDefinitionBuilder");

        generateSetObjectIfConfigRefNotEmpty(parseChild, element, beanDefinitionBuilder);

        for (VariableElement variable : executableElement.getParameters()) {
            if (variable.asType().toString().contains(SourceCallback.class.getName()))
                continue;

            if (SchemaTypeConversion.isSupported(variable.asType().toString())) {
                parseChild.body().add(generateAddPropertyValue(element, beanDefinitionBuilder, variable));
            }
        }

        JVar assembler = generateBeanAssembler(parseChild, element, beanDefinitionBuilder);
        generatePostProcessCall(parseChild, element, assembler);
    }

    private void generateSetObjectIfConfigRefNotEmpty(JMethod parseChild, JVar element, JVar beanDefinitionBuilder) {
        JConditional isConfigRefEmpty = parseChild.body()._if(JOp.not(generateIsEmptyConfigRef(element)));
        JInvocation addPropertyReference = beanDefinitionBuilder.invoke("addPropertyReference");
        addPropertyReference.arg("object");
        JInvocation getAttributeAlias = generateGetAttributeConfigRef();
        JInvocation getAttribute = element.invoke("getAttribute");
        getAttribute.arg(getAttributeAlias);
        addPropertyReference.arg(getAttribute);
        isConfigRefEmpty._then().add(addPropertyReference);
    }

    private JInvocation generateIsEmptyConfigRef(JVar element) {
        JInvocation getAttributeAlias = generateGetAttributeConfigRef();
        JInvocation getAttribute = element.invoke("getAttribute");
        getAttribute.arg(getAttributeAlias);
        JInvocation isEmpty = ref(StringUtils.class).staticInvoke("isEmpty");
        isEmpty.arg(getAttribute);
        return isEmpty;
    }

    private JInvocation generateGetAttributeConfigRef() {
        JInvocation getTargetPropertyConfiguration = JExpr.invoke("getTargetPropertyConfiguration");
        JInvocation getAttributeAlias = getTargetPropertyConfiguration.invoke("getAttributeAlias");
        getAttributeAlias.arg("config-ref");
        return getAttributeAlias;
    }

    private JInvocation generateAddPropertyValue(JVar element, JVar beanDefinitionBuilder, VariableElement variable) {
        JInvocation getAttributeValue = JExpr.invoke("getAttributeValue");
        getAttributeValue.arg(element);
        getAttributeValue.arg(JExpr.lit(variable.getSimpleName().toString()));
        JInvocation addPropertyValue = beanDefinitionBuilder.invoke("addPropertyValue");
        addPropertyValue.arg(JExpr.lit(variable.getSimpleName().toString()));
        addPropertyValue.arg(getAttributeValue);

        return addPropertyValue;
    }

    private JInvocation generateAddPropertyRefValue(JVar element, JVar beanDefinitionBuilder, VariableElement variable) {
        JInvocation getAttributeValue = JExpr.invoke("getAttributeValue");
        getAttributeValue.arg(element);
        getAttributeValue.arg(JExpr.lit(variable.getSimpleName().toString() + "-ref"));
        JInvocation addPropertyValue = beanDefinitionBuilder.invoke("addPropertyValue");
        addPropertyValue.arg(JExpr.lit(variable.getSimpleName().toString()));
        addPropertyValue.arg(getAttributeValue);

        return addPropertyValue;
    }


    private JVar generateBeanAssembler(JMethod parseChild, JVar element, JVar beanDefinitionBuilder) {
        JVar assembler = parseChild.body().decl(ref(BeanAssembler.class), "assembler");
        JInvocation getBeanAssembler = JExpr.invoke("getBeanAssembler");
        getBeanAssembler.arg(element);
        getBeanAssembler.arg(beanDefinitionBuilder);
        parseChild.body().assign(assembler, getBeanAssembler);
        return assembler;
    }

    private void generatePostProcessCall(JMethod parseChild, JVar element, JVar assembler) {
        JInvocation postProcess = parseChild.body().invoke("postProcess");
        postProcess.arg(JExpr.invoke("getParserContext"));
        postProcess.arg(assembler);
        postProcess.arg(element);
    }
}
