/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright 2011 MuleSoft, Inc.
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

import org.mule.devkit.model.code.writer.FileCodeWriter;
import org.mule.devkit.model.code.writer.ProgressCodeWriter;

import javax.lang.model.type.TypeMirror;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Root of the code DOM.
 * <p/>
 * <p/>
 * Here's your typical CodeModel application.
 * <p/>
 * <pre>
 * CodeModel cm = new CodeModel();
 *
 * // generate source code by populating the 'cm' tree.
 * cm._class(...);
 * ...
 *
 * // write them out
 * cm.build(new File("."));
 * </pre>
 * <p/>
 * <p/>
 * Every CodeModel node is always owned by one {@link CodeModel} object
 * at any given time (which can be often accesesd by the <tt>owner()</tt> method.)
 * <p/>
 * As such, when you generate Java code, most of the operation works
 * in a top-down fashion. For example, you create a class from {@link CodeModel},
 * which gives you a {@link DefinedClass}. Then you invoke a method on it
 * to generate a new method, which gives you {@link Method}, and so on.
 * <p/>
 * There are a few exceptions to this, most notably building {@link Expression}s,
 * but generally you work with CodeModel in a top-down fashion.
 * <p/>
 * Because of this design, most of the CodeModel classes aren't directly instanciable.
 * <p/>
 * <p/>
 * <h2>Where to go from here?</h2>
 * <p/>
 * Most of the time you'd want to populate new type definitions in a {@link CodeModel}.
 * See {@link #_class(String, ClassType)}.
 */
public final class CodeModel {

    /**
     * The packages that this JCodeWriter contains.
     */
    private HashMap<String, Package> packages = new HashMap<String, Package>();

    /**
     * All JReferencedClasses are pooled here.
     */
    private final HashMap<Class<?>, ReferencedClass> refClasses = new HashMap<Class<?>, ReferencedClass>();

    private CodeWriter codeWriter;

    /**
     * Obtains a reference to the special "null" type.
     */
    public final NullType NULL = new NullType(this);
    // primitive types 
    public final PrimitiveType VOID = new PrimitiveType(this, "void", Void.class);
    public final PrimitiveType BOOLEAN = new PrimitiveType(this, "boolean", Boolean.class);
    public final PrimitiveType BYTE = new PrimitiveType(this, "byte", Byte.class);
    public final PrimitiveType SHORT = new PrimitiveType(this, "short", Short.class);
    public final PrimitiveType CHAR = new PrimitiveType(this, "char", Character.class);
    public final PrimitiveType INT = new PrimitiveType(this, "int", Integer.class);
    public final PrimitiveType FLOAT = new PrimitiveType(this, "float", Float.class);
    public final PrimitiveType LONG = new PrimitiveType(this, "long", Long.class);
    public final PrimitiveType DOUBLE = new PrimitiveType(this, "double", Double.class);

    /**
     * If the flag is true, we will consider two classes "Foo" and "foo"
     * as a collision.
     */
    protected static final boolean isCaseSensitiveFileSystem = getFileSystemCaseSensitivity();

    private static boolean getFileSystemCaseSensitivity() {
        try {
            // let the system property override, in case the user really
            // wants to override.
            if (System.getProperty("com.sun.codemodel.FileSystemCaseSensitive") != null) {
                return true;
            }
        } catch (Exception e) {
        }

        // on Unix, it's case sensitive.
        return (File.separatorChar == '/');
    }


    public CodeModel(CodeWriter codeWriter) {
        this.codeWriter = codeWriter;
    }

    /**
     * Add a package to the list of packages to be generated
     *
     * @param name Name of the package. Use "" to indicate the root package.
     * @return Newly generated package
     */
    public Package _package(String name) {
        Package p = packages.get(name);
        if (p == null) {
            p = new Package(name, this);
            packages.put(name, p);
        }
        return p;
    }

    public final Package rootPackage() {
        return _package("");
    }

    /**
     * Returns an iterator that walks the packages defined using this code
     * writer.
     */
    public Iterator<Package> packages() {
        return packages.values().iterator();
    }

    /**
     * Creates a new generated class.
     *
     * @throws ClassAlreadyExistsException When the specified class/interface was already created.
     */
    public DefinedClass _class(String fullyqualifiedName) throws ClassAlreadyExistsException {
        return _class(fullyqualifiedName, ClassType.CLASS);
    }

    /**
     * Creates a dummy, unknown {@link TypeReference} that represents a given name.
     * <p/>
     * <p/>
     * This method is useful when the code generation needs to include the user-specified
     * class that may or may not exist, and only thing known about it is a class name.
     */
    public TypeReference directClass(String name) {
        return new DirectClass(this, name);
    }

    /**
     * Creates a new generated class.
     *
     * @throws ClassAlreadyExistsException When the specified class/interface was already created.
     */
    public DefinedClass _class(int mods, String fullyqualifiedName, ClassType t) throws ClassAlreadyExistsException {
        int idx = fullyqualifiedName.lastIndexOf('.');
        if (idx < 0) {
            return rootPackage()._class(fullyqualifiedName);
        } else {
            return _package(fullyqualifiedName.substring(0, idx))
                    ._class(mods, fullyqualifiedName.substring(idx + 1), t);
        }
    }

    /**
     * Creates a new generated class.
     *
     * @throws ClassAlreadyExistsException When the specified class/interface was already created.
     */
    public DefinedClass _class(String fullyqualifiedName, ClassType t) throws ClassAlreadyExistsException {
        return _class(Modifier.PUBLIC, fullyqualifiedName, t);
    }

    /**
     * Gets a reference to the already created generated class.
     *
     * @return null
     *         If the class is not yet created.
     * @see Package#_getClass(String)
     */
    public DefinedClass _getClass(String fullyQualifiedName) {
        int idx = fullyQualifiedName.lastIndexOf('.');
        if (idx < 0) {
            return rootPackage()._getClass(fullyQualifiedName);
        } else {
            return _package(fullyQualifiedName.substring(0, idx))
                    ._getClass(fullyQualifiedName.substring(idx + 1));
        }
    }

    /**
     * Creates a new anonymous class.
     *
     * @deprecated The naming convention doesn't match the rest of the CodeModel.
     *             Use {@link #anonymousClass(TypeReference)} instead.
     */
    public DefinedClass newAnonymousClass(TypeReference baseType) {
        return new AnonymousClass(baseType);
    }

    /**
     * Creates a new anonymous class.
     */
    public DefinedClass anonymousClass(TypeReference baseType) {
        return new AnonymousClass(baseType);
    }

    public DefinedClass anonymousClass(Class<?> baseType) {
        return anonymousClass(ref(baseType));
    }

    /**
     * Generates Java source code.
     * A convenience method for <code>build(destDir,destDir,System.out)</code>.
     *
     * @param status  if non-null, progress indication will be sent to this stream.
     * @param destDir source files are generated into this directory.
     */
    private void build(File destDir, PrintStream status) throws IOException {
        build(destDir, destDir, status);
    }

    /**
     * Generates Java source code.
     * A convenience method that calls {@link #build(CodeWriter, CodeWriter)}.
     *
     * @param status      if non-null, progress indication will be sent to this stream.
     * @param srcDir      Java source files are generated into this directory.
     * @param resourceDir Other resource files are generated into this directory.
     */
    private void build(File srcDir, File resourceDir, PrintStream status) throws IOException {
        CodeWriter src = new FileCodeWriter(srcDir);
        CodeWriter res = new FileCodeWriter(resourceDir);
        if (status != null) {
            src = new ProgressCodeWriter(src, status);
            res = new ProgressCodeWriter(res, status);
        }
        build(src, res);
    }

    /**
     * A convenience method for <code>build(destDir,System.out)</code>.
     */
    private void build(File destDir) throws IOException {
        build(destDir, System.out);
    }

    /**
     * A convenience method for <code>build(srcDir,resourceDir,System.out)</code>.
     */
    private void build(File srcDir, File resourceDir) throws IOException {
        build(srcDir, resourceDir, System.out);
    }

    public void build() throws IOException {
        build(this.codeWriter);
    }

    /**
     * A convenience method for <code>build(out,out)</code>.
     */
    private void build(CodeWriter out) throws IOException {
        build(out, out);
    }

    /**
     * Generates Java source code.
     */
    private void build(CodeWriter source, CodeWriter resource) throws IOException {
        Package[] pkgs = packages.values().toArray(new Package[packages.size()]);
        // avoid concurrent modification exception
        for (Package pkg : pkgs) {
            pkg.build(source, resource);
        }
        source.close();
        resource.close();
    }

    /**
     * Returns the number of files to be generated if
     * {@link #build} is invoked now.
     */
    public int countArtifacts() {
        int r = 0;
        Package[] pkgs = packages.values().toArray(new Package[packages.size()]);
        // avoid concurrent modification exception
        for (Package pkg : pkgs) {
            r += pkg.countArtifacts();
        }
        return r;
    }

    public Type ref(TypeMirror typeMirror) {
        return ref(typeMirror.toString());
    }

    /**
     * Obtains a reference to an existing class from its Class object.
     * <p/>
     * <p/>
     * The parameter may not be primitive.
     *
     * @see #_ref(Class) for the version that handles more cases.
     */
    public TypeReference ref(Class<?> clazz) {
        ReferencedClass jrc = (ReferencedClass) refClasses.get(clazz);
        if (jrc == null) {
            if (clazz.isPrimitive()) {
                throw new IllegalArgumentException(clazz + " is a primitive");
            }
            if (clazz.isArray()) {
                return new ArrayClass(this, _ref(clazz.getComponentType()));
            } else {
                jrc = new ReferencedClass(clazz);
                refClasses.put(clazz, jrc);
            }
        }
        return jrc;
    }

    public Type _ref(Class<?> c) {
        if (c.isPrimitive()) {
            return Type.parse(this, c.getName());
        } else {
            return ref(c);
        }
    }

    /**
     * Obtains a reference to an existing class from its fully-qualified
     * class name.
     * <p/>
     * <p/>
     * First, this method attempts to load the class of the given name.
     * If that fails, we assume that the class is derived straight from
     * {@link Object}, and return a {@link TypeReference}.
     */
    public Type ref(String fullyQualifiedClassName) {
        try {
            return parseType(fullyQualifiedClassName);
        } catch (ClassNotFoundException e) {
            // fall through
        }
        return refClass(fullyQualifiedClassName);
    }

    private TypeReference refClass(String fullyQualifiedClassName) {
        try {
            // try the context class loader first
            return ref(Thread.currentThread().getContextClassLoader().loadClass(fullyQualifiedClassName));
        } catch (ClassNotFoundException e) {
            // fall through
        }
        // then the default mechanism.
        try {
            return ref(Class.forName(fullyQualifiedClassName));
        } catch (ClassNotFoundException e1) {
            // fall through
        }

        // assume it's not visible to us.
        return new DirectClass(this, fullyQualifiedClassName);
    }

    /**
     * Cached for {@link #wildcard()}.
     */
    private TypeReference wildcard;

    /**
     * Gets a {@link TypeReference} representation for "?",
     * which is equivalent to "? extends Object".
     */
    public TypeReference wildcard() {
        if (wildcard == null) {
            wildcard = ref(Object.class).wildcard();
        }
        return wildcard;
    }

    /**
     * Obtains a type object from a type name.
     * <p/>
     * <p/>
     * This method handles primitive types, arrays, and existing {@link Class}es.
     *
     * @throws ClassNotFoundException If the specified type is not found.
     */
    public Type parseType(String name) throws ClassNotFoundException {
        // array
        if (name.endsWith("[]")) {
            return parseType(name.substring(0, name.length() - 2)).array();
        }

        // try primitive type
        try {
            return Type.parse(this, name);
        } catch (IllegalArgumentException e) {
            ;
        }

        // existing class
        return new TypeNameParser(name).parseTypeName();
    }

    private final class TypeNameParser {
        private final String s;
        private int idx;

        public TypeNameParser(String s) {
            this.s = s;
        }

        /**
         * Parses a type name token T (which can be potentially of the form Tr&ly;T1,T2,...>,
         * or "? extends/super T".)
         *
         * @return the index of the character next to T.
         */
        TypeReference parseTypeName() throws ClassNotFoundException {
            int start = idx;

            if (s.charAt(idx) == '?') {
                // wildcard
                idx++;
                ws();
                String head = s.substring(idx);
                if (head.startsWith("extends")) {
                    idx += 7;
                    ws();
                    return parseTypeName().wildcard();
                } else if (head.startsWith("super")) {
                    throw new UnsupportedOperationException("? super T not implemented");
                } else {
                    // not supported
                    //throw new IllegalArgumentException("only extends/super can follow ?, but found " + s.substring(idx));
                    return refClass("java.lang.Object").wildcard();
                }
            }

            while (idx < s.length()) {
                char ch = s.charAt(idx);
                if (Character.isJavaIdentifierStart(ch)
                        || Character.isJavaIdentifierPart(ch)
                        || ch == '.') {
                    idx++;
                } else {
                    break;
                }
            }

            TypeReference clazz = refClass(s.substring(start, idx));

            return parseSuffix(clazz);
        }

        /**
         * Parses additional left-associative suffixes, like type arguments
         * and array specifiers.
         */
        private TypeReference parseSuffix(TypeReference clazz) throws ClassNotFoundException {
            if (idx == s.length()) {
                return clazz; // hit EOL
            }

            char ch = s.charAt(idx);

            if (ch == '<') {
                return parseSuffix(parseArguments(clazz));
            }

            if (ch == '[') {
                if (s.charAt(idx + 1) == ']') {
                    idx += 2;
                    return parseSuffix(clazz.array());
                }
                throw new IllegalArgumentException("Expected ']' but found " + s.substring(idx + 1));
            }

            return clazz;
        }

        /**
         * Skips whitespaces
         */
        private void ws() {
            while (Character.isWhitespace(s.charAt(idx)) && idx < s.length()) {
                idx++;
            }
        }

        /**
         * Parses '&lt;T1,T2,...,Tn>'
         *
         * @return the index of the character next to '>'
         */
        private TypeReference parseArguments(TypeReference rawType) throws ClassNotFoundException {
            if (s.charAt(idx) != '<') {
                throw new IllegalArgumentException();
            }
            idx++;

            List<TypeReference> args = new ArrayList<TypeReference>();

            while (true) {
                args.add(parseTypeName());
                if (idx == s.length()) {
                    throw new IllegalArgumentException("Missing '>' in " + s);
                }
                char ch = s.charAt(idx);
                if (ch == '>') {
                    return rawType.narrow(args.toArray(new TypeReference[args.size()]));
                }

                if (ch != ',') {
                    throw new IllegalArgumentException(s);
                }
                idx++;
            }

        }
    }

    /**
     * References to existing classes.
     * <p/>
     * <p/>
     * ReferencedClass is kept in a pool so that they are shared.
     * There is one pool for each CodeModel object.
     * <p/>
     * <p/>
     * It is impossible to cache ReferencedClass globally only because
     * there is the _package() method, which obtains the owner Package
     * object, which is scoped to CodeModel.
     */
    private class ReferencedClass extends TypeReference implements Declaration {
        private final Class<?> _class;

        ReferencedClass(Class<?> _clazz) {
            super(CodeModel.this);
            this._class = _clazz;
            assert !_class.isArray();
        }

        public String name() {
            return _class.getSimpleName().replace('$', '.');
        }

        public String fullName() {
            return _class.getName().replace('$', '.');
        }

        public String binaryName() {
            return _class.getName();
        }

        public TypeReference outer() {
            Class<?> p = _class.getDeclaringClass();
            if (p == null) {
                return null;
            }
            return ref(p);
        }

        public Package _package() {
            String name = fullName();

            // this type is array
            if (name.indexOf('[') != -1) {
                return CodeModel.this._package("");
            }

            // other normal case
            int idx = name.lastIndexOf('.');
            if (idx < 0) {
                return CodeModel.this._package("");
            } else {
                return CodeModel.this._package(name.substring(0, idx));
            }
        }

        public TypeReference _extends() {
            Class<?> sp = _class.getSuperclass();
            if (sp == null) {
                if (isInterface()) {
                    return owner().ref(Object.class);
                }
                return null;
            } else {
                return ref(sp);
            }
        }

        public Iterator<TypeReference> _implements() {
            final Class<?>[] interfaces = _class.getInterfaces();
            return new Iterator<TypeReference>() {
                private int idx = 0;

                public boolean hasNext() {
                    return idx < interfaces.length;
                }

                public TypeReference next() {
                    return CodeModel.this.ref(interfaces[idx++]);
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public boolean isInterface() {
            return _class.isInterface();
        }

        public boolean isAbstract() {
            return java.lang.reflect.Modifier.isAbstract(_class.getModifiers());
        }

        public PrimitiveType getPrimitiveType() {
            Class<?> v = boxToPrimitive.get(_class);
            if (v != null) {
                return parse(CodeModel.this, v.getName());
            } else {
                return null;
            }
        }

        public boolean isArray() {
            return false;
        }

        public void declare(Formatter f) {
        }

        public TypeVariable[] typeParams() {
            // TODO: does JDK 1.5 reflection provides these information?
            return super.typeParams();
        }

        protected TypeReference substituteParams(TypeVariable[] variables, List<TypeReference> bindings) {
            // TODO: does JDK 1.5 reflection provides these information?
            return this;
        }
    }

    /**
     * Get code writer
     *
     * @return Code writer
     */
    public CodeWriter getCodeWriter() {
        return codeWriter;
    }

    /**
     * Conversion from primitive type {@link Class} (such as {@link Integer#TYPE}
     * to its boxed type (such as <tt>Integer.class</tt>)
     */
    public static final Map<Class<?>, Class<?>> primitiveToBox;
    /**
     * The reverse look up for {@link #primitiveToBox}
     */
    public static final Map<Class<?>, Class<?>> boxToPrimitive;

    static {
        Map<Class<?>, Class<?>> m1 = new HashMap<Class<?>, Class<?>>();
        Map<Class<?>, Class<?>> m2 = new HashMap<Class<?>, Class<?>>();

        m1.put(Boolean.class, Boolean.TYPE);
        m1.put(Byte.class, Byte.TYPE);
        m1.put(Character.class, Character.TYPE);
        m1.put(Double.class, Double.TYPE);
        m1.put(Float.class, Float.TYPE);
        m1.put(Integer.class, Integer.TYPE);
        m1.put(Long.class, Long.TYPE);
        m1.put(Short.class, Short.TYPE);
        m1.put(Void.class, Void.TYPE);

        for (Map.Entry<Class<?>, Class<?>> e : m1.entrySet()) {
            m2.put(e.getValue(), e.getKey());
        }

        boxToPrimitive = Collections.unmodifiableMap(m1);
        primitiveToBox = Collections.unmodifiableMap(m2);

    }
}
