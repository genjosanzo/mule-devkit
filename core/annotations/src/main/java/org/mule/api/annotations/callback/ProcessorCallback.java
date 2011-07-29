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

package org.mule.api.annotations.callback;

import java.util.Map;

/**
 * Callback interface used by {@link org.mule.api.annotations.Processor} annotated methods.
 *
 * The method paremeters of type {@link ProcessorCallback} will be able to receive other
 * message processors. The {@link org.mule.api.annotations.Processor} annotated method
 * can use the process method to execute them.
 */
public interface ProcessorCallback {
    /**
     * Dispatch original message to the processor chain
     *
     * @param properties Additional invocation properties
     * @return The return payload for the processor chain
     */
    Object process(Map<String, Object> properties) throws Exception;

    /**
     * Dispatch message to the processor chain
     *
     * @param payload The payload of the message
     * @param properties Additional invocation properties
     * @return The return payload for the processor chain
     */
    Object process(Object payload, Map<String, Object> properties) throws Exception;

    /**
     * Dispatch message to the processor chain
     *
     * @param payload The payload of the message
     * @return The return payload for the processor chain
     */
    Object process(Object payload) throws Exception;

    /**
     * Dispatch original message to the processor chain
     *
     * @return The return payload for the processor chain
     */
    Object process() throws Exception;
}
