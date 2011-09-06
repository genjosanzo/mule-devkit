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
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.devkit.generation.DevkitTypeElement;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Package;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public class NamespaceHandlerGenerator extends AbstractMessageGenerator {

    @Override
    protected boolean shouldGenerate(DevkitTypeElement typeElement) {
        return true;
    }

    @Override
    protected void doGenerate(DevkitTypeElement typeElement) {
        DefinedClass namespaceHandlerClass = getNamespaceHandlerClass(typeElement);

        Method init = namespaceHandlerClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "init");
        init.javadoc().add("Invoked by the {@link DefaultBeanDefinitionDocumentReader} after construction but before any custom elements are parsed. \n@see NamespaceHandlerSupport#registerBeanDefinitionParser(String, BeanDefinitionParser)");

        registerConfig(init, typeElement);
        registerBeanDefinitionParserForEachProcessor(typeElement, init);
        registerBeanDefinitionParserForEachSource(typeElement, init);
        registerBeanDefinitionParserForEachTransformer(typeElement, init);
    }

    private DefinedClass getNamespaceHandlerClass(Element typeElement) {
        String namespaceHandlerName = context.getNameUtils().generateClassName((TypeElement) typeElement, ".config.spring", "NamespaceHandler");
        Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(namespaceHandlerName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(namespaceHandlerName), NamespaceHandlerSupport.class);

        Module module = typeElement.getAnnotation(Module.class);
        String targetNamespace = module.namespace();
        if (targetNamespace == null || targetNamespace.length() == 0) {
            targetNamespace = SchemaConstants.BASE_NAMESPACE + module.name();
        }
        clazz.javadoc().add("Registers bean definitions parsers for handling elements in <code>" + targetNamespace + "</code>.");

        return clazz;
    }

    private void registerConfig(Method init, TypeElement pojo) {
        DefinedClass configBeanDefinitionParser = context.getClassForRole(context.getNameUtils().generateConfigDefParserRoleKey(pojo));
        init.body().invoke("registerBeanDefinitionParser").arg("config").arg(ExpressionFactory._new(configBeanDefinitionParser));
    }

    private void registerBeanDefinitionParserForEachProcessor(DevkitTypeElement typeElement, Method init) {
        for (ExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            registerBeanDefinitionParserForProcessor(init, executableElement);
        }
    }

    private void registerBeanDefinitionParserForEachSource(DevkitTypeElement typeElement, Method init) {
        for (ExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Source.class)) {
            registerBeanDefinitionParserForSource(init, executableElement);
        }
    }

    private void registerBeanDefinitionParserForEachTransformer(DevkitTypeElement typeElement, Method init) {
        for (ExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Transformer.class)) {
            Invocation registerMuleBeanDefinitionParser = init.body().invoke("registerBeanDefinitionParser");
            registerMuleBeanDefinitionParser.arg(ExpressionFactory.lit(context.getNameUtils().uncamel(executableElement.getSimpleName().toString())));
            String transformerClassName = context.getNameUtils().generateClassName(executableElement, "Transformer");
            transformerClassName = context.getNameUtils().getPackageName(transformerClassName) + ".config." + context.getNameUtils().getClassName(transformerClassName);
            registerMuleBeanDefinitionParser.arg(ExpressionFactory._new(ref(MessageProcessorDefinitionParser.class)).arg(ref(transformerClassName).boxify().dotclass()));
        }
    }

    private void registerBeanDefinitionParserForProcessor(Method init, ExecutableElement executableElement) {
        DefinedClass beanDefinitionParser = getBeanDefinitionParserClass(executableElement);

        Processor processor = executableElement.getAnnotation(Processor.class);
        String elementName = executableElement.getSimpleName().toString();
        if (processor.name().length() != 0)
            elementName = processor.name();

        init.body().invoke("registerBeanDefinitionParser").arg(ExpressionFactory.lit(context.getNameUtils().uncamel(elementName))).arg(ExpressionFactory._new(beanDefinitionParser));
    }

    private void registerBeanDefinitionParserForSource(Method init, ExecutableElement executableElement) {
        DefinedClass beanDefinitionParser = getBeanDefinitionParserClass(executableElement);

        Source source = executableElement.getAnnotation(Source.class);
        String elementName = executableElement.getSimpleName().toString();
        if (source.name().length() != 0)
            elementName = source.name();

        init.body().invoke("registerBeanDefinitionParser").arg(ExpressionFactory.lit(context.getNameUtils().uncamel(elementName))).arg(ExpressionFactory._new(beanDefinitionParser));
    }
}