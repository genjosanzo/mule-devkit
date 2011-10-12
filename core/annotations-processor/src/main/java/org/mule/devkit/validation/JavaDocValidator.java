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

package org.mule.devkit.validation;

import org.apache.commons.lang.StringUtils;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.api.annotations.session.SessionCreate;
import org.mule.api.annotations.session.SessionDestroy;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.util.IOUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.util.Map;

public class JavaDocValidator implements Validator {

    @Override
    public boolean shouldValidate(Map<String, String> options) {
        return !options.containsKey("skipJavaDocValidation");
    }

    @Override
    public void validate(DevKitTypeElement typeElement, GeneratorContext context) throws ValidationException {
        if (!typeElement.hasAnnotation(Module.class) && !typeElement.hasAnnotation(Connector.class)) {
            return;
        }

        if (!hasComment(typeElement.getInnerTypeElement(), context)) {
            throw new ValidationException(typeElement, "Class " + typeElement.getQualifiedName().toString() + " is not properly documented. A summary is missing.");
        }

        if (!context.getJavaDocUtils().hasTag("author", typeElement.getInnerTypeElement())) {
            throw new ValidationException(typeElement, "Class " + typeElement.getQualifiedName().toString() + " needs to have an @author tag.");
        }

        for (VariableElement variable : typeElement.getFieldsAnnotatedWith(Configurable.class)) {
            if (!hasComment(variable, context)) {
                throw new ValidationException(variable, "Field " + variable.getSimpleName().toString() + " is not properly documented. The description is missing.");
            }
        }

        for (ExecutableElement method : typeElement.getMethodsAnnotatedWith(Processor.class)) {
            validateMethod(typeElement, context, method);
        }

        for (ExecutableElement method : typeElement.getMethodsAnnotatedWith(Source.class)) {
            validateMethod(typeElement, context, method);
        }

        for (ExecutableElement method : typeElement.getMethodsAnnotatedWith(Transformer.class)) {
            validateMethod(typeElement, context, method);
        }

        for (ExecutableElement method : typeElement.getMethodsAnnotatedWith(SessionCreate.class)) {
            validateAllParameters(context, method);
        }

        for (ExecutableElement method : typeElement.getMethodsAnnotatedWith(SessionDestroy.class)) {
            validateAllParameters(context, method);
        }
    }

    private void validateAllParameters(GeneratorContext context, ExecutableElement method) throws ValidationException {
        for (VariableElement variable : method.getParameters()) {
            if (!hasParameterComment(variable.getSimpleName().toString(), variable.getEnclosingElement(), context)) {
                throw new ValidationException(variable, "Parameter " + variable.getSimpleName().toString() + " of method " + method.getSimpleName().toString() + " is not properly documented. A matching @param in the method documentation was not found. ");
            }
        }
    }

    private void validateMethod(DevKitTypeElement typeElement, GeneratorContext context, ExecutableElement method) throws ValidationException {
        if (!hasComment(method, context)) {
            throw new ValidationException(method, "Method " + method.getSimpleName().toString() + " is not properly documented. A description of what it can do is missing.");
        }

        if (!context.getJavaDocUtils().hasTag("sample.xml", method)) {
            throw new ValidationException(typeElement, "Method " + method.getSimpleName().toString() + " does not contain an example using {@sample.xml} tag.");
        }

        if (!method.getReturnType().toString().equals("void")) {
            if (!context.getJavaDocUtils().hasTag("return", method)) {
                throw new ValidationException(typeElement, "The return type of a non-void method must be documented. Method " + method.getSimpleName().toString() + " is at fault. Missing @return.");
            }
        }

        if (exampleDoesNotExist(context, method)) {
            throw new ValidationException(typeElement, "Method " + method.getSimpleName().toString() + " does not have the example pointed by the {@sample.xml} tag");
        }

        validateAllParameters(context, method);
    }

    private boolean hasComment(Element element, GeneratorContext context) {
        String comment = context.getJavaDocUtils().getSummary(element);
        return StringUtils.isNotBlank(comment);

    }

    private boolean hasParameterComment(String paramName, Element element, GeneratorContext context) {
        String comment = context.getJavaDocUtils().getParameterSummary(paramName, element);
        return StringUtils.isNotBlank(comment);
    }

    private boolean exampleDoesNotExist(GeneratorContext context, ExecutableElement method) throws ValidationException {
        String sample = context.getJavaDocUtils().getTagContent("sample.xml", method);
        String[] split = sample.split(" ");
        String pathToExamplesFile = split[0];
        String exampleName = split[1];

        if (pathToExamplesFile.contains("../")) {
            pathToExamplesFile = pathToExamplesFile.substring(pathToExamplesFile.lastIndexOf("../") + 3);
        }

        try {
            String examplesFileContent = IOUtils.getResourceAsString(pathToExamplesFile, getClass());
            return !examplesFileContent.contains("BEGIN_INCLUDE(" + exampleName);
        } catch (IOException e) {
            throw new ValidationException(method, "Error loading examples file from path: " + pathToExamplesFile, e);
        }
    }
}