package org.mule.devkit.apt.code;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import org.apache.commons.lang.StringUtils;
import org.mule.config.spring.handlers.AbstractPojoNamespaceHandler;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.util.ClassNameUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.Arrays;
import java.util.List;

public class NamespaceHandlerCodeGenerator extends AbstractCodeGenerator {
    public NamespaceHandlerCodeGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement type) throws CodeGenerationException {
        String namespaceHandlerName = getContext().getElements().getBinaryName(type) + "NamespaceHandler";

        JDefinedClass namespaceHandlerClass = getOrCreateClass(namespaceHandlerName);
        namespaceHandlerClass._extends(AbstractPojoNamespaceHandler.class);

        JMethod init = namespaceHandlerClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "init");

        registerBeanDefinitionParserForEachProcessor(type, init);
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

    private void registerBeanDefinitionParser(JMethod init, ExecutableElement executableElement) {
        Processor processor = executableElement.getAnnotation(Processor.class);
        String beanDefinitionParserName = getBeanDefinitionParserClassNameFor(executableElement);
        JDefinedClass beanDefinitionParser = getOrCreateClass(beanDefinitionParserName);
        String elementName = executableElement.getSimpleName().toString();
        if( processor.name().length() != 0 )
            elementName = processor.name();

        init.body().invoke("registerMuleBeanDefinitionParser").arg(JExpr.lit(elementName)).arg(JExpr._new(beanDefinitionParser));
    }

    private String getBeanDefinitionParserClassNameFor(ExecutableElement executableElement) {
        TypeElement parentClass = ElementFilter.typesIn(Arrays.asList(executableElement.getEnclosingElement())).get(0);
        String packageName = ClassNameUtils.getPackageName(getContext().getElements().getBinaryName(parentClass).toString());
        String className = StringUtils.capitalize(executableElement.getSimpleName().toString()) + "BeanDefinitionParser";

        return packageName + "." + className;

    }

    private JDefinedClass getOrCreateClass(String className) {
        JClass generatedAnnotation = getContext().getCodeModel().ref("javax.annotation.Generated");

        try {
            JPackage pkg = getContext().getCodeModel()._package(ClassNameUtils.getPackageName(className));
            return pkg._class(ClassNameUtils.getClassName(className));
        } catch (JClassAlreadyExistsException e) {
            return e.getExistingClass();
        }
    }
}
