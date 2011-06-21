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

import org.apache.commons.lang.UnhandledException;
import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.Expression;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.JExpr;
import org.mule.devkit.model.code.ForLoop;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.JPackage;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Variable;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
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

public class AnyXmlChildDefinitionParserGenerator extends AbstractModuleGenerator {

    public static final String ANY_XML_CHILD_DEFINITION_PARSER_ROLE = "AnyXmlChildDefinitionParser";

    public void generate(Element type) throws GenerationException {

        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Processor processor = executableElement.getAnnotation(Processor.class);

            if (processor == null)
                continue;

            // generate extra parser
            for (VariableElement variable : executableElement.getParameters()) {
                if (context.getTypeMirrorUtils().isXmlType(variable.asType())) {
                    DefinedClass anyXmlChildDefinitionParser = getAnyXmlChildDefinitionParserClass(variable);
                    generateAnyXmlChildDefinitionParser(anyXmlChildDefinitionParser);

                    break;
                }
            }
        }
    }

    private DefinedClass getAnyXmlChildDefinitionParserClass(VariableElement variableElement) {
        String anyXmlChildDefinitionParserClassName = context.getNameUtils().generateClassNameInPackage(variableElement, "AnyXmlChildDefinitionParser");
        JPackage pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(anyXmlChildDefinitionParserClassName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(anyXmlChildDefinitionParserClassName), ChildDefinitionParser.class);

        context.setClassRole(ANY_XML_CHILD_DEFINITION_PARSER_ROLE, clazz);

        return clazz;
    }


    private void generateAnyXmlChildDefinitionParser(DefinedClass anyXmlChildDefinitionParser) {
        // generate constructor
        generateConstructor(anyXmlChildDefinitionParser);

        // getBeanClass
        generateGetBeanClass(anyXmlChildDefinitionParser, ref(String.class).dotclass());

        // processProperty
        generateProcessProperty(anyXmlChildDefinitionParser);

        // generateParseInternal
        generateParseInternal(anyXmlChildDefinitionParser);
    }

    private void generateGetBeanClass(DefinedClass beanDefinitionparser, Expression expr) {
        Method getBeanClass = beanDefinitionparser.method(Modifier.PROTECTED, ref(Class.class), "getBeanClass");
        Variable element = getBeanClass.param(ref(org.w3c.dom.Element.class), "element");
        getBeanClass.body()._return(expr);

    }

    private void generateParseInternal(DefinedClass anyXmlChildDefinitionParser) {
        Method parseInternal = anyXmlChildDefinitionParser.method(Modifier.PROTECTED, ref(AbstractBeanDefinition.class), "parseInternal");
        Variable element = parseInternal.param(ref(org.w3c.dom.Element.class), "element");
        Variable parserContext = parseInternal.param(ref(ParserContext.class), "parserContext");

        Variable bd = parseInternal.body().decl(ref(AbstractBeanDefinition.class), "bd");
        Invocation superParserInternal = JExpr._super().invoke("parseInternal");
        superParserInternal.arg(element);
        superParserInternal.arg(parserContext);

        parseInternal.body().assign(bd, superParserInternal);

        Invocation setAttribute = bd.invoke("setAttribute");
        setAttribute.arg(ref(MuleHierarchicalBeanDefinitionParserDelegate.class).boxify().staticRef("MULE_NO_RECURSE"));
        setAttribute.arg(ref(Boolean.class).boxify().staticRef("TRUE"));
        parseInternal.body().add(setAttribute);

        Conditional ifBlock = parseInternal.body()._if(Op.eq(ref(Node.class).staticRef("ELEMENT_NODE"), element.invoke("getNodeType")));

        Variable nodeList = ifBlock._then().decl(ref(NodeList.class), "childs", element.invoke("getChildNodes"));
        Variable i = ifBlock._then().decl(context.getCodeModel().INT, "i");
        ForLoop forLoop = ifBlock._then()._for();
        forLoop.init(i, JExpr.lit(0));
        forLoop.test(Op.lt(i, nodeList.invoke("getLength")));
        forLoop.update(Op.incr(i));
        Variable child = forLoop.body().decl(ref(Node.class), "child", nodeList.invoke("item").arg(i));

        Conditional ifBlock2 = forLoop.body()._if(Op.eq(ref(Node.class).staticRef("ELEMENT_NODE"), child.invoke("getNodeType")));

        TryStatement tryBlock = ifBlock2._then()._try();
        Variable domSource = tryBlock.body().decl(ref(DOMSource.class), "domSource", JExpr._new(ref(DOMSource.class)).arg(child));
        Variable stringWriter = tryBlock.body().decl(ref(StringWriter.class), "stringWriter", JExpr._new(ref(StringWriter.class)));
        Variable streamResult = tryBlock.body().decl(ref(StreamResult.class), "result", JExpr._new(ref(StreamResult.class)).arg(stringWriter));
        Variable tf = tryBlock.body().decl(ref(TransformerFactory.class), "tf", ref(TransformerFactory.class).staticInvoke("newInstance"));
        Variable transformer = tryBlock.body().decl(ref(Transformer.class), "transformer", tf.invoke("newTransformer"));
        Invocation transform = transformer.invoke("transform");
        transform.arg(domSource);
        transform.arg(streamResult);
        tryBlock.body().add(transform);
        tryBlock.body().add(stringWriter.invoke("flush"));

        //bd.getPropertyValues().add(clazzProperty, arguments);
        Invocation add = bd.invoke("getConstructorArgumentValues").invoke("addIndexedArgumentValue");
        add.arg(JExpr.lit(0));
        add.arg(stringWriter.invoke("toString"));
        tryBlock.body().add(add);

        generateReThrow(tryBlock, TransformerConfigurationException.class);
        generateReThrow(tryBlock, TransformerException.class);
        generateReThrow(tryBlock, TransformerFactoryConfigurationError.class);

        parseInternal.body()._return(bd);
    }

    private void generateReThrow(TryStatement tryBlock, Class<?> clazz) {
        CatchBlock catchBlock = tryBlock._catch(ref(clazz).boxify());
        Variable e = catchBlock.param("e");
        catchBlock.body()._throw(JExpr._new(ref(UnhandledException.class)).arg(e));
    }

    private void generateConstructor(DefinedClass anyXmlChildDefinitionParser) {
        Method constructor = anyXmlChildDefinitionParser.constructor(Modifier.PUBLIC);
        Variable setterMethod = constructor.param(ref(String.class), "setterMethod");
        Variable clazz = constructor.param(ref(Class.class), "clazz");

        Invocation superInvocation = constructor.body().invoke("super");
        superInvocation.arg(setterMethod);
        superInvocation.arg(clazz);

        Invocation addIgnored = constructor.body().invoke("addIgnored");
        addIgnored.arg("xmlns");
    }

    private void generateProcessProperty(DefinedClass xmlAnyChildDefinitionParser) {
        Method processProperty = xmlAnyChildDefinitionParser.method(Modifier.PROTECTED, context.getCodeModel().VOID, "processProperty");
        Variable attribute = processProperty.param(ref(Attr.class), "attribute");
        Variable beanAssembler = processProperty.param(ref(BeanAssembler.class), "assembler");
    }


}
