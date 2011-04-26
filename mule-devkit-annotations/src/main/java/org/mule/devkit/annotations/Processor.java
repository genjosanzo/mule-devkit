package org.mule.devkit.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks a method inside a {@link Module} as a callable from within a Mule flow. Each
 * parameter on this method will be featured as an attribute on the Mule XML invocation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Processor {
    /**
     * The xml name of the element that will invoke this processor
     */
    String name() default "";
}
