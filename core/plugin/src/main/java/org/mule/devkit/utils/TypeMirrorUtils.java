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

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

public final class TypeMirrorUtils {
    private Types types;

    public TypeMirrorUtils(Types types)
    {
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

    public boolean isCollection(TypeMirror type)
    {
        return isArrayOrList(type) || isMap(type);
    }

    public boolean isArrayOrList(TypeMirror type) {
        if (type.getKind() == TypeKind.ARRAY) {
            return true;
        }

        if (type.toString().contains(java.util.List.class.getName())) {
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
        if (type.toString().contains(java.util.Map.class.getName())) {
            return true;
        }

        List<? extends TypeMirror> inherits = types.directSupertypes(type);
        for (TypeMirror inherit : inherits) {
            if (isMap( inherit)) {
                return true;
            }
        }

        return false;
    }

    public boolean isEnum(TypeMirror type) {
        if (type.toString().contains(Enum.class.getName())) {
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

}
