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

import org.mule.devkit.model.code.util.ClassNameComparator;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Java method.
 */
public class Method extends AbstractGenerifiable implements Declaration, Annotable, DocCommentable {

    /**
     * Modifiers for this method
     */
    private Modifiers mods;

    /**
     * Return type for this method
     */
    private Type type = null;

    /**
     * Name of this method
     */
    private String name = null;

    /**
     * List of parameters for this method's declaration
     */
    private final List<Variable> params = new ArrayList<Variable>();

    /**
     * Set of exceptions that this method may throw.
     * A set instance lazily created.
     */
    private Set<TypeReference> _throws;

    /**
     * Block of statements that makes up the body this method
     */
    private Block body = null;

    private DefinedClass outer;

    /**
     * javadoc comments for this Method
     */
    private DocComment jdoc = null;

    /**
     * Variable parameter for this method's varargs declaration
     * introduced in J2SE 1.5
     */
    private Variable varParam = null;

    /**
     * Annotations on this variable. Lazily created.
     */
    private List<AnnotationUse> annotations = null;


    private boolean isConstructor() {
        return type == null;
    }

    /**
     * To set the default value for the
     * annotation member
     */
    private Expression defaultValue = null;


    /**
     * Method constructor
     *
     * @param mods Modifiers for this method's declaration
     * @param type Return type for the method
     * @param name Name of this method
     */
    Method(DefinedClass outer, int mods, Type type, String name) {
        this.mods = Modifiers.forMethod(mods);
        this.type = type;
        this.name = name;
        this.outer = outer;
    }

    /**
     * Constructor constructor
     *
     * @param mods   Modifiers for this constructor's declaration
     * @param _class TypeReference containing this constructor
     */
    Method(int mods, DefinedClass _class) {
        this.mods = Modifiers.forMethod(mods);
        this.type = null;
        this.name = _class.name();
        this.outer = _class;
    }

    private Set<TypeReference> getThrows() {
        if (_throws == null) {
            _throws = new TreeSet<TypeReference>(ClassNameComparator.theInstance);
        }
        return _throws;
    }

    /**
     * Add an exception to the list of exceptions that this
     * method may throw.
     *
     * @param exception Name of an exception that this method may throw
     */
    public Method _throws(TypeReference exception) {
        getThrows().add(exception);
        return this;
    }

    public Method _throws(Class<? extends Throwable> exception) {
        return _throws(outer.owner().ref(exception));
    }

    /**
     * Returns the list of variable of this method.
     *
     * @return List of parameters of this method. This list is not modifiable.
     */
    public List<Variable> params() {
        return Collections.<Variable>unmodifiableList(params);
    }

    /**
     * Add the specified variable to the list of parameters
     * for this method signature.
     *
     * @param type Type of the parameter being added
     * @param name Name of the parameter being added
     * @return New parameter variable
     */
    public Variable param(int mods, Type type, String name) {
        Variable v = new Variable(Modifiers.forVar(mods), type, name, null);
        params.add(v);
        return v;
    }

    public Variable param(Type type, String name) {
        return param(Modifier.NONE, type, name);
    }

    public Variable param(int mods, Class<?> type, String name) {
        return param(mods, outer.owner()._ref(type), name);
    }

    public Variable param(Class<?> type, String name) {
        return param(outer.owner()._ref(type), name);
    }

    /**
     * @see #varParam(Type, String)
     */
    public Variable varParam(Class<?> type, String name) {
        return varParam(outer.owner()._ref(type), name);
    }

