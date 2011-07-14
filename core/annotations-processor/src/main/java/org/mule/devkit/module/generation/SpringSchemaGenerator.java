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

package org.mule.devkit.module.generation;

import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.schema.SchemaLocation;

import javax.lang.model.element.Element;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class SpringSchemaGenerator extends AbstractModuleGenerator {
    public void generate(Element element) throws GenerationException {
        try {
            OutputStream springSchemasStream = context.getCodeModel().getCodeWriter().openBinary(null, "META-INF/spring.schemas");
            OutputStreamWriter springSchemasOut = new OutputStreamWriter(springSchemasStream, "UTF-8");

            for (SchemaLocation schemaLocation : context.getSchemaModel().getSchemaLocations()) {
                springSchemasOut.write(schemaLocation.getLocation().replace("://", "\\://") + "=" + schemaLocation.getFileName() + "\n");
            }

            springSchemasOut.flush();
            springSchemasOut.close();
        } catch (IOException ioe) {
            throw new GenerationException(ioe);
        }
    }
}
