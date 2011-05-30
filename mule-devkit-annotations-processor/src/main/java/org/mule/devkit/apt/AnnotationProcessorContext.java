package org.mule.devkit.apt;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import org.mule.devkit.annotations.Module;
import org.mule.devkit.apt.generator.schema.FileTypeSchema;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationProcessorContext {
    private JCodeModel codeModel;
    private Elements elements;
    private Types types;
    private Map<Module, FileTypeSchema> schemas;
    private CodeWriter codeWriter;
    private List<JDefinedClass> classesToRegisterAtBoot;

    public AnnotationProcessorContext() {
        this.schemas = new HashMap<Module, FileTypeSchema>();
        this.classesToRegisterAtBoot = new ArrayList<JDefinedClass>();
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

    public CodeWriter getCodeWriter() {
        return codeWriter;
    }

    public void setCodeWriter(CodeWriter codeWriter) {
        this.codeWriter = codeWriter;
    }

    public List<JDefinedClass> getClassesToRegisterAtBoot() {
        return classesToRegisterAtBoot;
    }

    public void registerClassAtBoot(JDefinedClass clazz) {
        this.classesToRegisterAtBoot.add(clazz);
    }

    public Types getTypes() {
        return types;
    }

    public void setTypes(Types types) {
        this.types = types;
    }
}
