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

package org.mule.devkit.apt.generator;

import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.JClassAlreadyExistsException;
import org.mule.devkit.model.code.JPackage;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.util.ClassNameUtils;
import org.mule.devkit.generation.GenerationException;

import javax.lang.model.element.TypeElement;

public class MetadataGenerator extends ContextualizedGenerator {
    public MetadataGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement type) throws GenerationException {
        String metadataInterfaceName = getContext().getElements().getBinaryName(type) + "Metadata";

        try {
            JPackage pkg = getContext().getCodeModel()._package(ClassNameUtils.getPackageName(metadataInterfaceName));
            DefinedClass jc = pkg._interface(ClassNameUtils.getClassName(metadataInterfaceName));
        } catch (JClassAlreadyExistsException e) {
            throw new GenerationException("Internal Error: Class " + metadataInterfaceName + " already exists");
        }
    }
}
