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
package org.mule.devkit.dynamic.api.helper;

import org.mule.api.ConnectionManager;

/**
 * Helper methods for {@link ConnectionManager}.
 */
public final class ConnectionManagers {

    private static final String SET_USERNAME_METHOD_NAME = Reflections.setterMethodName("username");
    private static final String SET_PASSWORD_METHOD_NAME = Reflections.setterMethodName("password");
    private static final String SET_SECURITY_TOKEN_METHOD_NAME = Reflections.setterMethodName("securityToken");

    private ConnectionManagers() {
    }

    public static void setUsername(final ConnectionManager<?, ?> connectionManager, final String username) {
        Reflections.invoke(connectionManager, ConnectionManagers.SET_USERNAME_METHOD_NAME, username);
    }

    public static void setPassword(final ConnectionManager<?, ?> connectionManager, final String password) {
        Reflections.invoke(connectionManager, ConnectionManagers.SET_PASSWORD_METHOD_NAME, password);
    }

    public static void setSecurityToken(final ConnectionManager<?, ?> connectionManager, final String securityToken) {
        Reflections.invoke(connectionManager, ConnectionManagers.SET_SECURITY_TOKEN_METHOD_NAME, securityToken);
    }

}