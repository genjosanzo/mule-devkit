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
package org.mule.devkit.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.mule.devkit.generation.GeneratorContext;
import org.mule.devkit.model.code.*;
import org.mule.devkit.module.generation.AbstractModuleGenerator;

public class FieldBuilder {

    private static GeneratorContext generatorContext;
    private DefinedClass targetClass;
    private Class<?> type;
    private String name;
    private String javadoc;
    private boolean getter;
    private boolean setter;
    private boolean privateVisibility;
    private boolean instanceVariable;
    private int modifiers;
    private AbstractModuleGenerator generator;
    private Object initialValue;

    public static FieldBuilder newConstantFieldBuilder(DefinedClass targetClass, AbstractModuleGenerator generator) {
        FieldBuilder fieldBuilder = new FieldBuilder(targetClass, generator);
        fieldBuilder.privateVisibility();
        fieldBuilder.staticField();
        fieldBuilder.finalField();
        return fieldBuilder;
    }

    public static FieldVariable newLoggerField(DefinedClass targetClass, AbstractModuleGenerator generator) {
        return new FieldBuilder(targetClass, generator).
                privateVisibility().
                staticField().
                finalField().
                name("LOGGER").
                type(Logger.class).
                initialValue(generator.ref(Logger.class).staticInvoke("getLogger").arg(ExpressionFactory.dotclass(targetClass))).
                build();
    }

    public FieldBuilder(DefinedClass targetClass, AbstractModuleGenerator generator) {
        Validate.notNull(targetClass, "the target class cannot be null");
        Validate.notNull(generator, "the generator cannot be null");
        this.targetClass = targetClass;
        this.generator = generator;
    }

    public FieldBuilder name(String name) {
        this.name = name;
        return this;
    }

    public FieldBuilder type(Class<?> type) {
        this.type = type;
        return this;
    }

    public FieldBuilder javadoc(String javadoc) {
        this.javadoc = javadoc;
        return this;
    }

    public FieldBuilder privateVisibility() {
        modifiers = modifiers | Modifier.PRIVATE;
        privateVisibility = true;
        return this;
    }

    public FieldBuilder getterAndSetter() {
        getter = true;
        setter = true;
        return this;
    }

    public FieldBuilder getter() {
        getter = true;
        return this;
    }

    public FieldBuilder setter() {
        setter = true;
        return this;
    }

    public FieldBuilder staticField() {
        modifiers = modifiers | Modifier.STATIC;
        return this;
    }

    public FieldBuilder finalField() {
        modifiers = modifiers | Modifier.FINAL;
        return this;
    }

    public FieldBuilder initialValue(Expression initialValue) {
        this.initialValue = initialValue;
        return this;
    }

    public FieldBuilder initialValue(String initialValue) {
        this.initialValue = initialValue;
        return this;
    }

    public FieldBuilder initialValue(int initialValue) {
        this.initialValue = initialValue;
        return this;
    }

    private Method generateSetter(FieldVariable field) {
        Method setter = targetClass.method(Modifier.PUBLIC, generatorContext.getCodeModel().VOID, "set" + StringUtils.capitalize(field.name()));
        setter.javadoc().add("Sets " + field.name());
        setter.javadoc().addParam("value Value to set");
        Variable value = setter.param(field.type(), "value");
        setter.body().assign(ExpressionFactory._this().ref(field), value);

        return setter;
    }

    private Method generateGetter(FieldVariable field) {
        Method setter = targetClass.method(Modifier.PUBLIC, field.type(), "get" + StringUtils.capitalize(field.name()));
        setter.javadoc().add("Retrieves " + field.name());
        setter.body()._return(ExpressionFactory._this().ref(field));

        return setter;
    }

    public FieldVariable build() {
        Validate.notNull(generatorContext, "The GeneratorConext needs to be set");
        Validate.notNull(name, "The name must be set");
        Validate.notNull(type, "The type must be set");
        FieldVariable field = targetClass.field(modifiers, generator.ref(type), name);
        if (javadoc != null && !javadoc.isEmpty()) {
            field.javadoc().add(javadoc);
        }
        if (getter) {
            generateGetter(field);
        }
        if (setter) {
            generateSetter(field);
        }
        if (initialValue != null) {
            if (initialValue instanceof Expression) {
                field.init((Expression) initialValue);
            } else if (initialValue instanceof String) {
                field.init(ExpressionFactory.lit((String) initialValue));
            } else {
                field.init(ExpressionFactory.lit((Integer) initialValue));
            }
        }
        return field;
    }

    public static void setGeneratorContext(GeneratorContext generatorContext) {
        FieldBuilder.generatorContext = generatorContext;
    }
}