package org.mule.devkit.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field inside a {@link Module} as being configurable. A user will be able to use XML attributes to set this
 * bean properties when using the Module.
 *
 * The field must have setter and getters.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Configurable {
    /**
     * The name that the user of the module will use to configure this field.
     *
     * @return The name of the XML attribute
     */
    String name() default "";

    /**
     * Denotes if this field is optional or not. If it is not optional then the user
     * of the module must provide a value for this field before they can use the module.
     *
     * Defaults to false.
     *
     * @return True if optional, false otherwise
     */
    boolean optional() default false;

    /**
     * Default value for this field
     */
    String defaultValue() default "";
}
