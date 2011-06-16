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

import org.mule.devkit.annotations.Module;
import org.mule.devkit.generation.Generator;
import org.mule.devkit.apt.generator.MetadataGenerator;
import org.mule.devkit.apt.generator.mule.*;
import org.mule.devkit.apt.generator.schema.SchemaGenerator;
import org.mule.devkit.apt.generator.spring.*;
import org.mule.devkit.module.validation.ModuleValidator;
import org.mule.devkit.validation.Validator;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

@SupportedAnnotationTypes(value = {"org.mule.devkit.annotations.Module"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ModuleAnnotationProcessor extends AbstractAnnotationProcessor {
    private ModuleValidator moduleValidator = new ModuleValidator();

    public ModuleAnnotationProcessor() {
    }

    public void preCodeGeneration(TypeElement e) {
        Module module = e.getAnnotation(Module.class);
    }

    @Override
    public void postCodeGeneration(TypeElement e) {
    }

    @Override
    public Validator<TypeElement> getValidator() {
        return moduleValidator;
    }

    @Override
    public List<Generator> getCodeGenerators() {
        List<Generator> generators = new ArrayList<Generator>();
        generators.add(new MetadataGenerator(getContext()));
        generators.add(new NamespaceHandlerGenerator(getContext()));
        generators.add(new BeanDefinitionParserGenerator(getContext()));
        generators.add(new AnyXmlChildDefinitionParserGenerator(getContext()));
        generators.add(new MessageProcessorGenerator(getContext()));
        generators.add(new MessageSourceGenerator(getContext()));
        generators.add(new LifecycleWrapperGenerator(getContext()));
        generators.add(new SchemaGenerator(getContext()));
        generators.add(new SpringSchemaGenerator(getContext()));
        generators.add(new SpringNamespaceHandlerGenerator(getContext()));
        generators.add(new JaxbTransformerGenerator(getContext()));
        generators.add(new DummyInboundEndpointGenerator(getContext()));
        generators.add(new RegistryBootstrapGenerator(getContext()));
        generators.add(new TransformerGenerator(getContext()));
        return generators;
    }
}
