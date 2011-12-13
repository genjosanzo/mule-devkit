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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Helper methods for {@link Class}.
 */
public final class Classes {

    private Classes() {
    }

    /**
     * @param clazz
     * @return all subclasses of specified {@link Class}
     */
    public static List<Class<?>> allSuperClasses(final Class<?> clazz) {
        final List<Class<?>> allSuperClasses = new LinkedList<Class<?>>();
        Class<?> superClass = clazz.getSuperclass();
        while (superClass != null) {
            allSuperClasses.add(superClass);
            superClass = superClass.getSuperclass();
        }
        return allSuperClasses;
    }

    /**
     * @param clazz
     * @return all declared {@link Field} of specified {@link Class} and all super {@link Class}es
     */
    public static List<Field> allDeclaredFields(final Class<?> clazz) {
        final List<Field> allDeclaredFields = new LinkedList<Field>();
        for (final Class<?> superClazz : allSuperClasses(clazz)) {
            allDeclaredFields.addAll(Arrays.asList(superClazz.getDeclaredFields()));
        }
        return allDeclaredFields;
    }

    /**
     * @param classLoader
     * @param name
     * @return loaded {@link Class} if any; null otherwise
     */
    public static Class<?> loadClass(final ClassLoader classLoader, final String name) {
        try {
            return classLoader.loadClass(name);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param <T>
     * @param clazz
     * @return new {@link Class} instance; null if instantiation fails
     */
    public static <T> T newInstance(final Class<?> clazz) {
        try {
            return (T) clazz.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param method
     * @return dash-based representation of a {@link Method#getName()}. e.g. getMyProperty => get-my-property
     */
    public static String methodNameToDashBased(final Method method) {
        final String methodName = method.getName();
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < methodName.length(); i++) {
            final char character = methodName.charAt(i);
            if (Character.isUpperCase(character)) {
                builder.append("-").append(Character.toLowerCase(character));
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    /**
     * @param <T>
     * @param clazz
     * @param annotationClass
     * @return annotation with specified {@link Class}, if any. Inspect parent {@link Class}es.
     */
    public static <T extends Annotation> T getDeclaredAnnotation(final Class<?> clazz, final Class<T> annotationClass) {
        for (final Class<?> superClass : allSuperClasses(clazz)) {
            final T annotation = superClass.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

}