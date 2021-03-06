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

import org.mule.api.ConnectionException;
import org.mule.api.ConnectionExceptionCode;
import org.mule.api.annotations.*;
import org.mule.api.annotations.param.ConnectionKey;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.callback.SourceCallback;

import java.util.Random;

@Connector(name = "connector")
public class ConnectorModule {
    @Configurable
    @Optional
    @Default("http://www.mulesoft.org")
    private String url;


    private Integer sessionId;
    private String username;
    private String password;
    private static int retryCount = 0;

    @Source
    public void stream(SourceCallback callback) {
        for (int i = 0; i < 10; i++) {
            try {
                callback.process(Integer.valueOf(i));
            } catch (Exception e) {

            }
        }
    }
    
    @Processor
    @InvalidateConnectionOn(exception = RuntimeException.class)
    public boolean invalidateConnectionUntilThirdRetry() {
        if( retryCount < 9 ) {
            retryCount++;
            throw new RuntimeException("API failed");
        }

        return this.sessionId != null;
    }    

    @Processor
    public boolean verifySession() {
        return this.sessionId != null;
    }

    @Processor
    public String getUsername() {
        return this.username;
    }

    @Processor
    public String getPassword() {
        return this.password;
    }

    @Processor
    public Integer getSessionId() {
        return this.sessionId;
    }

    @Processor
    @InvalidateConnectionOn(exception = RuntimeException.class)
    public void invalidateConnectionOnException() {
        throw new RuntimeException();
    }

    @Connect
    public void connect(@ConnectionKey String username, String password) throws ConnectionException {
        if (!this.url.equals("http://www.mulesoft.org")) {
            throw new ConnectionException(ConnectionExceptionCode.CANNOT_REACH, "", "");
        }

        this.username = username;
        this.password = password;

        Random generator = new Random();
        this.sessionId = generator.nextInt();
    }

    @Disconnect
    public void disconnect() {
        this.sessionId = null;
    }
    
    @ConnectionIdentifier
    public String connectionId() {
        return Integer.toHexString(this.sessionId);
    }

    @ValidateConnection
    public boolean isConnected() {
        return this.sessionId != null;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
