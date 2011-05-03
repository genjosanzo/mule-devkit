package org.mule.devkit.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Parameter {
    /**
     * The name that the user of the module will use to configure this parameter.
     *
     * @return The name of the XML attribute
     */
    String name() default "";

    /**
     * Denotes if this field is optional or not. If it is not optional then the user
     * of the module must provide a value for this parameter before they can invoke
     * the processor.
     *
     * Defaults to false.
     *
     * @return True if optional, false otherwise
     */
    boolean optional() default false;

    /**
     * Default value for this parameter
     */
    String defaultValue() default "";
}
