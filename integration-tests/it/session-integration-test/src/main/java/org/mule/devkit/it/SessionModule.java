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
import org.mule.api.annotations.Source;
import org.mule.api.annotations.callback.SourceCallback;
import org.mule.api.annotations.param.Session;
import org.mule.api.annotations.param.SessionKey;
import org.mule.api.annotations.session.InvalidateSessionOn;
import org.mule.api.annotations.session.SessionCreate;
import org.mule.api.annotations.session.SessionDestroy;

import java.util.Random;

@Module(name = "session")
public class SessionModule {
    @Source
    public void stream(@Session CustomSession session, SourceCallback callback) {
        for( int i = 0; i < 10; i++ ) {
            try {
                callback.process(Integer.valueOf(i));
            } catch (Exception e) {

            }
        }
    }

    @Processor
    public boolean verifySession(@Session CustomSession session) {
        return session.getSessionId() != null;
    }

    @Processor
    public String getUsername(@Session CustomSession session) {
        return session.getUsername();
    }

    @Processor
    public String getPassword(@Session CustomSession session) {
        return session.getPassword();
    }

    @Processor
    public Integer getSessionId(@Session CustomSession session) {
        return session.getSessionId();
    }

    @Processor
    @InvalidateSessionOn(exception = InvalidSession.class)
    public void invalidateSessionOnException(@Session CustomSession session) throws InvalidSession {
        throw new InvalidSession();
    }

    @SessionCreate
    public CustomSession createSession(@SessionKey String username, String password) {
        return new CustomSession(username, password);
    }

    @SessionDestroy
    public void destroySession(@Session CustomSession session) {
        session.setSessionId(null);
    }

    public class CustomSession {
        private Integer sessionId;
        private String username;
        private String password;

        public CustomSession(String username, String password) {
            this.username = username;
            this.password = password;
            Random generator = new Random();
            this.sessionId = generator.nextInt();
        }

        public Integer getSessionId() {
            return sessionId;
        }

        public void setSessionId(Integer sessionId) {
            this.sessionId = sessionId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
