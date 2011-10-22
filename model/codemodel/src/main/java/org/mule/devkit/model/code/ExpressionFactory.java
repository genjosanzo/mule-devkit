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
 * Factory methods that generate various {@link Expression}s.
 */
public abstract class ExpressionFactory {

    /**
     * This class is not instanciable.
     */
    private ExpressionFactory() {
    }

    public static Expression assign(AssignmentTarget lhs, Expression rhs) {
        return new Assignment(lhs, rhs);
    }

    public static Expression assignPlus(AssignmentTarget lhs, Expression rhs) {
        return new Assignment(lhs, rhs, "+");
    }

    public static Invocation _new(TypeReference c) {
        return new Invocation(c);
    }

    public static Invocation _new(Type t) {
        return new Invocation(t);
    }

    public static Invocation invoke(String method) {
        return new Invocation((Expression) null, method);
    }

    public static Invocation invoke(Method method) {
        return new Invocation((Expression) null, method);
    }

    public static Invocation invoke(Expression lhs, Method method) {
        return new Invocation(lhs, method);
    }

    public static Invocation invoke(Expression lhs, String method) {
        return new Invocation(lhs, method);
    }

    public static FieldRef ref(String field) {
        return new FieldRef((Expression) null, field);
    }

    public static FieldRef ref(Expression lhs, Variable field) {
        return new FieldRef(lhs, field);
    }

    public static FieldRef ref(Expression lhs, String field) {
        return new FieldRef(lhs, field);
    }

    public static FieldRef refthis(String field) {
        return new FieldRef(null, field, true);
    }

    public static Expression dotclass(final TypeReference cl) {
        return new AbstractExpression() {
            public void generate(Formatter f) {
                TypeReference c;
                if (cl instanceof NarrowedClass) {
                    c = ((NarrowedClass) cl).basis;
                } else {
                    c = cl;
                }
                f.g(c).p(".class");
            }
        };
    }

    public static ArrayCompRef component(Expression lhs, Expression index) {
        return new ArrayCompRef(lhs, index);
    }

    public static Cast cast(Type type, Expression expr) {
        return new Cast(type, expr);
    }

    public static JArray newArray(Type type) {
        return newArray(type, null);
    }

    /**
     * Generates {@code new T[size]}.
     *
     * @param type The type of the array component. 'T' or {@code new T[size]}.
     */
    public static JArray newArray(Type type, Expression size) {
        // you cannot create an array whose component type is a generic
        return new JArray(type.erasure(), size);
    }

    /**
     * Generates {@code new T[size]}.
     *
     * @param type The type of the array component. 'T' or {@code new T[size]}.
     */
    public static JArray newArray(Type type, int size) {
        return newArray(type, lit(size));
    }


    private static final Expression __this = new Atom("this");

    /**
     * Returns a reference to "this", an implicit reference
     * to the current object.
     */
    public static Expression _this() {
        return __this;
    }

    private static final Expression __super = new Atom("super");

    /**
     * Returns a reference to "super", an implicit reference
     * to the super class.
     */
    public static Expression _super() {
        return __super;
    }


    /* -- Literals -- */

    private static final Expression __null = new Atom("null");

    public static Expression _null() {
        return __null;
    }

    /**
     * Boolean constant that represents <code>true</code>
     */
    public static final Expression TRUE = new Atom("true");

    /**
     * Boolean constant that represents <code>false</code>
     */
    public static final Expression FALSE = new Atom("false");

    public static Expression lit(boolean b) {
        return b ? TRUE : FALSE;
    }

    public static Expression lit(int n) {
        return new Atom(Integer.toString(n));
    }

    public static Expression lit(long n) {
        return new Atom(Long.toString(n) + "L");
    }

    public static Expression lit(float f) {
        if (f == Float.NEGATIVE_INFINITY) {
            return new Atom("java.lang.Float.NEGATIVE_INFINITY");
        } else if (f == Float.POSITIVE_INFINITY) {
            return new Atom("java.lang.Float.POSITIVE_INFINITY");
        } else if (Float.isNaN(f)) {
            return new Atom("java.lang.Float.NaN");
        } else {
            return new Atom(Float.toString(f) + "F");
        }
    }

    public static Expression lit(double d) {
        if (d == Double.NEGATIVE_INFINITY) {
            return new Atom("java.lang.Double.NEGATIVE_INFINITY");
        } else if (d == Double.POSITIVE_INFINITY) {
            return new Atom("java.lang.Double.POSITIVE_INFINITY");
        } else if (Double.isNaN(d)) {
            return new Atom("java.lang.Double.NaN");
        } else {
            return new Atom(Double.toString(d) + "D");
        }
    }

    static final String charEscape = "\b\t\n\f\r\"\'\\";
    static final String charMacro = "btnfr\"'\\";

    /**
     * Escapes the given string, then surrounds it by the specified
     * quotation mark.
     */
    public static String quotify(char quote, String s) {
        int n = s.length();
        StringBuilder sb = new StringBuilder(n + 2);
        sb.append(quote);
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            int j = charEscape.indexOf(c);
            if (j >= 0) {
                if ((quote == '"' && c == '\'') || (quote == '\'' && c == '"')) {
                    sb.append(c);
                } else {
                    sb.append('\\');
                    sb.append(charMacro.charAt(j));
                }
            } else {
                // technically Unicode escape shouldn't be done here,
                // for it's a lexical level handling.
                // 
                // However, various tools are so broken around this area,
                // so just to be on the safe side, it's better to do
                // the escaping here (regardless of the actual file encoding)
                //
                // see bug 
                if (c < 0x20 || 0x7E < c) {
                    // not printable. use Unicode escape
                    sb.append("\\u");
                    String hex = Integer.toHexString(((int) c) & 0xFFFF);
                    for (int k = hex.length(); k < 4; k++) {
                        sb.append('0');
                    }
                    sb.append(hex);
                } else {
                    sb.append(c);
                }
            }
        }
        sb.append(quote);
        return sb.toString();
    }

    public static Expression lit(char c) {
        return new Atom(quotify('\'', "" + c));
    }

    public static Expression lit(String s) {
        return new StringLiteral(s);
    }

    /**
     * Creates an expression directly from a source code fragment.
     * <p/>
     * <p/>
     * This method can be used as a short-cut to create a Expression.
     * For example, instead of <code>_a.gt(_b)</code>, you can write
     * it as: <code>ExpressionFactory.direct("a>b")</code>.
     * <p/>
     * <p/>
     * Be warned that there is a danger in using this method,
     * as it obfuscates the object model.
     */
    public static Expression direct(final String source) {
        return new AbstractExpression() {
            public void generate(Formatter f) {
                f.p('(').p(source).p(')');
            }
        };
    }
}

