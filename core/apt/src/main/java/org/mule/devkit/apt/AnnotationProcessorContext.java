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

package org.mule.devkit.apt;

import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.JCodeModel;
import org.mule.devkit.model.code.writer.FilerCodeWriter;
import org.mule.devkit.model.schema.SchemaModel;

import javax.annotation.processing.Filer;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;

public class AnnotationProcessorContext {
    private JCodeModel codeModel;
    private SchemaModel schemaModel;
    private List<DefinedClass> registerAtBoot;
    private Types types;

    public AnnotationProcessorContext(Filer filer, Types types) {
        this.registerAtBoot = new ArrayList<DefinedClass>();
        this.codeModel = new JCodeModel(new FilerCodeWriter(filer));
        this.schemaModel = new SchemaModel(new FilerCodeWriter(filer));
        this.types = types;
    }

    public JCodeModel getCodeModel() {
        return codeModel;
    }

    public List<DefinedClass> getRegisterAtBoot() {
        return registerAtBoot;
    }

    public void registerAtBoot(DefinedClass clazz) {
        this.registerAtBoot.add(clazz);
    }

    public SchemaModel getSchemaModel() {
        return schemaModel;
    }

    public Types getTypeUtils() {
        return this.types;
    }
}
