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
package org.mule.devkit.model.meta;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The meta model is used to describe a Mule module internals. It is leveraged by the dynamic module for the
 * reflection capabilities.
 */
public class MetaModel implements Initialisable, Disposable {
    private static Logger logger = LoggerFactory.getLogger(MetaModel.class);
    private static final String SPRING_SCHEMAS_LOCATION = "META-INF/spring.schemas";

    private Map<String, MetaModule> modules;
    private MuleContext muleContext;
    private Registry metaRegistry;

    public MetaModel(MuleContext muleContext) {
        this.modules = new HashMap<String, MetaModule>();
        this.muleContext = muleContext;
    }

    @Override
    protected void finalize() {
        if (this.muleContext != null) {
            this.muleContext.removeRegistry(this.metaRegistry);
        }
    }

    /**
     * Add a module for this model
     *
     * @param module Module to be added
     */
    public void addModule(MetaModule module) {
        this.modules.put(module.getNamespace(), module);
    }

    /**
     * Retrieve all modules available on the classpath
     * 
     * @return An iterator of {@link MetaModule}
     */
    public Iterator<MetaModule> getAll() {
        return modules.values().iterator();        
    }

    /**
     * Remove a module for this model
     *
     * @param name The name of the module to be removed
     */
    public void removeModule(String name) {
        this.modules.remove(name);
    }

    /**
     * Retrieve a module by its namespace
     * 
     * @param namespace The namespace of the module
     * @return A {@link MetaModule}
     */
    public MetaModule getModule(String namespace) {
        if (!this.modules.containsKey(namespace)) {
            throw new IllegalArgumentException("Unable to find a module for namespace " + namespace);
        }
        return this.modules.get(namespace);
    }

    public Registry getMetaRegistry() {
        return metaRegistry;
    }

    /**
     * Load the specified class mappings lazily.
     */
    private static synchronized Map<String, String> getSchemas(ClassLoader classLoader) {
        if (logger.isDebugEnabled()) {
            logger.debug("Loading schema mappings from [" + SPRING_SCHEMAS_LOCATION + "]");
        }
        try {
            Properties mappings =
                    PropertiesLoaderUtils.loadAllProperties(SPRING_SCHEMAS_LOCATION, classLoader);
            if (logger.isDebugEnabled()) {
                logger.debug("Loaded schema mappings: " + mappings);
            }
            Map<String, String> schemaMappings = new ConcurrentHashMap<String, String>();
            CollectionUtils.mergePropertiesIntoMap(mappings, schemaMappings);
            return schemaMappings;
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Unable to load schema mappings from location [" + SPRING_SCHEMAS_LOCATION + "]", ex);
        }
    }

    public synchronized void dispose() {
        if( this.metaRegistry != null ) {
            this.metaRegistry.dispose();
            this.muleContext.removeRegistry(this.metaRegistry);
        }
        this.metaRegistry = null;
    }

    public synchronized void initialise() throws InitialisationException {
        Map<String, String> schemas = getSchemas(muleContext.getExecutionClassLoader());
        for (String name : schemas.keySet()) {
            Resource resource = new ClassPathResource(schemas.get(name), muleContext.getExecutionClassLoader());
            MetaModule metaModule = null;
            try {
                metaModule = MetaModule.parseSchema(this, resource.getInputStream());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
            if (metaModule != null) {
                addModule(metaModule);
            }
        }

        this.metaRegistry = new MetaRegistry(muleContext);
        this.muleContext.addRegistry(this.metaRegistry);
        this.metaRegistry.initialise();
    }
}
