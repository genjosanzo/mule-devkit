package org.mule.devkit.module.generation;

import org.mule.devkit.generation.AbstractGenerator;
import org.mule.devkit.model.code.JClass;
import org.mule.devkit.model.code.JType;

import javax.lang.model.type.TypeMirror;

public abstract class AbstractModuleGenerator extends AbstractGenerator {

    public JType ref(TypeMirror typeMirror) {
        return this.context.getCodeModel().ref(typeMirror);
    }

    public JClass ref(Class<?> clazz) {
        return this.context.getCodeModel().ref(clazz);
    }

    public JClass ref(String fullyQualifiedClassName) {
        return this.context.getCodeModel().ref(fullyQualifiedClassName);
    }
}
