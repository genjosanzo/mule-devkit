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
package org.mule.api.annotations.studio;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Defines how to display the annotated field or parameter in a Mule Studio dialog.
 */
@Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
public @interface Display {

    /**
     * The name of the tab in which the annotated element should be rendered. If not tab is
     * specified it will be rendered in a default tab. To render more than one parameter or
     * field in the same the tab then this value should be exactly the same for them.
     * NOTE: not valid when annotating classes and methods.
     */
    String tab() default "";

    /**
     * A group is a subdivision of a Mule Studio dialog window in which one or inputs are rendered
     * together. If the element is not annotated with this annotation it will be part of a
     * default group. If a method or class specifies more than one different value,
     * groups will be rendered in the order they are declared. Within a single group, inputs are
     * rendered in the order they appear.
     * The name of the group in which the annotated element should be rendered. A group can
     * contain more than one element. In such case, these elements have to be annotated and
     * use the same value.
     * NOTE: not valid when annotating classes and methods.
     */
    String inputGroup() default "";

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