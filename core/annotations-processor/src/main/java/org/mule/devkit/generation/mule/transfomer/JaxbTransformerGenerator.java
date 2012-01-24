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

package org.mule.devkit.generation.mule.transfomer;

import org.apache.commons.lang.StringUtils;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.NamingContants;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.Package;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Variable;
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

public class JaxbTransformerGenerator extends AbstractModuleGenerator {

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return typeElement.hasAnnotation(Module.class) || typeElement.hasAnnotation(Connector.class);
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {
        for (ExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            for (VariableElement variable : executableElement.getParameters()) {
                if (context.getTypeMirrorUtils().isXmlType(variable.asType())) {
                    // get class
                    DefinedClass jaxbTransformerClass = getJaxbTransformerClass(executableElement, variable);

                    // declare weight
                    FieldVariable weighting = jaxbTransformerClass.field(Modifier.PRIVATE, context.getCodeModel().INT, "weighting", Op.plus(ref(DiscoverableTransformer.class).staticRef("DEFAULT_PRIORITY_WEIGHTING"), ExpressionFactory.lit(1)));

                    // load JAXB context
                    Method loadJaxbContext = generateLoadJaxbContext(jaxbTransformerClass);

                    // declare JAXB context
                    FieldVariable jaxbContext = jaxbTransformerClass.field(Modifier.PRIVATE | Modifier.STATIC, JAXBContext.class, "JAXB_CONTEXT", ExpressionFactory.invoke(loadJaxbContext).arg(ref(variable.asType()).boxify().dotclass()));

                    //generate constructor
                    generateConstructor(jaxbTransformerClass, variable);

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

    private void generateSetPriorityWeighting(DefinedClass jaxbTransformerClass, FieldVariable weighting) {
        Method setPriorityWeighting = jaxbTransformerClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "setPriorityWeighting");
        Variable localWeighting = setPriorityWeighting.param(context.getCodeModel().INT, "weighting");
        setPriorityWeighting.body().assign(ExpressionFactory._this().ref(weighting), localWeighting);
    }

    private void generateGetPriorityWeighting(DefinedClass jaxbTransformerClass, FieldVariable weighting) {
        Method getPriorityWeighting = jaxbTransformerClass.method(Modifier.PUBLIC, context.getCodeModel().INT, "getPriorityWeighting");
        getPriorityWeighting.body()._return(weighting);
    }

    private void generateDoTransform(DefinedClass jaxbTransformerClass, FieldVariable jaxbContext, VariableElement variable) {
        Method doTransform = jaxbTransformerClass.method(Modifier.PROTECTED, Object.class, "doTransform");
        doTransform._throws(TransformerException.class);
        Variable src = doTransform.param(Object.class, "src");
        Variable encoding = doTransform.param(String.class, "encoding");

        Variable result = doTransform.body().decl(ref(variable.asType()).boxify(), "result", ExpressionFactory._null());

        TryStatement tryBlock = doTransform.body()._try();
        Variable unmarshaller = tryBlock.body().decl(ref(Unmarshaller.class), "unmarshaller");
        tryBlock.body().assign(unmarshaller, jaxbContext.invoke("createUnmarshaller"));
        Variable inputStream = tryBlock.body().decl(ref(InputStream.class), "is", ExpressionFactory._new(ref(ByteArrayInputStream.class)).arg(
                ExpressionFactory.invoke(ExpressionFactory.cast(ref(String.class), src), "getBytes").arg(encoding)
        ));

        Variable streamSource = tryBlock.body().decl(ref(StreamSource.class), "ss", ExpressionFactory._new(ref(StreamSource.class)).arg(inputStream));
        Invocation unmarshal = unmarshaller.invoke("unmarshal");
        unmarshal.arg(streamSource);
        unmarshal.arg(ExpressionFactory.dotclass(ref(variable.asType()).boxify()));

        tryBlock.body().assign(result, unmarshal.invoke("getValue"));

        CatchBlock unsupportedEncodingCatch = tryBlock._catch(ref(UnsupportedEncodingException.class));
        Variable unsupportedEncoding = unsupportedEncodingCatch.param("unsupportedEncoding");

        generateThrowTransformFailedException(unsupportedEncodingCatch, unsupportedEncoding, variable);

        CatchBlock jaxbExceptionCatch = tryBlock._catch(ref(JAXBException.class));
        Variable jaxbException = jaxbExceptionCatch.param("jaxbException");

        generateThrowTransformFailedException(jaxbExceptionCatch, jaxbException, variable);

        doTransform.body()._return(result);
    }

    private void generateThrowTransformFailedException(CatchBlock catchBlock, Variable exception, VariableElement variable) {
        Invocation transformFailedInvoke = ref(CoreMessages.class).staticInvoke("transformFailed");
        transformFailedInvoke.arg("String");
        transformFailedInvoke.arg(ExpressionFactory.lit(ref(variable.asType()).boxify().fullName()));

        Invocation transformerException = ExpressionFactory._new(ref(TransformerException.class));
        transformerException.arg(transformFailedInvoke);
        transformerException.arg(ExpressionFactory._this());
        transformerException.arg(exception);
        catchBlock.body()._throw(transformerException);
    }

    private Method generateLoadJaxbContext(DefinedClass jaxbTransformerClass) {
        Method loadJaxbContext = jaxbTransformerClass.method(Modifier.PRIVATE | Modifier.STATIC, ref(JAXBContext.class), "loadJaxbContext");
        Variable clazz = loadJaxbContext.param(ref(Class.class), "clazz");
        Variable innerJaxbContext = loadJaxbContext.body().decl(ref(JAXBContext.class), "context");

        TryStatement tryBlock = loadJaxbContext.body()._try();
        tryBlock.body().assign(innerJaxbContext, ref(JAXBContext.class).staticInvoke("newInstance").arg(clazz));
        CatchBlock catchBlock = tryBlock._catch(ref(JAXBException.class));
        Variable e = catchBlock.param("e");
        catchBlock.body()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg(e));

        loadJaxbContext.body()._return(innerJaxbContext);

        return loadJaxbContext;
    }

    private void generateConstructor(DefinedClass jaxbTransformerClass, VariableElement variable) {
        // generate constructor
        Method constructor = jaxbTransformerClass.constructor(Modifier.PUBLIC);

        // register source data type
        registerSourceType(constructor);

        // register destination data type
        registerDestinationType(constructor, variable);

        DeclaredType declaredType = (DeclaredType) variable.asType();
        XmlType xmlType = declaredType.asElement().getAnnotation(XmlType.class);

        constructor.body().invoke("setName").arg(StringUtils.capitalize(xmlType.name()) + "JaxbTransformer");
    }

    private void registerDestinationType(Method constructor, VariableElement variable) {
        Invocation setReturnClass = constructor.body().invoke("setReturnClass");
        setReturnClass.arg(ExpressionFactory.dotclass(ref(variable.asType()).boxify()));
    }

    private void registerSourceType(Method constructor) {
        Invocation registerSourceType = constructor.body().invoke("registerSourceType");
        registerSourceType.arg(ref(DataTypeFactory.class).staticRef("STRING"));
    }

    private DefinedClass getJaxbTransformerClass(ExecutableElement executableElement, VariableElement variable) {
        DeclaredType declaredType = (DeclaredType) variable.asType();
        XmlType xmlType = declaredType.asElement().getAnnotation(XmlType.class);
        TypeElement parentClass = ElementFilter.typesIn(Arrays.asList(executableElement.getEnclosingElement())).get(0);
        String packageName = context.getNameUtils().getPackageName(context.getNameUtils().getBinaryName(parentClass)) + NamingContants.TRANSFORMERS_NAMESPACE;
        Package pkg = context.getCodeModel()._package(packageName);
        DefinedClass jaxbTransformer = pkg._class(StringUtils.capitalize(xmlType.name()) + "JaxbTransformer", AbstractTransformer.class, new Class<?>[]{DiscoverableTransformer.class});

        return jaxbTransformer;
    }
}