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

package org.mule.api.callback;

/**
 * Callback interface used by {@link org.mule.api.annotations.Processor} annotated methods which also are
 * declared as intercepting.
 *
 * This callback is there to facilitate the decision made by the processor to continue the chain or to stop
 * it.
 */
public interface InterceptCallback {
    /**
     * Message processors that call this method indicate that they wish the
     * processing to stop.
     */
    void doNotContinue();
}
