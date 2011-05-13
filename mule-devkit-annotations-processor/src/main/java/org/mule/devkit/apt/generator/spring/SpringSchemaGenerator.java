package org.mule.devkit.apt.generator.spring;

import com.sun.codemodel.JPackage;
import org.mule.devkit.annotations.Module;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.ContextualizedGenerator;
import org.mule.devkit.apt.generator.GenerationException;
import org.mule.devkit.apt.generator.schema.FileTypeSchema;

import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class SpringSchemaGenerator extends ContextualizedGenerator {
    public SpringSchemaGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement element) throws GenerationException {
        try {
            OutputStream springSchemasStream = getContext().getCodeWriter().openBinary(null, "META-INF/spring.schemas");
            OutputStreamWriter springSchemasOut = new OutputStreamWriter(springSchemasStream, "UTF-8");

            for (Module mod : getContext().getSchemas().keySet()) {
                FileTypeSchema fileTypeSchema = getContext().getSchemas().get(mod);

                springSchemasOut.write(fileTypeSchema.getSchema().getTargetNamespace().replace("://", "\\://") + "/" + mod.version() + "/mule-" + mod.name() + ".xsd=META-INF/mule-" + mod.name() + ".xsd\n");
            }

            springSchemasOut.flush();
            springSchemasOut.close();
        } catch (IOException ioe) {
            throw new GenerationException(ioe);
        }
    }
}
