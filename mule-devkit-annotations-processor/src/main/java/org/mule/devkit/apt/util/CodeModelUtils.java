package org.mule.devkit.apt.util;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

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

    public static boolean isArrayOrList(Types types, TypeMirror type) {
        if (type.getKind() == TypeKind.ARRAY) {
            return true;
        }

        if (type.toString().contains(java.util.List.class.getName())) {
            return true;
        }

        List<? extends TypeMirror> inherits = types.directSupertypes(type);
        for (TypeMirror inherit : inherits) {
            if (isArrayOrList(types, inherit)) {
                return true;
            }
        }

        return false;
    }
}
