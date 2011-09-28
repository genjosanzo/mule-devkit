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

package org.mule.devkit;

import org.mule.devkit.generation.Generator;
import org.mule.devkit.generation.adapter.*;
import org.mule.devkit.generation.callback.HttpCallbackGenerator;
import org.mule.devkit.generation.callback.InterceptCallbackGenerator;
import org.mule.devkit.generation.dsl.DSLWrapperGenerator;
import org.mule.devkit.generation.mule.*;
import org.mule.devkit.generation.mule.transfomer.EnumTransformerGenerator;
import org.mule.devkit.generation.mule.transfomer.JaxbTransformerGenerator;
import org.mule.devkit.generation.mule.transfomer.StringToDateTransformerGenerator;
import org.mule.devkit.generation.mule.transfomer.TransformerGenerator;
import org.mule.devkit.generation.spring.*;
import org.mule.devkit.validation.*;

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
        generators.add(new SessionManagerAdapterGenerator());
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
        //generators.add(new DSLWrapperGenerator());

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
