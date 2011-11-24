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
package org.mule.devkit.it;

import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuthAccessToken;
import org.mule.api.annotations.oauth.OAuthAccessTokenSecret;
import org.mule.api.annotations.oauth.OAuthConsumerKey;
import org.mule.api.annotations.oauth.OAuthConsumerSecret;

/**
 * This an OAuth test module
 *
 * @author MuleSoft, Inc.
 */
@Module(name = "oauth")
@OAuth(requestTokenUrl = OAuthModule.REQUEST_TOKEN_URL,
        authorizationUrl = OAuthModule.AUTHORIZATION_URL,
        accessTokenUrl = OAuthModule.ACCESS_TOKEN_URL)
public class OAuthModule {

    public static final int PORT = Constants.OAUTH_ENDPOINTS_PORT;
    public static final String REQUEST_TOKEN_URL = "http://localhost:" + PORT + "/requestToken";
    public static final String AUTHORIZATION_URL = "http://localhost:" + PORT + "/authorize";
    public static final String ACCESS_TOKEN_URL = "http://localhost:" + PORT + "/accessToken";
    public static final String PROTECTED_RESOURCE = "protected resource";
    public static final String NON_PROTECTED_RESOURCE = "non protected resource";

    /**
     * Consumer Key
     */
    @Configurable
    @OAuthConsumerKey
    private String consumerKey;

    /**
     * Consumer Secret
     */
    @Configurable
    @OAuthConsumerSecret
    private String consumerSecret;

    /**
     * Access protected resource
     *
     * @param accessToken Access token
     * @param accessTokenSecret Access token secret
     * @return
     */
    @Processor
    public String protectedResource(@OAuthAccessToken String accessToken, @OAuthAccessTokenSecret String accessTokenSecret) {
        if (!accessToken.equals(Constants.ACCESS_TOKEN) || !accessTokenSecret.equals(Constants.ACCESS_TOKEN_SECRET)) {
            throw new RuntimeException("Access token or access token secret do not match expected");
        }
        return PROTECTED_RESOURCE;
    }

    /**
     * Access a non-protected resource
     *
     * @return
     */
    @Processor
    public String nonProtectedResource() {
        return NON_PROTECTED_RESOURCE;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }
}