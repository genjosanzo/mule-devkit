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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/**
 * A Java package.
 */
public final class Package implements Declaration, Generable, ClassContainer, Annotable, Comparable<Package>, DocCommentable {

    /**
     * Name of the package.
     * May be the empty string for the root package.
     */
    private String name;

    private final CodeModel owner;

    /**
     * List of classes contained within this package keyed by their name.
     */
    private final Map<String, DefinedClass> classes = new TreeMap<String, DefinedClass>();

    /**
     * List of resources files inside this package.
     */
    private final Set<ResourceFile> resources = new HashSet<ResourceFile>();

    /**
     * All {@link TypeReference}s in this package keyed the upper case class name.
     * <p/>
     * This field is non-null only on Windows, to detect
     * "Foo" and "foo" as a collision.
     */
    private final Map<String, DefinedClass> upperCaseClassMap;

    /**
     * Lazily created list of package annotations.
     */
    private List<AnnotationUse> annotations = null;

    /**
     * package javadoc.
     */
    private DocComment jdoc = null;

    /**
     * Package constructor
     *
     * @param name Name of package
     * @param cw   The code writer being used to create this package
     * @throws IllegalArgumentException If each part of the package name is not a valid identifier
     */
    Package(String name, CodeModel cw) {
        this.owner = cw;
        if (name.equals(".")) {
            String msg = "Package name . is not allowed";
            throw new IllegalArgumentException(msg);
        }

        if (CodeModel.isCaseSensitiveFileSystem) {
            upperCaseClassMap = null;
        } else {
            upperCaseClassMap = new HashMap<String, DefinedClass>();
        }

        this.name = name;
    }


    public ClassContainer parentContainer() {
        return parent();
    }

    /**
     * Gets the parent package, or null if this class is the root package.
     */
    public Package parent() {
        if (name.length() == 0) {
            return null;
        }

        int idx = name.lastIndexOf('.');
        return owner._package(name.substring(0, idx));
    }

    public boolean isClass() {
        return false;
    }

    public boolean isPackage() {
        return true;
    }

    public Package getPackage() {
        return this;
    }

