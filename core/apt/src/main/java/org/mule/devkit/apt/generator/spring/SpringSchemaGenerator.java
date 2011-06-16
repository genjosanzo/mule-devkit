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

package org.mule.devkit.apt.generator.spring;

import org.mule.devkit.annotations.Module;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.ContextualizedGenerator;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.apt.generator.schema.FileTypeSchema;

import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class SpringSchemaGenerator extends ContextualizedGenerator {
    public SpringSchemaGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement element) throws GenerationException {
        try {
            OutputStream springSchemasStream = getContext().getCodeWriter().openBinary(null, "META-INF/spring.schemas");
            OutputStreamWriter springSchemasOut = new OutputStreamWriter(springSchemasStream, "UTF-8");

            for (Module mod : getContext().getSchemas().keySet()) {
                FileTypeSchema fileTypeSchema = getContext().getSchemas().get(mod);

                springSchemasOut.write(fileTypeSchema.getSchemaLocation().replace("://", "\\://") + "=META-INF/mule-" + mod.name() + ".xsd\n");
            }

            springSchemasOut.flush();
            springSchemasOut.close();
        } catch (IOException ioe) {
            throw new GenerationException(ioe);
        }
    }
}
