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

package org.mule.devkit.generation;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

public class TypeElementImpl implements TypeElement {

    protected TypeElement typeElement;

    public TypeElementImpl(TypeElement typeElement) {
        this.typeElement = typeElement;
    }

    @Override
    public NestingKind getNestingKind() {
        return typeElement.getNestingKind();
    }

    @Override
    public Name getQualifiedName() {
        return typeElement.getQualifiedName();
    }

    @Override
    public TypeMirror getSuperclass() {
        return typeElement.getSuperclass();
    }

    @Override
    public List<? extends TypeMirror> getInterfaces() {
        return typeElement.getInterfaces();
    }

    @Override
    public List<? extends TypeParameterElement> getTypeParameters() {
        return typeElement.getTypeParameters();
    }

    @Override
    public TypeMirror asType() {
        return typeElement.asType();
    }

    @Override
    public ElementKind getKind() {
        return typeElement.getKind();
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        return typeElement.getAnnotationMirrors();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return typeElement.getAnnotation(annotationType);
    }

    @Override
    public Set<Modifier> getModifiers() {
        return typeElement.getModifiers();
    }

    @Override
    public Name getSimpleName() {
        return typeElement.getSimpleName();
    }

    @Override
    public Element getEnclosingElement() {
        return typeElement.getEnclosingElement();
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
        return typeElement.getEnclosedElements();
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return typeElement.accept(v, p);
    }
}