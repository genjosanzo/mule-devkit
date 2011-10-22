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
 * A Java expression.
 * <p/>
 * <p/>
 * Unlike most of CodeModel, JExpressions are built bottom-up (
 * meaning you start from leaves and then gradually build compliated expressions
 * by combining them.)
 * <p/>
 * <p/>
 * {@link Expression} defines a series of composer methods,
 * which returns a complicated expression (by often taking other {@link Expression}s
 * as parameters.
 * For example, you can build "5+2" by
 * <tt>ExpressionFactory.lit(5).add(ExpressionFactory.lit(2))</tt>
 */
public interface Expression extends Generable {
    /**
     * Returns "-[this]" from "[this]".
     */
    Expression minus();

    /**
     * Returns "![this]" from "[this]".
     */
    Expression not();

    /**
     * Returns "~[this]" from "[this]".
     */
    Expression complement();

    /**
     * Returns "[this]++" from "[this]".
     */
    Expression incr();

    /**
     * Returns "[this]--" from "[this]".
     */
    Expression decr();

    /**
     * Returns "[this]+[right]"
     */
    Expression plus(Expression right);

    /**
     * Returns "[this]-[right]"
     */
    Expression minus(Expression right);

    /**
     * Returns "[this]*[right]"
     */
    Expression mul(Expression right);

    /**
     * Returns "[this]/[right]"
     */
    Expression div(Expression right);

    /**
     * Returns "[this]%[right]"
     */
    Expression mod(Expression right);

    /**
     * Returns "[this]&lt;&lt;[right]"
     */
    Expression shl(Expression right);

    /**
     * Returns "[this]>>[right]"
     */
    Expression shr(Expression right);

    /**
     * Returns "[this]>>>[right]"
     */
    Expression shrz(Expression right);

    /**
     * Bit-wise AND '&amp;'.
     */
    Expression band(Expression right);

    /**
     * Bit-wise OR '|'.
     */
    Expression bor(Expression right);

    /**
     * Logical AND '&amp;&amp;'.
     */
    Expression cand(Expression right);

    /**
     * Logical OR '||'.
     */
    Expression cor(Expression right);

    Expression xor(Expression right);

    Expression lt(Expression right);

    Expression lte(Expression right);

    Expression gt(Expression right);

    Expression gte(Expression right);

    Expression eq(Expression right);

    Expression ne(Expression right);

    /**
     * Returns "[this] instanceof [right]"
     */
    Expression _instanceof(Type right);

    /**
     * Returns "[this].[method]".
     * <p/>
     * Arguments shall be added to the returned {@link Invocation} object.
     */
    Invocation invoke(Method method);

    /**
     * Returns "[this].[method]".
     * <p/>
     * Arguments shall be added to the returned {@link Invocation} object.
     */
    Invocation invoke(String method);

    FieldRef ref(Variable field);

    FieldRef ref(String field);

    ArrayCompRef component(Expression index);
}
