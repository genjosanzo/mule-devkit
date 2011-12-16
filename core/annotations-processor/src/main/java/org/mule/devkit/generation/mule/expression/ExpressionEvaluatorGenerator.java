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

package org.mule.devkit.generation.mule.expression;

import org.apache.commons.lang.StringUtils;
import org.mule.api.MuleMessage;
import org.mule.api.annotations.ExpressionEvaluator;
import org.mule.api.annotations.ExpressionLanguage;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.InvocationHeaders;
import org.mule.api.annotations.param.OutboundHeaders;
import org.mule.api.annotations.param.Payload;
import org.mule.api.transformer.TransformerException;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Variable;
import org.mule.expression.ExpressionUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExpressionEvaluatorGenerator extends AbstractMessageGenerator {

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return typeElement.getMethodsAnnotatedWith(ExpressionEvaluator.class).size() > 0;
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {
        String name = typeElement.getAnnotation(ExpressionLanguage.class).name();

        ExecutableElement executableElement = typeElement.getMethodsAnnotatedWith(ExpressionEvaluator.class).get(0);
        DefinedClass evaluatorClass = getEvaluatorClass(name, typeElement);

        FieldVariable module = generateModuleField(typeElement, evaluatorClass);

        generateConstructor(typeElement, evaluatorClass, module);

        generateGetName(name, evaluatorClass);

        generateSetName(evaluatorClass);

        generateComputeClassHierarchyMethod(evaluatorClass);
        generateIsListClassMethod(evaluatorClass);
        generateIsMapClassMethod(evaluatorClass);
        generateIsListMethod(evaluatorClass);
        generateIsMapMethod(evaluatorClass);
        generateIsAssignableFrom(evaluatorClass);
        generateTransformMethod(evaluatorClass);

        Method evaluate = evaluatorClass.method(Modifier.PUBLIC, ref(Object.class), "evaluate");
        Variable expression = evaluate.param(ref(String.class), "expression");
        Variable message = evaluate.param(ref(MuleMessage.class), "message");

        TryStatement tryStatement = evaluate.body()._try();

        Invocation newArray = ExpressionFactory._new(ref(Class.class).array());
        for (VariableElement parameter : executableElement.getParameters()) {
            //tryStatement.body().assign(parameterClasses.component(ExpressionFactory.lit(argCount)), ref(parameter.asType()).boxify().dotclass());
            newArray.arg(ref(parameter.asType()).boxify().dotclass());
            //argCount++;
        }
        Variable parameterClasses = tryStatement.body().decl(ref(Class.class).array(), "parameterClasses", newArray);
        int argCount = 0;
        Invocation getMethod = module.invoke("getClass").invoke("getMethod").arg(executableElement.getSimpleName().toString()).arg(parameterClasses);
        Variable moduleEvaluate = tryStatement.body().decl(ref(java.lang.reflect.Method.class), "evaluateMethod", getMethod);
        List<Variable> types = new ArrayList<Variable>();
        for (VariableElement parameter : executableElement.getParameters()) {
            Variable var = tryStatement.body().decl(ref(Type.class), parameter.getSimpleName().toString() + "Type", moduleEvaluate.invoke("getGenericParameterTypes").component(ExpressionFactory.lit(types.size())));
            types.add(var);
        }

        argCount = 0;
        Invocation evaluateInvoke = module.invoke(executableElement.getSimpleName().toString());
        for (VariableElement parameter : executableElement.getParameters()) {
            if (parameter.getAnnotation(Payload.class) == null &&
                    parameter.getAnnotation(OutboundHeaders.class) == null &&
                    parameter.getAnnotation(InboundHeaders.class) == null &&
                    parameter.getAnnotation(InvocationHeaders.class) == null) {
                if (parameter.asType().toString().contains("String")) {
                    evaluateInvoke.arg(expression);
                }
            } else if (parameter.getAnnotation(Payload.class) != null) {
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()), ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(message.invoke("getPayload"))));
            } else if (parameter.getAnnotation(InboundHeaders.class) != null) {
                InboundHeaders inboundHeaders = parameter.getAnnotation(InboundHeaders.class);
                if (context.getTypeMirrorUtils().isArrayOrList(parameter.asType())) {
                    evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INBOUND:" + inboundHeaders.value()).arg(message).arg(ref(List.class).dotclass())
                            )));
                } else if (context.getTypeMirrorUtils().isMap(parameter.asType())) {
                    evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INBOUND:" + inboundHeaders.value()).arg(message).arg(ref(Map.class).dotclass())
                            )));
                } else {
                    evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INBOUND:" + inboundHeaders.value()).arg(message)
                            )));
                }
            } else if (parameter.getAnnotation(OutboundHeaders.class) != null) {
                OutboundHeaders outboundHeaders = parameter.getAnnotation(OutboundHeaders.class);
                evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()),
                        ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("OUTBOUND:*").arg(message).arg(ref(Map.class).dotclass())
                        )));
            } else if (parameter.getAnnotation(InvocationHeaders.class) != null) {
                InvocationHeaders invocationHeaders = parameter.getAnnotation(InvocationHeaders.class);
                if (context.getTypeMirrorUtils().isArrayOrList(parameter.asType())) {
                    evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INVOCATION:" + invocationHeaders.value()).arg(message).arg(ref(List.class).dotclass())
                            )));
                } else if (context.getTypeMirrorUtils().isMap(parameter.asType())) {
                    evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INVOCATION:" + invocationHeaders.value()).arg(message).arg(ref(Map.class).dotclass())
                            )));
                } else {
                    evaluateInvoke.arg(ExpressionFactory.cast(ref(parameter.asType()),
                            ExpressionFactory.invoke("transform").arg(message).arg(types.get(argCount)).arg(
                                    ref(ExpressionUtils.class).staticInvoke("getPropertyWithScope").arg("INVOCATION:" + invocationHeaders.value()).arg(message)
                            )));
                }
            }            argCount++;
        }

        tryStatement.body()._return(evaluateInvoke);

        catchAndRethrowAsRuntimeException(tryStatement, NoSuchMethodException.class);
        catchAndRethrowAsRuntimeException(tryStatement, TransformerException.class);

        context.registerAtBoot(evaluatorClass);
    }

    private void catchAndRethrowAsRuntimeException(TryStatement tryStatement, Class clazz) {
        CatchBlock catchBlock = tryStatement._catch(ref(clazz));
        Variable e = catchBlock.param("e");
        catchBlock.body()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg(e));
    }

    private FieldVariable generateModuleField(DevKitTypeElement typeElement, DefinedClass evaluatorClass) {
        return evaluatorClass.field(Modifier.PRIVATE, ref(typeElement.asType()), "module", ExpressionFactory._null());
    }

    private void generateConstructor(DevKitTypeElement typeElement, DefinedClass evaluatorClass, FieldVariable module) {
        Method constructor = evaluatorClass.constructor(Modifier.PUBLIC);
        constructor.body().assign(module, ExpressionFactory._new(ref(typeElement.asType())));
    }

    private void generateGetName(String name, DefinedClass evaluatorClass) {
        Method getName = evaluatorClass.method(Modifier.PUBLIC, ref(String.class), "getName");
        getName.body()._return(ExpressionFactory.lit(name));
    }

    private void generateSetName(DefinedClass evaluatorClass) {
        Method setName = evaluatorClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "setName");
        setName.param(ref(String.class), "name");
        setName.body()._throw(ExpressionFactory._new(ref(UnsupportedOperationException.class)));
    }

    private DefinedClass getEvaluatorClass(String name, Element variableElement) {
        String evaluatorClassName = context.getNameUtils().generateClassNameInPackage(variableElement, StringUtils.capitalize(name) + "ExpressionEvaluator");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(evaluatorClassName) + ".config");
        DefinedClass evaluator = pkg._class(context.getNameUtils().getClassName(evaluatorClassName), new Class<?>[]{org.mule.api.expression.ExpressionEvaluator.class});

        return evaluator;
    }

}