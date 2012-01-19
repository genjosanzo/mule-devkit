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
package org.mule.devkit.dynamic.api.model;

import org.mule.api.Capabilities;
import org.mule.api.Capability;
import org.mule.api.ConnectionManager;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.devkit.dynamic.api.helper.ConnectionManagers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//TODO Add support for OAuth1 and OAuth2
public class Module {

    /**
     * @see {@link org.mule.api.annotations.param.Default}
     * @see {@link org.mule.api.annotations.param.Optional}
     */
    public static class Parameter {

        private final String name;
        private final Class<?> type;
        private final boolean optional;
        private final String defaultValue;

        public Parameter(final String name, final Class<?> type, final boolean optional, final String defaultValue) {
            if (name == null) {
                throw new IllegalArgumentException("null name");
            }
            if (type == null) {
                throw new IllegalArgumentException("null type");
            }

            this.name = name;
            this.type = type;
            this.optional = optional;
            this.defaultValue = defaultValue;
        }

        public final String getName() {
            return this.name;
        }

        public final Class<?> getType() {
            return this.type;
        }

        public final boolean isOptional() {
            return this.optional;
        }

        public final String getDefaultValue() {
            return this.defaultValue;
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }

        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof Parameter)) {
                return false;
            }

            final Module module = (Module) other;
            return this.name.equals(module.name);
        }

        @Override
        public String toString() {
            return "name: <"+this.name+"> optional: <"+this.optional+">"+(this.defaultValue != null?" default: <"+this.defaultValue+">":"");
        }

    }

    public static class Processor {

        private final String name;
        private final MessageProcessor messageProcessor;
        private final List<Parameter> parameters;
        private final boolean intercepting;

        public Processor(final String name, final MessageProcessor messageProcessor, final List<Parameter> parameters, final boolean intercepting) {
            if (name == null) {
                throw new IllegalArgumentException("null name");
            }
            if (messageProcessor == null) {
                throw new IllegalArgumentException("null messageProcessor");
            }
            if (parameters == null) {
                throw new IllegalArgumentException("null parameters");
            }

            this.name = name;
            this.messageProcessor = messageProcessor;
            this.parameters = parameters;
            this.intercepting = intercepting;
        }

        public final String getName() {
            return this.name;
        }

        public final MessageProcessor getMessageProcessor() {
            return this.messageProcessor;
        }

        public final List<Parameter> getParameters() {
            return this.parameters;
        }

        public final boolean isIntercepting() {
            return this.intercepting;
        }

        @Override
        public String toString() {
            return "name: <"+this.name+"> type: <"+this.messageProcessor.getClass().getName()+"> parameters: <"+this.parameters+"> intercepting: <"+this.intercepting+">";
        }

    }

    public static class Source {

        private final String name;
        private final MessageSource messageSource;
        private final List<Parameter> parameters;

        public Source(final String name, final MessageSource messageSource, final List<Parameter> parameters) {
            if (name == null) {
                throw new IllegalArgumentException("null name");
            }
            if (messageSource == null) {
                throw new IllegalArgumentException("null messageSource");
            }
            if (parameters == null) {
                throw new IllegalArgumentException("null parameters");
            }

            this.name = name;
            this.messageSource = messageSource;
            this.parameters = parameters;
        }

        public final String getName() {
            return this.name;
        }

        public final MessageSource getMessageSource() {
            return this.messageSource;
        }

        public final List<Parameter> getParameters() {
            return this.parameters;
        }

        @Override
        public String toString() {
            return "name: <"+this.name+"> type: <"+this.messageSource.getClass().getName()+"> parameters: <"+this.parameters+">";
        }

    }

    /**
     * @see {@link org.mule.api.annotations.Transformer}
     */
    public static class Transformer {

        private final org.mule.api.transformer.Transformer transformer;
        private final int priorityWeighting;
        private final Class<?>[] sourceTypes;

        public Transformer(final org.mule.api.transformer.Transformer transformer, final int priorityWeighting, final Class<?>[] sourceTypes) {
            if (transformer == null) {
                throw new IllegalArgumentException("null transformer");
            }
            if (sourceTypes == null) {
                throw new IllegalArgumentException("null sourceTypes");
            }

            this.transformer = transformer;
            this.priorityWeighting = priorityWeighting;
            this.sourceTypes = sourceTypes;
        }

        public final org.mule.api.transformer.Transformer getTransformer() {
            return this.transformer;
        }

        public final int getPriorityWeighting() {
            return this.priorityWeighting;
        }

        public final Class<?>[] getSourceTypes() {
            return this.sourceTypes;
        }

        @Override
        public String toString() {
            return "type: <"+this.transformer.getClass().getName()+"> priorityWeighting: <"+this.priorityWeighting+"> sourceTypes: <"+Arrays.toString(this.sourceTypes) +">";
        }

    }

    private final String name;
    private final String minMuleVersion;
    private final Capabilities module;
    private final List<Parameter> parameters;
    private final List<Processor> processors;
    private final List<Source> sources;
    private final List<Transformer> transformers;
    private final ClassLoader classLoader;
    private final ConnectionManager<?, ?> connectionManager;

    public Module(final String name, final String minMuleVersion, final Capabilities module, final List<Parameter> parameters, final List<Processor> processors, final List<Source> sources, final List<Transformer> transformers, final ConnectionManager<?, ?> connectionManager, final ClassLoader classLoader) {
        if (name == null) {
            throw new IllegalArgumentException("null name");
        }
        if (minMuleVersion == null) {
            throw new IllegalArgumentException("null minMuleVersion");
        }
        if (module == null) {
            throw new IllegalArgumentException("null modules");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("null parameters");
        }
        if (processors == null) {
            throw new IllegalArgumentException("null processors");
        }
        if (sources == null) {
            throw new IllegalArgumentException("null sources");
        }
        if (transformers == null) {
            throw new IllegalArgumentException("null transformers");
        }
        if (classLoader == null) {
            throw new IllegalArgumentException("null classLoader");
        }

        this.name = name;
        this.minMuleVersion = minMuleVersion;
        this.module = module;
        this.parameters = Collections.unmodifiableList(new ArrayList<Parameter>(parameters));
        this.processors = Collections.unmodifiableList(new ArrayList<Processor>(processors));
        this.sources = Collections.unmodifiableList(new ArrayList<Source>(sources));
        this.transformers = Collections.unmodifiableList(new ArrayList<Transformer>(transformers));
        this.classLoader = classLoader;
        this.connectionManager = connectionManager;

        if (connectionManager != null) {
            ensureConnectionManagementCapability();
        }
    }

    protected final void ensureCapability(final Capability capability) {
        if (!this.module.isCapableOf(capability)) {
            throw new IllegalArgumentException("Module does not support "+Capability.CONNECTION_MANAGEMENT_CAPABLE);
        }
    }

    protected final void ensureConnectionManagementCapability() {
        ensureCapability(Capability.CONNECTION_MANAGEMENT_CAPABLE);
    }

    public final String getName() {
        return this.name;
    }

    public final String getMinMuleVersion() {
        return this.minMuleVersion;
    }

    public final Capabilities getModule() {
        return this.module;
    }

    public Object getModuleObject() {
        if (getConnectionManager() != null) {
            return getConnectionManager();
        }
        return getModule();
    }

    public final List<Parameter> getParameters() {
        return this.parameters;
    }

    public final List<Processor> getProcessors() {
        return this.processors;
    }

    public final List<Source> getSources() {
        return this.sources;
    }

    public final List<Transformer> getTransformers() {
        return this.transformers;
    }

    public final ConnectionManager<?, ?> getConnectionManager() {
        return this.connectionManager;
    }

    public final ClassLoader getClassLoader() {
        return this.classLoader;
    }

    public final void setUsername(final String username) {
        ensureConnectionManagementCapability();

        ConnectionManagers.setUsername(this.connectionManager, username);
    }

    public final void setPassword(final String password) {
        ensureConnectionManagementCapability();

        ConnectionManagers.setPassword(this.connectionManager, password);
    }

    public final void setSecurityToken(final String securityToken) {
        ensureConnectionManagementCapability();

        ConnectionManagers.setSecurityToken(this.connectionManager, securityToken);
    }

}