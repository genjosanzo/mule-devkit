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

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

/**
 * This component is just for test usage.
 */
public class ComponentY implements Callable {

    public static boolean wasExecuted;

    public Object onCall(MuleEventContext eventContext) throws Exception {
        wasExecuted = true;
        return "";
    }

    public static boolean wasExecuted() {
        return wasExecuted;
    }
}