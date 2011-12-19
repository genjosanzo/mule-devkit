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

import org.mule.api.annotations.ExpressionEnricher;
import org.mule.api.annotations.ExpressionLanguage;
import org.mule.api.annotations.param.Payload;

import java.util.Map;

@ExpressionLanguage(name = "map-payload")
public class MapPayloadExpressionLanguage {
    @ExpressionEnricher
    public void enrich(String expression, Object object, @Payload Object payload) {
        if (payload == null) {
            throw new IllegalArgumentException();
        }
        if( !(payload instanceof Map) ) {
            throw new IllegalArgumentException();
        }
        Map mapPayload = (Map)payload;

        mapPayload.put(expression, object);
    }
}
