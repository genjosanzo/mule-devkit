package org.mule.devkit.apt.generator;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import org.apache.commons.lang.StringUtils;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.util.ClassNameUtils;
import org.w3c.dom.Element;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractCodeGenerator extends ContextualizedGenerator {
    public AbstractCodeGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    protected JDefinedClass getOrCreateClass(String className) {
        JClass generatedAnnotation = getContext().getCodeModel().ref("javax.annotation.Generated");

        try {
            JPackage pkg = getContext().getCodeModel()._package(ClassNameUtils.getPackageName(className));
            return pkg._class(ClassNameUtils.getClassName(className));
        } catch (JClassAlreadyExistsException e) {
            return e.getExistingClass();
        }
    }

    protected JDefinedClass getOrCreateClass(String className, JClass ext) {
        JClass generatedAnnotation = getContext().getCodeModel().ref("javax.annotation.Generated");

        try {
            JPackage pkg = getContext().getCodeModel()._package(ClassNameUtils.getPackageName(className));
            JDefinedClass clazz = pkg._class(ClassNameUtils.getClassName(className));
            clazz._extends(ext);

            return clazz;
        } catch (JClassAlreadyExistsException e) {
            return e.getExistingClass();
        }
    }

    protected JDefinedClass getOrCreateClass(String className, Class<?> ext) {
        JClass generatedAnnotation = getContext().getCodeModel().ref("javax.annotation.Generated");

        try {
            JPackage pkg = getContext().getCodeModel()._package(ClassNameUtils.getPackageName(className));
            JDefinedClass clazz = pkg._class(ClassNameUtils.getClassName(className));
            clazz._extends(ext);

            return clazz;
        } catch (JClassAlreadyExistsException e) {
            return e.getExistingClass();
        }
    }

    protected JDefinedClass getOrCreateClass(String className, List<Class> impls) {
        JClass generatedAnnotation = getContext().getCodeModel().ref("javax.annotation.Generated");

        try {
            JPackage pkg = getContext().getCodeModel()._package(ClassNameUtils.getPackageName(className));
            JDefinedClass clazz = pkg._class(ClassNameUtils.getClassName(className));
            for (Class impl : impls)
                clazz._implements(impl);

            return clazz;
        } catch (JClassAlreadyExistsException e) {
            return e.getExistingClass();
        }
    }


    protected JDefinedClass getBeanDefinitionParserClass(ExecutableElement executableElement) {
        String beanDefinitionParserName = getBeanDefinitionParserClassNameFor(executableElement);
        JDefinedClass beanDefinitionParser = getOrCreateClass(beanDefinitionParserName, ChildDefinitionParser.class);

        return beanDefinitionParser;
    }

    protected String getBeanDefinitionParserClassNameFor(ExecutableElement executableElement) {
        TypeElement parentClass = ElementFilter.typesIn(Arrays.asList(executableElement.getEnclosingElement())).get(0);
        String packageName = ClassNameUtils.getPackageName(getContext().getElements().getBinaryName(parentClass).toString());
        String className = StringUtils.capitalize(executableElement.getSimpleName().toString()) + "BeanDefinitionParser";

        return packageName + "." + className;

    }

    protected String getLifecycleWrapperClassNameFor(TypeElement typeElement) {
        String packageName = ClassNameUtils.getPackageName(getContext().getElements().getBinaryName(typeElement).toString());
        String className = StringUtils.capitalize(typeElement.getSimpleName().toString()) + "LifecycleWrapper";

        return packageName + "." + className;
    }

    protected JDefinedClass getLifecycleWrapperClass(TypeElement typeElement) {
        String lifecycleWrapperClassName = getLifecycleWrapperClassNameFor(typeElement);
        JDefinedClass lifecycleWrapper = getOrCreateClass(lifecycleWrapperClassName, ref(typeElement.asType()).boxify());

        return lifecycleWrapper;
    }

    protected String getMessageProcessorClassNameFor(ExecutableElement executableElement) {
        TypeElement parentClass = ElementFilter.typesIn(Arrays.asList(executableElement.getEnclosingElement())).get(0);
        String packageName = ClassNameUtils.getPackageName(getContext().getElements().getBinaryName(parentClass).toString());
        String className = StringUtils.capitalize(executableElement.getSimpleName().toString()) + "MessageProcessor";

        return packageName + "." + className;

    }

    protected JDefinedClass getMessageProcessorClass(ExecutableElement executableElement) {
        String messageProcessorClassName = getMessageProcessorClassNameFor(executableElement);
        JDefinedClass messageProcessor = getOrCreateClass(messageProcessorClassName, Arrays.asList(new Class[]{Initialisable.class, MessageProcessor.class, MuleContextAware.class}));

        return messageProcessor;
    }

    protected String getAnyXmlChildDefinitionParserClassNameFor(VariableElement variableElement) {
        TypeElement parentClass = ElementFilter.typesIn(Arrays.asList(variableElement.getEnclosingElement().getEnclosingElement())).get(0);
        String packageName = ClassNameUtils.getPackageName(getContext().getElements().getBinaryName(parentClass).toString());
        String className = "AnyXmlChildDefinitionParser";

        return packageName + "." + className;

    }


    protected JDefinedClass getAnyXmlChildDefinitionParserClass(VariableElement variableElement) {
        String anyXmlChildDefinitionParserClassName = getAnyXmlChildDefinitionParserClassNameFor(variableElement);
        JDefinedClass anyXmlChildDefinitionParser = getOrCreateClass(anyXmlChildDefinitionParserClassName, ChildDefinitionParser.class);

        return anyXmlChildDefinitionParser;
    }

    protected String getDummyInboundEndpointClassName(ExecutableElement executableElement) {
        TypeElement parentClass = ElementFilter.typesIn(Arrays.asList(executableElement.getEnclosingElement())).get(0);
        String packageName = ClassNameUtils.getPackageName(getContext().getElements().getBinaryName(parentClass).toString());
        String className = "DummyInboundEndpoint";

        return packageName + "." + className;
    }

    protected JDefinedClass getDummyInboundEndpointClass(ExecutableElement executableElement) {
        String dummyInboundEndpointName = getDummyInboundEndpointClassName(executableElement);
        JDefinedClass dummyInboundEndpoint = getOrCreateClass(dummyInboundEndpointName, ImmutableEndpoint.class);

        return dummyInboundEndpoint;
    }

    protected JClass ref(Class<?> clazz)
    {
        JClass ret = null;
        try {
            ret = getContext().getCodeModel().ref(clazz).boxify();
        } catch (MirroredTypeException mte) {
            ret = ref(mte.getTypeMirror()).boxify();
        }

        return ret;
    }

    protected JType ref(TypeMirror typeMirror) {
        return ref(typeMirror.toString());
    }

    protected JType ref(String typeMirror) {
        JType jtype = null;
        try {
            jtype = getContext().getCodeModel().parseType(typeMirror);
        } catch (ClassNotFoundException e) {
            jtype = getContext().getCodeModel().ref(typeMirror);
        }

        return jtype;
    }

    protected void generateGetBeanClass(JDefinedClass beanDefinitionparser, JExpression expr) {
        JMethod getBeanClass = beanDefinitionparser.method(JMod.PROTECTED, ref(Class.class), "getBeanClass");
        JVar element = getBeanClass.param(ref(Element.class), "element");
        getBeanClass.body()._return(expr);

    }

    protected String getTransformerNameFor(ExecutableElement executableElement) {
        TypeElement parentClass = ElementFilter.typesIn(Arrays.asList(executableElement.getEnclosingElement())).get(0);
        String packageName = ClassNameUtils.getPackageName(getContext().getElements().getBinaryName(parentClass).toString());
        String className = StringUtils.capitalize(executableElement.getSimpleName().toString()) + "Transformer";

        return packageName + "." + className;
    }


}
