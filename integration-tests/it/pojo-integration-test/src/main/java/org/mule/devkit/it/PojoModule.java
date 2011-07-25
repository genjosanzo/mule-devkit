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

import java.util.List;
import java.util.Map;

@Module(name = "pojo")
public class PojoModule {
    @Configurable
    @Optional
    private Apple apple;

    @Processor
    public boolean isBitten(Apple apple) {
        return apple.isBitten();
    }

    @Processor
    public boolean areAllBitten(List<Apple> apples) {
        for (Apple apple : apples) {
            if (!apple.isBitten()) {
                return false;
            }
        }

        return true;
    }

    @Processor
    public boolean areAllBittenMap(Map<String, Apple> apples) {
        for (String key : apples.keySet()) {
            if (!apples.get(key).isBitten()) {
                return false;
            }
        }

        return true;
    }

    private void setApple(Apple apple) {
        this.apple = apple;
    }
}
