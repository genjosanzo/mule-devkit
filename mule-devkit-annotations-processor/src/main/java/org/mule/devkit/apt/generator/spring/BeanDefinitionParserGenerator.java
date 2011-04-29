package org.mule.devkit.apt.generator.spring;

import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import org.apache.commons.lang.StringUtils;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.GenerationException;
import org.mule.devkit.apt.util.ClassNameUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.Arrays;
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

        // add constructor
        JMethod constructor = beanDefinitionparser.constructor(JMod.PUBLIC);
        JDefinedClass messageProcessorClass = getMessageProcessorClass(executableElement);
        constructor.body().invoke("super").arg(JExpr.lit("messageProcessor")).arg(JExpr.dotclass(messageProcessorClass));
    }


}
