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

import org.mule.api.NestedProcessor;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.param.Optional;

import java.util.List;

@Module(name = "callback")
public class NestedProcessorModule {
    @Processor
    public Object callbackWithPayload(NestedProcessor innerProcessor) throws Exception {
        return innerProcessor.process("payload");
    }

    @Processor
    public Object callback(@Optional NestedProcessor innerProcessor) throws Exception {
        return innerProcessor.process();
    }

    @Processor
    public Object callbackList(@Optional List<NestedProcessor> innerProcessor) throws Exception {
        return innerProcessor.get(0).process();
    }

    @Processor
    public Object processItems(List<String> items, NestedProcessor processors) throws Exception {
        return processors.process();
    }

    @Processor
    public Object processItemsOneByOne(List<String> items, List<NestedProcessor> processors) throws Exception {
        for (NestedProcessor nestedProcessor : processors) {
            nestedProcessor.process();
        }

        return processors.size();
    }

    @Processor
    public String setPayload(String payload) {
        return payload;
    }

}
