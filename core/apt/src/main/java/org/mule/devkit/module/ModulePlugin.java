package org.mule.devkit.module;

import org.mule.devkit.Plugin;
import org.mule.devkit.generation.Generator;
import org.mule.devkit.module.generation.AnyXmlChildDefinitionParserGenerator;
import org.mule.devkit.module.generation.BeanDefinitionParserGenerator;
import org.mule.devkit.module.generation.DummyInboundEndpointGenerator;
import org.mule.devkit.module.generation.JaxbTransformerGenerator;
import org.mule.devkit.module.generation.LifecycleWrapperGenerator;
import org.mule.devkit.module.generation.MessageProcessorGenerator;
import org.mule.devkit.module.generation.MessageSourceGenerator;
import org.mule.devkit.module.generation.NamespaceHandlerGenerator;
import org.mule.devkit.module.generation.RegistryBootstrapGenerator;
import org.mule.devkit.module.generation.SchemaGenerator;
import org.mule.devkit.module.generation.SpringNamespaceHandlerGenerator;
import org.mule.devkit.module.generation.SpringSchemaGenerator;
import org.mule.devkit.module.generation.TransformerGenerator;
import org.mule.devkit.module.validation.ModuleValidator;
import org.mule.devkit.validation.Validator;

import java.util.ArrayList;
import java.util.List;

public class ModulePlugin implements Plugin {
    private List<Validator> validators;
    private List<Generator> generators;

    public ModulePlugin() {
        this.generators = new ArrayList<Generator>();
        this.generators.add(new DummyInboundEndpointGenerator());
        this.generators.add(new JaxbTransformerGenerator());
        this.generators.add(new TransformerGenerator());
        this.generators.add(new RegistryBootstrapGenerator());
        this.generators.add(new SchemaGenerator());
        this.generators.add(new SpringSchemaGenerator());
        this.generators.add(new LifecycleWrapperGenerator());
        this.generators.add(new MessageSourceGenerator());
        this.generators.add(new NamespaceHandlerGenerator());
        this.generators.add(new BeanDefinitionParserGenerator());
        this.generators.add(new AnyXmlChildDefinitionParserGenerator());
        this.generators.add(new MessageProcessorGenerator());
        this.generators.add(new SpringNamespaceHandlerGenerator());
        this.generators.add(new TransformerGenerator());
        //return generators;

        this.validators = new ArrayList<Validator>();
        this.validators.add(new ModuleValidator());
    }

    public List<Validator> getValidators() {
        return this.validators;
    }

    public List<Generator> getGenerators() {
        return this.generators;
    }
}
