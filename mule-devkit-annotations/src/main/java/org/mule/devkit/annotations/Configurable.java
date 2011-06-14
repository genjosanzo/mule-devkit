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

package org.mule.devkit.annotations;

import java.lang.annotation.*;

/**
 * Marks a field inside a {@link Module} as being configurable. A user will be able to use XML attributes to set this
 * bean properties when using the Module.
 *
 * The field must have setter and getters.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Configurable {
    /**
     * The name that the user of the module will use to configure this field.
     *
     * @return The name of the XML attribute
     */
    String name() default "";

    /**
     * Denotes if this field is optional or not. If it is not optional then the user
     * of the module must provide a value for this field before they can use the module.
     *
     * Defaults to false.
     *
     * @return True if optional, false otherwise
     */
    boolean optional() default false;

    /**
     * Default value for this field
     */
    String defaultValue() default "";
}
