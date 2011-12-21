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
package org.mule.api.annotations.display;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Adds displaying information to a field or parameter.
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface VariableDisplay {

    /**
     * A group is a logical way to display one or more input fields together. If no inputGroup is specified then a
     * dafult input group is assumed. To place more than one element in the same input group, use the same value for
     * inputGroup attribute
     */
    String inputGroup() default "";

    /**
     * A tab is a logical way to group input groups together. This attributes specifies the name of the tab in which the
     * annotated element should be displayed rendered. If no tab is specified then a default tab is assumed. To display
     * more than one parameter or field in the same the tab then this value should be exactly the same for all of them.
     */
    String tab() default "";

    /**
     * The caption is a short name for the annotated element. If this value is not specified it will
     * inferred from the annotated element name.
     */
    String caption() default "";

    /**
     * The description is a friendly explanation for the annotated element. If this value is not specified
     * the javadoc of the annotated element.
     */
    String description() default "";
}