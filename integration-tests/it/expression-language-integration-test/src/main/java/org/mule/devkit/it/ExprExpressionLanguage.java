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

import org.mule.api.annotations.ExpressionEvaluator;
import org.mule.api.annotations.ExpressionLanguage;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.InvocationHeaders;
import org.mule.api.annotations.param.OutboundHeaders;
import org.mule.api.annotations.param.Payload;

import java.util.Map;

@ExpressionLanguage(name = "expr")
public class ExprExpressionLanguage {

    @ExpressionEvaluator
    public Object evaluate(String expression,
                           @Payload Object payload,
                           @InboundHeaders("*") Map<String, Object> inboundHeaders,
                           @InvocationHeaders("*") Map<String, Object> invocationHeaders,
                           @OutboundHeaders Map<String, Object> outboundHeaders) {
        if (payload == null) {
            throw new IllegalArgumentException();
        } else if (inboundHeaders == null) {
            throw new IllegalArgumentException();
        } else if (invocationHeaders == null) {
            throw new IllegalArgumentException();
        } else if (outboundHeaders == null) {
            throw new IllegalArgumentException();
        }
        return expression;
    }
}
