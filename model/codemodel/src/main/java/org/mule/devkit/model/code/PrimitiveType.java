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


/**
 * Java built-in primitive types.
 * <p/>
 * Instances of this class can be obtained as constants of {@link CodeModel},
 * such as {@link CodeModel#BOOLEAN}.
 */
public final class PrimitiveType extends Type {

    private final String typeName;
    private final CodeModel owner;
    /**
     * Corresponding wrapper class.
     * For example, this would be "java.lang.Short" for short.
     */
    private final TypeReference wrapperClass;

    PrimitiveType(CodeModel owner, String typeName, Class<?> wrapper) {
        this.owner = owner;
        this.typeName = typeName;
        this.wrapperClass = owner.ref(wrapper);
    }

    public CodeModel owner() {
        return owner;
    }

    public String fullName() {
        return typeName;
    }

    public String name() {
        return fullName();
    }

    public boolean isPrimitive() {
        return true;
    }

    private TypeReference arrayClass;

    public TypeReference array() {
        if (arrayClass == null) {
            arrayClass = new ArrayClass(owner, this);
        }
        return arrayClass;
    }

    /**
     * Obtains the wrapper class for this primitive type.
     * For example, this method returns a reference to java.lang.Integer
     * if this object represents int.
     */
    public TypeReference boxify() {
        return wrapperClass;
    }

    /**
     * @deprecated calling this method from {@link PrimitiveType}
     *             would be meaningless, since it's always guaranteed to
     *             return <tt>this</tt>.
     */
    public Type unboxify() {
        return this;
    }

    /**
     * @deprecated Use {@link #boxify()}.
     */
    public TypeReference getWrapperClass() {
        return boxify();
    }

    /**
     * Wraps an expression of this type to the corresponding wrapper class.
     * For example, if this class represents "float", this method will return
     * the expression <code>new Float(x)</code> for the paramter x.
     * <p/>
     * REVISIT: it's not clear how this method works for VOID.
     */
    public Expression wrap(Expression exp) {
        return ExpressionFactory._new(boxify()).arg(exp);
    }

    /**
     * Do the opposite of the wrap method.
     * <p/>
     * REVISIT: it's not clear how this method works for VOID.
     */
    public Expression unwrap(Expression exp) {
        // it just so happens that the unwrap method is always
        // things like "intValue" or "booleanValue".
        return exp.invoke(typeName + "Value");
    }

    public void generate(Formatter f) {
        f.p(typeName);
    }
}
