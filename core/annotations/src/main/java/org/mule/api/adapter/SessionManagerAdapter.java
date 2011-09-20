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
package org.mule.api.adapter;


import org.mule.config.PoolingProfile;

/**
 * Wrapper around {@link org.mule.api.annotations.Module} annotated class that will infuse it with
 * session management capabilities.
 *
 * It can receive a {@link PoolingProfile} which is a configuration object used to
 * define the session pooling parameters for the module.
 *
 * @param <K> Session key
 * @param <S> Actual session object
 */
public interface SessionManagerAdapter<K, S> {

    /**
     * Set the session pooling profile
     *
     * @param value PoolingProfile representing the pooling parameters
     */
    void setSessionPoolingProfile(PoolingProfile value);

    /**
     * Retrieve the session pooling profile
     *
     * @return PoolingProfile representing the pooling parameters
     */
    PoolingProfile getSessionPoolingProfile();

    /**
     * Borrow a session from the pool
     *
     * @param sessionKey Key used to borrow the session
     * @return An existing session, or a newly created one
     * @throws Exception If the session cannot be created
     */
    S borrowSession(K sessionKey) throws Exception;

    /**
     * Return a session to the pool
     *
     * @param sessionKey Key used to borrow the session
     * @param session Session to be returned to the pool
     * @throws Exception If the session cannot be returned
     */
    void returnSession(K sessionKey, S session) throws Exception;

    /**
     * Destroy a session
     *
     * @param sessionKey Key used to borrow the session
     * @param session Session to be destroyed
     * @throws Exception If the session could not be destroyed.
     */
    void destroySession(K sessionKey, S session) throws Exception;
}