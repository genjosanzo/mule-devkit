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
package org.mule.api.oauth;

/**
 * Adds OAuth 1.0a capabilities to the pojo
 */
public interface OAuth1Adapter {

    /**
     * Retrieve OAuth verifier
     *
     * @return A String representing the OAuth verifier
     */
    String getOauthVerifier();

    /**
     * Set OAuth verifier
     *
     * @param value OAuth verifier to set
     */
    void setOauthVerifier(String value);

    /**
     * Retrieve redirect url
     */
    String getRedirectUrl();

    /**
     * Retrieve access token
     */
    String getAccessToken();

    /**
     * Set access token
     *
     * @param value
     */
    void setAccessToken(String value);

    /**
     * Retrieve access token secret
     */
    String getAccessTokenSecret();

    /**
     * Set access token secret
     *
     * @param value Access token secret
     */
    void setAccessTokenSecret(String value);

    /**
     * Retrieve authorization url
     */
    String getAuthorizationUrl() throws UnableToAcquireRequestTokenException;


    /**
     * Acquire access token and secret
     *
     * @throws UnableToAcquireAccessTokenException
     */
    void fetchAccessToken() throws UnableToAcquireAccessTokenException;

    /**
     * Set the callback to be called when the access token and secret need to be saved for
     * later restoration
     *
     * @param saveCallback Callback to be called
     */
    void setSaveAccessTokenCallback(SaveAccessTokenCallback saveCallback);

    /**
     * Set the callback to be called when the access token and secret need to be restored
     *
     * @param restoreCallback Callback to be called
     */
    void setRestoreAccessTokenCallback(RestoreAccessTokenCallback restoreCallback);
}
