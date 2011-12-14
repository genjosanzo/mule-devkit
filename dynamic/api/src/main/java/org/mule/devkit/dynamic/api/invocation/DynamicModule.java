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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mule.api.Capabilities;
import org.mule.api.Capability;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.source.MessageSource;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.devkit.dynamic.api.helper.LifeCycles;
import org.mule.devkit.dynamic.api.helper.MuleContexts;
import org.mule.devkit.dynamic.api.helper.Parameters;
import org.mule.devkit.dynamic.api.helper.Reflections;
import org.mule.devkit.dynamic.api.model.Module;
import org.mule.transformer.types.DataTypeFactory;

public class DynamicModule implements Disposable {

    /**
     * Encapsulate logic dealing with event received from a {@link org.mule.api.annotations.Source}.
     * @param <T> 
     */
    public interface Listener<T> {

        /**
         * Called every time associated {@link org.mule.api.annotations.Source} fires an event.
         * @param event 
         */
        void onEvent(T event);

    }

    private static final Logger LOGGER = Logger.getLogger(DynamicModule.class.getPackage().getName());

    private final MuleContext context;
    private final Module module;
    private static final String MODULE_OBJECT_REGISTRY_KEY = "moduleObject";
    private final int retryMax;
    protected static final int DEFAULT_RETRY_MAX = 5;
    private final Map<String, Object> parameters;
    private final Map<Class<?>, Invoker> invokerCache = new HashMap<Class<?>, Invoker>();
    private final Map<Class<?>, Registrar> registrarCache = new HashMap<Class<?>, Registrar>();

    public DynamicModule(final Module module) {
        this(module, Collections.<String, Object>emptyMap());
    }

    public DynamicModule(final Module module, final Map<String, Object> overriddenParameters) {
        this(module, overriddenParameters, DynamicModule.DEFAULT_RETRY_MAX);
    }

