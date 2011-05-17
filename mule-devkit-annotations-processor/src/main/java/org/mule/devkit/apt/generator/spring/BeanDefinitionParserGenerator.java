package org.mule.devkit.apt.generator.spring;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
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
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;

public class BeanDefinitionParserGenerator extends AbstractCodeGenerator {
    private List<String> typeList;

    public BeanDefinitionParserGenerator(AnnotationProcessorContext context) {
        super(context);

        buildTypeList();
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

    private void generateBeanDefinitionParser(ExecutableElement executableElement) {
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

    private void generateGetAttributeValue(JDefinedClass beanDefinitionparser) {
        JMethod getAttributeValue = beanDefinitionparser.method(JMod.PROTECTED, ref(String.class), "getAttributeValue");
        JVar element = getAttributeValue.param(ref(Element.class), "element");
        JVar attributeName = getAttributeValue.param(ref(String.class), "attributeName");

        JInvocation getAttribute = element.invoke("getAttribute").arg(attributeName);

        JInvocation isEmpty = ref(StringUtils.class).boxify().staticInvoke("isEmpty");
        isEmpty.arg(getAttribute);

        JBlock ifIsEmpty = getAttributeValue.body()._if(isEmpty.not())._then();
        ifIsEmpty._return(getAttribute);

        getAttributeValue.body()._return(JExpr._null());
    }


    private void generateParseChild(JDefinedClass beanDefinitionparser, ExecutableElement executableElement) {
        JMethod parseChild = beanDefinitionparser.method(JMod.PROTECTED, getContext().getCodeModel().VOID, "parseChild");
        JVar element = parseChild.param(ref(Element.class), "element");
        JVar parserContext = parseChild.param(ref(ParserContext.class), "parserContext");
        JVar beanDefinitionBuilder = parseChild.param(ref(BeanDefinitionBuilder.class), "beanDefinitionBuilder");

        generateSetObjectIfConfigRefNotEmpty(parseChild, element, beanDefinitionBuilder);

        for (VariableElement variable : executableElement.getParameters()) {

            if (isTypeSupported(variable)) {
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

    private boolean isTypeSupported(VariableElement variableElement) {
        return typeList.contains(variableElement.asType().toString());
    }

    private void buildTypeList() {
        typeList = new ArrayList<String>();
        typeList.add("java.lang.String");
        typeList.add("int");
        typeList.add("float");
        typeList.add("long");
        typeList.add("byte");
        typeList.add("short");
        typeList.add("double");
        typeList.add("boolean");
        typeList.add("char");
        typeList.add("java.lang.Integer");
        typeList.add("java.lang.Float");
        typeList.add("java.lang.Long");
        typeList.add("java.lang.Byte");
        typeList.add("java.lang.Short");
        typeList.add("java.lang.Double");
        typeList.add("java.lang.Boolean");
        typeList.add("java.lang.Character");
        typeList.add("java.util.Date");
        typeList.add("java.net.URL");
        typeList.add("java.net.URI");
    }

}
