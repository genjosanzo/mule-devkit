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

package org.mule.devkit.apt;

import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DefaultDevKitTypeElement;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.generation.Generator;
import org.mule.devkit.validation.ValidationException;
import org.mule.devkit.validation.Validator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public abstract class AbstractAnnotationProcessor extends AbstractProcessor {

    private GeneratorContext context;

    /**
     * Retrieve a list of validators for the specified object type
     *
     * @return A list of validators implementing Validator
     */
    public abstract List<Validator> getValidators();

    /**
     * Retrieve a list of generators for the specified object type
     *
     * @return A list of validators implementing Generator
     */
    public abstract List<Generator> getGenerators();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        createContext();

        for (TypeElement annotation : annotations) {
            context.note("Searching for classes annotated with @" + annotation.getSimpleName().toString());
            Set<? extends Element> elements = env.getElementsAnnotatedWith(annotation);
            Set<TypeElement> typeElements = ElementFilter.typesIn(elements);
            for (TypeElement e : typeElements) {
                DevKitTypeElement devKitTypeElement = new DefaultDevKitTypeElement(e);
                context.note("Validating " + devKitTypeElement.getSimpleName().toString() + " class");
                for (Validator validator : getValidators()) {
                    try {
                        if (validator.shouldValidate(devKitTypeElement, context)) {
                            validator.validate(devKitTypeElement, context);
                        }
                    } catch (ValidationException tve) {
                        context.error(tve.getMessage(), tve.getElement());
                        return false;
                    }
                }
            }
            for (TypeElement e : typeElements) {
                DevKitTypeElement devKitTypeElement = new DefaultDevKitTypeElement(e);
                context.note("Generating code for " + devKitTypeElement.getSimpleName().toString() + " class");
                for (Generator generator : getGenerators()) {
                    try {
                        generator.generate(devKitTypeElement, context);
                    } catch (GenerationException ge) {
                        context.error(ge.getMessage());
                        return false;
                    }
                }
            }
        }


        try {
            context.getCodeModel().build();
        } catch (IOException e) {
            context.error(e.getMessage());
            return false;
        }

        try {
            context.getSchemaModel().build();
        } catch (IOException e) {
            context.error(e.getMessage());
            return false;
        }

        try {
            context.getStudioModel().build();
        } catch (IOException e) {
            context.error(e.getMessage());
            return false;
        }

        return true;
    }

    private void createContext() {
        context = new GeneratorContext(processingEnv);
    }

    protected GeneratorContext getContext() {
        return context;
    }
}
