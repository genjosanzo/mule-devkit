package org.mule.devkit.apt.util;

public class ClassNameUtils {
    public static String getClassName(String fullyQualifiedClassName)
    {
        int lastDot = fullyQualifiedClassName.lastIndexOf('.');
        return fullyQualifiedClassName.substring(lastDot + 1);
    }

    public static String getPackageName(String fullyQualifiedClassName)
    {
        int lastDot = fullyQualifiedClassName.lastIndexOf('.');
        return fullyQualifiedClassName.substring(0, lastDot);
    }
}
