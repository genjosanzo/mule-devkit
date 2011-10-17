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
import org.mule.api.annotations.lifecycle.Start;

import javax.annotation.PostConstruct;

@Module(name = "lifecycle")
public class LifecycleModule
{
    @Configurable
    private String cycleName;


    private boolean isStarted = false;
    private boolean hasBeenInitialized = false;

    @PostConstruct
    public void init() {
        if( cycleName == null ) {
            throw new RuntimeException("@PostConstruct before setting variables");
        }

        hasBeenInitialized = true;
    }

    @Start
    public void startMeUp() throws Exception {
        isStarted = true;
    }

    @Processor
    public boolean isStarted() {
        return isStarted;
    }

    @Processor
    public boolean hasBeenInitialized() {
        return hasBeenInitialized;
    }

    public String getCycleName() {
        return cycleName;
    }

    public void setCycleName(String cycleName) {
        this.cycleName = cycleName;
    }
}
