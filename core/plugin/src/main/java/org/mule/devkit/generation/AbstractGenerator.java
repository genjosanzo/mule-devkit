package org.mule.devkit.generation;

import org.mule.devkit.model.code.JCodeModel;

public abstract class AbstractGenerator implements Generator {
    protected GeneratorContext context;

    public void setContext(GeneratorContext context) {
        this.context = context;
    }

    protected JCodeModel getCodeModel()
    {
        return this.context.getCodeModel();
    }
}
