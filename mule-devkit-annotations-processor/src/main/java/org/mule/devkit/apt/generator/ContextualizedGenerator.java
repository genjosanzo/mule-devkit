package org.mule.devkit.apt.generator;

import org.mule.devkit.apt.AnnotationProcessorContext;

public abstract class ContextualizedGenerator implements Generator {
    private AnnotationProcessorContext context;

    public ContextualizedGenerator(AnnotationProcessorContext context) {
        this.context = context;
    }

    public AnnotationProcessorContext getContext() {
        return context;
    }
}
