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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation will mark a method as a Transformer. A {@link org.mule.api.transformer.Transformer} will be
 * generated. The signature for the method must match the following:
 *
 * public Object method(Object payload)
 *
 * It can only have one argument and it must be {@link Object}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Transformer
{
    /**
     * The 'priorityWeighting property is used to resolve conflicts where there is more than one transformers that match
     * the selection criteria.  10 is the highest priority and 1 is the lowest.
     *
     * @return the priority weighting for this transformer. If the class defines more than one transform method, every transform
     *         method will have the same weighting.
     */
    int priorityWeighting() default 5;

    /**
     * SourceTypes define additional types that this transformer will accepts as a sourceType (beyond the method parameter).
     * At run time if the current message matches one of these source types, Mule will attempt to transform from
     * the source type to the method parameter type.  This means that transformations can be chained. The user can create
     * other transformers to be a part of this chain.
     *
     * @return an array of class types which allow the transformer to be matched on
     */
    Class[] sourceTypes() default {};
}
