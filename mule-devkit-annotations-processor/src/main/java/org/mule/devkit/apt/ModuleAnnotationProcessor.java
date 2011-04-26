package org.mule.devkit.apt;

import org.mule.devkit.annotations.Module;
import org.mule.devkit.apt.code.CodeGenerator;
import org.mule.devkit.apt.code.MetadataCodeGenerator;
import org.mule.devkit.apt.code.NamespaceHandlerCodeGenerator;
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
    public List<CodeGenerator> getCodeGenerators() {
        List<CodeGenerator> codeGenerators = new ArrayList<CodeGenerator>();
        codeGenerators.add(new MetadataCodeGenerator(getContext()));
        codeGenerators.add(new NamespaceHandlerCodeGenerator(getContext()));
        return codeGenerators;
    }
}
