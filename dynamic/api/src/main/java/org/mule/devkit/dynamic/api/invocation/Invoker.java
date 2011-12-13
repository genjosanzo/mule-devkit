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

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.devkit.dynamic.api.helper.LifeCycles;
import org.mule.devkit.dynamic.api.helper.MuleEvents;
import org.mule.devkit.dynamic.api.helper.Reflections;

public class Invoker implements Disposable {

    private final MuleContext context;
    private final MessageProcessor messageProcessor;
    private final int retryMax;
    private static final String RETRY_MAX_FIELD_NAME = "retryMax";

    public Invoker(final MuleContext context, final MessageProcessor messageProcessor, final int retryMax) {
        if (context == null) {
            throw new IllegalArgumentException("null context");
        }
        if (messageProcessor == null) {
            throw new IllegalArgumentException("null messageProcessor");
        }

        this.context = context;
        this.messageProcessor = messageProcessor;
        this.retryMax = retryMax;

        try {
            initialise();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initialise() throws InitialisationException, MuleException {
        Reflections.set(this.messageProcessor, Invoker.RETRY_MAX_FIELD_NAME, this.retryMax);

        MuleContextAware.class.cast(this.messageProcessor).setMuleContext(this.context);
        LifeCycles.initialise(this.messageProcessor);
        LifeCycles.start(this.messageProcessor);
    }

    public final <T> T invoke(final Map<String, Object> processorParameters) throws MuleException {
        if (processorParameters == null) {
            throw new IllegalArgumentException("null processorParameters");
        }

        //Set all parameter values on the MessageProcessor.
        Reflections.set(this.messageProcessor, processorParameters);

        final MuleEvent muleEvent = MuleEvents.defaultMuleEvent(processorParameters, this.context);
        return (T) this.messageProcessor.process(muleEvent).getMessage().getPayload();
    }

    @Override
    public final void dispose() {
        try {
            LifeCycles.stop(this.messageProcessor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        LifeCycles.dispose(this.messageProcessor);
    }

}