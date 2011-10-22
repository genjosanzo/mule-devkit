/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.mule.devkit.model.code;


import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents an arrays as annotation members
 * <p/>
 * <p/>
 * This class implements {@link Annotable} to allow
 * new annotations to be added as a member of the array.
 *
 * @author Bhakti Mehta (bhakti.mehta@sun.com)
 */
public final class AnnotationArrayMember extends AnnotationValue implements Annotable {
    private final List<AnnotationValue> values = new ArrayList<AnnotationValue>();
    private final CodeModel owner;

    AnnotationArrayMember(CodeModel owner) {
        this.owner = owner;
    }

    /**
     * Adds an array member to this annotation
     *
     * @param value Adds a string value to the array member
     * @return The AnnotationArrayMember. More elements can be added by calling
     *         the same method multiple times
     */
    public AnnotationArrayMember param(String value) {
        AnnotationValue annotationValue = new AnnotationStringValue(ExpressionFactory.lit(value));
        values.add(annotationValue);
        return this;
    }

    /**
     * Adds an array member to this annotation
     *
     * @param value Adds a boolean value to the array member
     * @return The AnnotationArrayMember. More elements can be added by calling
     *         the same method multiple times
     */
    public AnnotationArrayMember param(boolean value) {
        AnnotationValue annotationValue = new AnnotationStringValue(ExpressionFactory.lit(value));
        values.add(annotationValue);
        return this;
    }

    /**
     * Adds an array member to this annotation
     *
     * @param value Adds a byte value to the array member
     * @return The AnnotationArrayMember. More elements can be added by calling
     *         the same method multiple times
     */
    public AnnotationArrayMember param(byte value) {
        AnnotationValue annotationValue = new AnnotationStringValue(ExpressionFactory.lit(value));
        values.add(annotationValue);
        return this;
    }

    /**
     * Adds an array member to this annotation
     *
     * @param value Adds a char value to the array member
     * @return The AnnotationArrayMember. More elements can be added by calling
     *         the same method multiple times
     */
    public AnnotationArrayMember param(char value) {
        AnnotationValue annotationValue = new AnnotationStringValue(ExpressionFactory.lit(value));
        values.add(annotationValue);
        return this;
    }

    /**
     * Adds an array member to this annotation
     *
     * @param value Adds a double value to the array member
     * @return The AnnotationArrayMember. More elements can be added by calling
     *         the same method multiple times
     */
    public AnnotationArrayMember param(double value) {
        AnnotationValue annotationValue = new AnnotationStringValue(ExpressionFactory.lit(value));
        values.add(annotationValue);
        return this;
    }

    /**
     * Adds an array member to this annotation
     *
     * @param value Adds a long value to the array member
     * @return The AnnotationArrayMember. More elements can be added by calling
     *         the same method multiple times
     */
    public AnnotationArrayMember param(long value) {
        AnnotationValue annotationValue = new AnnotationStringValue(ExpressionFactory.lit(value));
        values.add(annotationValue);
        return this;
    }

    /**
     * Adds an array member to this annotation
     *
     * @param value Adds a short value to the array member
     * @return The AnnotationArrayMember. More elements can be added by calling
     *         the same method multiple times
     */
    public AnnotationArrayMember param(short value) {
        AnnotationValue annotationValue = new AnnotationStringValue(ExpressionFactory.lit(value));
        values.add(annotationValue);
        return this;
    }

    /**
     * Adds an array member to this annotation
     *
     * @param value Adds an int value to the array member
     * @return The AnnotationArrayMember. More elements can be added by calling
     *         the same method multiple times
     */
    public AnnotationArrayMember param(int value) {
        AnnotationValue annotationValue = new AnnotationStringValue(ExpressionFactory.lit(value));
        values.add(annotationValue);
        return this;
    }

