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
import java.util.List;


/**
 * For statement
 */

public class ForLoop implements Statement {

    private List<Object> inits = new ArrayList<Object>();
    private Expression test = null;
    private List<Expression> updates = new ArrayList<Expression>();
    private Block body = null;

    public Variable init(int mods, Type type, String var, Expression e) {
        Variable v = new Variable(Modifiers.forVar(mods), type, var, e);
        inits.add(v);
        return v;
    }

    public Variable init(Type type, String var, Expression e) {
        return init(Modifier.NONE, type, var, e);
    }

    public void init(Variable v, Expression e) {
        inits.add(ExpressionFactory.assign(v, e));
    }

    public void test(Expression e) {
        this.test = e;
    }

    public void update(Expression e) {
        updates.add(e);
    }

    public Block body() {
        if (body == null) {
            body = new Block();
        }
        return body;
    }

    public void state(Formatter f) {
        f.p("for (");
        boolean first = true;
        for (Object o : inits) {
            if (!first) {
                f.p(',');
            }
            if (o instanceof Variable) {
                f.b((Variable) o);
            } else {
                f.g((Expression) o);
            }
            first = false;
        }
        f.p(';').g(test).p(';').g(updates).p(')');
        if (body != null) {
            f.g(body).nl();
        } else {
            f.p(';').nl();
        }
    }

}
