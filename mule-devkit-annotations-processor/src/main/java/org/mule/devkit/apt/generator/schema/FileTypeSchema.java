package org.mule.devkit.apt.generator.schema;

import javax.lang.model.element.TypeElement;
import java.io.OutputStream;

public class FileTypeSchema {
    private OutputStream os;
    private Schema schema;
    private TypeElement typeElement;

    public FileTypeSchema(OutputStream os, Schema schema, TypeElement typeElement) {
        this.os = os;
        this.schema = schema;
        this.typeElement = typeElement;
    }

    public OutputStream getOs() {
        return os;
    }

    public void setOs(OutputStream os) {
        this.os = os;
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
