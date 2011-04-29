package org.mule.devkit.apt.util;

public final class ClassNameUtils {
    private ClassNameUtils()
    {}

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
