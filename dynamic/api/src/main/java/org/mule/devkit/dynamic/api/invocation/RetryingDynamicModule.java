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
package org.mule.devkit.dynamic.api.invocation;

import java.util.Map;

import java.util.concurrent.atomic.AtomicReference;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.retry.policies.AbstractPolicyTemplate;
import org.mule.devkit.dynamic.api.model.Module;

/**
 * {@link DynamicModule} specialization relying on a {@link RetryPolicyTemplate} to implement retry capacity.
 */
public class RetryingDynamicModule extends DynamicModule {

    private final AbstractPolicyTemplate retryPolicyTemplate;

    public RetryingDynamicModule(final Module module, final Map<String, Object> overriddenParameters, final AbstractPolicyTemplate retryPolicyTemplate) {
        this(module, overriddenParameters, DynamicModule.DEFAULT_RETRY_MAX, retryPolicyTemplate);
    }

    public RetryingDynamicModule(final Module module, final Map<String, Object> overriddenParameters, final int retryMax, final AbstractPolicyTemplate retryPolicyTemplate) {
        super(module, overriddenParameters, retryMax);

        if (retryPolicyTemplate == null) {
            throw new IllegalArgumentException("null retryPolicyTemplate");
        }
        retryPolicyTemplate.setMuleContext(getMuleContext());
        this.retryPolicyTemplate = retryPolicyTemplate;
    }

    @Override
    protected <T> T invoke(final MessageProcessor messageProcessor, final Map<String, Object> parameters) throws InitialisationException, MuleException {
        //Force underlying Invoker initialsation. Ensure no InitialisationException won't be thrown in retry loop.
        getInvoker(messageProcessor);

        try {
            final AtomicReference<T> result = new AtomicReference<T>();
            final RetryContext retryContext = this.retryPolicyTemplate.execute(new RetryCallback() {
                @Override
                public void doWork(final RetryContext context) throws Exception {
                    result.set((T) RetryingDynamicModule.super.invoke(messageProcessor, parameters));
                }
                @Override
                public String getWorkDescription() {
                    return "RetryingDynamicModule";
                }
            }, null);
            if (!retryContext.isOk()) {
                throw new RuntimeException(retryContext.getLastFailure());
            }
            return result.get();
        } catch (InitialisationException e) {
            throw e;
        } catch (MuleException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}