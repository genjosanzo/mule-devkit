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

package org.mule.devkit.generation.adapter;

import org.mule.api.Capabilities;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.DevkitTypeElement;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.TypeReference;

import javax.lang.model.element.TypeElement;

public class CapabilitiesAdapterGenerator extends AbstractModuleGenerator {

    @Override
    protected boolean shouldGenerate(DevkitTypeElement typeElement) {
        return true;
    }

    @Override
    protected void doGenerate(DevkitTypeElement typeElement) {
        DefinedClass capabilitiesAdapter = getCapabilitiesAdapterClass(typeElement);
        capabilitiesAdapter.javadoc().add("A <code>" + capabilitiesAdapter.name() + "</code> is a wrapper around ");
        capabilitiesAdapter.javadoc().add(ref(typeElement.asType()));
        capabilitiesAdapter.javadoc().add(" that implements {@link Capabilities} interface.");

        generateIsCapableOf(typeElement, capabilitiesAdapter);

    }

    private DefinedClass getCapabilitiesAdapterClass(TypeElement typeElement) {
        String lifecycleAdapterName = context.getNameUtils().generateClassName(typeElement, ".config", "CapabilitiesAdapter");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(lifecycleAdapterName));

        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(lifecycleAdapterName), (TypeReference) ref(typeElement.asType()));
        clazz._implements(Capabilities.class);

        context.setClassRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement), clazz);

        return clazz;
    }
}