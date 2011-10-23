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

package org.mule.devkit.model.schema;

public class SchemaLocation {
    private String fileName;
    private String location;
    private Schema schema;
    private String namespaceHandler;
    private String targetNamespace;
    private String className;

    public SchemaLocation(Schema schema, String targetNamespace, String fileName, String location, String namespaceHandler, String className) {
        this.fileName = fileName;
        this.location = location;
        this.schema = schema;
        this.namespaceHandler = namespaceHandler;
        this.targetNamespace = targetNamespace;
        this.className = className;
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

    public String getTargetNamespace() {
        return targetNamespace;
    }

    public String getClassName() {
        return className;
    }
}

