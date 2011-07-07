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

import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Transformer;
import org.mule.config.spring.handlers.AbstractPojoNamespaceHandler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.api.annotations.Source;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Package;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.springframework.beans.factory.config.ListFactoryBean;
import org.springframework.beans.factory.config.MapFactoryBean;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;

public class NamespaceHandlerGenerator extends AbstractMessageGenerator {
    private int counter;

    public void generate(Element type) throws GenerationException {
        DefinedClass namespaceHandlerClass = getNamespaceHandlerClass(type);

        Method init = namespaceHandlerClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "init");
        init.javadoc().add("Invoked by the {@link DefaultBeanDefinitionDocumentReader} after construction but before any custom elements are parsed. \n@see NamespaceHandlerSupport#registerBeanDefinitionParser(String, BeanDefinitionParser)");

        registerConfig(init, type);
        registerBeanDefinitionParserForEachProcessor(type, init);
        registerBeanDefinitionParserForEachSource(type, init);
        registerBeanDefinitionParserForEachTransformer(type, init);
    }

    private DefinedClass getNamespaceHandlerClass(Element typeElement) {
        String namespaceHandlerName = context.getNameUtils().generateClassName((TypeElement) typeElement, ".config.spring", "NamespaceHandler");
        Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(namespaceHandlerName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(namespaceHandlerName), AbstractPojoNamespaceHandler.class);

        Module module = typeElement.getAnnotation(Module.class);
        String targetNamespace = module.namespace();
        if (targetNamespace == null || targetNamespace.length() == 0) {
            targetNamespace = SchemaConstants.BASE_NAMESPACE + module.name();
        }
        clazz.javadoc().add("Registers bean definitions parsers for handling elements in <code>" + targetNamespace + "</code>.");

        return clazz;
    }

    private void registerConfig(Method init, Element pojo) {
        Invocation registerPojo = ExpressionFactory.invoke("registerPojo");
        registerPojo.arg("config");
        DefinedClass pojoClass = context.getClassForRole(context.getNameUtils().generatePojoRoleKey((TypeElement) pojo));
        registerPojo.arg(pojoClass.dotclass());
        init.body().add(registerPojo);

        java.util.List<VariableElement> variables = ElementFilter.fieldsIn(pojo.getEnclosedElements());
        for (VariableElement variable : variables) {
            registerMuleBeanDefinitionParserFor(init, variable, ref(pojo.asType()).boxify());
        }
    }

    private void registerMuleBeanDefinitionParserFor(Method init, VariableElement variable, TypeReference parentClass) {
        if (context.getTypeMirrorUtils().isXmlType(variable.asType())) {
            DefinedClass anyXmlChildDefinitionParserClass = context.getClassForRole(AnyXmlChildDefinitionParserGenerator.ANY_XML_CHILD_DEFINITION_PARSER_ROLE);
            Invocation newAnyXmlChildDefinitionParser = ExpressionFactory._new(anyXmlChildDefinitionParserClass);
            newAnyXmlChildDefinitionParser.arg(ExpressionFactory.lit(variable.getSimpleName().toString()));
            newAnyXmlChildDefinitionParser.arg(ExpressionFactory.dotclass(parentClass));

            init.body().invoke("registerMuleBeanDefinitionParser").arg(ExpressionFactory.lit(variable.getSimpleName().toString())).arg(newAnyXmlChildDefinitionParser);
        } else {
            if (context.getTypeMirrorUtils().isArrayOrList(variable.asType())) {
                Invocation childDefinitionParserNew = ExpressionFactory._new(ref(ChildDefinitionParser.class));
                childDefinitionParserNew.arg(ExpressionFactory.lit(variable.getSimpleName().toString()));
                childDefinitionParserNew.arg(ref(ListFactoryBean.class).dotclass());

                Variable childDefinitionParser = init.body().decl(ref(ChildDefinitionParser.class), "x_" + counter, childDefinitionParserNew);
                counter++;
                Invocation addAlias = childDefinitionParser.invoke("addAlias").arg("ref").arg("sourceList");
                init.body().add(addAlias);

                init.body().invoke("registerMuleBeanDefinitionParser").arg(ExpressionFactory.lit(context.getNameUtils().uncamel(variable.getSimpleName().toString()))).arg(childDefinitionParser);

                DefinedClass listEntryChildDefinitionParser = context.getClassForRole(ListEntryChildDefinitionParserGenerator.ROLE);
                Invocation childListEntryDefinitionParser = ExpressionFactory._new(listEntryChildDefinitionParser);

                init.body().invoke("registerMuleBeanDefinitionParser").arg(ExpressionFactory.lit(context.getNameUtils().uncamel(context.getNameUtils().singularize(variable.getSimpleName().toString())))).arg(childListEntryDefinitionParser);
            } else if (context.getTypeMirrorUtils().isMap(variable.asType())) {
                DefinedClass freeFormChildDefinitionParser = context.getClassForRole(FreeFormMapChildDefinitionParserGenerator.ROLE);
                Invocation childDefinitionParserNew = ExpressionFactory._new(freeFormChildDefinitionParser);
                childDefinitionParserNew.arg("sourceMap");
                childDefinitionParserNew.arg(ExpressionFactory.lit(variable.getSimpleName().toString()));
                childDefinitionParserNew.arg(ref(MapFactoryBean.class).dotclass());

                Variable childDefinitionParser = init.body().decl(freeFormChildDefinitionParser, "x_" + counter, childDefinitionParserNew);
                counter++;
                Invocation addAlias = childDefinitionParser.invoke("addAlias").arg("ref").arg("sourceMap");
                init.body().add(addAlias);

                init.body().invoke("registerMuleBeanDefinitionParser").arg(ExpressionFactory.lit(context.getNameUtils().uncamel(variable.getSimpleName().toString()))).arg(childDefinitionParser);

                DefinedClass mapEntryChildDefinitionParser = context.getClassForRole(MapEntryChildDefinitionParserGenerator.ROLE);
                Invocation childMapEntryDefinitionParser = ExpressionFactory._new(mapEntryChildDefinitionParser);

                init.body().invoke("registerMuleBeanDefinitionParser").arg(ExpressionFactory.lit(context.getNameUtils().uncamel(context.getNameUtils().singularize(variable.getSimpleName().toString())))).arg(childMapEntryDefinitionParser);
            }
        }
    }

    private void registerBeanDefinitionParserForEachProcessor(Element type, Method init) {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Processor processor = executableElement.getAnnotation(Processor.class);

            if (processor == null)
                continue;

            registerBeanDefinitionParserForProcessor(init, executableElement);
        }
    }

    private void registerBeanDefinitionParserForEachSource(Element type, Method init) {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Source source = executableElement.getAnnotation(Source.class);

            if (source == null)
                continue;

            registerBeanDefinitionParserForSource(init, executableElement);
        }
    }

    private void registerBeanDefinitionParserForEachTransformer(Element type, Method init) {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Transformer transformer = executableElement.getAnnotation(Transformer.class);

            if (transformer == null)
                continue;

            Invocation registerMuleBeanDefinitionParser = init.body().invoke("registerMuleBeanDefinitionParser");
            registerMuleBeanDefinitionParser.arg(ExpressionFactory.lit(context.getNameUtils().uncamel(executableElement.getSimpleName().toString())));
            String transformerClassName = context.getNameUtils().generateClassName(executableElement, "Transformer");
            transformerClassName = context.getNameUtils().getPackageName(transformerClassName) + ".config." + context.getNameUtils().getClassName(transformerClassName);
            registerMuleBeanDefinitionParser.arg(ExpressionFactory._new(ref(MessageProcessorDefinitionParser.class)).arg(ref(transformerClassName).boxify().dotclass()));
        }
    }

    private void registerBeanDefinitionParserForProcessor(Method init, ExecutableElement executableElement) {
        DefinedClass beanDefinitionParser = getBeanDefinitionParserClass(executableElement);
        DefinedClass messageProcessor = getMessageProcessorClass(executableElement);

        Processor processor = executableElement.getAnnotation(Processor.class);
        String elementName = executableElement.getSimpleName().toString();
        if (processor.name().length() != 0)
            elementName = processor.name();

        init.body().invoke("registerMuleBeanDefinitionParser").arg(ExpressionFactory.lit(context.getNameUtils().uncamel(elementName))).arg(ExpressionFactory._new(beanDefinitionParser));

        for (VariableElement variable : executableElement.getParameters()) {
            registerMuleBeanDefinitionParserFor(init, variable, messageProcessor);
        }
    }

    private void registerBeanDefinitionParserForSource(Method init, ExecutableElement executableElement) {
        DefinedClass beanDefinitionParser = getBeanDefinitionParserClass(executableElement);
        DefinedClass messageSource = getMessageSourceClass(executableElement);

        Source source = executableElement.getAnnotation(Source.class);
        String elementName = executableElement.getSimpleName().toString();
        if (source.name().length() != 0)
            elementName = source.name();

        init.body().invoke("registerMuleBeanDefinitionParser").arg(ExpressionFactory.lit(context.getNameUtils().uncamel(elementName))).arg(ExpressionFactory._new(beanDefinitionParser));

        for (VariableElement variable : executableElement.getParameters()) {
            registerMuleBeanDefinitionParserFor(init, variable, messageSource);
        }
    }
}
