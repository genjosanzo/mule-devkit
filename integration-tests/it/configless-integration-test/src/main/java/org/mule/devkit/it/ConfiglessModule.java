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
package org.mule.devkit.it;

import org.mule.api.MuleContext;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;

import javax.inject.Inject;

/**
 * Basic module
 *
 * @author MuleSoft, Inc.
 */
@Module(name = "configless")
public class ConfiglessModule {
    @Inject
    private MuleContext context;

    /**
     * Passthru string
     *
     * @param value Value to passthru
     * @return The same string
     */
    @Processor
    public String passthruString(String value) {
        if( context == null ) {
            throw new RuntimeException();
        }
        return value;
    }

    public void setContext(MuleContext context) {
        this.context = context;
    }
}
