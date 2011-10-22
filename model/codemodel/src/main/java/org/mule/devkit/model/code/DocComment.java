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

import java.util.HashMap;
import java.util.Map;

/**
 * JavaDoc comment.
 * <p/>
 * <p/>
 * A javadoc comment consists of multiple parts. There's the main part (that comes the first in
 * in the comment section), then the parameter parts (@param), the return part (@return),
 * and the throws parts (@throws).
 * <p/>
 * TODO: it would be nice if we have JComment class and we can derive this class from there.
 */
public class DocComment extends CommentPart implements Generable {

    private static final long serialVersionUID = 1L;

    /**
     * list of @param tags
     */
    private final Map<String, CommentPart> atParams = new HashMap<String, CommentPart>();

    /**
     * list of xdoclets
     */
    private final Map<String, Map<String, String>> atXdoclets = new HashMap<String, Map<String, String>>();

    /**
     * list of @throws tags
     */
    private final Map<TypeReference, CommentPart> atThrows = new HashMap<TypeReference, CommentPart>();

    /**
     * The @return tag part.
     */
    private CommentPart atReturn = null;

    /**
     * The @deprecated tag
     */
    private CommentPart atDeprecated = null;

    private final CodeModel owner;


    public DocComment(CodeModel owner) {
        this.owner = owner;
    }

    public DocComment append(Object o) {
        add(o);
        return this;
    }

    /**
     * Append a text to a @param tag to the javadoc
     */
    public CommentPart addParam(String param) {
        CommentPart p = atParams.get(param);
        if (p == null) {
            atParams.put(param, p = new CommentPart());
        }
        return p;
    }

    /**
     * Append a text to an @param tag.
     */
    public CommentPart addParam(Variable param) {
        return addParam(param.name());
    }


    /**
     * add an @throws tag to the javadoc
     */
    public CommentPart addThrows(Class<? extends Throwable> exception) {
        return addThrows(owner.ref(exception));
    }

    /**
     * add an @throws tag to the javadoc
     */
    public CommentPart addThrows(TypeReference exception) {
        CommentPart p = atThrows.get(exception);
        if (p == null) {
            atThrows.put(exception, p = new CommentPart());
        }
        return p;
    }

    /**
     * Appends a text to @return tag.
     */
    public CommentPart addReturn(String ret) {
        if (atReturn == null) {
            atReturn = new CommentPart();
        }
        return atReturn;
    }

    /**
     * add an @deprecated tag to the javadoc, with the associated message.
     */
    public CommentPart addDeprecated() {
        if (atDeprecated == null) {
            atDeprecated = new CommentPart();
        }
        return atDeprecated;
    }

    /**
     * add an xdoclet.
     */
    public Map<String, String> addXdoclet(String name) {
        Map<String, String> p = atXdoclets.get(name);
        if (p == null) {
            atXdoclets.put(name, p = new HashMap<String, String>());
        }
        return p;
    }

    /**
     * add an xdoclet.
     */
    public Map<String, String> addXdoclet(String name, Map<String, String> attributes) {
        Map<String, String> p = atXdoclets.get(name);
        if (p == null) {
            atXdoclets.put(name, p = new HashMap<String, String>());
        }
        p.putAll(attributes);
        return p;
    }

    /**
     * add an xdoclet.
     */
    public Map<String, String> addXdoclet(String name, String attribute, String value) {
        Map<String, String> p = atXdoclets.get(name);
        if (p == null) {
            atXdoclets.put(name, p = new HashMap<String, String>());
        }
        p.put(attribute, value);
        return p;
    }

    public void generate(Formatter f) {
        // I realized that we can't use StringTokenizer because
        // this will recognize multiple \n as one token.

        f.p("/**").nl();

        format(f, " * ");

        f.p(" * ").nl();
        for (Map.Entry<String, CommentPart> e : atParams.entrySet()) {
            f.p(" * @param ").p(e.getKey()).nl();
            e.getValue().format(f, INDENT);
        }
        if (atReturn != null) {
            f.p(" * @return").nl();
            atReturn.format(f, INDENT);
        }
        for (Map.Entry<TypeReference, CommentPart> e : atThrows.entrySet()) {
            f.p(" * @throws ").t(e.getKey()).nl();
            e.getValue().format(f, INDENT);
        }
        if (atDeprecated != null) {
            f.p(" * @deprecated").nl();
            atDeprecated.format(f, INDENT);
        }
        for (Map.Entry<String, Map<String, String>> e : atXdoclets.entrySet()) {
            f.p(" * @").p(e.getKey());
            if (e.getValue() != null) {
                for (Map.Entry<String, String> a : e.getValue().entrySet()) {
                    f.p(" ").p(a.getKey()).p("= \"").p(a.getValue()).p("\"");
                }
            }
            f.nl();
        }
        f.p(" */").nl();
    }

    private static final String INDENT = " *     ";
}

