package org.mule.devkit.apt.util;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.xml.bind.annotation.XmlType;

public final class CodeModelUtils {
    private CodeModelUtils() {
    }

    public static boolean isXmlType(VariableElement variable) {
        TypeMirror variableType = variable.asType();
        if (variableType.getKind() == TypeKind.DECLARED) {

            DeclaredType declaredType = (DeclaredType) variableType;
            XmlType xmlType = declaredType.asElement().getAnnotation(XmlType.class);

            if (xmlType != null) {
                return true;
            }
        }

        return false;

    }
}
