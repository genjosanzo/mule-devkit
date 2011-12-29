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

package org.mule.devkit.validation;

import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;

import javax.lang.model.element.ExecutableElement;

public class StudioValidator extends JavaDocValidator {

    @Override
    public boolean shouldValidate(DevKitTypeElement typeElement, GeneratorContext context) {
        return !super.shouldValidate(typeElement, context) && !context.isEnvOptionSet("skipStudioPluginPackage");
    }

    @Override
    public void validate(DevKitTypeElement typeElement, GeneratorContext context) throws ValidationException {
        try {
            super.validate(typeElement, context);
        } catch (ValidationException e) {
            throw new ValidationException(typeElement, "Cannot generate Mule Studio plugin if required javadoc comments are not present. " +
                    "If you want to skip the generation of the Mule Studio plugin use -Ddevkit.studio.package.skip=true. Error is: " + e.getMessage(), e);
        }
    }

    protected boolean exampleDoesNotExist(GeneratorContext context, ExecutableElement method) throws ValidationException {
        // do not check for example correctness
        return false;
    }
}