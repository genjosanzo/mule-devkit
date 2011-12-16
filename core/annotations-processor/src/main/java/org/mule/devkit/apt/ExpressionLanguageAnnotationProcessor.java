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

import org.mule.devkit.generation.Generator;
import org.mule.devkit.generation.mule.RegistryBootstrapGenerator;
import org.mule.devkit.generation.mule.expression.ExpressionEvaluatorGenerator;
import org.mule.devkit.validation.ExpressionLanguageValidator;
import org.mule.devkit.validation.JavaDocValidator;
import org.mule.devkit.validation.Validator;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SupportedAnnotationTypes(value = {"org.mule.api.annotations.ExpressionLanguage"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ExpressionLanguageAnnotationProcessor extends AbstractAnnotationProcessor {
    private List<Validator> validators;
    private List<Generator> generators;

    public ExpressionLanguageAnnotationProcessor() {
        generators = new ArrayList<Generator>();
        generators.add(new ExpressionEvaluatorGenerator());
        generators.add(new RegistryBootstrapGenerator());

        validators = new ArrayList<Validator>();
        validators.add(new JavaDocValidator());
        validators.add(new ExpressionLanguageValidator());

    }


    @Override
    public List<Validator> getValidators() {
        return Collections.unmodifiableList(validators);
    }

    @Override
    public List<Generator> getGenerators() {
        return Collections.unmodifiableList(generators);
    }
}
