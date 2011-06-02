package org.mule.devkit.apt.generator.spring;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import org.mule.config.spring.handlers.AbstractPojoNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.annotations.Transformer;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.AbstractCodeGenerator;
import org.mule.devkit.apt.generator.GenerationException;
import org.mule.devkit.apt.util.CodeModelUtils;
import org.mule.devkit.apt.util.Inflection;
import org.mule.devkit.apt.util.NameUtils;
import org.springframework.beans.factory.config.ListFactoryBean;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;

public class NamespaceHandlerGenerator extends AbstractCodeGenerator {
    public NamespaceHandlerGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement type) throws GenerationException {
        String namespaceHandlerName = getContext().getElements().getBinaryName(type) + "NamespaceHandler";

        JDefinedClass namespaceHandlerClass = getOrCreateClass(namespaceHandlerName);
        namespaceHandlerClass._extends(AbstractPojoNamespaceHandler.class);

        JMethod init = namespaceHandlerClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "init");

        registerConfig(init, type);
        registerBeanDefinitionParserForEachProcessor(type, init);
        registerBeanDefinitionParserForEachTransformer(type, init);
    }

    private void registerConfig(JMethod init, TypeElement module) {
        JInvocation registerPojo = JExpr.invoke("registerPojo");
        registerPojo.arg("config");
        registerPojo.arg(getLifecycleWrapperClass(module).boxify().dotclass());
        init.body().add(registerPojo);

        java.util.List<VariableElement> variables = ElementFilter.fieldsIn(module.getEnclosedElements());
        for (VariableElement variable : variables) {
            registerMuleBeanDefinitionParserFor(init, variable, ref(module.asType()).boxify());
        }
    }

    private void registerMuleBeanDefinitionParserFor(JMethod init, VariableElement variable, JClass parentClass) {
        if (CodeModelUtils.isXmlType(variable)) {
            JInvocation newAnyXmlChildDefinitionParser = JExpr._new(getAnyXmlChildDefinitionParserClass(variable));
            newAnyXmlChildDefinitionParser.arg(JExpr.lit(variable.getSimpleName().toString()));
            newAnyXmlChildDefinitionParser.arg(JExpr.dotclass(parentClass));

            init.body().invoke("registerMuleBeanDefinitionParser").arg(JExpr.lit(variable.getSimpleName().toString())).arg(newAnyXmlChildDefinitionParser);
        } else {
            if (CodeModelUtils.isArrayOrList(getContext().getTypes(), variable.asType())) {
                JInvocation childDefinitionParser = JExpr._new(ref(ChildDefinitionParser.class));
                childDefinitionParser.arg(JExpr.lit(variable.getSimpleName().toString()));
                childDefinitionParser.arg(ref(ListFactoryBean.class).dotclass());

                init.body().invoke("registerMuleBeanDefinitionParser").arg(JExpr.lit(variable.getSimpleName().toString())).arg(childDefinitionParser);

                JInvocation childListEntryDefinitionParser = JExpr._new(ref(ChildListEntryDefinitionParser.class));
                childListEntryDefinitionParser.arg("sourceList");

                init.body().invoke("registerMuleBeanDefinitionParser").arg(JExpr.lit(Inflection.singularize(variable.getSimpleName().toString()))).arg(childListEntryDefinitionParser);


                /*
                registerMuleBeanDefinitionParser("${parameter.getName()}", new ChildDefinitionParser("${parameter.getName()}", ListFactoryBean.class));
                registerMuleBeanDefinitionParser("<@singularize>${parameter.getName()}</@singularize>", new ChildListEntryDefinitionParser("sourceList"));
                <#elseif parameter.getType().isMap()>
                registerMuleBeanDefinitionParser("${parameter.getName()}", new ChildDefinitionParser("${parameter.getName()}", MapFactoryBean.class));
                registerMuleBeanDefinitionParser("<@singularize>${parameter.getName()}</@singularize>", new ChildMapEntryDefinitionParser("sourceMap"));
                */
            }
        }
    }

    private void registerBeanDefinitionParserForEachProcessor(TypeElement type, JMethod init) {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Processor processor = executableElement.getAnnotation(Processor.class);

            if (processor == null)
                continue;

            registerBeanDefinitionParser(init, executableElement);
        }
    }

    private void registerBeanDefinitionParserForEachTransformer(TypeElement type, JMethod init) {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Transformer transformer = executableElement.getAnnotation(Transformer.class);

            if (transformer == null)
                continue;

            JInvocation registerMuleBeanDefinitionParser = init.body().invoke("registerMuleBeanDefinitionParser");
            registerMuleBeanDefinitionParser.arg(JExpr.lit(NameUtils.uncamel(executableElement.getSimpleName().toString())));
            registerMuleBeanDefinitionParser.arg(JExpr._new(ref(MessageProcessorDefinitionParser.class)).arg(ref(getTransformerNameFor(executableElement)).boxify().dotclass()));
        }
    }

    private void registerBeanDefinitionParser(JMethod init, ExecutableElement executableElement) {
        JDefinedClass beanDefinitionParser = getBeanDefinitionParserClass(executableElement);
        JDefinedClass messageProcessor = getMessageProcessorClass(executableElement);

        Processor processor = executableElement.getAnnotation(Processor.class);
        String elementName = executableElement.getSimpleName().toString();
        if (processor.name().length() != 0)
            elementName = processor.name();

        init.body().invoke("registerMuleBeanDefinitionParser").arg(JExpr.lit(NameUtils.uncamel(elementName))).arg(JExpr._new(beanDefinitionParser));

        for (VariableElement variable : executableElement.getParameters()) {
            registerMuleBeanDefinitionParserFor(init, variable, messageProcessor);
        }
    }
}
