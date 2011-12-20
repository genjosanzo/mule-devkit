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

package org.mule.devkit.generation.mule.studio;

import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class MuleStudioManifestGenerator extends AbstractMessageGenerator {

    public static final String MANIFEST_FILE_NAME = "META-INF/MANIFEST.MF";

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return !context.hasOption("skipStudioPluginPackage");
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) throws GenerationException {
        PrintStream printStream = null;
        try {
            OutputStream outputStream = context.getCodeModel().getCodeWriter().openBinary(null, MANIFEST_FILE_NAME);
            printStream = new PrintStream(outputStream);
            printStream.append(getManifestContents(typeElement));
            printStream.flush();
        } catch (IOException e) {
            throw new GenerationException("Could not create MANIFEST for Studio plugin", e);
        } finally {
            IOUtils.closeQuietly(printStream);
        }
    }

    private String getManifestContents(DevKitTypeElement typeElement) {
        StringBuilder manfiestContentBuilder = new StringBuilder(100);
        manfiestContentBuilder.append("Manifest-Version: 1.0\n");
        manfiestContentBuilder.append("Bundle-ManifestVersion: 2\n");
        manfiestContentBuilder.append("Bundle-Name: ").append(context.getNameUtils().friendlyNameFromCamelCase(typeElement.name())).append("\n");
        manfiestContentBuilder.append("Bundle-SymbolicName: org.mule.tooling.ui.contribution.").append(typeElement.name()).append(";singleton:=true\n");
        manfiestContentBuilder.append("Bundle-Activator: org.mule.tooling.ui.contribution.").append(typeElement.name()).append(".Activator\n");
        manfiestContentBuilder.append("Bundle-Vendor: ").append(context.getJavaDocUtils().getTagContent("author", typeElement.getInnerTypeElement())).append("\n");
        manfiestContentBuilder.append("Require-Bundle: org.eclipse.ui,\n");
        manfiestContentBuilder.append(" org.eclipse.core.runtime,\n");
        manfiestContentBuilder.append(" org.mule.tooling.core;bundle-version=\"1.0.0\"\n");
        manfiestContentBuilder.append("Bundle-RequiredExecutionEnvironment: JavaSE-1.6\n");
        manfiestContentBuilder.append("Bundle-ActivationPolicy: lazy\n");
        manfiestContentBuilder.append("Eclipse-BundleShape: dir");
        return manfiestContentBuilder.toString();
    }
}