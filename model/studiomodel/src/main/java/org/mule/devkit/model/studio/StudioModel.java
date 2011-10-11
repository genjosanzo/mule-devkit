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

import com.thoughtworks.xstream.XStream;
import org.mule.devkit.model.code.CodeWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

public class StudioModel {

    private CodeWriter codeWriter;
    private XStream xStream;
    private Namespace namespace;

    public StudioModel(CodeWriter codeWriter, XStream xStream) {
        this.codeWriter = codeWriter;
        this.xStream = xStream;
    }

    public void build() throws IOException {
        if(namespace == null) {
            return;
        }
        try {
            String studioXml = xStream.toXML(namespace).replaceAll("__abstract", "abstract").replaceAll("__extends", "extends"); // TODO;

            OutputStream springSchemasStream = codeWriter.openBinary(null, "META-INF/studio.xml");
            OutputStreamWriter springSchemasOut = new OutputStreamWriter(springSchemasStream, "UTF-8");
            springSchemasOut.write(studioXml);
            springSchemasOut.flush();
            springSchemasOut.close();
        } catch (UnsupportedEncodingException e) {
            throw new IOException(e);
        }
    }

    public XStream getXStream() {
        return xStream;
    }

    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }
}