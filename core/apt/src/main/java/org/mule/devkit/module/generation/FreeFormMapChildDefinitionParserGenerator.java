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

import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.annotations.Source;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.Expression;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.ForLoop;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.Variable;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FreeFormMapChildDefinitionParserGenerator extends AbstractMessageGenerator {

    public static final String ROLE = "FreeFormMapChildDefinitionParser";

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
                if (context.getTypeMirrorUtils().isMap(variable.asType())) {
                    shouldGenerate = true;
                }
            }
        }

        if (shouldGenerate) {
            DefinedClass freeFormMapChildDefinitionParser = getFreeFormMapChildDefinitionParserClass((TypeElement) type);
            generateFreeFormMapChildDefinitionParser(freeFormMapChildDefinitionParser);
        }
    }

    private void generateFreeFormMapChildDefinitionParser(DefinedClass freeFormMapChildDefinitionParser) {

        Method constructor = freeFormMapChildDefinitionParser.constructor(Modifier.PUBLIC);
        Variable setterMethod = constructor.param(String.class, "setterMethod");
        Variable clazz = constructor.param(Class.class, "clazz");

        Invocation sup = constructor.body().invoke("super");
        sup.arg(setterMethod);
        sup.arg(clazz);

        constructor.body().invoke("addIgnored").arg("xmlns");

        Method parseInternal = freeFormMapChildDefinitionParser.method(Modifier.PROTECTED, AbstractBeanDefinition.class, "parseInternal");
        Variable element = parseInternal.param(org.w3c.dom.Element.class, "element");
        Variable parserContext = parseInternal.param(ParserContext.class, "parserContext");

        Variable bd = parseInternal.body().decl(ref(AbstractBeanDefinition.class), "bd", ExpressionFactory._null());
        Variable isFreeForm = parseInternal.body().decl(context.getCodeModel().BOOLEAN, "isFreeForm", ExpressionFactory.FALSE);

        Invocation superParseInternal = ExpressionFactory._super().invoke("parseInternal");
        superParseInternal.arg(element);
        superParseInternal.arg(parserContext);
        parseInternal.body().assign(bd, superParseInternal);

        isFreeForm(parseInternal, element, isFreeForm);

        Conditional ifBlock = parseInternal.body()._if(isFreeForm);
        Invocation setAttribute = bd.invoke("setAttribute");
        setAttribute.arg(ref(MuleHierarchicalBeanDefinitionParserDelegate.class).boxify().staticRef("MULE_NO_RECURSE"));
        setAttribute.arg(ref(Boolean.class).boxify().staticRef("TRUE"));
        ifBlock._then().add(setAttribute);

        Variable map = ifBlock._then().decl(ref(Map.class), "map", ExpressionFactory._new(ref(HashMap.class)));

        Variable nodeList = ifBlock._then().decl(ref(NodeList.class), "childs", element.invoke("getChildNodes"));
        Variable i = ifBlock._then().decl(context.getCodeModel().INT, "i");
        ForLoop forLoop = ifBlock._then()._for();
        forLoop.init(i, ExpressionFactory.lit(0));
        forLoop.test(Op.lt(i, nodeList.invoke("getLength")));
        forLoop.update(Op.incr(i));
        Variable child = forLoop.body().decl(ref(Node.class), "child", nodeList.invoke("item").arg(i));

        Conditional ifBlock2 = forLoop.body()._if(Op.eq(ref(Node.class).staticRef("ELEMENT_NODE"), child.invoke("getNodeType")));
        Invocation put = map.invoke("put");
        put.arg(child.invoke("getLocalName"));
        put.arg(child.invoke("getTextContent"));
        ifBlock2._then().add(put);

        ifBlock._then().add(bd.invoke("getPropertyValues").invoke("addPropertyValue").arg("sourceMap").arg(map));

        parseInternal.body()._return(bd);
    }

    private void isFreeForm(Method parseInternal, Variable element, Variable freeForm) {
        Conditional ifBlock = parseInternal.body()._if(Op.eq(ref(Node.class).staticRef("ELEMENT_NODE"), element.invoke("getNodeType")));

        Variable nodeList = ifBlock._then().decl(ref(NodeList.class), "childs", element.invoke("getChildNodes"));
        Variable i = ifBlock._then().decl(context.getCodeModel().INT, "i");
        ForLoop forLoop = ifBlock._then()._for();
        forLoop.init(i, ExpressionFactory.lit(0));
        forLoop.test(Op.lt(i, nodeList.invoke("getLength")));
        forLoop.update(Op.incr(i));
        Variable child = forLoop.body().decl(ref(Node.class), "child", nodeList.invoke("item").arg(i));

        Expression targetNamespace = ExpressionFactory.lit(context.getSchemaModel().getSchemaLocations().get(0).getSchema().getTargetNamespace());
        Conditional ifBlock2 = forLoop.body()._if(
                Op.cand(Op.eq(ref(Node.class).staticRef("ELEMENT_NODE"), child.invoke("getNodeType")),
                        Op.not(targetNamespace.invoke("equals").arg(child.invoke("getNamespaceURI"))))
        );
        ifBlock2._then().assign(freeForm, ExpressionFactory.TRUE);
        ifBlock2._then()._break();
    }

    private DefinedClass getFreeFormMapChildDefinitionParserClass(TypeElement typeElement) {
        String ListEntryChildDefinitionParserName = context.getNameUtils().generateClassNameInPackage(typeElement, "FreeFormMapChildDefinitionParser");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(ListEntryChildDefinitionParserName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(ListEntryChildDefinitionParserName), ChildDefinitionParser.class);

        context.setClassRole(ROLE, clazz);

        return clazz;
    }
}
