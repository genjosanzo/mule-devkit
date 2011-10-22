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
package org.mule.api;

/**
 * Exception thrown when the method annotated with {@link org.mule.api.annotations.Connect} fails to
 * connect properly.
 */
public class ConnectionException extends Exception {
    /**
     * Exception code
     */
    private ConnectionExceptionCode code;

    /**
     * Third-party code
     */
    private String thirdPartyCode;

    /**
     * Create a new connection exception
     *
     * @param code           Code describing what went wrong. Use {@link ConnectionExceptionCode.UNKNOWN} for unexpected problems.
     * @param thirdPartyCode Code as provided by the third party API
     * @param message        Message describing what went wrong
     */
    public ConnectionException(ConnectionExceptionCode code, String thirdPartyCode, String message) {
        super(message);

        this.code = code;
        this.thirdPartyCode = thirdPartyCode;
    }

    /**
     * Create a new connection exception
     *
     * @param code           Code describing what went wrong. Use {@link ConnectionExceptionCode.UNKNOWN} for unexpected problems.
     * @param thirdPartyCode Code as provided by the third party API
     * @param throwable      Inner exception
     * @param message        Message describing what went wrong
     */
    public ConnectionException(ConnectionExceptionCode code, String thirdPartyCode, String message, Throwable throwable) {
        super(message, throwable);

        this.code = code;
        this.thirdPartyCode = thirdPartyCode;
    }

    /**
     * Get a code for what went wrong
     *
     * @return A {@link ConnectionExceptionCode}
     */
    public ConnectionExceptionCode getCode() {
        return code;
    }

    /**
     * Get a code for what went wrong as provided
     * by the third party API
     *
     * @return A string with the code
     */
    public String getThirdPartyCode() {
        return thirdPartyCode;
    }
}
