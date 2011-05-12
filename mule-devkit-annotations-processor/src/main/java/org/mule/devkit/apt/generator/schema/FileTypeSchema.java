package org.mule.devkit.apt.generator.schema;

import javax.lang.model.element.TypeElement;
import java.io.File;

public class FileTypeSchema {
    private File file;
    private Schema schema;
    private TypeElement typeElement;

    public FileTypeSchema(File file, Schema schema, TypeElement typeElement) {
        this.file = file;
        this.schema = schema;
        this.typeElement = typeElement;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public void setTypeElement(TypeElement typeElement) {
        this.typeElement = typeElement;
    }
}
