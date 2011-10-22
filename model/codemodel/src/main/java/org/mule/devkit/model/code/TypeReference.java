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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a Java reference type, such as a class, an interface,
 * an enum, an array type, a parameterized type.
 * <p/>
 * <p/>
 * To be exact, this object represents an "use" of a reference type,
 * not necessarily a declaration of it, which is modeled as {@link DefinedClass}.
 */
public abstract class TypeReference extends Type {
    protected TypeReference(CodeModel _owner) {
        this._owner = _owner;
    }

    /**
     * Gets the name of this class.
     *
     * @return name of this class, without any qualification.
     *         For example, this method returns "String" for
     *         <code>java.lang.String</code>.
     */
    abstract public String name();

    /**
     * Gets the package to which this class belongs.
     * TODO: shall we move move this down?
     */
    abstract public Package _package();

    /**
     * Returns the class in which this class is nested, or <tt>null</tt> if
     * this is a top-level class.
     */
    public TypeReference outer() {
        return null;
    }

    private final CodeModel _owner;

    /**
     * Gets the CodeModel object to which this object belongs.
     */
    public final CodeModel owner() {
        return _owner;
    }

    /**
     * Gets the super class of this class.
     *
     * @return Returns the TypeReference representing the superclass of the
     *         entity (class or interface) represented by this {@link TypeReference}.
     *         Even if no super class is given explicitly or this {@link TypeReference}
     *         is not a class, this method still returns
     *         {@link TypeReference} for {@link Object}.
     *         If this TypeReference represents {@link Object}, return null.
     */
    abstract public TypeReference _extends();

    /**
     * Iterates all super interfaces directly implemented by
     * this class/interface.
     *
     * @return A non-null valid iterator that iterates all
     *         {@link TypeReference} objects that represents those interfaces
     *         implemented by this object.
     */
    abstract public Iterator<TypeReference> _implements();

    /**
     * Iterates all the type parameters of this class/interface.
     * <p/>
     * <p/>
     * For example, if this {@link TypeReference} represents
     * <code>Set&lt;T></code>, this method returns an array
     * that contains single {@link TypeVariable} for 'T'.
     */
    public TypeVariable[] typeParams() {
        return EMPTY_ARRAY;
    }

    /**
     * Sometimes useful reusable empty array.
     */
    protected static final TypeVariable[] EMPTY_ARRAY = new TypeVariable[0];

    /**
     * Checks if this object represents an interface.
     */
    abstract public boolean isInterface();

    /**
     * Checks if this class is an abstract class.
     */
    abstract public boolean isAbstract();

    /**
     * If this class represents one of the wrapper classes
     * defined in the java.lang package, return the corresponding
     * primitive type. Otherwise null.
     */
    public PrimitiveType getPrimitiveType() {
        return null;
    }

    /**
     * @deprecated calling this method from {@link TypeReference}
     *             would be meaningless, since it's always guaranteed to
     *             return <tt>this</tt>.
     */
    public TypeReference boxify() {
        return this;
    }

    public Type unboxify() {
        PrimitiveType pt = getPrimitiveType();
        return pt == null ? (Type) this : pt;
    }

    public TypeReference erasure() {
        return this;
    }

