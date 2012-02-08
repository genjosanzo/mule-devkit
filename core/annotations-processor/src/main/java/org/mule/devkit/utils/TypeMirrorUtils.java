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

import org.mule.api.MuleMessage;
import org.mule.api.NestedProcessor;
import org.mule.api.annotations.oauth.OAuthAccessToken;
import org.mule.api.annotations.oauth.OAuthAccessTokenSecret;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.InvocationHeaders;
import org.mule.api.annotations.param.OutboundHeaders;
import org.mule.api.annotations.param.Payload;
import org.mule.api.annotations.param.SessionHeaders;
import org.mule.api.callback.HttpCallback;
import org.mule.api.callback.SourceCallback;

import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.xml.bind.annotation.XmlType;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TypeMirrorUtils {
    private static final List<Class<?>> PARAMETER_TYPES_TO_IGNORE = Arrays.asList(SourceCallback.class, MuleMessage.class);
    private static final List<Class<? extends Annotation>> PARAMETERS_ANNOTATIONS_TO_IGNORE =
            Arrays.asList(InboundHeaders.class, InvocationHeaders.class, OutboundHeaders.class, SessionHeaders.class, Payload.class, OAuthAccessToken.class, OAuthAccessTokenSecret.class);

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

    public boolean ignoreParameter(Element element) {
        String variableType = element.asType().toString();
        for (Class<?> typeToIgnore : PARAMETER_TYPES_TO_IGNORE) {
            if (variableType.contains(typeToIgnore.getName())) {
                return true;
            }
        }
        for (Class<? extends Annotation> annotationToIgnore : PARAMETERS_ANNOTATIONS_TO_IGNORE) {
            if (element.getAnnotation(annotationToIgnore) != null) {
                return true;
            }
        }
        return false;
    }

    public boolean isString(Element element) {
        String className = element.asType().toString();
        return className.startsWith(String.class.getName());
    }

    public boolean isBoolean(Element element) {
        String className = element.asType().toString();
        return className.startsWith(Boolean.class.getName()) || className.startsWith("boolean");
    }

    public boolean isInteger(Element element) {
        String className = element.asType().toString();
        return className.startsWith(Integer.class.getName()) || className.startsWith("int");
    }

    public boolean isLong(Element element) {
        String className = element.asType().toString();
        return className.startsWith(Long.class.getName()) || className.startsWith("long");
    }

    public boolean isFloat(Element element) {
        String className = element.asType().toString();
        return className.startsWith(Float.class.getName()) || className.startsWith("float");
    }

    public boolean isDouble(Element element) {
        String className = element.asType().toString();
        return className.startsWith(Double.class.getName()) || className.startsWith("double");
    }

    public boolean isChar(Element element) {
        String className = element.asType().toString();
        return className.startsWith(Character.class.getName()) || className.startsWith("char");
    }

    public boolean isEnum(Element element) {
        return isEnum(element.asType());
    }

    public boolean isCollection(Element element) {
        return isCollection(element.asType());
    }

    public boolean isHttpCallback(Element element) {
        return element.asType().toString().startsWith(HttpCallback.class.getName());
    }

    public boolean isURL(Element element) {
        return element.asType().toString().startsWith(URL.class.getName());
    }

    public boolean isDate(Element element) {
        return element.asType().toString().startsWith(Date.class.getName());
    }
}