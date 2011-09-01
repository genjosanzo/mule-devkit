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

import org.mule.api.annotations.Processor;
import org.mule.api.annotations.callback.ProcessorCallback;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Variable;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.Map;

public class StringProcessorCallbackGenerator extends AbstractModuleGenerator {
    public static final String ROLE = "StringProcessorCallback";

    public void generate(Element element) throws GenerationException {
        boolean shouldGenerate = false;

        java.util.List<ExecutableElement> methods = ElementFilter.methodsIn(element.getEnclosedElements());
        for (ExecutableElement method : methods) {
            Processor processor = method.getAnnotation(Processor.class);
            if (processor == null)
                continue;

            for (VariableElement variable : method.getParameters()) {
                if (variable.asType().toString().contains(ProcessorCallback.class.getName())) {
                    shouldGenerate = true;
                    break;
                }
            }
        }

        if (shouldGenerate) {
            generateCallbackClass((TypeElement) element);
        }
    }

    private DefinedClass generateCallbackClass(TypeElement typeElement) {
        DefinedClass callbackClass = getProcessorCallbackClass(typeElement);
        callbackClass._implements(ref(ProcessorCallback.class));

        FieldVariable output = callbackClass.field(Modifier.PRIVATE, ref(String.class), "output");
        output.javadoc().add("Output string to be returned on process");

        generateSetter(callbackClass, output);

        generateCallbackConstructor(callbackClass, output);

        generateCallbackProcess(callbackClass, output);

        generateCallbackProcessWithPayload(callbackClass, output);

        generateCallbackProcessWithProperties(callbackClass, output);

        generateCallbackProcessWithPayloadAndProperties(callbackClass, output);

        context.setClassRole(ROLE, callbackClass);

        return callbackClass;
    }

    private void generateCallbackProcessWithPayload(DefinedClass callbackClass, FieldVariable output) {
        Method process = callbackClass.method(Modifier.PUBLIC, ref(Object.class), "process");
        process._throws(ref(Exception.class));
        Variable payload = process.param(ref(Object.class), "payload");

        process.body()._return(output);
    }

    private void generateCallbackProcessWithPayloadAndProperties(DefinedClass callbackClass, FieldVariable output) {
        Method process = callbackClass.method(Modifier.PUBLIC, ref(Object.class), "process");
        process._throws(ref(Exception.class));
        Variable payload = process.param(ref(Object.class), "payload");
        Variable properties = process.param(ref(Map.class).narrow(ref(String.class)).narrow(ref(Object.class)), "properties");


        process.body()._return(output);
    }

    private void generateCallbackProcessWithProperties(DefinedClass callbackClass, FieldVariable output) {
        Method process = callbackClass.method(Modifier.PUBLIC, ref(Object.class), "processWithExtraProperties");
        process._throws(ref(Exception.class));
        Variable properties = process.param(ref(Map.class).narrow(ref(String.class)).narrow(ref(Object.class)), "properties");

        process.body()._return(output);
    }

    private void generateCallbackProcess(DefinedClass callbackClass, FieldVariable output) {
        Method process = callbackClass.method(Modifier.PUBLIC, ref(Object.class), "process");
        process._throws(ref(Exception.class));

        process.body()._return(output);
    }

    private void generateCallbackConstructor(DefinedClass callbackClass, FieldVariable output) {
        Method constructor = callbackClass.constructor(Modifier.PUBLIC);
        Variable output2 = constructor.param(ref(String.class), "output");
        constructor.body().assign(ExpressionFactory._this().ref(output), output2);
    }

    private DefinedClass getProcessorCallbackClass(TypeElement type) {
        String processorCallbackClassName = context.getNameUtils().generateClassNameInPackage(type, ".config.spring", "StringProcessorCallback");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(processorCallbackClassName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(processorCallbackClassName));

        context.setClassRole(ROLE, clazz);

        return clazz;
    }
}
