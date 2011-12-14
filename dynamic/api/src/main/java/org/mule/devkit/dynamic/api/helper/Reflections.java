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

import java.lang.reflect.Field;

import java.util.Map;
import org.mule.util.StringUtils;

/**
 * Helper methods for reflection.
 */
public final class Reflections {

    private Reflections() {
    }

    public static Field setAccessible(final Object object, final String propertyName) {
        try {
            final Field field = object.getClass().getDeclaredField(propertyName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to make <"+propertyName+"> accessible", e);
        }
    }

    /**
     * @param propertyName
     * @return default getter name for specified property
     */
    public static String getterMethodName(final String propertyName) {
        return "get"+StringUtils.capitalize(propertyName);
    }

    /**
     * Get value of property for specified object.
     * @param object
     * @param propertyName
     */
    public static Object get(final Object object, final String propertyName) {
        try {
            return Reflections.invoke(object, Reflections.getterMethodName(propertyName), void.class);
        } catch (RuntimeException e) {
            final Field field = Reflections.setAccessible(object, propertyName);
            try {
                return field.get(object);
            } catch(IllegalAccessException ee) {
                throw new RuntimeException(ee);
            }
        }
    }

    /**
     * @param propertyName
     * @return default setter name for specified property
     */
    public static String setterMethodName(final String propertyName) {
        return "set"+StringUtils.capitalize(propertyName);
    }

    /**
     * Sets property to value for specified object.
     * @param object
     * @param propertyName
     * @param value 
     */
    public static void set(final Object object, final String propertyName, final Object value) {
        try {
            Reflections.invoke(object, Reflections.setterMethodName(propertyName), value);
        } catch (RuntimeException e) {
            final Field field = Reflections.setAccessible(object, propertyName);
            try {
                field.set(object, value);
            } catch(IllegalAccessException ee) {
                throw new RuntimeException(ee);
            }
        }
    }

    /**
     * Sets parameters for specified object.
     * @param object
     * @param parameters
     */
    public static void set(final Object object, final Map<String, Object> parameters) {
        for (final Map.Entry<String, Object> entry : parameters.entrySet()) {
            final String parameterName = entry.getKey();
            try {
                Reflections.invoke(object, Reflections.setterMethodName(parameterName), entry.getValue(), Object.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set parameter <"+parameterName+">", e);
            }
        }
    }

    /**
     * @param type
     * @return primitive equivalent type for specified {@link Class}
     * @throws IllegalArgumentException ig specified {@link Class} is not {@link Class#isPrimitive() }
     */
    public static Class<?> toPrimitive(final Class<?> type) {
        if (type.equals(Integer.class)) {
            return int.class;
        } else if (type.equals(Float.class)) {
            return float.class;
        } else if (type.equals(Long.class)) {
            return long.class;
        } else if (type.equals(Double.class)) {
            return double.class;
        } else if (type.equals(Character.class)) {
            return char.class;
        } else if (type.equals(Byte.class)) {
            return byte.class;
        } else if (type.equals(Short.class)) {
            return short.class;
        } else if (type.equals(Boolean.class)) {
            return boolean.class;
        }
        throw new IllegalArgumentException("Unrecognized primitive type <"+type+">");
    }

    /**
     * @param type
     * @return bridge equivalent type for specified {@link Class}
     * @throws IllegalArgumentException ig specified {@link Class} is not {@link Class#isPrimitive() }
     */
    public static Class<?> toType(final Class<?> type) {
        if (type.equals(int.class)) {
            return Integer.class;
        } else if (type.equals(float.class)) {
            return Float.class;
        } else if (type.equals(long.class)) {
            return Long.class;
        } else if (type.equals(double.class)) {
            return Double.class;
        } else if (type.equals(char.class)) {
            return Character.class;
        } else if (type.equals(byte.class)) {
            return Byte.class;
        } else if (type.equals(short.class)) {
            return Short.class;
        } else if (type.equals(boolean.class)) {
            return Boolean.class;
        }
        throw new IllegalArgumentException("Unrecognized primitive type <"+type+">");
    }

    /**
     * @param type
     * @return type representation of provided {@link Class}. Namely convert primitive to their type counterpart.
     * @see Class#isPrimitive() 
     * @see #toType(java.lang.Class) 
     */
    public static Class<?> asType(final Class<?> type) {
        if (type.isPrimitive()) {
            return toType(type);
        }
        return type;
    }

    /**
     * @param <T>
     * @param object
     * @param method
     * @param argument
     * @return result of dynamic invocation of `method` on `object` with `argument`.
     * @see #asTypes(java.lang.Object[]) for inferred type from arguments
     */
    public static <T> T invoke(final Object object, final String method, final Object argument) {
        try {
            return Reflections.<T>invoke(object, method, argument, argument.getClass());
        } catch (RuntimeException e) {
            if (!argument.getClass().isPrimitive()) {
                throw e;
            } else {
                return Reflections.<T>invoke(object, method, argument, Reflections.toPrimitive(argument.getClass()));
            }
        }
    }

    /**
     * @param <T>
     * @param object
     * @param method
     * @param argument
     * @param argumentType 
     * @return result of dynamic invocation of `method` on `object` with `argument`.
     */
    public static <T> T invoke(final Object object, final String method, final Object argument, final Class<?> argumentType) {
        try {
            return (T) object.getClass().getMethod(method, argumentType).invoke(object, argument);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke <"+method+"> with arguments <"+argument+"> on <"+object+">", e);
        }
    }

    /**
     * @param <T>
     * @param object
     * @param method
     * @return result of dynamic invocation of `method` on `object` with no argument.
     */
    public static <T> T invoke(final Object object, final String method) {
        try {
            return (T) object.getClass().getMethod(method).invoke(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke <"+method+"> on <"+object+">", e);
        }
    }

}