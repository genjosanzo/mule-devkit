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
package org.mule.api;

/**
 * Enumeration of possible capabilities of Mule modules. Each capability represents
 * a bit in a bit array. The capabilities of a particular module can be queried using
 * {@link Capabilities}
 */
public enum Capability {

    /**
     * This capability indicates that the module implements {@link org.mule.api.lifecycle.Lifecycle}
     */
    LIFECYCLE_CAPABLE(0),

    /**
     * This capability indicates that the module implements {@link org.mule.api.annotations.adapter.SessionManagerAdapter}
     */
    SESSION_MANAGEMENT_CAPABLE(1),

    /**
     * This capability indicates that the module implements {@link org.mule.api.annotations.adapter.OAuth1Adapter}
     */
    OAUTH1_CAPABLE(2),

    /**
     * This capability indicates that the module implements {@link org.mule.api.annotations.adapter.OAuth2Adapter}
     */
    OAUTH2_CAPABLE(3),

    /**
     * This capability indicates that the module implements {@link org.mule.api.annotations.adapter.PoolingAdapter}
     */
    POOLING_CAPABLE(4);

    private int bit;

    Capability(int bit) {
        this.bit = bit;
    }

    public int getBit() {
        return this.bit;
    }
}
