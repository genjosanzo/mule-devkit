package org.mule.devkit.apt.generator.spring;

import org.mule.devkit.annotations.Module;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.ContextualizedGenerator;
import org.mule.devkit.apt.generator.GenerationException;
import org.mule.devkit.apt.generator.schema.FileTypeSchema;

import javax.lang.model.element.TypeElement;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class SpringSchemaGenerator extends ContextualizedGenerator {
    public SpringSchemaGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement element) throws GenerationException {
        File metaInf = new File(getContext().getGeneratedSources(), "META-INF");
        if( !metaInf.exists() )
            metaInf.mkdirs();

        File springSchemas = new File(metaInf, "spring.schemas");

        try {
            springSchemas.createNewFile();

            FileOutputStream springSchemasStream = new FileOutputStream(springSchemas);
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
