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
package org.mule.api.annotations.param;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on component methods, this annotation marks the method parameter that will be used to pass in one or more of the headers received.
 * This annotation value can define a single header, a comma-separated list of header names, '*' to denote all headers, or a comma-separated list
 * of wildcard expressions such as 'MULE_*, X-*'. By default, if a named header is not present on the current message, an exception will be thrown.
 * However, if the header name is defined with the '?' post fix, it will be marked as optional.
 * <p/>
 * When defining multiple header names or using wildcards, the parameter can be a {@link java.util.Map} or {@link java.util.List}. If a
 * {@link java.util.Map} is used, the header name and value is passed in. If {@link java.util.List} is used, just the header values are used.
 * If a single header name is defined, the header type can be used as the parameter type, though {@link java.util.List} or {@link java.util.Map}
 * can be used too.
 * <p/>
 * The Inbound headers collection is immutable, so the headers Map or List passed in will be immutable too. Attempting to write to the Map or List will result in an {@link UnsupportedOperationException}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SessionHeaders {
    /**
     * Defines the headers that should be injected into the parameter. This can be a single header, a comma-separated
     * list of header names,'*' to denote all headers or a comma-separated list of wildcard expressions. By default,
     * if a named header is not present, an exception will be thrown. However, if the header name is defined with the
     * '?' post fix, it will be marked as optional.
     * The optional '?' post fix is not supported when using wildcard expressions
     *
     * @return the header expression used to query the message for headers
     */
    String value();
}