    /**
     * Adds an array member to this annotation
     *
     * @param value Adds a float value to the array member
     * @return The AnnotationArrayMember. More elements can be added by calling
     *         the same method multiple times
     */
    public AnnotationArrayMember param(float value) {
        AnnotationValue annotationValue = new AnnotationStringValue(ExpressionFactory.lit(value));
        values.add(annotationValue);
        return this;
    }

    /**
     * Adds a enum array member to this annotation
     *
     * @param value Adds a enum value to the array member
     * @return The AnnotationArrayMember. More elements can be added by calling
     *         the same method multiple times
     */
    public AnnotationArrayMember param(final Enum<?> value) {
        AnnotationValue annotationValue = new AnnotationValue() {
            public void generate(Formatter f) {
                f.t(owner.ref(value.getDeclaringClass())).p('.').p(value.name());
            }
        };
        values.add(annotationValue);
        return this;
    }

    /**
     * Adds a enum array member to this annotation
     *
     * @param value Adds a enum value to the array member
     * @return The AnnotationArrayMember. More elements can be added by calling
     *         the same method multiple times
     */
    public AnnotationArrayMember param(final EnumConstant value) {
        AnnotationValue annotationValue = new AnnotationStringValue(value);
        values.add(annotationValue);
        return this;
    }

    /**
     * Adds an expression array member to this annotation
     *
     * @param value Adds an expression value to the array member
     * @return The AnnotationArrayMember. More elements can be added by calling
     *         the same method multiple times
     */
    public AnnotationArrayMember param(final Expression value) {
        AnnotationValue annotationValue = new AnnotationStringValue(value);
        values.add(annotationValue);
        return this;
    }

    /**
     * Adds a class array member to this annotation
     *
     * @param value Adds a class value to the array member
     * @return The AnnotationArrayMember. More elements can be added by calling
     *         the same method multiple times
     */
    public AnnotationArrayMember param(final Class<?> value) {
        AnnotationValue annotationValue = new AnnotationStringValue(
                new AbstractExpression() {
                    public void generate(Formatter f) {
                        f.p(value.getName().replace('$', '.'));
                        f.p(".class");
                    }
                });
        values.add(annotationValue);
        return this;
    }

    public AnnotationArrayMember param(Type type) {
        TypeReference clazz = type.boxify();
        AnnotationValue annotationValue = new AnnotationStringValue(clazz.dotclass());
        values.add(annotationValue);
        return this;
    }

    /**
     * Adds a new annotation to the array.
     */
    public AnnotationUse annotate(Class<? extends Annotation> clazz) {
        return annotate(owner.ref(clazz));
    }

    /**
     * Adds a new annotation to the array.
     */
    public AnnotationUse annotate(TypeReference clazz) {
        AnnotationUse a = new AnnotationUse(clazz);
        values.add(a);
        return a;
    }

    public <W extends AnnotationWriter> W annotate2(Class<W> clazz) {
        return TypedAnnotationWriter.create(clazz, this);
    }

    /**
     * {@link Annotable#annotations()}
     */
    @SuppressWarnings("unchecked")
    public Collection<AnnotationUse> annotations() {
        // this invocation is invalid if the caller isn't adding annotations into an array
        // so this potentially type-unsafe conversion would be justified.
        return Collections.<AnnotationUse>unmodifiableList((List) values);
    }

    /**
     * Adds an annotation member to this annotation  array
     * This can be used for e.g &#64;XmlCollection(values= &#64;XmlCollectionItem(type=Foo.class))
     *
     * @param value Adds a annotation  to the array member
     * @return The AnnotationArrayMember. More elements can be added by calling
     *         the same method multiple times
     * @deprecated use {@link #annotate}
     */
    public AnnotationArrayMember param(AnnotationUse value) {
        values.add(value);
        return this;
    }

    public void generate(Formatter f) {
        f.p('{').nl().i();

        boolean first = true;
        for (AnnotationValue aValue : values) {
            if (!first) {
                f.p(',').nl();
            }
            f.g(aValue);
            first = false;
        }
        f.nl().o().p('}');
    }
}

