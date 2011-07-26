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

import org.mule.api.annotations.callback.ProcessorCallback;
import org.mule.api.annotations.param.Optional;

public class Apple {
    @Optional
    private boolean isBitten;

    @Optional
    private int weight;

    @Optional
    private ProcessorCallback whenBitten;

    public boolean isBitten() {
        return isBitten;
    }

    public void setIsBitten(boolean bitten) {
        isBitten = bitten;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;

        try {
            whenBitten.process(this.weight);
        } catch(Exception e) {
            // do nothing
        }
    }

    public ProcessorCallback getWhenBitten() {
        return whenBitten;
    }

    public void setWhenBitten(ProcessorCallback whenBitten) {
        this.whenBitten = whenBitten;
    }
}