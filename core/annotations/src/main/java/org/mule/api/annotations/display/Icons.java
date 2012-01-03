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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Used in {@link org.mule.api.annotations.Module} and {@link org.mule.api.annotations.Connector} annotated classes
 * to associate icons with them. Provided values are relative the annotated type location. If values are not provided
 * then {@link this#GENERIC_CLOUD_CONNECTOR_LARGE}, {@link this#GENERIC_CLOUD_CONNECTOR_SMALL},
 * {@link this#GENERIC_ENDPOINT_LARGE}, {@link this#GENERIC_ENDPOINT_SMALL}, {@link this#GENERIC_TRANSFORMER_LARGE} and
 * {@link this#GENERIC_TRANSFORMER_SMALL} will be used as needed formatting the strings with
 * {@link org.mule.api.annotations.Module#name()} or {@link org.mule.api.annotations.Connector#name()}
 */
@Target(ElementType.TYPE)
@Documented
public @interface Icons {

    String GENERIC_CLOUD_CONNECTOR_SMALL = "../../../icons/%s-connector-24x16.png";
    String GENERIC_CLOUD_CONNECTOR_LARGE = "../../../icons/%s-connector-48x32.png";
    String GENERIC_TRANSFORMER_SMALL = "../../../icons/%s-transformer-24x16.png";
    String GENERIC_TRANSFORMER_LARGE = "../../../icons/%s-transformer-48x32.png";
    String GENERIC_ENDPOINT_SMALL = "../../../icons/%s-endpoint-24x16.png";
    String GENERIC_ENDPOINT_LARGE = "../../../icons/%s-endpoint-48x32.png";

    String connectorSmall() default GENERIC_CLOUD_CONNECTOR_SMALL;

    String connectorLarge() default GENERIC_CLOUD_CONNECTOR_LARGE;

    String transformerSmall() default GENERIC_TRANSFORMER_SMALL;

    String transformerLarge() default GENERIC_TRANSFORMER_LARGE;

    String endpointSmall() default GENERIC_ENDPOINT_SMALL;

    String endpointLarge() default GENERIC_ENDPOINT_LARGE;
}