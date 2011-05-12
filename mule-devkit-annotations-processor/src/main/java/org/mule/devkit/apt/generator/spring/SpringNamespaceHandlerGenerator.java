package org.mule.devkit.apt.generator.spring;

import com.sun.codemodel.JDefinedClass;
import org.mule.devkit.annotations.Module;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.AbstractCodeGenerator;
import org.mule.devkit.apt.generator.GenerationException;
import org.mule.devkit.apt.generator.schema.FileTypeSchema;

import javax.lang.model.element.TypeElement;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class SpringNamespaceHandlerGenerator extends AbstractCodeGenerator {
    public SpringNamespaceHandlerGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement element) throws GenerationException {
        File metaInf = new File(getContext().getGeneratedSources(), "META-INF");
        if( !metaInf.exists() )
            metaInf.mkdirs();

        File springNamespaceHandlers = new File(metaInf, "spring.handlers");

        try {
            springNamespaceHandlers.createNewFile();

            FileOutputStream springNamespaceHandlersStream = new FileOutputStream(springNamespaceHandlers);
            OutputStreamWriter springNamespaceHandlersOut = new OutputStreamWriter(springNamespaceHandlersStream, "UTF-8");

            for (Module mod : getContext().getSchemas().keySet()) {
                FileTypeSchema fileTypeSchema = getContext().getSchemas().get(mod);
                String namespaceHandlerName = getContext().getElements().getBinaryName(fileTypeSchema.getTypeElement()) + "NamespaceHandler";
                springNamespaceHandlersOut.write(fileTypeSchema.getSchema().getTargetNamespace() + "=" + namespaceHandlerName + "\n");
            }

            springNamespaceHandlersOut.flush();
            springNamespaceHandlersOut.close();
        } catch (IOException ioe) {
            throw new GenerationException(ioe);
        }    }
}
