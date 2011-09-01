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

package org.mule.devkit.module.generation;

import org.apache.commons.lang.StringUtils;
import org.mule.api.MuleContext;
import org.mule.api.annotations.session.SessionCreate;
import org.mule.api.annotations.session.SessionDestroy;
import org.mule.devkit.generation.AbstractGenerator;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.Expression;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.Type;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.List;

public abstract class AbstractModuleGenerator extends AbstractGenerator {

    public Type ref(TypeMirror typeMirror) {
        return this.context.getCodeModel().ref(typeMirror);
    }

    public TypeReference ref(Class<?> clazz) {
        return this.context.getCodeModel().ref(clazz);
    }

    public Type ref(String fullyQualifiedClassName) {
        return this.context.getCodeModel().ref(fullyQualifiedClassName);
    }

    protected Method generateSetter(DefinedClass clazz, FieldVariable field) {
        Method setter = clazz.method(Modifier.PUBLIC, context.getCodeModel().VOID, "set" + StringUtils.capitalize(field.name()));
        setter.javadoc().add("Sets " + field.name());
        setter.javadoc().addParam("value Value to set");
        Variable value = setter.param(field.type(), "value");
        setter.body().assign(ExpressionFactory._this().ref(field), value);

        return setter;
    }

    protected Method generateGetter(DefinedClass clazz, FieldVariable field) {
        Method setter = clazz.method(Modifier.PUBLIC, field.type(), "get" + StringUtils.capitalize(field.name()));
        setter.javadoc().add("Retrieves " + field.name());
        setter.body()._return(ExpressionFactory._this().ref(field));

        return setter;
    }

    protected FieldVariable generateFieldForMuleContext(DefinedClass messageProcessorClass) {
        FieldVariable muleContext = messageProcessorClass.field(Modifier.PRIVATE, ref(MuleContext.class), "muleContext");
        muleContext.javadoc().add("Mule Context");

        return muleContext;
    }

    protected Expression isNull(Invocation invocation) {
        return Op.eq(invocation, ExpressionFactory._null());
    }

    protected String getterMethodForFieldAnnotatedWith(TypeElement typeElement, Class<? extends Annotation> annotation) {
        return methodForFieldAnnotatedWith(typeElement, annotation, "get");
    }

    protected String setterMethodForFieldAnnotatedWith(TypeElement typeElement, Class<? extends Annotation> annotation) {
        return methodForFieldAnnotatedWith(typeElement, annotation, "set");
    }

    protected boolean classHasMethodWithParameterOfType(TypeElement typeElement, Class<?> parameterType) {
        List<ExecutableElement> methods = ElementFilter.methodsIn(typeElement.getEnclosedElements());
        for (ExecutableElement method : methods) {
            for (VariableElement parameter : method.getParameters()) {
                if (parameter.asType().toString().contains(parameterType.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String methodForFieldAnnotatedWith(TypeElement typeElement, Class<? extends Annotation> annotation, String prefix) {
        List<VariableElement> fields = ElementFilter.fieldsIn(typeElement.getEnclosedElements());
        for (VariableElement field : fields) {
            if (field.getAnnotation(annotation) != null) {
                return prefix + StringUtils.capitalize(field.getSimpleName().toString());
            }
        }
        return null;
    }

    protected ExecutableElement createSessionForClass(javax.lang.model.element.Element element) {
        List<? extends javax.lang.model.element.Element> enclosedElements = element.getEnclosedElements();
        List<javax.lang.model.element.ExecutableElement> executableElements = ElementFilter.methodsIn(enclosedElements);
        ExecutableElement createSession = null;
        for (ExecutableElement executableElement : executableElements) {
            if (executableElement.getAnnotation(SessionCreate.class) != null) {
                createSession = executableElement;
                break;
            }
        }

        return createSession;
    }

    protected ExecutableElement destroySessionForMethod(javax.lang.model.element.Element element) {
        return createSessionForClass(element.getEnclosingElement());
    }

    protected ExecutableElement destroySessionForClass(javax.lang.model.element.Element element) {
        List<? extends javax.lang.model.element.Element> enclosedElements = element.getEnclosedElements();
        List<javax.lang.model.element.ExecutableElement> executableElements = ElementFilter.methodsIn(enclosedElements);
        ExecutableElement destroySession = null;
        for (ExecutableElement executableElement : executableElements) {
            if (executableElement.getAnnotation(SessionDestroy.class) != null) {
                destroySession = executableElement;
                break;
            }
        }

        return destroySession;
    }

    protected ExecutableElement createSessionForMethod(javax.lang.model.element.Element element) {
        return createSessionForClass(element.getEnclosingElement());
    }
}