    /**
     * Checks the relationship between two classes.
     * <p/>
     * This method works in the same way as {@link Class#isAssignableFrom(Class)}
     * works. For example, baseClass.isAssignableFrom(derivedClass)==true.
     */
    public final boolean isAssignableFrom(TypeReference derived) {
        // to avoid the confusion, always use "this" explicitly in this method.

        // null can be assigned to any type.
        if (derived instanceof NullType) {
            return true;
        }

        if (this == derived) {
            return true;
        }

        // the only class that is assignable from an interface is
        // java.lang.Object
        if (this == _package().owner().ref(Object.class)) {
            return true;
        }

        TypeReference b = derived._extends();
        if (b != null && this.isAssignableFrom(b)) {
            return true;
        }

        if (this.isInterface()) {
            Iterator<TypeReference> itfs = derived._implements();
            while (itfs.hasNext()) {
                if (this.isAssignableFrom(itfs.next())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Gets the parameterization of the given base type.
     * <p/>
     * <p/>
     * For example, given the following
     * <pre><xmp>
     * interface Foo<T> extends List<List<T>> {}
     * interface Bar extends Foo<String> {}
     * </xmp></pre>
     * This method works like this:
     * <pre><xmp>
     * getBaseClass( Bar, List ) = List<List<String>
     * getBaseClass( Bar, Foo  ) = Foo<String>
     * getBaseClass( Foo<? extends Number>, Collection ) = Collection<List<? extends Number>>
     * getBaseClass( ArrayList<? extends BigInteger>, List ) = List<? extends BigInteger>
     * </xmp></pre>
     *
     * @param baseType The class whose parameterization we are interested in.
     * @return The use of {@code baseType} in {@code this} type.
     *         or null if the type is not assignable to the base type.
     */
    public final TypeReference getBaseClass(TypeReference baseType) {

        if (this.erasure().equals(baseType)) {
            return this;
        }

        TypeReference b = _extends();
        if (b != null) {
            TypeReference bc = b.getBaseClass(baseType);
            if (bc != null) {
                return bc;
            }
        }

        Iterator<TypeReference> itfs = _implements();
        while (itfs.hasNext()) {
            TypeReference bc = itfs.next().getBaseClass(baseType);
            if (bc != null) {
                return bc;
            }
        }

        return null;
    }

    public final TypeReference getBaseClass(Class<?> baseType) {
        return getBaseClass(owner().ref(baseType));
    }


    private TypeReference arrayClass;

    public TypeReference array() {
        if (arrayClass == null) {
            arrayClass = new ArrayClass(owner(), this);
        }
        return arrayClass;
    }

    /**
     * "Narrows" a generic class to a concrete class by specifying
     * a type argument.
     * <p/>
     * <p/>
     * <code>.narrow(X)</code> builds <code>Set&lt;X></code> from <code>Set</code>.
     */
    public TypeReference narrow(Class<?> clazz) {
        return narrow(owner().ref(clazz));
    }

    public TypeReference narrow(Class<?>... clazz) {
        TypeReference[] r = new TypeReference[clazz.length];
        for (int i = 0; i < clazz.length; i++) {
            r[i] = owner().ref(clazz[i]);
        }
        return narrow(r);
    }

    /**
     * "Narrows" a generic class to a concrete class by specifying
     * a type argument.
     * <p/>
     * <p/>
     * <code>.narrow(X)</code> builds <code>Set&lt;X></code> from <code>Set</code>.
     */
    public TypeReference narrow(TypeReference clazz) {
        return new NarrowedClass(this, clazz);
    }

    public TypeReference narrow(Type type) {
        return narrow(type.boxify());
    }

    public TypeReference narrow(TypeReference... clazz) {
        return new NarrowedClass(this, Arrays.asList(clazz.clone()));
    }

    public TypeReference narrow(List<? extends TypeReference> clazz) {
        return new NarrowedClass(this, new ArrayList<TypeReference>(clazz));
    }

    /**
     * If this class is parameterized, return the type parameter of the given index.
     */
    public List<TypeReference> getTypeParameters() {
        return Collections.emptyList();
    }

    /**
     * Returns true if this class is a parameterized class.
     */
    public final boolean isParameterized() {
        return erasure() != this;
    }

    /**
     * Create "? extends T" from T.
     *
     * @return never null
     */
    public final TypeReference wildcard() {
        return new TypeWildcard(this);
    }

    /**
     * Substitutes the type variables with their actual arguments.
     * <p/>
     * <p/>
     * For example, when this class is Map&lt;String,Map&lt;V>>,
     * (where V then doing
     * substituteParams( V, Integer ) returns a {@link TypeReference}
     * for <code>Map&lt;String,Map&lt;Integer>></code>.
     * <p/>
     * <p/>
     * This method needs to work recursively.
     */
    protected abstract TypeReference substituteParams(TypeVariable[] variables, List<TypeReference> bindings);

    public String toString() {
        return this.getClass().getName() + '(' + name() + ')';
    }


    public final Expression dotclass() {
        return ExpressionFactory.dotclass(this);
    }

    /**
     * Generates a static method invocation.
     */
    public final Invocation staticInvoke(Method method) {
        return new Invocation(this, method);
    }

    /**
     * Generates a static method invocation.
     */
    public final Invocation staticInvoke(String method) {
        return new Invocation(this, method);
    }

    /**
     * Static field reference.
     */
    public final FieldRef staticRef(String field) {
        return new FieldRef(this, field);
    }

    /**
     * Static field reference.
     */
    public final FieldRef staticRef(Variable field) {
        return new FieldRef(this, field);
    }

    public void generate(Formatter f) {
        f.t(this);
    }

    /**
     * Prints the class name in javadoc @link format.
     */
    void printLink(Formatter f) {
        f.p("{@link ").g(this).p('}');
    }
}
