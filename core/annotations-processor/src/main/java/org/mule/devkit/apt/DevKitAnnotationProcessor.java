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
import org.mule.devkit.generation.adapter.CapabilitiesAdapterGenerator;
import org.mule.devkit.generation.adapter.ConnectionManagerGenerator;
import org.mule.devkit.generation.adapter.HttpCallbackAdapterGenerator;
import org.mule.devkit.generation.adapter.InjectAdapterGenerator;
import org.mule.devkit.generation.adapter.LifecycleAdapterFactoryGenerator;
import org.mule.devkit.generation.adapter.LifecycleAdapterGenerator;
import org.mule.devkit.generation.adapter.OAuth1AdapterGenerator;
import org.mule.devkit.generation.adapter.OAuth2AdapterGenerator;
import org.mule.devkit.generation.adapter.PoolAdapterGenerator;
import org.mule.devkit.generation.callback.HttpCallbackGenerator;
import org.mule.devkit.generation.mule.MessageProcessorGenerator;
import org.mule.devkit.generation.mule.MessageSourceGenerator;
import org.mule.devkit.generation.mule.NestedProcessorChainGenerator;
import org.mule.devkit.generation.mule.NestedProcessorStringGenerator;
import org.mule.devkit.generation.mule.RegistryBootstrapGenerator;
import org.mule.devkit.generation.mule.expression.ExpressionEnricherGenerator;
import org.mule.devkit.generation.mule.expression.ExpressionEvaluatorGenerator;
import org.mule.devkit.generation.mule.oauth.AuthorizeBeanDefinitionParserGenerator;
import org.mule.devkit.generation.mule.oauth.AuthorizeMessageProcessorGenerator;
import org.mule.devkit.generation.mule.oauth.DefaultRestoreAccessTokenCallbackFactoryGenerator;
import org.mule.devkit.generation.mule.oauth.DefaultRestoreAccessTokenCallbackGenerator;
import org.mule.devkit.generation.mule.oauth.DefaultSaveAccessTokenCallbackFactoryGenerator;
import org.mule.devkit.generation.mule.oauth.DefaultSaveAccessTokenCallbackGenerator;
import org.mule.devkit.generation.mule.studio.MuleStudioPluginGenerator;
import org.mule.devkit.generation.mule.transfomer.EnumTransformerGenerator;
import org.mule.devkit.generation.mule.transfomer.JaxbTransformerGenerator;
import org.mule.devkit.generation.mule.transfomer.StringToDateTransformerGenerator;
import org.mule.devkit.generation.mule.transfomer.TransformerGenerator;
import org.mule.devkit.generation.spring.BeanDefinitionParserGenerator;
import org.mule.devkit.generation.spring.NamespaceHandlerGenerator;
import org.mule.devkit.generation.spring.SchemaGenerator;
import org.mule.devkit.validation.BasicValidator;
import org.mule.devkit.validation.ConnectorValidator;
import org.mule.devkit.validation.InjectValidator;
import org.mule.devkit.validation.JavaDocValidator;
import org.mule.devkit.validation.OAuthValidator;
import org.mule.devkit.validation.ProcessorValidator;
import org.mule.devkit.validation.SourceValidator;
import org.mule.devkit.validation.StudioValidator;
import org.mule.devkit.validation.TransformerValidator;
import org.mule.devkit.validation.Validator;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SupportedAnnotationTypes(value = {"org.mule.api.annotations.Connector",
                                   "org.mule.api.annotations.ExpressionLanguage",
                                   "org.mule.api.annotations.Module"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class DevKitAnnotationProcessor extends AbstractAnnotationProcessor {
    private List<Validator> validators;
    private List<Generator> generators;

    public DevKitAnnotationProcessor() {
        generators = new ArrayList<Generator>();
        generators.add(new StringToDateTransformerGenerator());
        generators.add(new HttpCallbackGenerator());
        generators.add(new CapabilitiesAdapterGenerator());
        generators.add(new LifecycleAdapterGenerator());
        generators.add(new InjectAdapterGenerator());
        generators.add(new HttpCallbackAdapterGenerator());
        generators.add(new OAuth1AdapterGenerator());
        generators.add(new OAuth2AdapterGenerator());
        generators.add(new LifecycleAdapterFactoryGenerator());
        generators.add(new ConnectionManagerGenerator()); // this should be the last on the chain of adapters
        generators.add(new PoolAdapterGenerator());
        generators.add(new JaxbTransformerGenerator());
        generators.add(new TransformerGenerator());
        generators.add(new EnumTransformerGenerator());
        generators.add(new NestedProcessorChainGenerator());
        generators.add(new NestedProcessorStringGenerator());
        generators.add(new DefaultSaveAccessTokenCallbackGenerator());
        generators.add(new DefaultRestoreAccessTokenCallbackGenerator());
        generators.add(new DefaultRestoreAccessTokenCallbackFactoryGenerator());
        generators.add(new DefaultSaveAccessTokenCallbackFactoryGenerator());
        generators.add(new BeanDefinitionParserGenerator());
        generators.add(new MessageSourceGenerator());
        generators.add(new MessageProcessorGenerator());
        generators.add(new AuthorizeMessageProcessorGenerator());
        generators.add(new AuthorizeBeanDefinitionParserGenerator());
        generators.add(new NamespaceHandlerGenerator());
        generators.add(new ExpressionEvaluatorGenerator());
        generators.add(new ExpressionEnricherGenerator());
        generators.add(new RegistryBootstrapGenerator());
        generators.add(new MuleStudioPluginGenerator());
        generators.add(new SchemaGenerator());

        validators = new ArrayList<Validator>();
        validators.add(new JavaDocValidator());
        validators.add(new StudioValidator());
        validators.add(new BasicValidator());
        validators.add(new OAuthValidator());
        validators.add(new ProcessorValidator());
        validators.add(new ConnectorValidator());
        validators.add(new SourceValidator());
        validators.add(new TransformerValidator());
        validators.add(new InjectValidator());

    }

    public DevKitAnnotationProcessor(List<Validator> validators, List<Generator> generators) {
        this.validators = new ArrayList<Validator>(validators);
        this.generators = new ArrayList<Generator>(generators);
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
