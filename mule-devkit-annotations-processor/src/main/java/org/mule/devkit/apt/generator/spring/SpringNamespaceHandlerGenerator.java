package org.mule.devkit.apt.generator.spring;

import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JPackage;
import org.mule.devkit.annotations.Module;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.AbstractCodeGenerator;
import org.mule.devkit.apt.generator.GenerationException;
import org.mule.devkit.apt.generator.schema.FileTypeSchema;

import javax.lang.model.element.TypeElement;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class SpringNamespaceHandlerGenerator extends AbstractCodeGenerator {
    public SpringNamespaceHandlerGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement element) throws GenerationException {

        try {
            JPackage metaInf = getContext().getCodeModel()._package("META-INF");
            OutputStream springNamespaceHandlersStream = getContext().getCodeWriter().openBinary(metaInf, "spring.handlers");
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
