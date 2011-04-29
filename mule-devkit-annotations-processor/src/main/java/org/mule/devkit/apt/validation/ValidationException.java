package org.mule.devkit.apt.validation;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

public class ValidationException extends Exception {
    private Element element;

    public ValidationException(Element element) {
        super();

        this.element = element;
    }

    public ValidationException(Element element, String s) {
        super(s);

        this.element = element;
    }

    public ValidationException(Element element, String s, Throwable throwable) {
        super(s, throwable);

        this.element = element;
    }


    public ValidationException(Element element, Throwable throwable) {
        super(throwable);

        this.element = element;
    }

    public Element getElement() {
        return element;
    }
}
