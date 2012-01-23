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
package org.mule.devkit.model.code.builders;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.Expression;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Type;
import org.mule.devkit.model.code.Variable;

public class FieldBuilder {
    private DefinedClass targetClass;
    private Class<?> type;
    private Type typeRef;
    private String name;
    private String javadoc;
    private boolean getter;
    private boolean setter;
    private int modifiers;
    private Object initialValue;

    public static FieldBuilder newConstantFieldBuilder(DefinedClass targetClass) {
        FieldBuilder fieldBuilder = new FieldBuilder(targetClass);
        fieldBuilder.privateVisibility();
        fieldBuilder.staticField();
        fieldBuilder.finalField();
        return fieldBuilder;
    }

    public static FieldVariable newLoggerField(DefinedClass targetClass) {
        return new FieldBuilder(targetClass).
                privateVisibility().
                staticField().
                finalField().
                name("LOGGER").
                type(Logger.class).
                initialValue(targetClass.owner().ref(Logger.class).staticInvoke("getLogger").arg(ExpressionFactory.dotclass(targetClass))).
                build();
    }

    public FieldBuilder(DefinedClass targetClass) {
        Validate.notNull(targetClass, "the target class cannot be null");
        this.targetClass = targetClass;
        privateVisibility();
    }

    public FieldBuilder name(String name) {
        this.name = name;
        return this;
    }

    public FieldBuilder type(Class<?> type) {
        this.type = type;
        return this;
    }

    public FieldBuilder type(Type typeRef) {
        this.typeRef = typeRef;
        return this;
    }

    public FieldBuilder javadoc(String javadoc) {
        this.javadoc = javadoc;
        return this;
    }

    public FieldBuilder privateVisibility() {
        modifiers = modifiers | Modifier.PRIVATE;
        return this;
    }

    public FieldBuilder publicVisibility() {
        modifiers = Modifier.PUBLIC;
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
        Method setter = targetClass.method(Modifier.PUBLIC, targetClass.owner().VOID, "set" + StringUtils.capitalize(field.name()));
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
        if (typeRef == null) {
            this.typeRef = targetClass.owner().ref(type);
        }
        if(name == null) {
            name = StringUtils.uncapitalize(type.getName());
        }
        FieldVariable field = targetClass.field(modifiers, typeRef, name);
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
}