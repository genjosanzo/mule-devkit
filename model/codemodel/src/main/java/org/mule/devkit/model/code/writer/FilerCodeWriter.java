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

package org.mule.devkit.model.code.writer;

import org.mule.devkit.model.code.CodeWriter;
import org.mule.devkit.model.code.Package;

import javax.annotation.processing.Filer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import static javax.tools.StandardLocation.SOURCE_OUTPUT;

public final class FilerCodeWriter extends CodeWriter {

    private final Filer filer;

    public FilerCodeWriter(Filer filer) {
        this.filer = filer;
    }

    @Override
    public OutputStream openBinary(Package pkg, String fileName) throws IOException {
        if (pkg != null) {
            return filer.createResource(SOURCE_OUTPUT, pkg.name(), fileName).openOutputStream();
        } else {
            return filer.createResource(SOURCE_OUTPUT, "", fileName).openOutputStream();
        }
    }

    @Override
    public Writer openSource(org.mule.devkit.model.code.Package pkg, String fileName) throws IOException {
        String name;
        if (pkg.isUnnamed()) {
            name = fileName;
        } else {
            name = pkg.name() + '.' + fileName;
        }

        name = name.substring(0, name.length() - 5);   // strip ".java"

        return filer.createSourceFile(name).openWriter();
    }

    @Override
    public void close() {
    }
}