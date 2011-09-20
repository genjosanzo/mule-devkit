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

import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.callback.HttpCallback;
import org.mule.api.annotations.param.Optional;

import java.io.IOException;
import java.net.URL;

@Module(name = "callback")
public class HttpCallbackProcessorModule {

    @Processor
    public void doA(HttpCallback onEventX, HttpCallback onEventY) {
        try {
            simulateCallback(onEventX.getUrl());
            simulateCallback(onEventY.getUrl());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Processor
    public void doB(@Optional HttpCallback onEventX) {
        try {
            if (onEventX != null) {
                simulateCallback(onEventX.getUrl());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void simulateCallback(String callbackUrl) throws IOException {
        new URL(callbackUrl).openConnection().getContent();
    }
}