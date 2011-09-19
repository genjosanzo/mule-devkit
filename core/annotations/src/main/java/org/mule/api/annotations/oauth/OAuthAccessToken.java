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
package org.mule.api.annotations.oauth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Parameters annotated with @OAuthAccessToken need to be inside a class annotated with {@link OAuth} or {@link OAuth2}.
 * The parameter must be of type String. When the method that contains this annotated parameter is invoked, a OAuth
 * access token will be passed in case the Resource Owner already authorized the Consumer, otherwise the method will not
 * be invoked and the Resource Owner will be redirected to the {@link OAuth#authorizationUrl()} or {@link OAuth2#authorizationUrl()}
 * depending on the class level annotation used.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface OAuthAccessToken {

}