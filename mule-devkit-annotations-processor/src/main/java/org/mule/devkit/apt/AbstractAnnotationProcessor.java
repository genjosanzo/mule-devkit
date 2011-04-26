package org.mule.devkit.apt;

import com.sun.codemodel.JCodeModel;
import org.mule.devkit.apt.code.CodeGenerationException;
import org.mule.devkit.apt.code.CodeGenerator;
import org.mule.devkit.apt.validation.TypeValidationException;
import org.mule.devkit.apt.validation.TypeValidator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public abstract class AbstractAnnotationProcessor extends AbstractProcessor {
    private AnnotationProcessorContext context;
    private File generatedSources;
    private File generatedResources;

    public AbstractAnnotationProcessor() {
        generatedSources = new File("target/generated-sources");
        if( !generatedSources.exists() )
        {
            generatedSources.mkdirs();
        }

        generatedResources = new File("target/generated-resources");
        if( !generatedResources.exists() )
        {
            generatedResources.mkdirs();
        }
    }

    private void createContext()
    {
        context = new AnnotationProcessorContext();
        context.setCodeModel(new JCodeModel());
        context.setElements(processingEnv.getElementUtils());
    }

    protected AnnotationProcessorContext getContext() {
        return context;
    }

    protected void note(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }

    protected void error(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {

        createContext();

        note("AbstractAnnotationProcessor: annotations=" + annotations + ", roundEnv=" + env);

        for (TypeElement annotation : annotations) {
            Set<? extends Element> elements = env.getElementsAnnotatedWith(annotation);
            Set<TypeElement> typeElements = ElementFilter.typesIn(elements);
            for (TypeElement e : typeElements) {

                try {
                    getValidator().validate(e);
                    preCodeGeneration(e);
                    generateCode(e);
                    postCodeGeneration(e);
                } catch (TypeValidationException tve) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, tve.getMessage(), tve.getType());
                }
            }
        }

        try {
            context.getCodeModel().build(generatedSources, generatedResources);
        } catch (IOException e) {
            error(e.getMessage());
        }


        return true;
    }

    public void generateCode(TypeElement e) {
        List<CodeGenerator> codeGenerators = getCodeGenerators();

        for( CodeGenerator codeGenerator : codeGenerators )
        {
            try
            {
                codeGenerator.generate(e);
            }
            catch( CodeGenerationException cge )
            {
                error(cge.getMessage());
            }
        }
    }

    public abstract void preCodeGeneration(TypeElement e);

    public abstract void postCodeGeneration(TypeElement e);

    public abstract TypeValidator getValidator();

    public abstract List<CodeGenerator> getCodeGenerators();
}
