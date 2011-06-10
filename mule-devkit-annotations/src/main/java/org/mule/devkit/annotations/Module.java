package org.mule.devkit.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation defines a class that will export its functionality as a Mule module.
 *
 * There are a few restrictions as to which types as valid for this annotation:
 * - It cannot be an interface
 * - It must be public
 * - It cannot have a typed parameter (no generic)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Module {
    /**
     * The name of the module.
     */
    String name();

    /**
     * The version of the module. Defaults to 1.0.
     */
    String version() default DEFAULT_VERSION;

    /**
     * Namespace of the module
     */
    String namespace() default "";

    /**
     * Location URI for the schema
     */
    String schemaLocation() default "";

    String DEFAULT_VERSION = "1.0";

}
