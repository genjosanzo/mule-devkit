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

import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.context.notification.CustomNotification;

/**
 * Custom notification fired when there is a change in connectivity
 */
public class ConnectivityNotification extends CustomNotification implements BlockingServerEvent {
    public static final int NO_CONNECTIVITY_EVENT_ACTION = 250001;

    /**
     * Connection exception
     */
    private ConnectionException connectionException;

    static
    {
        registerAction("no connectivity", ConnectivityNotification.NO_CONNECTIVITY_EVENT_ACTION);
    }

    public ConnectivityNotification(final MuleEvent source, ConnectionException exception) {
        super(source, ConnectivityNotification.NO_CONNECTIVITY_EVENT_ACTION);

        this.connectionException = exception;
    }

    @Override
    public final MuleEvent getSource()
    {
        return (MuleEvent) super.getSource();
    }

    public ConnectionException getConnectionException() {
        return connectionException;
    }
}
