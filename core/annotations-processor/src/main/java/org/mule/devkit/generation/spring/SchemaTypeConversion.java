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

package org.mule.devkit.generation.spring;

import javax.xml.namespace.QName;

public final class SchemaTypeConversion {
    public static boolean isSupported(String typeName) {
        return convertType(typeName, "") != null;
    }

    public static QName convertType(String typeName, String targetNamespace) {
        if (typeName.equals("java.lang.String")) {
            return new QName(SchemaConstants.XSD_NAMESPACE, "string", "xs");
        //} else if( typeName.equals("java.lang.Object")) {
            //return new QName(SchemaConstants.XSD_NAMESPACE, "string", "xs");
            //return new QName(SchemaConstants.MULE_NAMESPACE, "propertyPlaceholderType", "mule");
        } else if (typeName.equals("int")) {
            return new QName(targetNamespace, "integerType");
        } else if (typeName.equals("float")) {
            return new QName(targetNamespace, "floatType");
        } else if (typeName.equals("long")) {
            return new QName(targetNamespace, "longType");
        } else if (typeName.equals("byte")) {
            return new QName(targetNamespace, "byteType");
        } else if (typeName.equals("short")) {
            return new QName(targetNamespace, "integerType");
        } else if (typeName.equals("double")) {
            return new QName(targetNamespace, "doubleType");
        } else if (typeName.equals("boolean")) {
            return new QName(targetNamespace, "booleanType");
        } else if (typeName.equals("char")) {
            return new QName(targetNamespace, "charType");
        } else if (typeName.equals("java.lang.Integer")) {
            return new QName(targetNamespace, "integerType");
        } else if (typeName.equals("java.lang.Float")) {
            return new QName(targetNamespace, "floatType");
        } else if (typeName.equals("java.lang.Long")) {
            return new QName(targetNamespace, "longType");
        } else if (typeName.equals("java.lang.Byte")) {
            return new QName(targetNamespace, "byteType");
        } else if (typeName.equals("java.lang.Short")) {
            return new QName(targetNamespace, "integerType");
        } else if (typeName.equals("java.lang.Double")) {
            return new QName(targetNamespace, "doubleType");
        } else if (typeName.equals("java.lang.Boolean")) {
            return new QName(targetNamespace, "booleanType");
        } else if (typeName.equals("java.lang.Character")) {
            return new QName(targetNamespace, "charType");
        } else if (typeName.equals("java.util.Date")) {
            return new QName(targetNamespace, "dateTimeType");
        } else if (typeName.equals("java.net.URL")) {
            return new QName(targetNamespace, "anyUriType");
        } else if (typeName.equals("java.net.URI")) {
            return new QName(targetNamespace, "anyUriType");
        }

        return null;
    }
}
