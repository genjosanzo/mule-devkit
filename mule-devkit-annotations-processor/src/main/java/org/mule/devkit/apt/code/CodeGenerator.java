package org.mule.devkit.apt.code;

import javax.lang.model.element.TypeElement;

public interface CodeGenerator {
    void generate(TypeElement element) throws CodeGenerationException;
}
