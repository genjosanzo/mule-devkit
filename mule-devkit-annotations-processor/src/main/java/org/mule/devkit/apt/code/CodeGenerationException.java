package org.mule.devkit.apt.code;

public class CodeGenerationException extends Exception {

    public CodeGenerationException() {
        super();
    }

    public CodeGenerationException(String s) {
        super(s);
    }

    public CodeGenerationException(String s, Throwable throwable) {
        super(s, throwable);
    }


    public CodeGenerationException(Throwable throwable) {
        super(throwable);
    }
}
