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
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This module allows the dynamic creation, configuration and invocation
 * of mule modules developed using the Mule DevKit.
 *
 * @author MuleSoft, Inc.
 */
@Module(name = "dynamic", schemaVersion = "3.0")
public class DynamicModule implements MuleContextAware {
    private static Logger logger = LoggerFactory.getLogger(DynamicModule.class);
    public static final String SPRING_CLASSES_LOCATION = "META-INF/spring.classes";

    /**
     * Mule Context
     */
    private MuleContext muleContext;

    /**
     * Dynamic Spring Registry
     */
    private Registry dynamicSpringRegistry;

    /**
     * Namespace to class maps
     */
    private volatile Map<String, String> classMappings;

    /**
     * Initialise the module
     *
     * @throws Exception If it cannot be initialised
     */
    @PostConstruct
    public void initialise() throws Exception {
        this.classMappings = getClassMappings();

        dynamicSpringRegistry = new DynamicSpringRegistry(muleContext);

        muleContext.addRegistry(dynamicSpringRegistry);
        dynamicSpringRegistry.initialise();
    }

    /**
     * Clean up internal data
     */
    @PreDestroy
    public void dispose() {
        if (dynamicSpringRegistry != null) {
            muleContext.removeRegistry(dynamicSpringRegistry);
            dynamicSpringRegistry = null;
        }
        if (this.classMappings != null) {
            this.classMappings = null;
        }
    }

    /**
     * Create and configure a new module without attributes
     *
     * @param namespace The namespace of the module to dynamically configure
     * @param name      The name of the module
     * @throws CannotCreateException if the module cannot be created
     */
    public void create(String namespace, String name) throws CannotCreateException {
        create(namespace, name, null);
    }

    /**
     * Create and configure a new module
     * <p/>
     * {@sample.xml ../../../doc/mule-module-dynamic.xml.sample dynamic:create}
     *
     * @param namespace  The namespace of the module to dynamically configure
     * @param name       The name of the module
     * @param attributes Configuration attributes
     * @throws CannotCreateException if the module cannot be created
     */
    @Processor
    public void create(String namespace, String name, @Optional Map<String, Object> attributes) throws CannotCreateException {
        if (!this.classMappings.containsKey(namespace)) {
            throw new CannotCreateException(namespace, "Unable to find a module with namespace [" + namespace + "] that can be dynamically configured.");
        }

        String className = this.classMappings.get(namespace);
        try {
            BeanDefinitionBuilder dynamicModuleBuilder = BeanDefinitionBuilder.rootBeanDefinition(className);
            if (Initialisable.class.isAssignableFrom(Class.forName(className))) {
                dynamicModuleBuilder.setInitMethodName(Initialisable.PHASE_NAME);
            }
            if (Disposable.class.isAssignableFrom(Class.forName(className))) {
                dynamicModuleBuilder.setDestroyMethodName(Disposable.PHASE_NAME);
            }
            for (String attributeName : attributes.keySet()) {
                dynamicModuleBuilder.addPropertyValue(attributeName, attributes.get(attributeName));
            }

            this.dynamicSpringRegistry.registerObject(name, dynamicModuleBuilder.getBeanDefinition());
        } catch (ClassNotFoundException e) {
            throw new CannotCreateException(namespace, "Cannot find class " + className + ".", e);
        } catch (RegistrationException e) {
            throw new CannotCreateException(namespace, "Unable to create module", e);
        }
    }

    /**
     * Destroy a previously created module
     *
     * {@sample.xml ../../../doc/mule-module-dynamic.xml.sample dynamic:destroy}
     *
     * @param namespace The namespace of the module to destroy
     * @param name The name previously used to destroy it
     * @throws CannotDestroyException if the module cannot be destroyed
     */
    @Processor
    public void destroy(String namespace, String name) throws CannotDestroyException {
        try {
            this.dynamicSpringRegistry.unregisterObject(name);
        } catch (RegistrationException e) {
            throw new CannotDestroyException(namespace, "Unable to destroy module", e);
        }
    }

    /**
     * Load the specified class mappings lazily.
     */
    private synchronized Map<String, String> getClassMappings() {
        if (logger.isDebugEnabled()) {
            logger.debug("Loading class mappings from [" + SPRING_CLASSES_LOCATION + "]");
        }
        try {
            Properties mappings =
                    PropertiesLoaderUtils.loadAllProperties(SPRING_CLASSES_LOCATION, muleContext.getExecutionClassLoader());
            if (logger.isDebugEnabled()) {
                logger.debug("Loaded class mappings: " + mappings);
            }
            Map<String, String> schemaMappings = new ConcurrentHashMap<String, String>();
            CollectionUtils.mergePropertiesIntoMap(mappings, schemaMappings);
            return schemaMappings;
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Unable to load class mappings from location [" + SPRING_CLASSES_LOCATION + "]", ex);
        }
    }

    /**
     * Set the Mule context
     *
     * @param context Mule context to set
     */
    public void setMuleContext(MuleContext context) {
        this.muleContext = context;
    }
}
