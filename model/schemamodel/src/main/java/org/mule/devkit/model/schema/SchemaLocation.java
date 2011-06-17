package org.mule.devkit.model.schema;

public class SchemaLocation {
    private String fileName;
    private String location;
    private Schema schema;
    private String namespaceHandler;

    public SchemaLocation(Schema schema, String fileName, String location, String namespaceHandler)
    {
        this.fileName = fileName;
        this.location = location;
        this.schema = schema;
        this.namespaceHandler = namespaceHandler;
    }

    public String getFileName() {
        return fileName;
    }

    public String getLocation() {
        return location;
    }

    public Schema getSchema() {
        return schema;
    }

    public String getNamespaceHandler() {
        return namespaceHandler;
    }
}