    /**
     * Add a class to this package.
     *
     * @param mods Modifiers for this class declaration
     * @param name Name of class to be added to this package
     * @return Newly generated class
     * @throws ClassAlreadyExistsException When the specified class/interface was already created.
     */
    public DefinedClass _class(int mods, String name) throws ClassAlreadyExistsException {
        return _class(mods, name, ClassType.CLASS);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated
     */
    public DefinedClass _class(int mods, String name, boolean isInterface) throws ClassAlreadyExistsException {
        return _class(mods, name, isInterface ? ClassType.INTERFACE : ClassType.CLASS);
    }

    public DefinedClass _class(int mods, String name, ClassType classTypeVal) throws ClassAlreadyExistsException {
        if (classes.containsKey(name)) {
            throw new ClassAlreadyExistsException(classes.get(name));
        } else {
            // XXX problems caught in the NC constructor
            DefinedClass c = new DefinedClass(this, mods, name, classTypeVal);

            if (upperCaseClassMap != null) {
                DefinedClass dc = upperCaseClassMap.get(name.toUpperCase());
                if (dc != null) {
                    throw new ClassAlreadyExistsException(dc);
                }
                upperCaseClassMap.put(name.toUpperCase(), c);
            }
            classes.put(name, c);
            return c;
        }
    }

    /**
     * Adds a public class to this package.
     */
    public DefinedClass _class(String name) {
        try {
            return _class(Modifier.PUBLIC, name);
        } catch (ClassAlreadyExistsException ee) {
            return ee.getExistingClass();
        }
    }

    /**
     * Adds a public class to this package.
     */
    public DefinedClass _class(String name, TypeReference _extends) {
        try {
            DefinedClass clazz = _class(Modifier.PUBLIC, name);
            clazz._extends(_extends);

            return clazz;
        } catch (ClassAlreadyExistsException ee) {
            return ee.getExistingClass();
        }
    }

    /**
     * Adds a public class to this package.
     */
    public DefinedClass _class(String name, Class<?> _extends) {
        try {
            DefinedClass clazz = _class(Modifier.PUBLIC, name);
            clazz._extends(_extends);

            return clazz;
        } catch (ClassAlreadyExistsException ee) {
            return ee.getExistingClass();
        }
    }

    /**
     * Adds a public class to this package.
     */
    public DefinedClass _class(String name, Class<?> _extends, Class<?>[] _implements) {
        try {
            DefinedClass clazz = _class(Modifier.PUBLIC, name);
            clazz._extends(_extends);
            for(Class<?> _implement : _implements)
            {
                clazz._implements(_implement);
            }

            return clazz;
        } catch (ClassAlreadyExistsException ee) {
            return ee.getExistingClass();
        }
    }

    /**
     * Adds a public class to this package.
     */
    public DefinedClass _class(String name, Class<?>[] _implements) {
        try {
            DefinedClass clazz = _class(Modifier.PUBLIC, name);
            for(Class<?> _implement : _implements)
            {
                clazz._implements(_implement);
            }

            return clazz;
        } catch (ClassAlreadyExistsException ee) {
            return ee.getExistingClass();
        }
    }


    /**
     * Gets a reference to the already created {@link DefinedClass}.
     *
     * @return null
     *         If the class is not yet created.
     */
    public DefinedClass _getClass(String name) {
        if (classes.containsKey(name)) {
            return classes.get(name);
        } else {
            return null;
        }
    }

    /**
     * Order is based on the lexicological order of the package name.
     */
    public int compareTo(Package that) {
        return this.name.compareTo(that.name);
    }

    /**
     * Add an interface to this package.
     *
     * @param mods Modifiers for this interface declaration
     * @param name Name of interface to be added to this package
     * @return Newly generated interface
     */
    public DefinedClass _interface(int mods, String name) throws ClassAlreadyExistsException {
        return _class(mods, name, ClassType.INTERFACE);
    }

    /**
     * Adds a public interface to this package.
     */
    public DefinedClass _interface(String name) throws ClassAlreadyExistsException {
        return _interface(Modifier.PUBLIC, name);
    }

    /**
     * Add an annotationType Declaration to this package
     *
     * @param name Name of the annotation Type declaration to be added to this package
     * @return newly created Annotation Type Declaration
     * @throws ClassAlreadyExistsException When the specified class/interface was already created.
     */
    public DefinedClass _annotationTypeDeclaration(String name) throws ClassAlreadyExistsException {
        return _class(Modifier.PUBLIC, name, ClassType.ANNOTATION_TYPE_DECL);
    }

    /**
     * Add a public enum to this package
     *
     * @param name Name of the enum to be added to this package
     * @return newly created Enum
     * @throws ClassAlreadyExistsException When the specified class/interface was already created.
     */
    public DefinedClass _enum(String name) throws ClassAlreadyExistsException {
        return _class(Modifier.PUBLIC, name, ClassType.ENUM);
    }

    /**
     * Adds a new resource file to this package.
     */
    public ResourceFile addResourceFile(ResourceFile rsrc) {
        resources.add(rsrc);
        return rsrc;
    }

    /**
     * Checks if a resource of the given name exists.
     */
    public boolean hasResourceFile(String name) {
        for (ResourceFile r : resources) {
            if (r.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Iterates all resource files in this package.
     */
    public Iterator<ResourceFile> propertyFiles() {
        return resources.iterator();
    }

    /**
     * Creates, if necessary, and returns the package javadoc for this
     * DefinedClass.
     *
     * @return JDocComment containing javadocs for this class
     */
    public DocComment javadoc() {
        if (jdoc == null) {
            jdoc = new DocComment(owner());
        }
        return jdoc;
    }

    /**
     * Removes a class from this package.
     */
    public void remove(TypeReference c) {
        if (c._package() != this) {
            throw new IllegalArgumentException(
                    "the specified class is not a member of this package," + " or it is a referenced class");
        }

        // note that c may not be a member of classes.
        // this happens when someone is trying to remove a non generated class
        classes.remove(c.name());
        if (upperCaseClassMap != null) {
            upperCaseClassMap.remove(c.name().toUpperCase());
        }
    }

    /**
     * Reference a class within this package.
     */
    public TypeReference ref(String name) throws ClassNotFoundException {
        if (name.indexOf('.') >= 0) {
            throw new IllegalArgumentException("TypeReference name contains '.': " + name);
        }

        String n = "";
        if (!isUnnamed()) {
            n = this.name + '.';
        }
        n += name;

        return owner.ref(Class.forName(n));
    }

    /**
     * Gets a reference to a sub package of this package.
     */
    public Package subPackage(String pkg) {
        if (isUnnamed()) {
            return owner()._package(pkg);
        } else {
            return owner()._package(name + '.' + pkg);
        }
    }

    /**
     * Returns an iterator that walks the top-level classes defined in this
     * package.
     */
    public Iterator<DefinedClass> classes() {
        return classes.values().iterator();
    }

    /**
     * Checks if a given name is already defined as a class/interface
     */
    public boolean isDefined(String classLocalName) {
        Iterator<DefinedClass> itr = classes();
        while (itr.hasNext()) {
            if ((itr.next()).name().equals(classLocalName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if this package is the root, unnamed package.
     */
    public final boolean isUnnamed() {
        return name.length() == 0;
    }

    /**
     * Get the name of this package
     *
     * @return The name of this package, or the empty string if this is the
     *         null package. For example, this method returns strings like
     *         <code>"java.lang"</code>
     */
    public String name() {
        return name;
    }

    /**
     * Return the code model root object being used to create this package.
     */
    public final CodeModel owner() {
        return owner;
    }


    public AnnotationUse annotate(TypeReference clazz) {
        if (isUnnamed()) {
            throw new IllegalArgumentException("the root package cannot be annotated");
        }
        if (annotations == null) {
            annotations = new ArrayList<AnnotationUse>();
        }
        AnnotationUse a = new AnnotationUse(clazz);
        annotations.add(a);
        return a;
    }

    public AnnotationUse annotate(Class<? extends Annotation> clazz) {
        return annotate(owner.ref(clazz));
    }

    public <W extends AnnotationWriter> W annotate2(Class<W> clazz) {
        return TypedAnnotationWriter.create(clazz, this);
    }

    public Collection<AnnotationUse> annotations() {
        if (annotations == null) {
            annotations = new ArrayList<AnnotationUse>();
        }
        return Collections.unmodifiableList(annotations);
    }

    /**
     * Convert the package name to directory path equivalent
     */
    File toPath(File dir) {
        if (name == null) {
            return dir;
        }
        return new File(dir, name.replace('.', File.separatorChar));
    }

    public void declare(Formatter f) {
        if (name.length() != 0) {
            f.p("package").p(name).p(';').nl();
        }
    }

    public void generate(Formatter f) {
        f.p(name);
    }


    void build(CodeWriter src, CodeWriter res) throws IOException {

        // write classes
        for (DefinedClass c : classes.values()) {
            if (c.isHidden()) {
                continue;   // don't generate this file
            }

            Formatter f = createJavaSourceFileWriter(src, c.name());
            f.write(c);
            f.close();
        }

        // write package annotations
        if (annotations != null || jdoc != null) {
            Formatter f = createJavaSourceFileWriter(src, "package-info");

            if (jdoc != null) {
                f.g(jdoc);
            }

            // TODO: think about importing
            if (annotations != null) {
                for (AnnotationUse a : annotations) {
                    f.g(a).nl();
                }
            }
            f.d(this);

            f.close();
        }

        // write resources
        for (ResourceFile rsrc : resources) {
            CodeWriter cw = rsrc.isResource() ? res : src;
            OutputStream os = new BufferedOutputStream(cw.openBinary(this, rsrc.name()));
            rsrc.build(os);
            os.close();
        }
    }

    /*package*/ int countArtifacts() {
        int r = 0;
        for (DefinedClass c : classes.values()) {
            if (c.isHidden()) {
                continue;   // don't generate this file
            }
            r++;
        }

        if (annotations != null || jdoc != null) {
            r++;
        }

        r += resources.size();

        return r;
    }

    private Formatter createJavaSourceFileWriter(CodeWriter src, String className) throws IOException {
        Writer bw = new BufferedWriter(src.openSource(this, className + ".java"));
        return new Formatter(new PrintWriter(bw));
    }
}
