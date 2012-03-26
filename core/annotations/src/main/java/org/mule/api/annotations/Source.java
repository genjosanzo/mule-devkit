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

package org.mule.api.annotations;

import org.mule.MessageExchangePattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks a method inside a {@link Module} as a callable from within a Mule flow and capable of
 * generating Mule events. Each marked method will have a {@link org.mule.api.source.MessageSource} generated.
 * <p/>
 * The method must receive a {@link org.mule.api.callback.SourceCallback} as one of its arguments. It does not matter which parameter it is
 * as long it is there.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Source {
    /**
     * The xml name of the element that will invoke this source. This is optional and if it is not specified a name
     * will be derived from the name of the method.
     */
    String name() default "";

    /**
     * A user-friendly name for this processor.
     */
    String friendlyName() default "";

    /**
     * Does this message source must run on all cluster nodes, or just the primary one?
     */
    boolean primaryNodeOnly() default false;

    /**
     * Threading model
     */
    SourceThreadingModel threadingModel() default SourceThreadingModel.SINGLE_THREAD;

    /**
     * Message exchange pattern
     */
    MessageExchangePattern exchangePattern() default MessageExchangePattern.ONE_WAY;
}
