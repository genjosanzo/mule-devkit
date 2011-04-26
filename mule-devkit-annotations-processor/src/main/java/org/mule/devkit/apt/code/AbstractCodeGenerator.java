package org.mule.devkit.apt.code;

import org.mule.devkit.apt.AnnotationProcessorContext;

public abstract class AbstractCodeGenerator implements CodeGenerator {
    private AnnotationProcessorContext context;

    public AbstractCodeGenerator(AnnotationProcessorContext context) {
        this.context = context;
    }

    public AnnotationProcessorContext getContext() {
        return context;
    }
}
