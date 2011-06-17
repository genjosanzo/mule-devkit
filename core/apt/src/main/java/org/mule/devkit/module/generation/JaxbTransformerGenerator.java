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

import org.apache.commons.lang.StringUtils;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.JCatchBlock;
import org.mule.devkit.model.code.JExpr;
import org.mule.devkit.model.code.JFieldVar;
import org.mule.devkit.model.code.JInvocation;
import org.mule.devkit.model.code.JMethod;
import org.mule.devkit.model.code.JMod;
import org.mule.devkit.model.code.JOp;
import org.mule.devkit.model.code.JPackage;
import org.mule.devkit.model.code.JTryBlock;
import org.mule.devkit.model.code.JVar;
import org.mule.transformer.types.DataTypeFactory;

import javax.lang.model.element.Element;
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

public class JaxbTransformerGenerator extends AbstractModuleGenerator {
    public void generate(Element type) throws GenerationException {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Processor processor = executableElement.getAnnotation(Processor.class);

            if (processor == null)
                continue;

            for (VariableElement variable : executableElement.getParameters()) {
                if (context.getTypeMirrorUtils().isXmlType(variable.asType())) {
                    // get class
                    DefinedClass jaxbTransformerClass = getJaxbTransformerClass(executableElement, variable);

                    // declare weight
                    JFieldVar weighting = jaxbTransformerClass.field(JMod.PRIVATE, context.getCodeModel().INT, "weighting", JOp.plus(ref(DiscoverableTransformer.class).staticRef("DEFAULT_PRIORITY_WEIGHTING"), JExpr.lit(1)));

                    // load JAXB context
                    JMethod loadJaxbContext = generateLoadJaxbContext(jaxbTransformerClass);

                    // declare JAXB context
                    JFieldVar jaxbContext = jaxbTransformerClass.field(JMod.PRIVATE | JMod.STATIC, JAXBContext.class, "JAXB_CONTEXT", JExpr.invoke(loadJaxbContext).arg(ref(variable.asType()).boxify().dotclass()));

                    //generate constructor
                    generateConstructor(jaxbTransformerClass, executableElement, variable);

                    // doTransform
                    generateDoTransform(jaxbTransformerClass, jaxbContext, variable);

                    // set and get weight
                    generateGetPriorityWeighting(jaxbTransformerClass, weighting);
                    generateSetPriorityWeighting(jaxbTransformerClass, weighting);

                    context.registerAtBoot(jaxbTransformerClass);
                }
            }
        }

    }

    private void generateSetPriorityWeighting(DefinedClass jaxbTransformerClass, JFieldVar weighting) {
        JMethod setPriorityWeighting = jaxbTransformerClass.method(JMod.PUBLIC, context.getCodeModel().VOID, "setPriorityWeighting");
        JVar localWeighting = setPriorityWeighting.param(context.getCodeModel().INT, "weighting");
        setPriorityWeighting.body().assign(JExpr._this().ref(weighting), localWeighting);
    }

    private void generateGetPriorityWeighting(DefinedClass jaxbTransformerClass, JFieldVar weighting) {
        JMethod getPriorityWeighting = jaxbTransformerClass.method(JMod.PUBLIC, context.getCodeModel().INT, "getPriorityWeighting");
        getPriorityWeighting.body()._return(weighting);
    }

    private void generateDoTransform(DefinedClass jaxbTransformerClass, JFieldVar jaxbContext, VariableElement variable) {
        JMethod doTransform = jaxbTransformerClass.method(JMod.PROTECTED, Object.class, "doTransform");
        doTransform._throws(TransformerException.class);
        JVar src = doTransform.param(Object.class, "src");
        JVar encoding = doTransform.param(String.class, "encoding");

        JVar result = doTransform.body().decl(ref(variable.asType()).boxify(), "result", JExpr._null());

        JTryBlock tryBlock = doTransform.body()._try();
        JVar unmarshaller = tryBlock.body().decl(Unmarshaller.class, "unmarshaller");
        tryBlock.body().assign(unmarshaller, jaxbContext.invoke("createUnmarshaller"));
        JVar inputStream = tryBlock.body().decl(ref(InputStream.class), "is", JExpr._new(ref(ByteArrayInputStream.class)).arg(
                JExpr.invoke(JExpr.cast(ref(String.class), src), "getBytes").arg(encoding)
        ));

        JVar streamSource = tryBlock.body().decl(ref(StreamSource.class), "ss", JExpr._new(ref(StreamSource.class)).arg(inputStream));
        JInvocation unmarshal = unmarshaller.invoke("unmarshal");
        unmarshal.arg(streamSource);
        unmarshal.arg(JExpr.dotclass(ref(variable.asType()).boxify()));

        tryBlock.body().assign(result, unmarshal.invoke("getValue"));

        JCatchBlock unsupportedEncodingCatch = tryBlock._catch(ref(UnsupportedEncodingException.class));
        JVar unsupportedEncoding = unsupportedEncodingCatch.param("unsupportedEncoding");

        generateThrowTransformFailedException(unsupportedEncodingCatch, unsupportedEncoding, variable);

        JCatchBlock jaxbExceptionCatch = tryBlock._catch(ref(JAXBException.class));
        JVar jaxbException = jaxbExceptionCatch.param("jaxbException");

        generateThrowTransformFailedException(jaxbExceptionCatch, jaxbException, variable);

        doTransform.body()._return(result);
    }

    private void generateThrowTransformFailedException(JCatchBlock catchBlock, JVar exception, VariableElement variable) {
        JInvocation transformFailedInvoke = ref(CoreMessages.class).staticInvoke("transformFailed");
        transformFailedInvoke.arg("String");
        transformFailedInvoke.arg(JExpr.lit(ref(variable.asType()).boxify().fullName()));

        JInvocation transformerException = JExpr._new(ref(TransformerException.class));
        transformerException.arg(transformFailedInvoke);
        transformerException.arg(JExpr._this());
        transformerException.arg(exception);
        catchBlock.body()._throw(transformerException);
    }

    private JMethod generateLoadJaxbContext(DefinedClass jaxbTransformerClass) {
        JMethod loadJaxbContext = jaxbTransformerClass.method(JMod.PRIVATE | JMod.STATIC, ref(JAXBContext.class), "loadJaxbContext");
        JVar clazz = loadJaxbContext.param(ref(Class.class), "clazz");
        JVar innerJaxbContext = loadJaxbContext.body().decl(ref(JAXBContext.class), "context");

        JTryBlock tryBlock = loadJaxbContext.body()._try();
        tryBlock.body().assign(innerJaxbContext, ref(JAXBContext.class).staticInvoke("newInstance").arg(clazz));
        JCatchBlock catchBlock = tryBlock._catch(ref(JAXBException.class));
        JVar e = catchBlock.param("e");
        catchBlock.body()._throw(JExpr._new(ref(RuntimeException.class)).arg(e));

        loadJaxbContext.body()._return(innerJaxbContext);

        return loadJaxbContext;
    }

    private void generateConstructor(DefinedClass jaxbTransformerClass, ExecutableElement executableElement, VariableElement variable) {
        // generate constructor
        JMethod constructor = jaxbTransformerClass.constructor(JMod.PUBLIC);

        // register source data type
        registerSourceType(constructor);

        // register destination data type
        registerDestinationType(constructor, variable);

        DeclaredType declaredType = (DeclaredType) variable.asType();
        XmlType xmlType = declaredType.asElement().getAnnotation(XmlType.class);

        constructor.body().invoke("setName").arg(StringUtils.capitalize(xmlType.name()) + "JaxbTransformer");
    }

    private void registerDestinationType(JMethod constructor, VariableElement variable) {
        JInvocation setReturnClass = constructor.body().invoke("setReturnClass");
        setReturnClass.arg(JExpr.dotclass(ref(variable.asType()).boxify()));
    }

    private void registerSourceType(JMethod constructor) {
        JInvocation registerSourceType = constructor.body().invoke("registerSourceType");
        registerSourceType.arg(ref(DataTypeFactory.class).staticRef("STRING"));
    }

    private DefinedClass getJaxbTransformerClass(ExecutableElement executableElement, VariableElement variable) {
        DeclaredType declaredType = (DeclaredType) variable.asType();
        XmlType xmlType = declaredType.asElement().getAnnotation(XmlType.class);
        TypeElement parentClass = ElementFilter.typesIn(Arrays.asList(executableElement.getEnclosingElement())).get(0);
        String packageName = context.getNameUtils().getPackageName(context.getElementsUtils().getBinaryName(parentClass).toString());
        JPackage pkg = context.getCodeModel()._package(packageName);
        DefinedClass dummyInboundEndpoint = pkg._class(StringUtils.capitalize(xmlType.name()) + "JaxbTransformer");
        dummyInboundEndpoint._implements(DiscoverableTransformer.class);

        return dummyInboundEndpoint;
    }

}
