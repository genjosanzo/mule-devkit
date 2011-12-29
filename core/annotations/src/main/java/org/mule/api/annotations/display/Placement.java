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
 * Adds placement information to a field or parameter.
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface Placement {

    int FIRST = 1;
    int LAST = Integer.MAX_VALUE;

    /**
     * Gives the annotated element a relative order within its {@link this#group()}. The value provided may be repeated
     * and in that case the order is not guaranteed.
     * The value is relative meaning that an element with order 10 has higher precence than one with value 25. For
     * convenience you may use {@link this#FIRST} or {@link this#LAST}
     */
    int order();

    /**
     * A group is a logical way to display one or more variables together. If no group is specified then a
     * dafult group is assumed. To place more than one element in the same group, use the exact same values for
     * the this attribute
     */
    String group() default "";

    /**
     * A tab is a logical way to groups together. This attributes specifies the name of the tab in which the
     * annotated element should be displayed. If no tab is specified then a default tab is assumed. To display
     * more than one parameter or field in the same the tab then this value should be exactly the same for all of them.
     */
    String tab() default "";
}