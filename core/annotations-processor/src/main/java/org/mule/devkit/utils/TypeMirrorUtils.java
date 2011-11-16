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

import org.mule.api.NestedProcessor;
import org.mule.api.annotations.oauth.OAuthAccessToken;
import org.mule.api.annotations.oauth.OAuthAccessTokenSecret;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.InvocationHeaders;
import org.mule.api.annotations.param.OutboundHeaders;
import org.mule.api.annotations.param.Payload;
import org.mule.api.callback.SourceCallback;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.xml.bind.annotation.XmlType;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class TypeMirrorUtils {
    private static final List<Class<?>> PARAMETER_TYPES_TO_IGNORE = Arrays.asList( new Class<?>[] { SourceCallback.class });
    private static final List<Class<? extends Annotation>> PARAMETERS_ANNOTATIONS_TO_IGNORE =
            Arrays.asList(InboundHeaders.class, InvocationHeaders.class, OutboundHeaders.class, Payload.class, OAuthAccessToken.class, OAuthAccessTokenSecret.class);

    private Types types;

    public TypeMirrorUtils(Types types) {
        this.types = types;
    }

    public boolean isXmlType(TypeMirror type) {
        if (type.getKind() == TypeKind.DECLARED) {

            DeclaredType declaredType = (DeclaredType) type;
            XmlType xmlType = declaredType.asElement().getAnnotation(XmlType.class);

            if (xmlType != null) {
                return true;
            }
        }

        return false;
    }

    public boolean isCollection(TypeMirror type) {
        return isArrayOrList(type) || isMap(type);
    }

    public boolean isNestedProcessor(TypeMirror type) {
        if (type.toString().startsWith(NestedProcessor.class.getName())) {
            return true;
        }

        if (type.toString().startsWith(List.class.getName())) {
            DeclaredType variableType = (DeclaredType) type;
            List<? extends TypeMirror> variableTypeParameters = variableType.getTypeArguments();
            if (variableTypeParameters.isEmpty()) {
                return false;
            }

            if (variableTypeParameters.get(0).toString().startsWith(NestedProcessor.class.getName())) {
                return true;
            }
        }

        return false;
    }

    public boolean isArrayOrList(TypeMirror type) {
        if (type.toString().equals("byte[]")) {
            return false;
        }

        if (type.getKind() == TypeKind.ARRAY) {
            return true;
        }

        if (type.toString().startsWith(List.class.getName())) {
            return true;
        }

        List<? extends TypeMirror> inherits = types.directSupertypes(type);
        for (TypeMirror inherit : inherits) {
            if (isArrayOrList(inherit)) {
                return true;
            }
        }

        return false;
    }

    public boolean isMap(TypeMirror type) {
        if (type.toString().startsWith(Map.class.getName())) {
            return true;
        }

        List<? extends TypeMirror> inherits = types.directSupertypes(type);
        for (TypeMirror inherit : inherits) {
            if (isMap(inherit)) {
                return true;
            }
        }

        return false;
    }

    public boolean isEnum(TypeMirror type) {
        if (type.toString().startsWith(Enum.class.getName())) {
            return true;
        }

        List<? extends TypeMirror> inherits = types.directSupertypes(type);
        for (TypeMirror inherit : inherits) {
            if (isEnum(inherit)) {
                return true;
            }
        }

        return false;
    }

    public boolean ignoreParameter(VariableElement variable) {
        String variableType = variable.asType().toString();
        for (Class<?> typeToIgnore : PARAMETER_TYPES_TO_IGNORE) {
            if (variableType.contains(typeToIgnore.getName())) {
                return true;
            }
        }
        for (Class<? extends Annotation> annotationToIgnore : PARAMETERS_ANNOTATIONS_TO_IGNORE) {
            if (variable.getAnnotation(annotationToIgnore) != null) {
                return true;
            }
        }
        return false;
    }

}
