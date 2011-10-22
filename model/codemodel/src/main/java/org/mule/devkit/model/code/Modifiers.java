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

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Modifier groups.
 */
public class Modifiers implements Generable {

    //
// mask
//
    private static int VAR = Modifier.FINAL;
    private static int FIELD = (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED
            | Modifier.STATIC | Modifier.FINAL
            | Modifier.TRANSIENT | Modifier.VOLATILE);
    private static int METHOD = (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED | Modifier.FINAL
            | Modifier.ABSTRACT | Modifier.STATIC | Modifier.NATIVE | Modifier.SYNCHRONIZED);
    private static int CLASS = (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED
            | Modifier.STATIC | Modifier.FINAL | Modifier.ABSTRACT);
    private static int INTERFACE = Modifier.PUBLIC;
    /**
     * bit-packed representation of modifiers.
     */
    private int mods;

    private Modifiers(int mods) {
        this.mods = mods;
    }

    /**
     * Gets the bit-packed representaion of modifiers.
     */
    public int getValue() {
        return mods;
    }

    private static void check(int mods, int legal, String what) {
        if ((mods & ~legal) != 0) {
            throw new IllegalArgumentException("Illegal modifiers for "
                    + what + ": "
                    + new Modifiers(mods).toString());
        }
        /* ## check for illegal combinations too */
    }

    static Modifiers forVar(int mods) {
        check(mods, VAR, "variable");
        return new Modifiers(mods);
    }

    static Modifiers forField(int mods) {
        check(mods, FIELD, "field");
        return new Modifiers(mods);
    }

    static Modifiers forMethod(int mods) {
        check(mods, METHOD, "method");
        return new Modifiers(mods);
    }

    static Modifiers forClass(int mods) {
        check(mods, CLASS, "class");
        return new Modifiers(mods);
    }

    static Modifiers forInterface(int mods) {
        check(mods, INTERFACE, "class");
        return new Modifiers(mods);
    }

    public boolean isAbstract() {
        return (mods & Modifier.ABSTRACT) != 0;
    }

    public boolean isNative() {
        return (mods & Modifier.NATIVE) != 0;
    }

    public boolean isSynchronized() {
        return (mods & Modifier.SYNCHRONIZED) != 0;
    }

    public void setSynchronized(boolean newValue) {
        setFlag(Modifier.SYNCHRONIZED, newValue);
    }

    public void setPrivate() {
        setFlag(Modifier.PUBLIC, false);
        setFlag(Modifier.PROTECTED, false);
        setFlag(Modifier.PRIVATE, true);
    }

    public void setProtected() {
        setFlag(Modifier.PUBLIC, false);
        setFlag(Modifier.PROTECTED, true);
        setFlag(Modifier.PRIVATE, false);
    }

    public void setPublic() {
        setFlag(Modifier.PUBLIC, true);
        setFlag(Modifier.PROTECTED, false);
        setFlag(Modifier.PRIVATE, false);
    }

    public void setFinal(boolean newValue) {
        setFlag(Modifier.FINAL, newValue);
    }

    private void setFlag(int bit, boolean newValue) {
        mods = (mods & ~bit) | (newValue ? bit : 0);
    }

    public void generate(Formatter f) {
        if ((mods & Modifier.PUBLIC) != 0) {
            f.p("public");
        }
        if ((mods & Modifier.PROTECTED) != 0) {
            f.p("protected");
        }
        if ((mods & Modifier.PRIVATE) != 0) {
            f.p("private");
        }
        if ((mods & Modifier.FINAL) != 0) {
            f.p("final");
        }
        if ((mods & Modifier.STATIC) != 0) {
            f.p("static");
        }
        if ((mods & Modifier.ABSTRACT) != 0) {
            f.p("abstract");
        }
        if ((mods & Modifier.NATIVE) != 0) {
            f.p("native");
        }
        if ((mods & Modifier.SYNCHRONIZED) != 0) {
            f.p("synchronized");
        }
        if ((mods & Modifier.TRANSIENT) != 0) {
            f.p("transient");
        }
        if ((mods & Modifier.VOLATILE) != 0) {
            f.p("volatile");
        }
    }

    @Override
    public String toString() {
        StringWriter s = new StringWriter();
        Formatter f = new Formatter(new PrintWriter(s));
        this.generate(f);
        return s.toString();
    }
}
