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


import org.mule.config.PoolingProfile;

/**
 * Wrapper around {@link org.mule.api.annotations.Connector} annotated class that will infuse it with
 * connection management capabilities.
 *
 * It can receive a {@link PoolingProfile} which is a configuration object used to
 * define the connection pooling parameters.
 *
 * @param <K> Connection key
 * @param <C> Actual connector object that represents a connection
 */
public interface ConnectionManager<K, C> {

    /**
     * Set the connection pooling profile
     *
     * @param value PoolingProfile representing the pooling parameters
     */
    void setConnectionPoolingProfile(PoolingProfile value);

    /**
     * Retrieve the connection pooling profile
     *
     * @return PoolingProfile representing the pooling parameters
     */
    PoolingProfile getConnectionPoolingProfile();

    /**
     * Borrow a connection from the pool
     *
     * @param connectorKey Key used to borrow the connector
     * @return An existing connector, or a newly created one
     * @throws Exception If the connection cannot be created
     */
    C acquireConnection(K connectorKey) throws Exception;

    /**
     * Return a connection to the pool
     *
     * @param connectorKey Key used to borrow the connector
     * @param connector connector to be returned to the pool
     * @throws Exception If the connection cannot be returned
     */
    void releaseConnection(K connectorKey, C connector) throws Exception;

    /**
     * Destroy a connection
     *
     * @param connectorKey Key used to borrow the connector
     * @param connector Connector to be destroyed
     * @throws Exception If the connection could not be destroyed.
     */
    void destroyConnection(K connectorKey, C connector) throws Exception;
}