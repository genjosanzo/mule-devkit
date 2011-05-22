package org.mule.devkit.annotations;

public interface SourceCallback<T> {
    T process(T message);
}
