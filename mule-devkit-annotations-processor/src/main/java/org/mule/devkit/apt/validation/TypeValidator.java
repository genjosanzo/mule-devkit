package org.mule.devkit.apt.validation;

import javax.lang.model.element.TypeElement;

public interface TypeValidator {
    void validate(TypeElement type) throws TypeValidationException;
}
