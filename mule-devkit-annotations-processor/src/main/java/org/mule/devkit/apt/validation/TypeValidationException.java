package org.mule.devkit.apt.validation;

import javax.lang.model.element.TypeElement;

public class TypeValidationException extends Exception {
    private TypeElement type;

    public TypeValidationException(TypeElement type) {
        super();

        this.type = type;
    }

    public TypeValidationException(TypeElement type, String s) {
        super(s);

        this.type = type;
    }

    public TypeValidationException(TypeElement type, String s, Throwable throwable) {
        super(s, throwable);

        this.type = type;
    }


    public TypeValidationException(TypeElement type, Throwable throwable) {
        super(throwable);

        this.type = type;
    }

    public TypeElement getType() {
        return type;
    }
}
