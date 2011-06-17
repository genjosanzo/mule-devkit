package org.mule.devkit.model.schema;

import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.mule.devkit.model.code.CodeWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public final class SchemaModel {
    private CodeWriter codeWriter;
    private java.util.List<SchemaLocation> schemas;

    public SchemaModel(CodeWriter codeWriter) {
        this.codeWriter = codeWriter;
        this.schemas = new ArrayList<SchemaLocation>();
    }

    public void addSchemaLocation(SchemaLocation schemaLocation) {
        this.schemas.add(schemaLocation);
    }

    public void build() throws IOException {

        try {
            for (SchemaLocation schemaLocation : this.schemas) {
                JAXBContext jaxbContext = JAXBContext.newInstance(Schema.class);
                Marshaller marshaller = jaxbContext.createMarshaller();
                NamespaceFilter outFilter = new NamespaceFilter("mule", "http://www.mulesoft.org/schema/mule/core", true);
                OutputFormat format = new OutputFormat();
                format.setIndent(true);
                format.setNewlines(true);
                OutputStream schemaStream = this.codeWriter.openBinary(null, schemaLocation.getFileName());

                XMLWriter writer = new XMLWriter(schemaStream, format);
                outFilter.setContentHandler(writer);
                marshaller.marshal(writer, outFilter);
            }
        } catch (JAXBException e) {
            throw new IOException(e);
        } catch (UnsupportedEncodingException e) {
            throw new IOException(e);
        }
    }

    public java.util.List<SchemaLocation> getSchemaLocations() {
        return this.schemas;
    }
}
