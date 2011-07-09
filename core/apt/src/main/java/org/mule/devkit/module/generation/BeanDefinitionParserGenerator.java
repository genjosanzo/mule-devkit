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
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.callback.SourceCallback;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.Expression;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.Variable;
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
        Method constructor = beanDefinitionparser.constructor(Modifier.PUBLIC);
        constructor.body().invoke("super").arg(ExpressionFactory.lit("messageSource")).arg(ExpressionFactory.dotclass(messageSourceClass));

        // add getBeanClass
        generateGetBeanClass(beanDefinitionparser, ExpressionFactory.dotclass(messageSourceClass));

        // add parseChild
        generateParseChild(beanDefinitionparser, executableElement);

        // add getAttributeValue
        generateGetAttributeValue(beanDefinitionparser);

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

        // add constructor
        Method constructor = beanDefinitionparser.constructor(Modifier.PUBLIC);
        constructor.body().invoke("super").arg(ExpressionFactory.lit("messageProcessor")).arg(ExpressionFactory.dotclass(messageProcessorClass));

        // add getBeanClass
        generateGetBeanClass(beanDefinitionparser, ExpressionFactory.dotclass(messageProcessorClass));

        // add parseChild
        generateParseChild(beanDefinitionparser, executableElement);

        // add getAttributeValue
        generateGetAttributeValue(beanDefinitionparser);

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
}
