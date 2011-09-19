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

package org.mule.devkit.module;

import org.mule.devkit.Plugin;
import org.mule.devkit.generation.Generator;
import org.mule.devkit.module.generation.BeanDefinitionParserGenerator;
import org.mule.devkit.module.generation.CapabilitiesAdapterGenerator;
import org.mule.devkit.module.generation.EnumTransformerGenerator;
import org.mule.devkit.module.generation.HttpCallbackAdapterGenerator;
import org.mule.devkit.module.generation.HttpCallbackGenerator;
import org.mule.devkit.module.generation.InterceptCallbackGenerator;
import org.mule.devkit.module.generation.JaxbTransformerGenerator;
import org.mule.devkit.module.generation.LifecycleAdapterFactoryGenerator;
import org.mule.devkit.module.generation.LifecycleAdapterGenerator;
import org.mule.devkit.module.generation.MessageProcessorGenerator;
import org.mule.devkit.module.generation.MessageSourceGenerator;
import org.mule.devkit.module.generation.NamespaceHandlerGenerator;
import org.mule.devkit.module.generation.NestedProcessorChainGenerator;
import org.mule.devkit.module.generation.NestedProcessorStringGenerator;
import org.mule.devkit.module.generation.OAuth1AdapterGenerator;
import org.mule.devkit.module.generation.OAuth2AdapterGenerator;
import org.mule.devkit.module.generation.PoolAdapterGenerator;
import org.mule.devkit.module.generation.RegistryBootstrapGenerator;
import org.mule.devkit.module.generation.SchemaGenerator;
import org.mule.devkit.module.generation.SessionAdapterGenerator;
import org.mule.devkit.module.generation.SpringNamespaceHandlerGenerator;
import org.mule.devkit.module.generation.SpringSchemaGenerator;
import org.mule.devkit.module.generation.StringToDateTransformerGenerator;
import org.mule.devkit.module.generation.TransformerGenerator;
import org.mule.devkit.module.validation.BasicValidator;
import org.mule.devkit.module.validation.OAuthValidator;
import org.mule.devkit.module.validation.ProcessorValidator;
import org.mule.devkit.module.validation.SessionValidator;
import org.mule.devkit.module.validation.SourceValidator;
import org.mule.devkit.module.validation.TransformerValidator;
import org.mule.devkit.validation.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModulePlugin implements Plugin {

    private List<Validator> validators;
    private List<Generator> generators;

    public ModulePlugin() {
        generators = new ArrayList<Generator>();
        generators.add(new SchemaGenerator());
        generators.add(new StringToDateTransformerGenerator());
        generators.add(new HttpCallbackGenerator());
        generators.add(new CapabilitiesAdapterGenerator());
        generators.add(new LifecycleAdapterGenerator());
        generators.add(new HttpCallbackAdapterGenerator());
        generators.add(new OAuth1AdapterGenerator());
        generators.add(new OAuth2AdapterGenerator());
        generators.add(new LifecycleAdapterFactoryGenerator());
        generators.add(new SessionAdapterGenerator());
        generators.add(new PoolAdapterGenerator());
        generators.add(new JaxbTransformerGenerator());
        generators.add(new TransformerGenerator());
        generators.add(new EnumTransformerGenerator());
        generators.add(new NestedProcessorChainGenerator());
        generators.add(new NestedProcessorStringGenerator());
        generators.add(new InterceptCallbackGenerator());
        generators.add(new BeanDefinitionParserGenerator());
        generators.add(new MessageSourceGenerator());
        generators.add(new MessageProcessorGenerator());
        generators.add(new NamespaceHandlerGenerator());
        generators.add(new SpringNamespaceHandlerGenerator());
        generators.add(new SpringSchemaGenerator());
        generators.add(new RegistryBootstrapGenerator());

        validators = new ArrayList<Validator>();
        validators.add(new BasicValidator());
        validators.add(new OAuthValidator());
        validators.add(new ProcessorValidator());
        validators.add(new SessionValidator());
        validators.add(new SourceValidator());
        validators.add(new TransformerValidator());

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
