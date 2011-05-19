package org.mule.devkit.apt.generator.mule;

import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JVar;
import org.apache.commons.lang.StringUtils;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.AbstractCodeGenerator;
import org.mule.devkit.apt.generator.GenerationException;
import org.mule.devkit.apt.util.ClassNameUtils;
import org.mule.devkit.apt.util.CodeModelUtils;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

public class JaxbTransformerGenerator extends AbstractCodeGenerator {
    public JaxbTransformerGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement type) throws GenerationException {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Processor processor = executableElement.getAnnotation(Processor.class);

            if (processor == null)
                continue;

            for (VariableElement variable : executableElement.getParameters()) {
                if (CodeModelUtils.isXmlType(variable)) {
                    // get class
                    JDefinedClass jaxbTransformerClass = getJaxbTransformerClass(executableElement, variable);

                    // declare weight
                    JFieldVar weighting = jaxbTransformerClass.field(JMod.PRIVATE, getContext().getCodeModel().INT, "weighting", JOp.plus(ref(DiscoverableTransformer.class).boxify().staticRef("DEFAULT_PRIORITY_WEIGHTING"), JExpr.lit(1)));

                    // load JAXB context
                    JMethod loadJaxbContext = generateLoadJaxbContext(jaxbTransformerClass);

                    // declare JAXB context
                    JFieldVar jaxbContext = jaxbTransformerClass.field(JMod.PRIVATE | JMod.STATIC, ref(JAXBContext.class), "JAXB_CONTEXT", JExpr.invoke(loadJaxbContext).arg(JExpr.dotclass(ref(variable.asType()).boxify())));

                    //generate constructor
                    generateConstructor(jaxbTransformerClass, executableElement, variable);

                    // doTransform
                    generateDoTransform(jaxbTransformerClass, jaxbContext, variable);

                    // set and get weight
                    generateGetPriorityWeighting(jaxbTransformerClass, weighting);
                    generateSetPriorityWeighting(jaxbTransformerClass, weighting);

                    getContext().registerClassAtBoot(jaxbTransformerClass);
                }
            }
        }

    }

    private void generateSetPriorityWeighting(JDefinedClass jaxbTransformerClass, JFieldVar weighting) {
        JMethod setPriorityWeighting = jaxbTransformerClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "setPriorityWeighting");
        JVar localWeighting = setPriorityWeighting.param(getContext().getCodeModel().INT, "weighting");
        setPriorityWeighting.body().assign(JExpr._this().ref(weighting), localWeighting);
    }

    private void generateGetPriorityWeighting(JDefinedClass jaxbTransformerClass, JFieldVar weighting) {
        JMethod getPriorityWeighting = jaxbTransformerClass.method(JMod.PUBLIC, getContext().getCodeModel().INT, "getPriorityWeighting");
        getPriorityWeighting.body()._return(weighting);
    }

    private void generateDoTransform(JDefinedClass jaxbTransformerClass, JFieldVar jaxbContext, VariableElement variable) {
        JMethod doTransform = jaxbTransformerClass.method(JMod.PROTECTED, ref(Object.class), "doTransform");
        doTransform._throws(TransformerException.class);
        JVar src = doTransform.param(ref(Object.class), "src");
        JVar encoding = doTransform.param(ref(String.class), "encoding");

        JVar result = doTransform.body().decl(ref(variable.asType()).boxify(), "result", JExpr._null());

        JTryBlock tryBlock = doTransform.body()._try();
        JVar unmarshaller = tryBlock.body().decl(ref(Unmarshaller.class), "unmarshaller");
        tryBlock.body().assign(unmarshaller, jaxbContext.invoke("createUnmarshaller"));
        JVar inputStream = tryBlock.body().decl(ref(InputStream.class).boxify(), "is", JExpr._new(ref(ByteArrayInputStream.class).boxify()).arg(
                JExpr.invoke(JExpr.cast(ref(String.class), src), "getBytes").arg(encoding)
        ));

        JVar streamSource = tryBlock.body().decl(ref(StreamSource.class), "ss", JExpr._new(ref(StreamSource.class)).arg(inputStream));
        JInvocation unmarshal = unmarshaller.invoke("unmarshal");
        unmarshal.arg(streamSource);
        unmarshal.arg(JExpr.dotclass(ref(variable.asType()).boxify()));

        tryBlock.body().assign(result, unmarshal.invoke("getValue"));

        JCatchBlock unsupportedEncodingCatch = tryBlock._catch(ref(UnsupportedEncodingException.class).boxify());
        JVar unsupportedEncoding = unsupportedEncodingCatch.param("unsupportedEncoding");

        generateThrowTransformFailedException(unsupportedEncodingCatch, unsupportedEncoding, variable);

        JCatchBlock jaxbExceptionCatch = tryBlock._catch(ref(JAXBException.class).boxify());
        JVar jaxbException = jaxbExceptionCatch.param("jaxbException");

        generateThrowTransformFailedException(jaxbExceptionCatch, jaxbException, variable);

        doTransform.body()._return(result);
    }

    private void generateThrowTransformFailedException(JCatchBlock catchBlock, JVar exception, VariableElement variable) {
        JInvocation transformFailedInvoke = ref(CoreMessages.class).boxify().staticInvoke("transformFailed");
        transformFailedInvoke.arg("String");
        transformFailedInvoke.arg(JExpr.lit(ref(variable.asType()).boxify().fullName()));

        JInvocation transformerException = JExpr._new(ref(TransformerException.class).boxify());
        transformerException.arg(transformFailedInvoke);
        transformerException.arg(JExpr._this());
        transformerException.arg(exception);
        catchBlock.body()._throw(transformerException);
    }

    private JMethod generateLoadJaxbContext(JDefinedClass jaxbTransformerClass) {
        JMethod loadJaxbContext = jaxbTransformerClass.method(JMod.PRIVATE | JMod.STATIC, ref(JAXBContext.class), "loadJaxbContext");
        JVar clazz = loadJaxbContext.param(ref(Class.class), "clazz");
        JVar innerJaxbContext = loadJaxbContext.body().decl(ref(JAXBContext.class), "context");

        JTryBlock tryBlock = loadJaxbContext.body()._try();
        tryBlock.body().assign(innerJaxbContext, ref(JAXBContext.class).boxify().staticInvoke("newInstance").arg(clazz));
        JCatchBlock catchBlock = tryBlock._catch(ref(JAXBException.class).boxify());
        JVar e = catchBlock.param("e");
        catchBlock.body()._throw(JExpr._new(ref(RuntimeException.class)).arg(e));

        loadJaxbContext.body()._return(innerJaxbContext);

        return loadJaxbContext;
    }

    private void generateConstructor(JDefinedClass jaxbTransformerClass, ExecutableElement executableElement, VariableElement variable) {
        // generate constructor
        JMethod constructor = jaxbTransformerClass.constructor(JMod.PUBLIC);

        // register source data type
        registerSourceType(constructor);

        // register destination data type
        registerDestinationType(constructor, variable);

        constructor.body().invoke("setName").arg(getJaxbTransformerNameFor(executableElement, variable));
    }

    private void registerDestinationType(JMethod constructor, VariableElement variable) {
        JInvocation setReturnClass = constructor.body().invoke("setReturnClass");
        setReturnClass.arg(JExpr.dotclass(ref(variable.asType()).boxify()));
    }

    private void registerSourceType(JMethod constructor) {
        JInvocation registerSourceType = constructor.body().invoke("registerSourceType");
        registerSourceType.arg(ref(DataTypeFactory.class).boxify().staticRef("STRING"));
    }

    private String getJaxbTransformerNameFor(ExecutableElement executableElement, VariableElement variable) {
        TypeElement parentClass = ElementFilter.typesIn(Arrays.asList(executableElement.getEnclosingElement())).get(0);
        String packageName = ClassNameUtils.getPackageName(getContext().getElements().getBinaryName(parentClass).toString());
        DeclaredType declaredType = (DeclaredType) variable.asType();
        XmlType xmlType = declaredType.asElement().getAnnotation(XmlType.class);

        String className = StringUtils.capitalize(xmlType.name()) + "JaxbTransformer";

        return packageName + "." + className;

    }

    private JDefinedClass getJaxbTransformerClass(ExecutableElement executableElement, VariableElement variable) {
        String jaxbTransformerClassName = getJaxbTransformerNameFor(executableElement, variable);
        JDefinedClass jaxbTransformer = getOrCreateClass(jaxbTransformerClassName, AbstractTransformer.class);
        jaxbTransformer._implements(DiscoverableTransformer.class);

        return jaxbTransformer;
    }

}
