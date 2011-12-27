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

import org.mule.devkit.generation.AbstractGenerator;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.generation.mule.studio.editor.MuleStudioEditorXmlGenerator;

import java.util.Arrays;
import java.util.List;

/**
 * Acts as a composite of generators that contribute to the structure of the Mule Studio plugin
 */
public class MuleStudioPluginGenerator extends AbstractMessageGenerator {

    public static final String[] GENERATED_FILES = new String[]{
            MuleStudioManifestGenerator.MANIFEST_FILE_NAME,
            MuleStudioEditorXmlGenerator.EDITOR_XML_FILE_NAME,
            MuleStudioPluginActivatorGenerator.ACTIVATOR_PATH,
            MuleStudioPluginXmlGenerator.PLUGIN_XML_FILE_NAME};

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return !context.hasOption("skipStudioPluginPackage");
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) throws GenerationException {
        List<? extends AbstractGenerator> muleStudioGenerators = Arrays.asList(
                new MuleStudioManifestGenerator(),
                new MuleStudioEditorXmlGenerator(),
                new MuleStudioPluginActivatorGenerator(),
                new MuleStudioPluginXmlGenerator());
        for (AbstractGenerator muleStudioGenerator : muleStudioGenerators) {
            muleStudioGenerator.generate(typeElement, context);
        }
    }
}