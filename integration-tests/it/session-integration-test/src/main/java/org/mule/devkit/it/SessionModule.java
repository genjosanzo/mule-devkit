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

import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;

import java.util.Random;

@Module(name = "session")
public class SessionModule
{
    @Configurable
    @SessionKey
    private String username;

    @Configurable
    @SessionKey
    private String password;

    @Session
    private Integer sessionId;

    @Processor
    @RequiresSession
    public boolean verifySession()
    {
        return sessionId != null;
    }

    @Processor
    @RequiresSession(invalidateOn=InvalidSession.class)
    public void invalidateSessionOnException() throws InvalidSession
    {
        throw new InvalidSession();
    }

    @SessionCreate
    public void createSession() {
        Random generator = new Random();
        this.sessionId = generator.nextInt();
    }

    @SessionDestroy
    public void destroySession() {
        this.sessionId = null;
    }
}
