package org.mule.devkit.apt;

import com.sun.codemodel.JCodeModel;
import org.mule.devkit.annotations.Module;
import org.mule.devkit.apt.generator.schema.FileTypeSchema;

import javax.lang.model.util.Elements;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AnnotationProcessorContext {
    private JCodeModel codeModel;
    private Elements elements;
    private Map<Module, FileTypeSchema> schemas;
    private File generatedSources;

    public AnnotationProcessorContext(File generatedSources) {
        this.schemas = new HashMap<Module, FileTypeSchema>();
        this.generatedSources = generatedSources;
    }

    public JCodeModel getCodeModel() {
        return codeModel;
    }

    public void setCodeModel(JCodeModel codeModel) {
        this.codeModel = codeModel;
    }

    public Elements getElements() {
        return elements;
    }

    public void setElements(Elements elements) {
        this.elements = elements;
    }

    public void addSchema(Module key, FileTypeSchema typeSchema) {
        this.schemas.put(key, typeSchema);
    }

    public Map<Module, FileTypeSchema> getSchemas() {
        return this.schemas;
    }

    public File getGeneratedSources() {
        return generatedSources;
    }
}