    public DynamicModule(final Module module, final Map<String, Object> overriddenParameters, final int retryMax) {
        if (module == null) {
            throw new IllegalArgumentException("null module");
        }
        if (overriddenParameters == null) {
            throw new IllegalArgumentException("null overriddenParameters");
        }
        if (retryMax <= 0) {
            throw new IllegalArgumentException("retryMax must be > 0");
        }

        validateParameterTypeCorrectness(module.getParameters(), overriddenParameters);
        ensureNoMissingParameters(module.getParameters(), overriddenParameters);

        try {
            this.context = MuleContexts.defaultMuleContext();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.module = module;
        this.retryMax = retryMax;
        this.parameters = allParameters(module.getParameters(), overriddenParameters);
        
        try {
            initialise();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected final MuleContext getMuleContext() {
        return this.context;
    }

    private void initialise() throws InitialisationException, RegistrationException, MuleException {
        final Capabilities capabilities = this.module.getModule();
        if (capabilities.isCapableOf(Capability.LIFECYCLE_CAPABLE)) {
            LifeCycles.initialise(capabilities);
            LifeCycles.start(capabilities);
        }
        if (this.module.getConnectionManager() != null) {
            LifeCycles.initialise(this.module.getConnectionManager());
        }

        //Apply parameters to the ModuleObject.
        final Object moduleObject = this.module.getModuleObject();
        for (final Map.Entry<String, Object> entry : this.parameters.entrySet()) {
            Reflections.set(moduleObject, entry.getKey(), entry.getValue());
        }

        this.context.getRegistry().registerObject(DynamicModule.MODULE_OBJECT_REGISTRY_KEY, moduleObject);
    }

    protected final void validateParameterTypeCorrectness(final List<Module.Parameter> defaultParameters, final Map<String, Object> overriddenParameters) {
        final List<String> incorrectParameterTypes = new LinkedList<String>();
        //Ensure all overridden parameter types are correct.
        for (final Map.Entry<String, Object> entry : overriddenParameters.entrySet()) {
            final String parameterName = entry.getKey();
            final Module.Parameter parameter = Parameters.getParameter(defaultParameters, parameterName);
            if (parameter == null) {
                continue;
            }

            final Class<?> expectedType = Reflections.asType(parameter.getType());
            final Class<?> type = entry.getValue().getClass();
            if (!expectedType.isAssignableFrom(type)) {
                final StringBuilder details = new StringBuilder(parameterName);
                details.append("(type ").append(type.getCanonicalName()).append(" is not assignable to ").append(expectedType.getCanonicalName()).append(")");
                incorrectParameterTypes.add(details.toString());
            }
        }
        if (!incorrectParameterTypes.isEmpty()) {
            final String terminaison = incorrectParameterTypes.size()>1?"s":"";
            throw new IllegalArgumentException("Incorrect type"+terminaison+" for parameter"+terminaison+" <"+incorrectParameterTypes+">");
        }
    }

    protected final void ensureNoMissingParameters(final List<Module.Parameter> defaultParameters, final Map<String, Object> overriddenParameters) {
        final List<String> missingMandatoryParameters = new LinkedList<String>();
        //Ensure all mandatory parameter values are provided.
        for (final Module.Parameter parameter : defaultParameters) {
            if (!parameter.isOptional() && parameter.getDefaultValue() == null
                && !overriddenParameters.containsKey(parameter.getName())) {
                missingMandatoryParameters.add(parameter.getName());
            }
        }
        if (!missingMandatoryParameters.isEmpty()) {
            final String terminaison = missingMandatoryParameters.size()>1?"s":"";
            throw new IllegalArgumentException("Value"+terminaison+" for parameter"+terminaison+" <"+missingMandatoryParameters+"> must be provided");
        }
    }

    /**
     * Aggregate all parameters: default and overridden ones.
     * Overridden parameters take precedence over default ones.
     * @return 
     */
    protected final Map<String, Object> allParameters(final List<Module.Parameter> defaultParameters, final Map<String, Object> overriddenParameters) {
        final Map<String, Object> allParameters = new HashMap<String, Object>();
        final Set<String> defaultParameterNames = new HashSet<String>();
        for (final Module.Parameter parameter : defaultParameters) {
            //Only add default values
            if (parameter.getDefaultValue() != null) {
                try {
                    final Transformer transformer = this.context.getRegistry().lookupTransformer(DataType.STRING_DATA_TYPE, DataTypeFactory.create(parameter.getType()));
                    allParameters.put(parameter.getName(), transformer.transform(parameter.getDefaultValue()));
                } catch (TransformerException e) {
                    throw new RuntimeException("Failed to transform <"+parameter.getDefaultValue()+">", e);
                }
                
            }
            defaultParameterNames.add(parameter.getName());
        }
        for (final Map.Entry<String, Object> entry : overriddenParameters.entrySet()) {
            //Only add existing parameters
            final String parameterName = entry.getKey();
            if (!defaultParameterNames.contains(parameterName)) {
                if (DynamicModule.LOGGER.isLoggable(Level.WARNING)) {
                    DynamicModule.LOGGER.log(Level.WARNING, "Value has been provided for unknown parameter <{0}>; it will be ignored", parameterName);
                }

                continue;
            }

            allParameters.put(parameterName, entry.getValue());
        }
        return allParameters;
    }

    /**
     * @param processorName
     * @return {@link Module.Processor} extracted from {@link Module$Processor}with specified name, null otherwise
     */
    protected final Module.Processor findProcessor(final String processorName) {
        for (final Module.Processor processor : this.module.getProcessors()) {
            if (processorName.equals(processor.getName())) {
                return processor;
            }
        }
        return null;
    }

    /**
     * @param messageProcessor
     * @return an {@link Invoker} for {@link MessageProcessor}. Creates it if needed.
     * @throws InitialisationException
     * @throws MuleException
     * @see #createInvoker(org.mule.api.processor.MessageProcessor) 
     */
    protected synchronized final Invoker getInvoker(final MessageProcessor messageProcessor) throws InitialisationException, MuleException {
        final Class<?> key = messageProcessor.getClass();
        if (this.invokerCache.containsKey(key)) {
            return this.invokerCache.get(key);
        }

        final Invoker invoker = new Invoker(this.context, messageProcessor, this.retryMax);
        this.invokerCache.put(key, invoker);
        return invoker;
    }

    /**
     * Invoke `processorName` with provided `overriddenParameters`. Non overridden parameters will rely on default value.
     * @param <T>
     * @param processorName
     * @param overriddenParameters
     * @return
     * @throws InitialisationException
     * @throws MuleException 
     */
    public final <T> T invoke(final String processorName, final Map<String, Object> overriddenParameters) throws InitialisationException, MuleException {
        if (processorName == null) {
            throw new IllegalArgumentException("The processor name cannot be null");
        }
        if (overriddenParameters == null) {
            throw new IllegalArgumentException("The overridenParameters cannot be null");
        }

        final Module.Processor processor = findProcessor(processorName);
        if (processor == null) {
            throw new IllegalArgumentException("Cannot find a Processor named <"+processorName+">");
        }

        validateParameterTypeCorrectness(processor.getParameters(), overriddenParameters);
        ensureNoMissingParameters(processor.getParameters(), overriddenParameters);

        return this.<T>invoke(processor.getMessageProcessor(), allParameters(processor.getParameters(), overriddenParameters));
    }

    protected <T> T invoke(final MessageProcessor messageProcessor, final Map<String, Object> parameters) throws InitialisationException, MuleException {
        return getInvoker(messageProcessor).<T>invoke(parameters);
    }

    /**
     * @param sourceName
     * @return {@link Module.Source} extracted from {@link Module$Source}with specified name, null otherwise
     */
    protected final Module.Source findSource(final String sourceName) {
        for (final Module.Source source : this.module.getSources()) {
            if (sourceName.equals(source.getName())) {
                return source;
            }
        }
        return null;
    }

    /**
     * @param messageProcessor
     * @return a cached {@link Invoker} for {@link MessageProcessor}.
     * @throws InitialisationException
     * @throws MuleException
     * @see #createInvoker(org.mule.api.processor.MessageProcessor) 
     */
    protected synchronized final Registrar getRegistrar(final MessageSource messageSource) throws InitialisationException, MuleException {
        final Class<?> key = messageSource.getClass();
        return this.registrarCache.get(key);
    }

    /**
     * @param messageSource
     * @return a new cached {@link Regsitrar}
     */
    protected synchronized final Registrar createAndCacheRegistrar(final MessageSource messageSource) {
        final Class<?> key = messageSource.getClass();
        final Registrar registrar = new Registrar(this.context, messageSource);
        this.registrarCache.put(key, registrar);
        return registrar;
    }

    /**
     * Subscribe {@link Listener} to `sourceName` {@link Source} with `overriddenParameters`.
     * @param sourceName
     * @param overriddenParameters
     * @param listener
     * @throws InitialisationException
     * @throws MuleException 
     */
    public synchronized final void subscribe(final String sourceName, final Map<String, Object> overriddenParameters, final Listener listener) throws InitialisationException, MuleException {
        if (sourceName == null) {
            throw new IllegalArgumentException("null sourceName");
        }
        if (overriddenParameters == null) {
            throw new IllegalArgumentException("null overriddenParameters");
        }

        final Module.Source source = findSource(sourceName);
        if (source == null) {
            throw new IllegalArgumentException("Cannot find a Source named <"+sourceName+">");
        }

        validateParameterTypeCorrectness(source.getParameters(), overriddenParameters);
        ensureNoMissingParameters(source.getParameters(), overriddenParameters);

        final Registrar registrar = getRegistrar(source.getMessageSource());
        if (registrar != null) {
            throw new IllegalStateException("Source <"+sourceName+"> is already subscribed");
        }
        createAndCacheRegistrar(source.getMessageSource()).start(allParameters(source.getParameters(), overriddenParameters), listener);
    }

    /**
     * Unsubscribe {@link Listener} previously registered to `sourceName` {@link Source}.
     * @param sourceName
     * @throws InitialisationException
     * @throws MuleException 
     */
    public final void unsubscribe(final String sourceName) throws MuleException {
        if (sourceName == null) {
            throw new IllegalArgumentException("null sourceName");
        }

        final Module.Source source = findSource(sourceName);
        if (source == null) {
            throw new IllegalArgumentException("Cannot find a Source named <"+sourceName+">");
        }

        final Registrar registrar = getRegistrar(source.getMessageSource());
        if (registrar == null) {
            throw new IllegalStateException("Source <"+sourceName+"> is not subscribed");
        }
        registrar.stop();
    }

    /**
     * Cleanup all internal resources:
     * * call {@link Invoker#dispose()} for all cached {@link Invoker}
     * * call {@link Registrar#stop()} for all cached {@link Registrar}
     * * call {@link MuleCOntext#dispose()}
     */
    @Override
    public final void dispose() {
        for (final Invoker invoker : this.invokerCache.values()) {
            invoker.dispose();
        }
        for (final Registrar registrar : this.registrarCache.values()) {
            try {
                registrar.stop();
            } catch (MuleException e) {
                if (DynamicModule.LOGGER.isLoggable(Level.WARNING)) {
                    DynamicModule.LOGGER.log(Level.WARNING, "Got exception while closing <"+registrar+">", e);
                }
            }
        }
        this.context.dispose();
    }

}