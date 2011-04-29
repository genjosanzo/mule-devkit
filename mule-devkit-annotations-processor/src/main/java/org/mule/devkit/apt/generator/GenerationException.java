package org.mule.devkit.apt.generator;

public class GenerationException extends Exception {

    public GenerationException() {
        super();
    }

    public GenerationException(String s) {
        super(s);
    }

    public GenerationException(String s, Throwable throwable) {
        super(s, throwable);
    }


    public GenerationException(Throwable throwable) {
        super(throwable);
    }
}
