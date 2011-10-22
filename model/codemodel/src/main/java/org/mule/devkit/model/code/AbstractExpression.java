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
 * Provides default implementations for {@link Expression}.
 */
public abstract class AbstractExpression implements Expression {
    //
    //
    // from Op
    //
    //
    public final Expression minus() {
        return Op.minus(this);
    }

    /**
     * Logical not <tt>'!x'</tt>.
     */
    public final Expression not() {
        return Op.not(this);
    }

    public final Expression complement() {
        return Op.complement(this);
    }

    public final Expression incr() {
        return Op.incr(this);
    }

    public final Expression decr() {
        return Op.decr(this);
    }

    public final Expression plus(Expression right) {
        return Op.plus(this, right);
    }

    public final Expression minus(Expression right) {
        return Op.minus(this, right);
    }

    public final Expression mul(Expression right) {
        return Op.mul(this, right);
    }

    public final Expression div(Expression right) {
        return Op.div(this, right);
    }

    public final Expression mod(Expression right) {
        return Op.mod(this, right);
    }

    public final Expression shl(Expression right) {
        return Op.shl(this, right);
    }

    public final Expression shr(Expression right) {
        return Op.shr(this, right);
    }

    public final Expression shrz(Expression right) {
        return Op.shrz(this, right);
    }

    public final Expression band(Expression right) {
        return Op.band(this, right);
    }

    public final Expression bor(Expression right) {
        return Op.bor(this, right);
    }

    public final Expression cand(Expression right) {
        return Op.cand(this, right);
    }

    public final Expression cor(Expression right) {
        return Op.cor(this, right);
    }

    public final Expression xor(Expression right) {
        return Op.xor(this, right);
    }

    public final Expression lt(Expression right) {
        return Op.lt(this, right);
    }

    public final Expression lte(Expression right) {
        return Op.lte(this, right);
    }

    public final Expression gt(Expression right) {
        return Op.gt(this, right);
    }

    public final Expression gte(Expression right) {
        return Op.gte(this, right);
    }

    public final Expression eq(Expression right) {
        return Op.eq(this, right);
    }

    public final Expression ne(Expression right) {
        return Op.ne(this, right);
    }

    public final Expression _instanceof(Type right) {
        return Op._instanceof(this, right);
    }

    //
    //
    // from ExpressionFactory
    //
    //
    public final Invocation invoke(Method method) {
        return ExpressionFactory.invoke(this, method);
    }

    public final Invocation invoke(String method) {
        return ExpressionFactory.invoke(this, method);
    }

    public final FieldRef ref(Variable field) {
        return ExpressionFactory.ref(this, field);
    }

    public final FieldRef ref(String field) {
        return ExpressionFactory.ref(this, field);
    }

    public final ArrayCompRef component(Expression index) {
        return ExpressionFactory.component(this, index);
    }
}
