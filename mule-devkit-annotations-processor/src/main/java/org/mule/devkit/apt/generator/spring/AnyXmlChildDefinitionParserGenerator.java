package org.mule.devkit.apt.generator.spring;

import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JForLoop;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JVar;
import org.apache.commons.lang.UnhandledException;
import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.AbstractCodeGenerator;
import org.mule.devkit.apt.generator.GenerationException;
import org.mule.devkit.apt.util.CodeModelUtils;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
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
            for (VariableElement variable : executableElement.getParameters()) {
                if (CodeModelUtils.isXmlType(variable)) {
                    JDefinedClass anyXmlChildDefinitionParser = getAnyXmlChildDefinitionParserClass(variable);
                    generateAnyXmlChildDefinitionParser(anyXmlChildDefinitionParser);

                    break;
                }
            }
        }
    }

    private void generateAnyXmlChildDefinitionParser(JDefinedClass anyXmlChildDefinitionParser) {
        // generate constructor
        generateConstructor(anyXmlChildDefinitionParser);

        // getBeanClass
        generateGetBeanClass(anyXmlChildDefinitionParser, ref(String.class).dotclass());

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

        JConditional ifBlock = parseInternal.body()._if(JOp.eq(ref(Node.class).staticRef("ELEMENT_NODE"), element.invoke("getNodeType")));

        JVar nodeList = ifBlock._then().decl(ref(NodeList.class), "childs", element.invoke("getChildNodes"));
        JVar i = ifBlock._then().decl(getContext().getCodeModel().INT, "i");
        JForLoop forLoop = ifBlock._then()._for();
        forLoop.init(i, JExpr.lit(0));
        forLoop.test(JOp.lt(i, nodeList.invoke("getLength")));
        forLoop.update(JOp.incr(i));
        JVar child = forLoop.body().decl(ref(Node.class), "child", nodeList.invoke("item").arg(i));

        JConditional ifBlock2 = forLoop.body()._if(JOp.eq(ref(Node.class).staticRef("ELEMENT_NODE"), child.invoke("getNodeType")));

        JTryBlock tryBlock = ifBlock2._then()._try();
        JVar domSource = tryBlock.body().decl(ref(DOMSource.class), "domSource", JExpr._new(ref(DOMSource.class)).arg(child));
        JVar stringWriter = tryBlock.body().decl(ref(StringWriter.class), "stringWriter", JExpr._new(ref(StringWriter.class)));
        JVar streamResult = tryBlock.body().decl(ref(StreamResult.class), "result", JExpr._new(ref(StreamResult.class)).arg(stringWriter));
        JVar tf = tryBlock.body().decl(ref(TransformerFactory.class), "tf", ref(TransformerFactory.class).staticInvoke("newInstance"));
        JVar transformer = tryBlock.body().decl(ref(Transformer.class), "transformer", tf.invoke("newTransformer"));
        JInvocation transform = transformer.invoke("transform");
        transform.arg(domSource);
        transform.arg(streamResult);
        tryBlock.body().add(transform);
        tryBlock.body().add(stringWriter.invoke("flush"));

        //bd.getPropertyValues().add(clazzProperty, arguments);
        JInvocation add = bd.invoke("getConstructorArgumentValues").invoke("addIndexedArgumentValue");
        add.arg(JExpr.lit(0));
        add.arg(stringWriter.invoke("toString"));
        tryBlock.body().add(add);

        generateReThrow(tryBlock, TransformerConfigurationException.class);
        generateReThrow(tryBlock, TransformerException.class);
        generateReThrow(tryBlock, TransformerFactoryConfigurationError.class);

        parseInternal.body()._return(bd);
    }

    private void generateReThrow(JTryBlock tryBlock, Class<?> clazz) {
        JCatchBlock catchBlock = tryBlock._catch(ref(clazz).boxify());
        JVar e = catchBlock.param("e");
        catchBlock.body()._throw(JExpr._new(ref(UnhandledException.class)).arg(e));
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
