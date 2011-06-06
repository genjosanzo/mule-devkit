package org.mule.devkit.annotations;

public interface SourceCallback {
    Object process(Object payload);
}
