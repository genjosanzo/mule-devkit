package org.mule.devkit.apt.generator.spring;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JVar;
import org.apache.commons.lang.StringUtils;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.AbstractCodeGenerator;
import org.mule.devkit.apt.generator.GenerationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.swing.*;
import java.util.List;

public class BeanDefinitionParserGenerator extends AbstractCodeGenerator {
    public BeanDefinitionParserGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement type) throws GenerationException {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Processor processor = executableElement.getAnnotation(Processor.class);

            if (processor == null)
                continue;

            generateBeanDefinitionParser(executableElement);
        }
    }

    private void generateBeanDefinitionParser(ExecutableElement executableElement)
    {
        // get class
        JDefinedClass beanDefinitionparser = getBeanDefinitionParserClass(executableElement);
        JDefinedClass messageProcessorClass = getMessageProcessorClass(executableElement);

        // add constructor
        JMethod constructor = beanDefinitionparser.constructor(JMod.PUBLIC);
        constructor.body().invoke("super").arg(JExpr.lit("messageProcessor")).arg(JExpr.dotclass(messageProcessorClass));

        // add getBeanClass
        generateGetBeanClass(beanDefinitionparser, messageProcessorClass);

        // add parseChild
        generateParseChild(beanDefinitionparser, executableElement);

        // add getAttributeValue
        generateGetAttributeValue(beanDefinitionparser);
    }

    private void generateGetAttributeValue(JDefinedClass beanDefinitionparser)
    {
        JMethod getAttributeValue = beanDefinitionparser.method(JMod.PROTECTED, ref(Class.class), "getAttributeValue");
        JVar element = getAttributeValue.param(ref(Element.class), "element");
        JVar attributeName = getAttributeValue.param(ref(String.class), "attributeName");

        JInvocation getAttribute = element.invoke("getAttribute").arg(attributeName);

        JInvocation isEmpty = ref(StringUtils.class).boxify().staticInvoke("isEmpty");
        isEmpty.arg(getAttribute);

        JBlock ifIsEmpty = getAttributeValue.body()._if(isEmpty)._then();
        ifIsEmpty._return(getAttribute);

        getAttributeValue.body()._return(JExpr._null());
    }


    private void generateGetBeanClass(JDefinedClass beanDefinitionparser, JDefinedClass messageProcessorClass)
    {
        JMethod getBeanClass = beanDefinitionparser.method(JMod.PROTECTED, ref(Class.class), "getBeanClass");
        JVar element = getBeanClass.param(ref(Element.class), "element");
        getBeanClass.body()._return(JExpr.dotclass(messageProcessorClass));

    }

    private void generateParseChild(JDefinedClass beanDefinitionparser, ExecutableElement executableElement)
    {
        JMethod parseChild = beanDefinitionparser.method(JMod.PROTECTED, getContext().getCodeModel().VOID, "parseChild");
        JVar element = parseChild.param(ref(Element.class), "element");
        JVar parserContext = parseChild.param(ref(ParserContext.class), "parserContext");
        JVar beanDefinitionBuilder = parseChild.param(ref(BeanDefinitionBuilder.class), "beanDefinitionBuilder");

        generateSetObjectIfConfigRefNotEmpty(parseChild, element, beanDefinitionBuilder);

        for (VariableElement variable : executableElement.getParameters()) {
            parseChild.body().add(generateAddPropertyValue(element, beanDefinitionBuilder, variable));
        }

        JVar assembler = generateBeanAssembler(parseChild, element, beanDefinitionBuilder);
        generatePostProcessCall(parseChild, element, assembler);
    }

    private void generateSetObjectIfConfigRefNotEmpty(JMethod parseChild, JVar element, JVar beanDefinitionBuilder) {
        JConditional isConfigRefEmpty = parseChild.body()._if(JOp.not(generateIsEmptyConfigRef(element)));
        JInvocation addPropertyReference = beanDefinitionBuilder.invoke("addPropertyReference");
        addPropertyReference.arg("object");
        addPropertyReference.arg(generateGetAttributeConfigRef());
        isConfigRefEmpty._then().add(addPropertyReference);
    }

    private JInvocation generateIsEmptyConfigRef(JVar element) {
        JInvocation getAttributeAlias = generateGetAttributeConfigRef();
        JInvocation getAttribute = element.invoke("getAttribute");
        getAttribute.arg(getAttributeAlias);
        JInvocation isEmpty = ref(StringUtils.class).boxify().staticInvoke("isEmpty");
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

    /*
    @Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        if (!StringUtils.isEmpty(element.getAttribute(getTargetPropertyConfiguration().getAttributeAlias(
            "config-ref"))))
        {
            builder.addPropertyReference("object",
                element.getAttribute(getTargetPropertyConfiguration().getAttributeAlias("config-ref")));
        }

        <#list method.getParameters() as parameter>
        <#if !parameter.getType().isArray() && !parameter.getType().isList() && !parameter.getType().isMap()>
        builder.addPropertyValue("${parameter.getName()}", getAttributeValue(element, "${parameter.getName()}"));
        </#if>
        </#list>

        BeanAssembler assembler = getBeanAssembler(element, builder);
        postProcess(getParserContext(), assembler, element);
    }

    private String getAttributeValue(Element element, String attributeName)
    {
        if (!StringUtils.isEmpty(element.getAttribute(attributeName)))
        {
            return element.getAttribute(attributeName);
        }

        return null;
    }

     */
}
