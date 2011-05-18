package org.mule.devkit.apt;

import org.mule.devkit.annotations.Module;
import org.mule.devkit.apt.generator.Generator;
import org.mule.devkit.apt.generator.MetadataGenerator;
import org.mule.devkit.apt.generator.mule.JaxbTransformerGenerator;
import org.mule.devkit.apt.generator.mule.MessageProcessorGenerator;
import org.mule.devkit.apt.generator.schema.SchemaGenerator;
import org.mule.devkit.apt.generator.spring.AnyXmlChildDefinitionParserGenerator;
import org.mule.devkit.apt.generator.spring.BeanDefinitionParserGenerator;
import org.mule.devkit.apt.generator.spring.NamespaceHandlerGenerator;
import org.mule.devkit.apt.generator.spring.SpringNamespaceHandlerGenerator;
import org.mule.devkit.apt.generator.spring.SpringSchemaGenerator;
import org.mule.devkit.apt.validation.ModuleTypeValidator;
import org.mule.devkit.apt.validation.TypeValidator;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

@SupportedAnnotationTypes(value = {"org.mule.devkit.annotations.Module"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ModuleAnnotationProcessor extends AbstractAnnotationProcessor {
    private ModuleTypeValidator moduleTypeValidator = new ModuleTypeValidator();

    public ModuleAnnotationProcessor() {
    }

    public void preCodeGeneration(TypeElement e) {
        Module module = e.getAnnotation(Module.class);
    }

    @Override
    public void postCodeGeneration(TypeElement e) {
    }

    @Override
    public TypeValidator getValidator() {
        return moduleTypeValidator;
    }

    @Override
    public List<Generator> getCodeGenerators() {
        List<Generator> generators = new ArrayList<Generator>();
        generators.add(new MetadataGenerator(getContext()));
        generators.add(new NamespaceHandlerGenerator(getContext()));
        generators.add(new BeanDefinitionParserGenerator(getContext()));
        generators.add(new AnyXmlChildDefinitionParserGenerator(getContext()));
        generators.add(new MessageProcessorGenerator(getContext()));
        generators.add(new SchemaGenerator(getContext()));
        generators.add(new SpringSchemaGenerator(getContext()));
        generators.add(new SpringNamespaceHandlerGenerator(getContext()));
        generators.add(new JaxbTransformerGenerator(getContext()));
        return generators;
    }
}
