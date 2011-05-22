package org.mule.devkit.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Transformer
{
    /**
     * The 'priorityWeighting property is used to resolve conflicts where there is more than one transformers that match
     * the selection criteria.  10 is the highest priority and 1 is the lowest.
     *
     * @return the priority weighting for this transformer. If the class defines more than one transform method, every transform
     *         method will have the same weighting.
     */
    int priorityWeighting() default 5;

    /**
     * SourceTypes define additional types that this transformer will accepts as a sourceType (beyond the method parameter).
     * At run time if the current message matches one of these source types, Mule will attempt to transform from
     * the source type to the method parameter type.  This means that transformations can be chained. The user can create
     * other transformers to be a part of this chain.
     *
     * @return an array of class types which allow the transformer to be matched on
     */
    Class[] sourceTypes() default {};
}
