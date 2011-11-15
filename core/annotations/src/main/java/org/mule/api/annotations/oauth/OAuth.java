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
 * A class annotated with @OAuth is applied to a @Module that uses the OAuth 1.0a protocol for authentication.
 * Basic information about the Service Provider is required to go through the OAuth 1.0a flow.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface OAuth {

    /**
     * The signature method to be used in the OAuth 1.0a flow. This method will be included in the auth_signature_method parameter.
     */
    OAuthMessageSigner messageSigner() default OAuthMessageSigner.HMAC_SHA1;

    /**
     * Defines where the OAuth 1.0a parameters should be included.
     */
    OAuthSigningStrategy signingStrategy() default OAuthSigningStrategy.AUTHORIZATION_HEADER;

    /**
     * The URL defined by the Service Provider used to obtain an un-authorized Request Token
     */
    String requestTokenUrl();

    /**
     * The URL defined by the Service Provider to obtain an Access Token
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
    String verifierRegex() default "oauth_verifier=([^&]+)";

    /**
     * In case the Service Provider only accepts a known redirect URL, assign this parameter to the path inside
     * your domain (denoted by the 'fullDomain' environment variable) that will be registered with Service Provider
     * as a redirect URL. If left empty (meaning the Service Provider accepts any URL as redirect URL), a random path
     * will be used.
     */
    String callbackPath() default "";
}