package org.mule.devkit.apt.generator;

import javax.lang.model.element.TypeElement;

public interface Generator {
    void generate(TypeElement element) throws GenerationException;
}
