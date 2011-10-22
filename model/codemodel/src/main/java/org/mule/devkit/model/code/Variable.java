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
 * Variables and fields.
 */

public class Variable extends AbstractExpression implements Declaration, AssignmentTarget, Annotable {

    /**
     * Modifiers.
     */
    private Modifiers mods;

    /**
     * Type of the variable
     */
    private Type type;

    /**
     * Name of the variable
     */
    private String name;

    /**
     * Initialization of the variable in its declaration
     */
    private Expression init;

    /**
     * Annotations on this variable. Lazily created.
     */
    private List<AnnotationUse> annotations = null;


    /**
     * Variable constructor
     *
     * @param type Datatype of this variable
     * @param name Name of this variable
     * @param init Value to initialize this variable to
     */
    Variable(Modifiers mods, Type type, String name, Expression init) {
        this.mods = mods;
        this.type = type;
        this.name = name;
        this.init = init;
    }


    /**
     * Initialize this variable
     *
     * @param init Expression to be used to initialize this field
     */
    public Variable init(Expression init) {
        this.init = init;
        return this;
    }

    /**
     * Get the name of this variable
     *
     * @return Name of the variable
     */
    public String name() {
        return name;
    }

    /**
     * Changes the name of this variable.
     */
    public void name(String name) {
        if (!JavaName.isJavaIdentifier(name)) {
            throw new IllegalArgumentException();
        }
        this.name = name;
    }

    /**
     * Return the type of this variable.
     *
     * @return always non-null.
     */
    public Type type() {
        return type;
    }

    /**
     * @return the current modifiers of this method.
     *         Always return non-null valid object.
     */
    public Modifiers mods() {
        return mods;
    }

    /**
     * Sets the type of this variable.
     *
     * @param newType must not be null.
     * @return the old type value. always non-null.
     */
    public Type type(Type newType) {
        Type r = type;
        if (newType == null) {
            throw new IllegalArgumentException();
        }
        type = newType;
        return r;
    }


    /**
     * Adds an annotation to this variable.
     *
     * @param clazz The annotation class to annotate the field with
     */
    public AnnotationUse annotate(TypeReference clazz) {
        if (annotations == null) {
            annotations = new ArrayList<AnnotationUse>();
        }
        AnnotationUse a = new AnnotationUse(clazz);
        annotations.add(a);
        return a;
    }

    /**
     * Adds an annotation to this variable.
     *
     * @param clazz The annotation class to annotate the field with
     */
    public AnnotationUse annotate(Class<? extends Annotation> clazz) {
        return annotate(type.owner().ref(clazz));
    }

    public <W extends AnnotationWriter> W annotate2(Class<W> clazz) {
        return TypedAnnotationWriter.create(clazz, this);
    }

    public Collection<AnnotationUse> annotations() {
        if (annotations == null) {
            annotations = new ArrayList<AnnotationUse>();
        }
        return Collections.unmodifiableList(annotations);
    }

    protected boolean isAnnotated() {
        return annotations != null;
    }

    public void bind(Formatter f) {
        if (annotations != null) {
            for (int i = 0; i < annotations.size(); i++) {
                f.g(annotations.get(i)).nl();
            }
        }
        f.g(mods).g(type).id(name);
        if (init != null) {
            f.p('=').g(init);
        }
    }

    public void declare(Formatter f) {
        f.b(this).p(';').nl();
    }

    public void generate(Formatter f) {
        f.id(name);
    }


    public Expression assign(Expression rhs) {
        return ExpressionFactory.assign(this, rhs);
    }

    public Expression assignPlus(Expression rhs) {
        return ExpressionFactory.assignPlus(this, rhs);
    }

}
