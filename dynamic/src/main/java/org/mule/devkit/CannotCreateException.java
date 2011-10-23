package org.mule.devkit;

public class CannotCreateException extends Exception {
    String namespace;

    public CannotCreateException(String namespace) {
        super();
        this.namespace = namespace;
    }

    public CannotCreateException(String namespace, String s) {
        super(s);
        this.namespace = namespace;
    }

    public CannotCreateException(String namespace, String s, Throwable throwable) {
        super(s, throwable);
        this.namespace = namespace;
    }

    public CannotCreateException(String namespace, Throwable throwable) {
        super(throwable);
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }
}
