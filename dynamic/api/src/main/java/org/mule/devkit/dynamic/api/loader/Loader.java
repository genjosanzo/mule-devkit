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

import org.mule.api.Capabilities;
import org.mule.api.ConnectionManager;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.callback.SourceCallback;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.devkit.dynamic.api.helper.Classes;
import org.mule.devkit.dynamic.api.helper.Reflections;
import org.mule.devkit.dynamic.api.model.Module;
import org.mule.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Loader {

    private static final String MESSAGE_PROCESSOR_CLASS_SUFFIX = "MessageProcessor";
    private static final String MESSAGE_SOURCE_CLASS_SUFFIX = "MessageSource";
    private static final String TRANSFORMER_CLASS_SUFFIX = "Transformer";
    private static final String PARAMETER_TYPE_FIELD_PREFIX = "_";
    private static final String PARAMETER_TYPE_FIELD_SUFFIX = "Type";

    protected final Class<?> findMessageProcessorClass(final Package modulePackage, final String processorName, final ClassLoader classLoader) {
        final String messageProcessorClassName = modulePackage.getName()+"."+StringUtils.capitalize(processorName)+Loader.MESSAGE_PROCESSOR_CLASS_SUFFIX;
        System.out.println(messageProcessorClassName);
        return Classes.loadClass(classLoader, messageProcessorClassName);
    }

    protected final Class<?> findMessageSourceClass(final Package modulePackage, final String sourceName, final ClassLoader classLoader) {
        final String messageSourceName = modulePackage.getName()+"."+StringUtils.capitalize(sourceName)+Loader.MESSAGE_SOURCE_CLASS_SUFFIX;
        return Classes.loadClass(classLoader, messageSourceName);
    }

    protected final Class<?> findTransformerClass(final Package modulePackage, final String transformerName, final ClassLoader classLoader) {
        final String transformerClassName = modulePackage.getName()+"."+StringUtils.capitalize(transformerName)+Loader.TRANSFORMER_CLASS_SUFFIX;
        return Classes.loadClass(classLoader, transformerClassName);
    }

    protected final Object extractAnnotation(final Class<?> moduleClass) {
        final org.mule.api.annotations.Module moduleAnnotation = Classes.getDeclaredAnnotation(moduleClass, org.mule.api.annotations.Module.class);
        if (moduleAnnotation != null) {
            return moduleAnnotation;
        } else {
            return Classes.getDeclaredAnnotation(moduleClass, org.mule.api.annotations.Connector.class);
        }
    }

    public final Module load(final Capabilities module, final ConnectionManager<?, ?> connectionManager) {
        return load(module, connectionManager, module.getClass().getPackage(), module.getClass().getClassLoader());
    }

    public final Module load(final Capabilities module, final ConnectionManager<?, ?> connectionManager, final Package modulePackage, final ClassLoader classLoader) {
        if (module == null) {
            throw new IllegalArgumentException("null module");
        }

        final Class<?> moduleClass = module.getClass();
        final Object annotation = extractAnnotation(moduleClass);
        if (annotation == null) {
            throw new IllegalArgumentException("Failed to find a Module annotation on <"+moduleClass.getCanonicalName()+">");
        }

        final String name = extractAnnotationName(annotation);
        final String minMuleVersion = extractMinMuleVersion(annotation);
        final List<Module.Parameter> parameters = listParameters(moduleClass);
        final List<Module.Processor> processors = listProcessors(modulePackage, moduleClass, classLoader);
        final List<Module.Source> sources = listSources(modulePackage, moduleClass, classLoader);
        final List<Module.Transformer> transformers = listTransformers(modulePackage, moduleClass, classLoader);
        return new Module(name, minMuleVersion, module, parameters, processors, sources, transformers, connectionManager, classLoader);
    }

    protected final String extractClassName(final String name) {
        final String strippedClassName = name.substring(0, name.lastIndexOf("."));
        return strippedClassName.replace('/', '.');
    }

    protected final String extractAnnotationName(final Object annotation) {
        return Reflections.invoke(annotation, "name");
    }

    protected final String extractMinMuleVersion(final Object annotation) {
        return Reflections.invoke(annotation, "minMuleVersion");
    }
    
    protected final List<Module.Parameter> listParameters(final Class<?> moduleClass) {
        final List<Module.Parameter> parameters = new LinkedList<Module.Parameter>();
        for (final Field field : Classes.allDeclaredFields(moduleClass)) {
            if (field.getAnnotation(Configurable.class) != null) {
                final boolean optional = field.getAnnotation(Optional.class) != null;
                final String defaultValue = field.getAnnotation(Default.class) != null ? field.getAnnotation(Default.class).value() : null;
                parameters.add(new Module.Parameter(field.getName(), field.getType(), optional, defaultValue));
            }
        }
        return parameters;
    }

    protected final String extractName(final Object annotation, final Method method) {
        final String annotationName = extractAnnotationName(annotation);
        if (!"".equals(annotationName)) {
            return annotationName;
        }

        return Classes.methodNameToDashBased(method);
    }

    protected final String[] extractMethodParameterNames(final Class<?> generatedClass) {
        final List<String> parameterNames = new LinkedList<String>();
        for (final Field field : generatedClass.getDeclaredFields()) {
            final String fieldName = field.getName();
            if (!(fieldName.startsWith(Loader.PARAMETER_TYPE_FIELD_PREFIX) && fieldName.endsWith(Loader.PARAMETER_TYPE_FIELD_SUFFIX))) {
                continue;
            }

            final String parameterName = StringUtils.uncapitalize(fieldName.substring(Loader.PARAMETER_TYPE_FIELD_PREFIX.length(), fieldName.length()-Loader.PARAMETER_TYPE_FIELD_SUFFIX.length()));
            parameterNames.add(parameterName);
        }
        return parameterNames.toArray(new String[parameterNames.size()]);
    }

    protected final Class<?>[] extractMethodParameterTypes(final Method method) {
        final List<Class<?>> parameterTypes = new LinkedList<Class<?>>();
        for (final Class<?> type : method.getParameterTypes()) {
            //SourceCallback is not a user parameter.
            if (SourceCallback.class.equals(type)) {
                continue;
            }

            parameterTypes.add(type);
        }
        return parameterTypes.toArray(new Class<?>[parameterTypes.size()]);
    }

    protected final List<Module.Parameter> listMethodParameters(final Class<?> moduleClass, final Method method, final Class<?> generatedClass) {
        final List<Module.Parameter> parameters = new LinkedList<Module.Parameter>();
        //Rely on the fact that parameters are added first in generated MessageProcessor/MessageSource.
        //TODO Pretty fragile. Replace with stronger alternative.
        final String[] parameterNames = extractMethodParameterNames(generatedClass);
        final Class<?>[] parameterTypes = extractMethodParameterTypes(method);
        final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterTypes.length; i++) {
            final String name = parameterNames[i];
            final Class<?> type = parameterTypes[i];
            final List<Annotation> annotations = Arrays.asList(parameterAnnotations[i]);
            boolean optional = false;
            String defaultValue = null;
            for (final Annotation annotation : annotations) {
                if (annotation instanceof Optional) {
                    optional = true;
                }
                if (annotation instanceof Default) {
                    defaultValue = Default.class.cast(annotation).value();
                }
            }

            parameters.add(new Module.Parameter(name, type, optional, defaultValue));
        }
        return parameters;
    }

    protected final List<Module.Processor> listProcessors(final Package modulePackage, final Class<?> moduleClass, final ClassLoader classLoader) {
        final List<Module.Processor> processors = new LinkedList<Module.Processor>();
        for (final Method method : moduleClass.getMethods()) {
            final Processor annotation = method.getAnnotation(Processor.class);
            if (annotation != null) {
                final Class<?> messageProcessorClass = findMessageProcessorClass(modulePackage, method.getName(), classLoader);
                if (messageProcessorClass == null) {
                    throw new IllegalArgumentException("Failed to find MessageProcessor class for processor <"+method.getName()+">");
                }
                final MessageProcessor messageProcessor = Classes.newInstance(messageProcessorClass);
                if (messageProcessor == null) {
                    throw new IllegalArgumentException("Failed to instantiate MessageProcessor class <"+messageProcessorClass.getCanonicalName()+">");
                }
                processors.add(new Module.Processor(extractName(annotation, method), messageProcessor, listMethodParameters(moduleClass, method, messageProcessorClass), annotation.intercepting()));
            }
        }
        return processors;
    }

    protected final List<Module.Source> listSources(final Package modulePackage, final Class<?> moduleClass, final ClassLoader classLoader) {
        final List<Module.Source> sources = new LinkedList<Module.Source>();
        for (final Method method : moduleClass.getMethods()) {
            final Source annotation = method.getAnnotation(Source.class);
            if (annotation != null) {
                final Class<?> messageSourceClass = findMessageSourceClass(modulePackage, method.getName(), classLoader);
                if (messageSourceClass == null) {
                    throw new IllegalArgumentException("Failed to find MessageSource class for processor <"+method.getName()+">");
                }
                final MessageSource messageSource = Classes.newInstance(messageSourceClass);
                if (messageSource == null) {
                    throw new IllegalArgumentException("Failed to instantiate MessageSource class <"+messageSourceClass.getCanonicalName()+">");
                }
                sources.add(new Module.Source(extractName(annotation, method), messageSource, listMethodParameters(moduleClass, method, messageSourceClass)));
            }
        }
        return sources;
    }

    protected final List<Module.Transformer> listTransformers(final Package modulePackage, final Class<?> moduleClass, final ClassLoader classLoader) {
        final List<Module.Transformer> transformers = new LinkedList<Module.Transformer>();
        for (final Method method : moduleClass.getMethods()) {
            final Transformer annotation = method.getAnnotation(Transformer.class);
            if (annotation != null) {
                final Class<?> transformerClass = findTransformerClass(modulePackage, method.getName(), classLoader);
                if (transformerClass == null) {
                    throw new IllegalArgumentException("Failed to find Transformer class for processor <"+method.getName()+">");
                }
                final org.mule.api.transformer.Transformer transformer = Classes.newInstance(transformerClass);
                if (transformer == null) {
                    throw new IllegalArgumentException("Failed to instantiate Transformer class <"+transformerClass.getCanonicalName()+">");
                }
                transformers.add(new Module.Transformer(transformer, annotation.priorityWeighting(), annotation.sourceTypes()));
            }
        }
        return transformers;
    }

}