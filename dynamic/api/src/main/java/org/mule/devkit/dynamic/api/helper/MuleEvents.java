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

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.session.DefaultMuleSession;

/**
 * Helper methods for {@link MuleEvent}.
 */
public class MuleEvents {

    /**
     * @param message
     * @param context
     * @return a default {@link MuleEvent}
     */
    public static MuleEvent defaultMuleEvent(final Object message, final MuleContext context) {
        return new DefaultMuleEvent(new DefaultMuleMessage(message, context), MessageExchangePattern.REQUEST_RESPONSE, new DefaultMuleSession(context));
    }

}