package org.mule.devkit.apt.util;

public final class NameUtils {

    public static String uncamel(String camelCaseName) {
        String result = "";
        String[] parts = camelCaseName.split("(?<!^)(?=[A-Z])");

        for (int i = 0; i < parts.length; i++)
            result += parts[i].toLowerCase() + (i < parts.length - 1 ? "-" : "");

        return result;
    }
}
