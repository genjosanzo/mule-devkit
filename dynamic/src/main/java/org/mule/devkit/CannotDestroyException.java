package org.mule.devkit;

public class CannotDestroyException extends Exception {
    String namespace;

    public CannotDestroyException(String namespace) {
        super();
        this.namespace = namespace;
    }

    public CannotDestroyException(String namespace, String s) {
        super(s);
        this.namespace = namespace;
    }

    public CannotDestroyException(String namespace, String s, Throwable throwable) {
        super(s, throwable);
        this.namespace = namespace;
    }

    public CannotDestroyException(String namespace, Throwable throwable) {
        super(throwable);
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }
}
