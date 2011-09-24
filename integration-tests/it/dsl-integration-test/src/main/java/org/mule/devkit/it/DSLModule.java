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

import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.param.Optional;

import java.lang.RuntimeException;

@Module(name = "dsltst")
public class DSLModule {
    @Configurable
    @Optional
    private String configValue = "";

    private boolean isExecuted = false;
    private String processedParamValue = null;

    @Processor
    public void processSometing(String param) throws Exception {
        this.processedParamValue = param;
        isExecuted = true;
    }

    @Processor
    public String getParamValue() {
        return processedParamValue;
    }

    @Processor
    public String getConfigValue() {
        return configValue;
    }

    @Processor
    public void checkProcessValue(String param) {
        if (!isExecuted){
            throw new RuntimeException("Never Executed!");
        }
        if (!param.equalsIgnoreCase(processedParamValue)){
            throw new RuntimeException("Error");
        }
    }

    @Processor
    public void checkConfigValue(String param) {
        if (!param.equalsIgnoreCase(configValue)){
            throw new RuntimeException("Error");
        }
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
}