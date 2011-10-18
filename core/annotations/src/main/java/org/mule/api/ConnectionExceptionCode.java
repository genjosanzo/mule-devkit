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
 * List of possible outcomes to a connection failure
 */
public enum ConnectionExceptionCode {
    /**
     * The host cannot be resolved to an IP address
     */
    UNKNOWN_HOST,
    /**
     * The destination cannot be reached. Either the host is wrong
     * or the port might be.
     */
    CANNOT_REACH,
    /**
     * The supplied credentials are not correct.
     */
    INCORRECT_CREDENTIALS,
    /**
     * The credentials used to authenticate has expired.
     */
    CREDENTIALS_EXPIRED,
    /**
     * Something else went wrong.
     */
    UNKNOWN;
}
