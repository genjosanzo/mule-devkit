package org.mule.devkit.apt.generator.schema;

import java.io.File;

public class FileSchema {
    private File file;
    private Schema schema;

    public FileSchema(File file, Schema schema) {
        this.file = file;
        this.schema = schema;
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
}
