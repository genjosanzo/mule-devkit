package org.mule.devkit.apt;

import com.sun.codemodel.JCodeModel;
import org.mule.devkit.apt.generator.schema.Schema;

import javax.lang.model.util.Elements;
import java.util.HashMap;
import java.util.Map;

public class AnnotationProcessorContext {
    private JCodeModel codeModel;
    private Elements elements;
    private Map<String, Schema> schemas;

    public AnnotationProcessorContext() {
        this.schemas = new HashMap<String, Schema>();
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

    public void addSchema(String key, Schema schema) {
        this.schemas.put(key, schema);
    }

    public Map<String, Schema> getSchemas()
    {
        return this.schemas;
    }
}
