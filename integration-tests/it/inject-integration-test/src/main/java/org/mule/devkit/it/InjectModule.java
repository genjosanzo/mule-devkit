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

import org.mule.api.MuleContext;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.registry.Registry;
import org.mule.api.security.SecurityManager;
import org.mule.api.store.ObjectStoreManager;
import org.mule.util.queue.QueueManager;

import javax.inject.Inject;
import javax.transaction.TransactionManager;

/**
 * Inject Module
 *
 * @author MuleSoft, Inc.
 */
@Module(name = "inject")
public class InjectModule {
    @Inject
    protected MuleContext muleContext;
    @Inject
    protected ObjectStoreManager objectStoreManager;
    @Inject
    protected TransactionManager transactionManager;
    @Inject
    protected QueueManager queueManager;
    @Inject
    protected MuleConfiguration configuration;
    @Inject
    protected LifecycleManager lifecycleManager;
    @Inject
    protected ExpressionManager expressionManager;
    @Inject
    protected EndpointFactory endpointFactory;
    @Inject
    protected SystemExceptionHandler systemExceptionHandler;
    @Inject
    protected SecurityManager securityManager;
    @Inject
    protected Registry registry;
    @Inject
    protected WorkManager workManager;

    /**
     * Verify injections
     */
    @Processor
    public void verify() throws Exception {
        if (muleContext == null ||
                objectStoreManager == null ||
                queueManager == null ||
                transactionManager == null ||
                securityManager == null ||
                configuration == null ||
                lifecycleManager == null ||
                expressionManager == null ||
                endpointFactory == null ||
                systemExceptionHandler == null ||
                registry == null ||
                workManager == null) {
            throw new IllegalStateException("One or more injections are missing");
        }
    }

    public void setMuleContext(MuleContext context) {
        this.muleContext = context;
    }

    public void setObjectStoreManager(ObjectStoreManager objectStoreManager) {
        this.objectStoreManager = objectStoreManager;
    }

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public void setQueueManager(QueueManager queueManager) {
        this.queueManager = queueManager;
    }

    public void setConfiguration(MuleConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setLifecycleManager(LifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }

    public void setExpressionManager(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
    }

    public void setEndpointFactory(EndpointFactory endpointFactory) {
        this.endpointFactory = endpointFactory;
    }

    public void setSystemExceptionHandler(SystemExceptionHandler systemExceptionHandler) {
        this.systemExceptionHandler = systemExceptionHandler;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void setWorkManager(WorkManager workManager) {
        this.workManager = workManager;
    }
}
