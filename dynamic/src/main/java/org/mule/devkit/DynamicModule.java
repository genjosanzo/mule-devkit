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
package org.mule.devkit;

import org.mule.api.MuleContext;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.param.Optional;
import org.mule.api.context.MuleContextAware;
import org.mule.devkit.model.meta.InstantiationException;
import org.mule.devkit.model.meta.MetaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;

/**
 * This module allows the dynamic creation, configuration and invocation
 * of mule modules developed using the Mule DevKit.
 *
 * @author MuleSoft, Inc.
 */
@Module(name = "dynamic", schemaVersion = "3.0")
public class DynamicModule implements MuleContextAware {
    private static Logger logger = LoggerFactory.getLogger(DynamicModule.class);

    private MetaModel metaModel;

    /**
     * Mule Context
     */
    private MuleContext muleContext;

    /**
     * Mule Meta Module
     */
    private MetaModel muleMetaModel;

    /**
     * Initialise the module
     *
     * @throws Exception If it cannot be initialised
     */
    @PostConstruct
    public void initialise() throws Exception {
        if (this.metaModel != null) {
            this.metaModel.initialise();
        }
    }

    /**
     * Dispose the module
     *
     * @throws Exception If it cannot be initialised
     */
    @PreDestroy
    public void dispose() {
        if (this.metaModel != null) {
            this.metaModel.dispose();
        }
    }

    /**
     * Create and configure a new module without attributes
     *
     * @param namespace The namespace of the module to dynamically configure
     * @param name      The name of the module
     * @throws org.mule.devkit.model.meta.InstantiationException
     *          if the module cannot be created
     */
    public void create(String namespace, String name) throws InstantiationException {
        create(namespace, name, null);
    }

    /**
     * Invokes the operation specified with operationName in the module specified with moduleName and
     * with attributes as its arguments.
     *
     * @param moduleName    The name of the module instance
     * @param operationName The name of the operation to invoke
     * @param arguments     A map representing the arguments to the invocation.
     * @throws InvocationTargetException If the operation does not exists, a module with the specified name
     *                                   cannot be found in the registry.
     */
    public void invoke(String moduleName, String operationName, Map<String, Object> arguments) throws Exception {
        //MuleSession muleSession = new DefaultMuleSession(this.muleContext);
        //MuleMessage muleMessage = new DefaultMuleMessage(arguments, this.muleContext);
        //MuleEvent muleEvent = new DefaultMuleEvent(muleMessage, MessageExchangePattern.REQUEST_RESPONSE, muleSession);


    }

    /**
     * Create and configure a new module
     * <p/>
     * {@sample.xml ../../../doc/mule-module-dynamic.xml.sample dynamic:create}
     *
     * @param namespace    The namespace of the module to dynamically configure
     * @param instanceName The name of the module
     * @param attributes   Configuration attributes
     * @throws org.mule.devkit.model.meta.InstantiationException
     *          if the module cannot be created
     */
    @Processor
    public void create(String namespace, String instanceName, @Optional Map<String, Object> attributes) throws InstantiationException {
        this.metaModel.getModule(namespace).newInstance(instanceName, attributes);
    }

    /**
     * Set the Mule context
     *
     * @param context Mule context to set
     */
    public void setMuleContext(MuleContext context) {
        this.muleContext = context;

        this.metaModel = new MetaModel(this.muleContext);
    }
}
