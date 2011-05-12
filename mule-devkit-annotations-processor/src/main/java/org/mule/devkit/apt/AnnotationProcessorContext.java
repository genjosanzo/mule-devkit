package org.mule.devkit.apt;

import com.sun.codemodel.JCodeModel;
import org.mule.devkit.annotations.Module;
import org.mule.devkit.apt.generator.schema.FileSchema;

import javax.lang.model.util.Elements;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AnnotationProcessorContext {
    private JCodeModel codeModel;
    private Elements elements;
    private Map<Module, FileSchema> schemas;
    private File generatedSources;

    public AnnotationProcessorContext(File generatedSources) {
        this.schemas = new HashMap<Module, FileSchema>();
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

    public void addSchema(Module key, FileSchema schema) {
        this.schemas.put(key, schema);
    }

    public Map<Module, FileSchema> getSchemas() {
        return this.schemas;
    }

    public File getGeneratedSources() {
        return generatedSources;
    }
}
