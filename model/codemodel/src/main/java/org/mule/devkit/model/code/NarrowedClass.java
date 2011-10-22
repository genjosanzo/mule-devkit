/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 * Represents X&lt;Y>.
 * <p/>
 * TODO: consider separating the decl and the use.
 *
 * @author Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
class NarrowedClass extends TypeReference {
    /**
     * A generic class with type parameters.
     */
    final TypeReference basis;
    /**
     * Arguments to those parameters.
     */
    private final List<TypeReference> args;

    NarrowedClass(TypeReference basis, TypeReference arg) {
        this(basis, Collections.singletonList(arg));
    }

    NarrowedClass(TypeReference basis, List<TypeReference> args) {
        super(basis.owner());
        this.basis = basis;
        assert !(basis instanceof NarrowedClass);
        this.args = args;
    }

    @Override
    public TypeReference narrow(TypeReference clazz) {
        List<TypeReference> newArgs = new ArrayList<TypeReference>(args);
        newArgs.add(clazz);
        return new NarrowedClass(basis, newArgs);
    }

    @Override
    public TypeReference narrow(TypeReference... clazz) {
        List<TypeReference> newArgs = new ArrayList<TypeReference>(args);
        newArgs.addAll(Arrays.asList(clazz));
        return new NarrowedClass(basis, newArgs);
    }

    public String name() {
        StringBuilder buf = new StringBuilder();
        buf.append(basis.name());
        buf.append('<');
        boolean first = true;
        for (TypeReference c : args) {
            if (first) {
                first = false;
            } else {
                buf.append(',');
            }
            buf.append(c.name());
        }
        buf.append('>');
        return buf.toString();
    }

    public String fullName() {
        StringBuilder buf = new StringBuilder();
        buf.append(basis.fullName());
        buf.append('<');
        boolean first = true;
        for (TypeReference c : args) {
            if (first) {
                first = false;
            } else {
                buf.append(',');
            }
            buf.append(c.fullName());
        }
        buf.append('>');
        return buf.toString();
    }

    @Override
    public String binaryName() {
        StringBuilder buf = new StringBuilder();
        buf.append(basis.binaryName());
        buf.append('<');
        boolean first = true;
        for (TypeReference c : args) {
            if (first) {
                first = false;
            } else {
                buf.append(',');
            }
            buf.append(c.binaryName());
        }
        buf.append('>');
        return buf.toString();
    }

    @Override
    public void generate(Formatter f) {
        f.t(basis).p('<').g(args).p(Formatter.CLOSE_TYPE_ARGS);
    }

    @Override
    void printLink(Formatter f) {
        basis.printLink(f);
        f.p("{@code <}");
        boolean first = true;
        for (TypeReference c : args) {
            if (first) {
                first = false;
            } else {
                f.p(',');
            }
            c.printLink(f);
        }
        f.p("{@code >}");
    }

    public Package _package() {
        return basis._package();
    }

    public TypeReference _extends() {
        TypeReference base = basis._extends();
        if (base == null) {
            return base;
        }
        return base.substituteParams(basis.typeParams(), args);
    }

    public Iterator<TypeReference> _implements() {
        return new Iterator<TypeReference>() {
            private final Iterator<TypeReference> core = basis._implements();

            public void remove() {
                core.remove();
            }

            public TypeReference next() {
                return core.next().substituteParams(basis.typeParams(), args);
            }

            public boolean hasNext() {
                return core.hasNext();
            }
        };
    }

    @Override
    public TypeReference erasure() {
        return basis;
    }

    public boolean isInterface() {
        return basis.isInterface();
    }

    public boolean isAbstract() {
        return basis.isAbstract();
    }

    @Override
    public boolean isArray() {
        return false;
    }


    //
    // Equality is based on value
    //

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NarrowedClass)) {
            return false;
        }
        return fullName().equals(((TypeReference) obj).fullName());
    }

    @Override
    public int hashCode() {
        return fullName().hashCode();
    }

    protected TypeReference substituteParams(TypeVariable[] variables, List<TypeReference> bindings) {
        TypeReference b = basis.substituteParams(variables, bindings);
        boolean different = b != basis;

        List<TypeReference> clazz = new ArrayList<TypeReference>(args.size());
        for (int i = 0; i < clazz.size(); i++) {
            TypeReference c = args.get(i).substituteParams(variables, bindings);
            clazz.set(i, c);
            different |= c != args.get(i);
        }

        if (different) {
            return new NarrowedClass(b, clazz);
        } else {
            return this;
        }
    }

    @Override
    public List<TypeReference> getTypeParameters() {
        return args;
    }
}
