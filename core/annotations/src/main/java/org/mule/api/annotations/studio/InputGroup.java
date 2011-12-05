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
 * Defines how to group the annotated field or parameter in a Mule Studio dialog. A group
 * is a subdivision of a Mule Studio dialog window in which one or inputs are rendered
 * together.
 *
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface InputGroup {

    /**
     * The name of the group in which the annotated element should be rendered. A group can
     * contain more than one element. In such case, these elements have to be annotated and
     * use the same {@link InputGroup#value()} constant.
     * @return
     */
    String value();
}