package org.mule.devkit.apt.validation;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class ModuleTypeValidator implements TypeValidator {

    public void validate(TypeElement type) throws TypeValidationException {
        // must not be an interface
        if (type.getKind() == ElementKind.INTERFACE) {
            throw new TypeValidationException(type, "@Module cannot be applied to an interface");
        }

        // must not have type parameters
        if (!type.getTypeParameters().isEmpty()) {
            throw new TypeValidationException(type, "@Module type cannot have type parameters");
        }

        // must be public
        if (!type.getModifiers().contains(Modifier.PUBLIC)) {
            throw new TypeValidationException(type, "@Module must be public");
        }
    }
}
