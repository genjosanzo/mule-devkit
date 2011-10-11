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

package org.mule.devkit.generation.mule;

import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.DefinedClass;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class RegistryBootstrapGenerator extends AbstractModuleGenerator {

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return true;
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) throws GenerationException {
        try {
            OutputStream registryBootstrapStream = context.getCodeModel().getCodeWriter().openBinary(null, "META-INF/services/org/mule/config/registry-bootstrap.properties");
            OutputStreamWriter registryBootstrapStreamOut = new OutputStreamWriter(registryBootstrapStream, "UTF-8");

            for (DefinedClass clazz : context.getRegisterAtBoot()) {
                registryBootstrapStreamOut.write(clazz.name() + "=" + clazz.fullName() + "\n");

            }

            registryBootstrapStreamOut.flush();
            registryBootstrapStreamOut.close();
        } catch (IOException ioe) {
            throw new GenerationException(ioe);
        }
    }
}