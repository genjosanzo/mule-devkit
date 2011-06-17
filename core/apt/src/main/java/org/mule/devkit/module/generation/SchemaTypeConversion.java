package org.mule.devkit.module.generation;

import javax.xml.namespace.QName;

public final class SchemaTypeConversion {
    public static boolean isSupported(String typeName) {
        return convertType(typeName, "") != null;
    }

    public static QName convertType(String typeName, String targetNamespace) {
        if (typeName.equals("java.lang.String")) {
            return new QName(SchemaConstants.XSD_NAMESPACE, "string", "xs");
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
