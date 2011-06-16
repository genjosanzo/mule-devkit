package org.mule.devkit.model.schema;

public class SchemaLocation {
    private String fileName;
    private String location;
    private Schema schema;

    public SchemaLocation(Schema schema, String fileName, String location)
    {
        this.fileName = fileName;
        this.location = location;
        this.schema = schema;
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
}
