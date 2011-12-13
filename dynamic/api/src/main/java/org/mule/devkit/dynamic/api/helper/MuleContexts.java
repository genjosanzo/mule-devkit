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

import java.util.ArrayList;
import java.util.List;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.bootstrap.SimpleRegistryBootstrap;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.devkit.dynamic.api.transformer.StringToURL;

/**
 *Helper methods for {@link MuleContext}.
 */
public final class MuleContexts {

    private MuleContexts() {
    }

    /**
     * @return a default {@link MuleContext}
     * @throws InitialisationException
     * @throws ConfigurationException 
     */
    public static MuleContext defaultMuleContext() throws InitialisationException, ConfigurationException, MuleException {
        final MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        final List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>();
        builders.add(new SimpleConfigurationBuilder(null));
        final MuleContext context = muleContextFactory.createMuleContext(builders, new DefaultMuleContextBuilder());
        //Register all default stuff
        final SimpleRegistryBootstrap bootstrap = new SimpleRegistryBootstrap();
        bootstrap.setMuleContext(context);
        bootstrap.initialise();
        context.getRegistry().registerTransformer(new StringToURL());
        return context;
    }

    public static void inject(final Object object, final MuleContext context) {
        if (object instanceof MuleContextAware) {
            MuleContextAware.class.cast(object).setMuleContext(context);
        }
    }

}