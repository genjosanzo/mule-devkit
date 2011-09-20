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

import org.mule.api.context.MuleContextAware;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.MessageFactory;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevkitTypeElement;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Package;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.mule.devkit.model.code.builders.FieldBuilder;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import javax.lang.model.element.TypeElement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StringToDateTransformerGenerator extends AbstractMessageGenerator {

    private static final String PATTERN = "yyyy-MM-dd'T'hh:mm:ss";
    private static final String SIMPLE_DATE_FORMAT_FIELD_NAME = "SIMPLE_DATE_FORMAT";
    private static final String TRANSFORMER_CLASS_NAME = "StringToDateTransformer";

    @Override
    protected boolean shouldGenerate(DevkitTypeElement typeElement) {
        return typeElement.hasProcessorMethodWithParameter(Date.class);
    }

    @Override
    protected void doGenerate(DevkitTypeElement typeElement) {
        DefinedClass transformerClass = getTransformerClass(typeElement);
        FieldVariable muleContext = generateFieldForMuleContext(transformerClass);
        FieldVariable simpleDateFormat = generateSimpleDateFormatField(transformerClass);
        FieldVariable weighting = transformerClass.field(Modifier.PRIVATE, context.getCodeModel().INT, "weighting", ref(DiscoverableTransformer.class).staticRef("DEFAULT_PRIORITY_WEIGHTING"));
        generateConstructor(transformerClass);
        generateSetMuleContextMethod(transformerClass, muleContext);
        generateDoTransform(transformerClass, simpleDateFormat);
        generateGetPriorityWeighting(transformerClass, weighting);
        generateSetPriorityWeighting(transformerClass, weighting);

        context.registerAtBoot(transformerClass);
    }

    private FieldVariable generateSimpleDateFormatField(DefinedClass transformerClass) {
        return FieldBuilder.newConstantFieldBuilder(transformerClass).
                type(SimpleDateFormat.class).name(SIMPLE_DATE_FORMAT_FIELD_NAME).
                initialValue(ExpressionFactory._new(ref(SimpleDateFormat.class)).arg(PATTERN)).
                build();
    }

    private void generateSetPriorityWeighting(DefinedClass transformerClass, FieldVariable weighting) {
        Method setPriorityWeighting = transformerClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "setPriorityWeighting");
        Variable localWeighting = setPriorityWeighting.param(context.getCodeModel().INT, "weighting");
        setPriorityWeighting.body().assign(ExpressionFactory._this().ref(weighting), localWeighting);
    }

    private void generateGetPriorityWeighting(DefinedClass transformerClass, FieldVariable weighting) {
        Method getPriorityWeighting = transformerClass.method(Modifier.PUBLIC, context.getCodeModel().INT, "getPriorityWeighting");
        getPriorityWeighting.body()._return(weighting);
    }

    private void generateDoTransform(DefinedClass definedClass, FieldVariable simpleDateFormat) {
        Method doTransform = definedClass.method(Modifier.PROTECTED, ref(Object.class), "doTransform");
        doTransform._throws(TransformerException.class);
        Variable src = doTransform.param(ref(Object.class), "src");
        doTransform.param(ref(String.class), "encoding");

        TryStatement tryStatement = doTransform.body()._try();
        tryStatement.body()._return(simpleDateFormat.invoke("parse").arg(src.invoke("toString")));
        CatchBlock catchBlock = tryStatement._catch(ref(ParseException.class));
        Variable exceptionCaught = catchBlock.param("e");
        Invocation errorMessage = ref(MessageFactory.class).staticInvoke("createStaticMessage").
                arg(ref(String.class).staticInvoke("format").arg("Could not parse %s using the format %s").arg(src).arg(simpleDateFormat.invoke("toPattern")));
        catchBlock.body()._throw(ExpressionFactory._new(
                ref(TransformerException.class)).
                arg(errorMessage).
                arg(ExpressionFactory._this()).
                arg(exceptionCaught));
    }

    private void generateConstructor(DefinedClass transformerClass) {
        Method constructor = transformerClass.constructor(Modifier.PUBLIC);
        registerSourceTypes(constructor);
        registerDestinationType(constructor, ref(Date.class));
        constructor.body().invoke("setName").arg(transformerClass.name());
    }

    private void registerDestinationType(Method constructor, TypeReference clazz) {
        Invocation setReturnClass = constructor.body().invoke("setReturnClass");
        setReturnClass.arg(ExpressionFactory.dotclass(clazz));
    }

    private void registerSourceTypes(Method constructor) {
        Invocation registerSourceType = constructor.body().invoke("registerSourceType");
        registerSourceType.arg(ref(DataTypeFactory.class).staticInvoke("create").arg(ref(String.class).boxify().dotclass()));
    }

    private DefinedClass getTransformerClass(TypeElement typeElement) {
        String transformerClassName = context.getNameUtils().generateClassNameInPackage(typeElement, TRANSFORMER_CLASS_NAME);
        Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(transformerClassName) + ".config");
        return pkg._class(context.getNameUtils().getClassName(transformerClassName), AbstractTransformer.class, new Class<?>[]{DiscoverableTransformer.class, MuleContextAware.class});
    }
}