    /**
     * Add the specified variable argument to the list of parameters
     * for this method signature.
     *
     * @param type Type of the parameter being added.
     * @param name Name of the parameter being added
     * @return the variable parameter
     * @throws IllegalStateException If this method is called twice.
     *                               varargs in J2SE 1.5 can appear only once in the
     *                               method signature.
     */
    public Variable varParam(Type type, String name) {
        if (!hasVarArgs()) {

            varParam =
                    new Variable(
                            Modifiers.forVar(Modifier.NONE),
                            type.array(),
                            name,
                            null);
            return varParam;
        } else {
            throw new IllegalStateException(
                    "Cannot have two varargs in a method,\n"
                            + "Check if varParam method of Method is"
                            + " invoked more than once");

        }

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
        return annotate(owner().ref(clazz));
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

    /**
     * Check if there are any varargs declared
     * for this method signature.
     */
    public boolean hasVarArgs() {
        return this.varParam != null;
    }

    public String name() {
        return name;
    }

    /**
     * Changes the name of the method.
     */
    public void name(String n) {
        this.name = n;
    }

    /**
     * Returns the return type.
     */
    public Type type() {
        return type;
    }

    /**
     * Overrides the return type.
     */
    public void type(Type t) {
        this.type = t;
    }

    /**
     * Returns all the parameter types in an array.
     *
     * @return If there's no parameter, an empty array will be returned.
     */
    public Type[] listParamTypes() {
        Type[] r = new Type[params.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = params.get(i).type();
        }
        return r;
    }

    /**
     * Returns  the varags parameter type.
     *
     * @return If there's no vararg parameter type, null will be returned.
     */
    public Type listVarParamType() {
        if (varParam != null) {
            return varParam.type();
        } else {
            return null;
        }
    }

    /**
     * Returns all the parameters in an array.
     *
     * @return If there's no parameter, an empty array will be returned.
     */
    public Variable[] listParams() {
        return params.toArray(new Variable[params.size()]);
    }

    /**
     * Returns the variable parameter
     *
     * @return If there's no parameter, null will be returned.
     */
    public Variable listVarParam() {
        return varParam;
    }

    /**
     * Returns true if the method has the specified signature.
     */
    public boolean hasSignature(Type[] argTypes) {
        Variable[] p = listParams();
        if (p.length != argTypes.length) {
            return false;
        }

        for (int i = 0; i < p.length; i++) {
            if (!p[i].type().equals(argTypes[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get the block that makes up body of this method
     *
     * @return Body of method
     */
    public Block body() {
        if (body == null) {
            body = new Block();
        }
        return body;
    }

    /**
     * Specify the default value for this annotation member
     *
     * @param value Default value for the annotation member
     */
    public void declareDefaultValue(Expression value) {
        this.defaultValue = value;
    }

    /**
     * Creates, if necessary, and returns the class javadoc for this
     * DefinedClass
     *
     * @return JDocComment containing javadocs for this class
     */
    public DocComment javadoc() {
        if (jdoc == null) {
            jdoc = new DocComment(owner());
        }
        return jdoc;
    }

    public void declare(Formatter f) {
        if (jdoc != null) {
            f.g(jdoc);
        }

        if (annotations != null) {
            for (AnnotationUse a : annotations) {
                f.g(a).nl();
            }
        }

        f.g(mods);

        // declare the generics parameters
        super.declare(f);

        if (!isConstructor()) {
            f.g(type);
        }
        f.id(name).p('(').i();
        // when parameters are printed in new lines, we want them to be indented.
        // there's a good chance no newlines happen, too, but just in case it does.
        boolean first = true;
        for (Variable var : params) {
            if (!first) {
                f.p(',');
            }
            if (var.isAnnotated()) {
                f.nl();
            }
            f.b(var);
            first = false;
        }
        if (hasVarArgs()) {
            if (!first) {
                f.p(',');
            }
            f.g(varParam.type().elementType());
            f.p("... ");
            f.id(varParam.name());
        }

        f.o().p(')');
        if (_throws != null && !_throws.isEmpty()) {
            f.nl().i().p("throws").g(_throws).nl().o();
        }

        if (defaultValue != null) {
            f.p("default ");
            f.g(defaultValue);
        }
        if (body != null) {
            f.s(body);
        } else if (
                !outer.isInterface() && !outer.isAnnotationTypeDeclaration() && !mods.isAbstract() && !mods.isNative()) {
            // Print an empty body for non-native, non-abstract methods
            f.s(new Block());
        } else {
            f.p(';').nl();
        }
    }

    /**
     * @return the current modifiers of this method.
     *         Always return non-null valid object.
     */
    public Modifiers mods() {
        return mods;
    }

    /**
     * @deprecated use {@link #mods()}
     */
    public Modifiers getMods() {
        return mods;
    }

    protected CodeModel owner() {
        return outer.owner();
    }
}
