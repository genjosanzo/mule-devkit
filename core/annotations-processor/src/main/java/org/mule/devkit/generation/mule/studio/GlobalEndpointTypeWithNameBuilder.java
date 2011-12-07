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

import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.studio.GlobalType;

public class GlobalEndpointTypeWithNameBuilder extends GlobalEndpointTypeBuilder {

    public static final String ABSTRACT_GLOBAL_ENDPOINT_LOCAL_ID = "abstractGlobalEndpoint";

    public GlobalEndpointTypeWithNameBuilder(GeneratorContext context, DevKitTypeElement typeElement) {
        super(context, null, typeElement);
    }

    @Override
    public GlobalType build() {
        GlobalType globalEndpoint = super.build();
        globalEndpoint.setAbstract(true);
        return globalEndpoint;
    }

    protected String getDescriptionBasedOnType() {
        return "";
    }

    protected String getExtendsBasedOnType() {
        return null;
    }

    protected String getLocalIdBasedOnType() {
        return ABSTRACT_GLOBAL_ENDPOINT_LOCAL_ID;
    }

    protected String getCaptionBasedOnType() {
        return "";
    }

    protected String getNameDescriptionBasedOnType() {
        return "Endpoint name";
    }
}