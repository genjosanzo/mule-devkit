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

import java.lang.String;

@Module(name = "basic")
public class BasicModule
{

    @Processor
    public char passthruChar(char value)
    {
        return value;
    }

    @Processor
    public String passthruString(String value)
    {
        return value;
    }

    @Processor
    public float passthruFloat(float value)
    {
        return value;
    }

    @Processor
    public boolean passthruBoolean(boolean value)
    {
        return value;
    }

    @Processor
    public int passthruInteger(int value)
    {
        return value;
    }

    @Processor
    public long passthruLong(long value)
    {
        return value;
    }

    @Processor
    public Float passthruComplexFloat(Float value)
    {
        return value;
    }

    @Processor
    public Boolean passthruComplexBoolean(Boolean value)
    {
        return value;
    }

    @Processor
    public Integer passthruComplexInteger(Integer value)
    {
        return value;
    }

    @Processor
    public Long passthruComplexLong(Long value)
    {
        return value;
    }

    public enum Mode
    {
        In,
        Out
    }

    @Processor
    public String passthruEnum(Mode mode)
    {
        return mode.name();
    }

    @Processor
    public String passthruComplexRef(MyComplexObject myComplexObject) {
        return myComplexObject.getValue();
    }
}
