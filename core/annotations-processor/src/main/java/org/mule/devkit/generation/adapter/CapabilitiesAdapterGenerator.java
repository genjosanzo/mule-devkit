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
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Module;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.NamingContants;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.TypeReference;

import javax.lang.model.element.TypeElement;

public class CapabilitiesAdapterGenerator extends AbstractModuleGenerator {

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return typeElement.hasAnnotation(Module.class) || typeElement.hasAnnotation(Connector.class);
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {
        DefinedClass capabilitiesAdapter = getCapabilitiesAdapterClass(typeElement);
        capabilitiesAdapter.javadoc().add("A <code>" + capabilitiesAdapter.name() + "</code> is a wrapper around ");
        capabilitiesAdapter.javadoc().add(ref(typeElement.asType()));
        capabilitiesAdapter.javadoc().add(" that implements {@link org.mule.api.Capabilities} interface.");

        generateIsCapableOf(typeElement, capabilitiesAdapter);

    }

    private DefinedClass getCapabilitiesAdapterClass(TypeElement typeElement) {
        String lifecycleAdapterName = context.getNameUtils().generateClassName(typeElement, NamingContants.ADAPTERS_NAMESPACE, NamingContants.CAPABILITIES_ADAPTER_CLASS_NAME_SUFFIX);
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(lifecycleAdapterName));

        TypeReference previous = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));

        if( previous == null ) {
            previous = (TypeReference) ref(typeElement.asType());
        }
        
        int modifiers = Modifier.PUBLIC;
        if( typeElement.getModifiers().contains(javax.lang.model.element.Modifier.ABSTRACT) ) {
            modifiers |= Modifier.ABSTRACT;
        }

        DefinedClass clazz = pkg._class(modifiers, context.getNameUtils().getClassName(lifecycleAdapterName), previous);
        clazz._implements(Capabilities.class);

        context.setClassRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement), clazz);

        return clazz;
    }
}