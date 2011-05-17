package org.mule.devkit.apt.generator.spring;

import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;
import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.AbstractCodeGenerator;
import org.mule.devkit.apt.generator.GenerationException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;

public class AnyXmlChildDefinitionParserGenerator extends AbstractCodeGenerator {
    public AnyXmlChildDefinitionParserGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement type) throws GenerationException {

        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Processor processor = executableElement.getAnnotation(Processor.class);

            if (processor == null)
                continue;

            // generate extra parser
            JDefinedClass anyXmlChildDefinitionParser = getAnyXmlChildDefinitionParserClass(executableElement);
            generateAnyXmlChildDefinitionParser(anyXmlChildDefinitionParser);

            break;
        }
    }

    private void generateAnyXmlChildDefinitionParser(JDefinedClass anyXmlChildDefinitionParser) {
        // generate constructor
        generateConstructor(anyXmlChildDefinitionParser);

        // getBeanClass
        generateGetBeanClass(anyXmlChildDefinitionParser, ref(String.class).boxify());

        // processProperty
        generateProcessProperty(anyXmlChildDefinitionParser);

        // generateParseInternal
        generateParseInternal(anyXmlChildDefinitionParser);
    }

    private void generateParseInternal(JDefinedClass anyXmlChildDefinitionParser) {
        JMethod parseInternal = anyXmlChildDefinitionParser.method(JMod.PROTECTED, ref(AbstractBeanDefinition.class), "parseInternal");
        JVar element = parseInternal.param(ref(Element.class), "element");
        JVar parserContext = parseInternal.param(ref(ParserContext.class), "parserContext");

        JVar bd = parseInternal.body().decl(ref(AbstractBeanDefinition.class), "bd");
        JInvocation superParserInternal = JExpr._super().invoke("parseInternal");
        superParserInternal.arg(element);
        superParserInternal.arg(parserContext);

        parseInternal.body().assign(bd, superParserInternal);

        JInvocation setAttribute = bd.invoke("setAttribute");
        setAttribute.arg(ref(MuleHierarchicalBeanDefinitionParserDelegate.class).boxify().staticRef("MULE_NO_RECURSE"));
        setAttribute.arg(ref(Boolean.class).boxify().staticRef("TRUE"));
        parseInternal.body().add(setAttribute);

        parseInternal.body()._return(bd);
    }

    private void generateConstructor(JDefinedClass anyXmlChildDefinitionParser) {
        JMethod constructor = anyXmlChildDefinitionParser.constructor(JMod.PUBLIC);
        JVar setterMethod = constructor.param(ref(String.class), "setterMethod");
        JVar clazz = constructor.param(ref(Class.class), "clazz");

        JInvocation superInvocation = constructor.body().invoke("super");
        superInvocation.arg(setterMethod);
        superInvocation.arg(clazz);

        JInvocation addIgnored = constructor.body().invoke("addIgnored");
        addIgnored.arg("xmlns");
    }

    private void generateProcessProperty(JDefinedClass xmlAnyChildDefinitionParser) {
        JMethod processProperty = xmlAnyChildDefinitionParser.method(JMod.PROTECTED, getContext().getCodeModel().VOID, "processProperty");
        JVar attribute = processProperty.param(ref(Attr.class), "attribute");
        JVar beanAssembler = processProperty.param(ref(BeanAssembler.class), "assembler");
    }


}
