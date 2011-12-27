/**
 * Mule Development Kit
 * Copyright 2010-2011 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mule.devkit.model.studio;

import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.mule.devkit.model.code.CodeWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class StudioModel {

    private CodeWriter codeWriter;
    private NamespaceType namespaceType;
    private String outputFileName;

    public StudioModel(CodeWriter codeWriter) {
        this.codeWriter = codeWriter;
    }

    public void build() throws IOException {
        try {
            if (namespaceType != null) {
                serializeXml();
            }
        } catch (JAXBException e) {
            throw new IOException(e);
        } catch (UnsupportedEncodingException e) {
            throw new IOException(e);
        }
    }

    private void serializeXml() throws JAXBException, IOException {
        JAXBContext jaxbContext = JAXBContext.newInstance(NamespaceType.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        NamespaceFilter outFilter = new NamespaceFilter("mule", "http://www.mulesoft.org/schema/mule/core", true);
        OutputFormat format = new OutputFormat();
        format.setIndent(true);
        format.setNewlines(true);
        OutputStream schemaStream = codeWriter.openBinary(null, outputFileName);
        XMLWriter writer = new XMLWriter(schemaStream, format);
        outFilter.setContentHandler(writer);
        marshaller.marshal(namespaceType, outFilter);
    }

    public void setNamespaceType(NamespaceType namespaceType) {
        this.namespaceType = namespaceType;
    }

    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }
}