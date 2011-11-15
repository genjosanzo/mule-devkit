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
 * The class annotated with @OAuth is applied to a @Module that uses the OAuth 1.0a protocol for authentication.
 * Basic information about the Service Provider is required to go through the OAuth flow.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface OAuth2 {

    /**
     * The URL defined by the Service Provider used to obtain an Access Token
     */
    String accessTokenUrl();

    /**
     * The URL defined by the Service Provider where the Resource Owner will be redirected to grant
     * authorization to the Consumer
     */
    String authorizationUrl();

    /**
     * A Java regular expression used to extract the verifier from the Service Provider response as a result
     * of the Resource Owner authorizing the Consumer.
     */
    String verifierRegex() default "code=([^&]+)";

    /**
     * A Java regular expression used to extract the Access Token from the Service Provider response.
     */
    String accessTokenRegex() default "\"access_token\":\"([^&]+?)\"";

    /**
     * A Java regular expression used to extract the expiration time of the Access Token (in seconds) from the
     * Service Provider response. If the this regular expression is not found in the Service Provider response
     * (whether the regular expression is wrong or the Access Token never expires), the Access Token will be
     * treated as if it would never expire.
     */
    String expirationRegex() default "\"expires_in\":([^&]+?),";

    /**
     * In case the Service Provider only accepts a known redirect URL, assign this parameter to the path inside
     * your domain (denoted by the 'fullDomain' environment variable) that will be registered with Service Provider
     * as a redirect URL. If left empty (meaning the Service Provider accepts any URL as redirect URL), a random path
     * will be used.
     */
    String callbackPath() default "";
}