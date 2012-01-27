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
 * TypeReference for generating expressions containing operators
 */

abstract public class Op {

    private Op() {
    }


    /**
     * Determine whether the top level of an expression involves an
     * operator.
     */
    static boolean hasTopOp(Expression e) {
        return (e instanceof UnaryOp) || (e instanceof BinaryOp);
    }

    /* -- Unary operators -- */

    static private class UnaryOp extends AbstractExpression {

        protected String op;
        protected Expression e;
        protected boolean opFirst = true;

        UnaryOp(String op, Expression e) {
            this.op = op;
            this.e = e;
        }

        UnaryOp(Expression e, String op) {
            this.op = op;
            this.e = e;
            opFirst = false;
        }

        public void generate(Formatter f) {
            if (opFirst) {
                f.p('(').p(op).g(e).p(')');
            } else {
                f.p('(').g(e).p(op).p(')');
            }
        }

    }

    public static Expression minus(Expression e) {
        return new UnaryOp("-", e);
    }

    /**
     * Logical not <tt>'!x'</tt>.
     */
    public static Expression not(Expression e) {
        if (e == ExpressionFactory.TRUE) {
            return ExpressionFactory.FALSE;
        }
        if (e == ExpressionFactory.FALSE) {
            return ExpressionFactory.TRUE;
        }
        return new UnaryOp("!", e);
    }

    public static Expression complement(Expression e) {
        return new UnaryOp("~", e);
    }

    static private class TightUnaryOp extends UnaryOp {

        TightUnaryOp(Expression e, String op) {
            super(e, op);
        }

        public void generate(Formatter f) {
            if (opFirst) {
                f.p(op).g(e);
            } else {
                f.g(e).p(op);
            }
        }

    }

    public static Expression incr(Expression e) {
        return new TightUnaryOp(e, "++");
    }

    public static Expression decr(Expression e) {
        return new TightUnaryOp(e, "--");
    }


    /* -- Binary operators -- */

    static private class BinaryOp extends AbstractExpression {

        String op;
        Expression left;
        Generable right;

        BinaryOp(String op, Expression left, Generable right) {
            this.left = left;
            this.op = op;
            this.right = right;
        }

        public void generate(Formatter f) {
            f.p('(');
            if( left instanceof Assignment ) {
                f.p('(').g(left).p(')');
            } else {
                f.g(left);
            }
            f.p(op).g(right).p(')');
        }

    }

    public static Expression plus(Expression left, Expression right) {
        return new BinaryOp("+", left, right);
    }

    public static Expression minus(Expression left, Expression right) {
        return new BinaryOp("-", left, right);
    }

    public static Expression mul(Expression left, Expression right) {
        return new BinaryOp("*", left, right);
    }

    public static Expression div(Expression left, Expression right) {
        return new BinaryOp("/", left, right);
    }

    public static Expression mod(Expression left, Expression right) {
        return new BinaryOp("%", left, right);
    }

    public static Expression shl(Expression left, Expression right) {
        return new BinaryOp("<<", left, right);
    }

    public static Expression shr(Expression left, Expression right) {
        return new BinaryOp(">>", left, right);
    }

    public static Expression shrz(Expression left, Expression right) {
        return new BinaryOp(">>>", left, right);
    }

    public static Expression band(Expression left, Expression right) {
        return new BinaryOp("&", left, right);
    }

    public static Expression bor(Expression left, Expression right) {
        return new BinaryOp("|", left, right);
    }

    public static Expression cand(Expression left, Expression right) {
        if (left == ExpressionFactory.TRUE) {
            return right;
        }
        if (right == ExpressionFactory.TRUE) {
            return left;
        }
        if (left == ExpressionFactory.FALSE) {
            return left;    // ExpressionFactory.FALSE
        }
        if (right == ExpressionFactory.FALSE) {
            return right;   // ExpressionFactory.FALSE
        }
        return new BinaryOp("&&", left, right);
    }

    public static Expression cor(Expression left, Expression right) {
        if (left == ExpressionFactory.TRUE) {
            return left;    // ExpressionFactory.TRUE
        }
        if (right == ExpressionFactory.TRUE) {
            return right;   // ExpressionFactory.FALSE
        }
        if (left == ExpressionFactory.FALSE) {
            return right;
        }
        if (right == ExpressionFactory.FALSE) {
            return left;
        }
        return new BinaryOp("||", left, right);
    }

    public static Expression xor(Expression left, Expression right) {
        return new BinaryOp("^", left, right);
    }

    public static Expression lt(Expression left, Expression right) {
        return new BinaryOp("<", left, right);
    }

    public static Expression lte(Expression left, Expression right) {
        return new BinaryOp("<=", left, right);
    }

    public static Expression gt(Expression left, Expression right) {
        return new BinaryOp(">", left, right);
    }

    public static Expression gte(Expression left, Expression right) {
        return new BinaryOp(">=", left, right);
    }

    public static Expression eq(Expression left, Expression right) {
        return new BinaryOp("==", left, right);
    }

    public static Expression ne(Expression left, Expression right) {
        return new BinaryOp("!=", left, right);
    }

    public static Expression _instanceof(Expression left, Type right) {
        return new BinaryOp("instanceof", left, right);
    }

    /* -- Ternary operators -- */

    static private class TernaryOp extends AbstractExpression {

        String op1;
        String op2;
        Expression e1;
        Expression e2;
        Expression e3;

        TernaryOp(String op1, String op2,
                  Expression e1, Expression e2, Expression e3) {
            this.e1 = e1;
            this.op1 = op1;
            this.e2 = e2;
            this.op2 = op2;
            this.e3 = e3;
        }

        public void generate(Formatter f) {
            f.p('(').g(e1).p(op1).g(e2).p(op2).g(e3).p(')');
        }

    }

    public static Expression cond(Expression cond,
                                  Expression ifTrue, Expression ifFalse) {
        return new TernaryOp("?", ":", cond, ifTrue, ifFalse);
    }

}
