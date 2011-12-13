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
package org.mule.devkit.dynamic.api.loader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mule.api.Capabilities;
import org.mule.api.Capability;
import org.mule.api.ConnectionManager;
import org.mule.devkit.dynamic.api.helper.Classes;
import org.mule.devkit.dynamic.api.helper.Jars;
import org.mule.devkit.dynamic.api.model.Module;

public class JarLoader extends Loader {

    private static final Logger LOGGER = Logger.getLogger(JarLoader.class.getPackage().getName());

    private static final String MODULE_CLASS_SUFFIX = "Module";
    private static final String CONNECTOR_CLASS_SUFFIX = "Connector";
    private static final String CONNECTION_MANAGER_CLASS_SUFFIX = "ConnectionManager";

    /**
     * @param fileNames
     * @return all potential {@link Module} class name among specified `fileNames`
     */
    protected final List<String> findPotentialModuleClassNames(final List<String> fileNames) {
        final List<String> potentialModuleClassNames = new LinkedList<String>();
        for (final String fileName : fileNames) {
            if (fileName.endsWith(JarLoader.MODULE_CLASS_SUFFIX+".class") ||
                    fileName.endsWith(JarLoader.CONNECTOR_CLASS_SUFFIX+".class")) {
                potentialModuleClassNames.add(fileName);
            }
        }
        return potentialModuleClassNames;
    }

    /**
     * @param fileNames
     * @param classLoader
     * @return first module found among `fileNames`
     */
    protected final Class<?> findModuleClass(final List<String> fileNames, final ClassLoader classLoader) {
        final List<String> potentialModuleClassNames = findPotentialModuleClassNames(fileNames);
        if (potentialModuleClassNames.isEmpty()) {
            throw new IllegalArgumentException("Failed to find potential Module class among <"+fileNames+">");
        }
        for (final String potentialModuleClassName : potentialModuleClassNames) {
            final String className = extractClassName(potentialModuleClassName);
            final Class<?> moduleClass = Classes.loadClass(classLoader, className);
            if (moduleClass == null) {
                throw new IllegalArgumentException("Failed to load <"+className+">");
            }
            if (moduleClass.getAnnotation(org.mule.api.annotations.Module.class) == null && moduleClass.getAnnotation(org.mule.api.annotations.Connector.class) == null) {
                if (JarLoader.LOGGER.isLoggable(Level.WARNING)) {
                    JarLoader.LOGGER.log(Level.WARNING, "Skipping invalid module <{0}>", className);
                }

                continue;
            }

            return moduleClass;
        }
        return null;
    }

    /**
     * @param moduleSubClasses
     * @return {@link Class} among specified classes having biggest number of parent {@link Class}es
     */
    protected final Class<?> findMostSpecificSubClass(final List<Class<?>> moduleSubClasses) {
        return Collections.max(moduleSubClasses, new Comparator<Class<?>>() {
            @Override
            public int compare(final Class<?> class1, final Class<?> class2) {
                return Integer.valueOf(Classes.allSuperClasses(class1).size()).compareTo(Classes.allSuperClasses(class2).size());
            }
        });
    }

    /**
     * @param generatedPackageName
     * @param moduleName
     * @param capabilities
     * @param classLoader
     * @return {@link ConnectionManager} for module if any, null otherwise
     */
    protected final ConnectionManager<?, ?> loadConnectionManager(final String generatedPackageName, final String moduleName, final Capabilities capabilities, final ClassLoader classLoader) {
        if (capabilities.isCapableOf(Capability.CONNECTION_MANAGEMENT_CAPABLE)) {
            final String connectionManagerClassName = generatedPackageName+"."+moduleName+JarLoader.CONNECTION_MANAGER_CLASS_SUFFIX;
            final Class<?> connectionManagerClass = Classes.loadClass(classLoader, connectionManagerClassName);
            if (connectionManagerClass == null) {
                throw new IllegalArgumentException("Failed to load ConnectionManager class <"+connectionManagerClassName+">");
            }
            final ConnectionManager<?, ?> connectionManager = Classes.newInstance(connectionManagerClass);
            if (connectionManager == null) {
                throw new IllegalArgumentException("Failed to instantiate ConnectionManager class <"+connectionManagerClass.getCanonicalName()+">");
            }
            return connectionManager;
        }
        return null;
    }

    /**
     * @param moduleClass
     * @param fileNames
     * @param classLoader
     * @return all {@link Module} sub {@link Class}es
     */
    protected final List<Class<?>> findModuleSubClasses(final Class<?> moduleClass, final List<String> fileNames, final URLClassLoader classLoader) {
        final String moduleClassSimpleName = moduleClass.getSimpleName();
        final List<Class<?>> subClasses = new LinkedList<Class<?>>();
        for (final String fileName : fileNames) {
            if (fileName.contains(moduleClassSimpleName)) {
                final Class<?> clazz = Classes.loadClass(classLoader, extractClassName(fileName));
                if (Classes.allSuperClasses(clazz).contains(moduleClass)) {
                    subClasses.add(clazz);
                }
            }
        }
        return subClasses;
    }

    /**
     * @param urls
     * @return a {@link Module} representation of first module found in specified `urls`
     * @throws IOException 
     */
    public final Module load(final List<URL> urls) throws IOException {
        final URL moduleJar = urls.get(0);
        final List<String> allFileNames = Jars.allFileNames(moduleJar);
        final URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
        final Class<?> moduleClass = findModuleClass(allFileNames, classLoader);
        if (moduleClass == null) {
            throw new IllegalArgumentException("Failed to find Module class in <"+moduleJar+">");
        }

        final List<Class<?>> moduleSubClasses = findModuleSubClasses(moduleClass, allFileNames, classLoader);
        final Class<?> mostSpecificSubClass = findMostSpecificSubClass(moduleSubClasses);
        final Capabilities module = Classes.newInstance(mostSpecificSubClass);
        if (module == null) {
            throw new IllegalArgumentException("Failed to instantiate Module class <"+moduleClass.getSimpleName()+">");
        }
        if (module.isCapableOf(Capability.CONNECTION_MANAGEMENT_CAPABLE)) {
            return load(module, loadConnectionManager(mostSpecificSubClass.getPackage().getName(), moduleClass.getSimpleName(), module, classLoader));
        } else {
            return load(module, null);
        }
    }

}