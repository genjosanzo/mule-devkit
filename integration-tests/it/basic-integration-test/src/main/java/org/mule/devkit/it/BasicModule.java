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

/**
 * Basic module
 *
 * @author MuleSoft, Inc.
 */
@Module(name = "basic")
public class BasicModule {
    /**
     * Passthru char
     *
     * @param value Value to passthru
     * @return The same char
     */
    @Processor
    public char passthruChar(char value) {
        return value;
    }

    /**
     * Passthru string
     *
     * @param value Value to passthru
     * @return The same string
     */
    @Processor
    public String passthruString(String value) {
        return value;
    }

    /**
     * Passthru float
     *
     * @param value Value to passthru
     * @return The same float
     */
    @Processor
    public float passthruFloat(float value) {
        return value;
    }

    /**
     * Passthru boolean
     *
     * @param value Value to passthru
     * @return The same boolean
     */
    @Processor
    public boolean passthruBoolean(boolean value) {
        return value;
    }

    /**
     * Passthru integer
     *
     * @param value Value to passthru
     * @return The same integer
     */
    @Processor
    public int passthruInteger(int value) {
        return value;
    }

    /**
     * Passthru long
     *
     * @param value Value to passthru
     * @return The same long
     */
    @Processor
    public long passthruLong(long value) {
        return value;
    }

    /**
     * Passthru complex float
     *
     * @param value Value to passthru
     * @return The same complex float
     */
    @Processor
    public Float passthruComplexFloat(Float value) {
        return value;
    }

    /**
     * Passthru complex boolean
     *
     * @param value Value to passthru
     * @return The same complex boolean
     */
    @Processor
    public Boolean passthruComplexBoolean(Boolean value) {
        return value;
    }

    /**
     * Passthru complex integer
     *
     * @param value Value to passthru
     * @return The same complex integer
     */
    @Processor
    public Integer passthruComplexInteger(Integer value) {
        return value;
    }

    /**
     * Passthru complex long
     *
     * @param value Value to passthru
     * @return The same complex long
     */
    @Processor
    public Long passthruComplexLong(Long value) {
        return value;
    }

    public enum Mode {
        In,
        Out
    }

    /**
     * Passthru mode enum
     *
     * @param mode Value to passthru
     * @return The same cmode enum
     */
    @Processor
    public String passthruEnum(Mode mode) {
        return mode.name();
    }

    /**
     * Passthru complex object
     *
     * @param myComplexObject Value to passthru
     * @return The same complex object
     */
    @Processor
    public String passthruComplexRef(MyComplexObject myComplexObject) {
        return myComplexObject.getValue();
    }
}
