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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A generated Java class/interface/enum/....
 *
 * <p>
 * This class models a declaration, and since a declaration can be always
 * used as a reference, it inherits {@link JClass}.
 *
 * <h2>Where to go from here?</h2>
 * <p>
 * You'd want to generate fields and methods on a class.
 * See {@link #method(int, Type, String)} and {@link #field(int, Type, String)}.
 */
public class DefinedClass
    extends JClass
    implements Declaration, ClassContainer, Generifiable, Annotable, DocCommentable {

    /** Name of this class. Null if anonymous. */
    private String name = null;
    
    /** Modifiers for the class declaration */
    private Modifiers mods;

    /** Name of the super class of this class. */
    private JClass superClass;

    /** List of interfaces that this class implements */
    private final Set<JClass> interfaces = new TreeSet<JClass>();

    /** Fields keyed by their names. */
    /*package*/ final Map<String,FieldVariable> fields = new LinkedHashMap<String,FieldVariable>();

    /** Static initializer, if this class has one */
    private Block init = null;

    /** class javadoc */
    private DocComment jdoc = null;

    /** Set of constructors for this class, if any */
    private final List<Method> constructors = new ArrayList<Method>();

    /** Set of methods that are members of this class */
    private final List<Method> methods = new ArrayList<Method>();

    /**
     * Nested classes as a map from name to DefinedClass.
     * The name is all capitalized in a case sensitive file system
     * ({@link CodeModel#isCaseSensitiveFileSystem}) to avoid conflicts.
     *
     * Lazily created to save footprint.
     *
     * @see #getClasses()
     */
    private Map<String,DefinedClass> classes;


    /**
     * Flag that controls whether this class should be really generated or not.
     * 
     * Sometimes it is useful to generate code that refers to class X,
     * without actually generating the code of X.
     * This flag is used to surpress X.java file in the output.
     */
    private boolean hideFile = false;

    /**
     * Client-app spcific metadata associated with this user-created class.
     */
    public Object metadata;

    /**
     * String that will be put directly inside the generated code.
     * Can be null.
     */
    private String directBlock;

    /**
     * If this is a package-member class, this is {@link JPackage}.
     * If this is a nested class, this is {@link DefinedClass}.
     * If this is an anonymous class, this constructor shouldn't be used.
     */
    private ClassContainer outer = null;

    
    /** Default value is class or interface
     *  or annotationTypeDeclaration
     *  or enum
     * 
     */
    private final ClassType classType;
    
    /** List containing the enum value declarations
     *  
     */
//    private List enumValues = new ArrayList();
    
    /**
     * Set of enum constants that are keyed by names.
     * In Java, enum constant order is actually significant,
     * because of order ID they get. So let's preserve the order.
     */
    private final Map<String,EnumConstant> enumConstantsByName = new LinkedHashMap<String,EnumConstant>();

    /**
     * Annotations on this variable. Lazily created.
     */
    private List<AnnotationUse> annotations = null;


    /**
     * Helper class to implement {@link Generifiable}.
     */
    private final AbstractGenerifiable generifiable = new AbstractGenerifiable() {
        protected CodeModel owner() {
            return DefinedClass.this.owner();
        }
    };

    DefinedClass(ClassContainer parent, int mods, String name, ClassType classTypeval) {
        this(mods, name, parent, parent.owner(), classTypeval);
    }

    /**
     * Constructor for creating anonymous inner class.
     */
    DefinedClass(
            CodeModel owner,
            int mods,
            String name) {
        this(mods, name, null, owner);
    }
    
    private DefinedClass(
            int mods,
            String name,
            ClassContainer parent,
            CodeModel owner) {
    	this (mods,name,parent,owner,ClassType.CLASS);
    }

    /**
     * JClass constructor
     *
     * @param mods
     *        Modifiers for this class declaration
     *
     * @param name
     *        Name of this class
     */
    private DefinedClass(
            int mods,
            String name,
            ClassContainer parent,
            CodeModel owner,
            ClassType classTypeVal) {
        super(owner);

        if(name!=null) {
            if (name.trim().length() == 0)
                throw new IllegalArgumentException("JClass name empty");
    
            if (!Character.isJavaIdentifierStart(name.charAt(0))) {
                String msg =
                    "JClass name "
                        + name
                        + " contains illegal character"
                        + " for beginning of identifier: "
                        + name.charAt(0);
                throw new IllegalArgumentException(msg);
            }
            for (int i = 1; i < name.length(); i++) {
                if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                    String msg =
                        "JClass name "
                            + name
                            + " contains illegal character "
                            + name.charAt(i);
                    throw new IllegalArgumentException(msg);
                }
            }
        }

        this.classType = classTypeVal;
        if (isInterface())
            this.mods = Modifiers.forInterface(mods);
        else
            this.mods = Modifiers.forClass(mods);

        this.name = name;

        this.outer = parent;
    }

    /**
     * Returns true if this is an anonymous class.
     */
    public final boolean isAnonymous() {
        return name == null;
    }

    /**
     * This class extends the specifed class.
     *
     * @param superClass
     *        Superclass for this class
     *
     * @return This class
     */
    public DefinedClass _extends(JClass superClass) {
        if (this.classType==ClassType.INTERFACE)
        	if(superClass.isInterface()){
        		return this._implements(superClass);
        	} else throw new IllegalArgumentException("unable to set the super class for an interface");
        if (superClass == null)
            throw new NullPointerException();
        
        for( JClass o=superClass.outer(); o!=null; o=o.outer() ){
            if(this==o){
                throw new IllegalArgumentException("Illegal class inheritance loop." +
                "  Outer class " + this.name + " may not subclass from inner class: " + o.name());
            }
        }
        
        this.superClass = superClass;
        return this;
    }

    public DefinedClass _extends(Class<?> superClass) {
        return _extends(owner().ref(superClass));
    }

    /**
     * Returns the class extended by this class.
     */
    public JClass _extends() {
        if(superClass==null)
            superClass = owner().ref(Object.class);
        return superClass;
    }

    /**
     * This class implements the specifed interface.
     *
     * @param iface
     *        Interface that this class implements
     *
     * @return This class
     */
    public DefinedClass _implements(JClass iface) {
        interfaces.add(iface);
        return this;
    }

    public DefinedClass _implements(Class<?> iface) {
        return _implements(owner().ref(iface));
    }

    /**
     * Returns an iterator that walks the nested classes defined in this
     * class.
     */
    public Iterator<JClass> _implements() {
        return interfaces.iterator();
    }

    /**
     * JClass name accessor.
     * 
     * <p>
     * For example, for <code>java.util.List</code>, this method
     * returns <code>"List"</code>"
     *
     * @return Name of this class
     */
    public String name() {
        return name;
    }
    
    /**
     * If the named enum already exists, the reference to it is returned.
     * Otherwise this method generates a new enum reference with the given
     * name and returns it.
     *
     * @param name
     *  	The name of the constant.
     * @return
     *      The generated type-safe enum constant.
     */
    public EnumConstant enumConstant(String name){
        EnumConstant ec = enumConstantsByName.get(name);
        if (null == ec) {
            ec = new EnumConstant(this, name);
            enumConstantsByName.put(name, ec);
        }
        return ec;
    }

    /**
     * Gets the fully qualified name of this class.
     */
    public String fullName() {
        if (outer instanceof DefinedClass)
            return ((DefinedClass) outer).fullName() + '.' + name();

        JPackage p = _package();
        if (p.isUnnamed())
            return name();
        else
            return p.name() + '.' + name();
    }

    @Override
    public String binaryName() {
        if (outer instanceof DefinedClass)
            return ((DefinedClass) outer).binaryName() + '$' + name();
        else
            return fullName();
    }

    public boolean isInterface() {
        return this.classType==ClassType.INTERFACE;
    }

    public boolean isAbstract() {
        return mods.isAbstract();
    }

    /**
     * Adds a field to the list of field members of this DefinedClass.
     *
     * @param mods
     *        Modifiers for this field
     *
     * @param type
     *        Type of this field
     *
     * @param name
     *        Name of this field
     *
     * @return Newly generated field
     */
    public FieldVariable field(int mods, Type type, String name) {
        return field(mods, type, name, null);
    }

    public FieldVariable field(int mods, Class<?> type, String name) {
        return field(mods, owner()._ref(type), name);
    }

    /**
     * Adds a field to the list of field members of this DefinedClass.
     *
     * @param mods
     *        Modifiers for this field.
     * @param type
     *        Type of this field.
     * @param name
     *        Name of this field.
     * @param init
     *        Initial value of this field.
     *
     * @return Newly generated field
     */
    public FieldVariable field(
        int mods,
        Type type,
        String name,
        Expression init) {
        FieldVariable f = new FieldVariable(this, Modifiers.forField(mods), type, name, init);

        if (fields.containsKey(name)) {
            throw new IllegalArgumentException("trying to create the same field twice: "+name);
        }
        
        fields.put(name, f);
        return f;
    }

    /**  This method indicates if the interface
     *   is an annotationTypeDeclaration
     *
     */
    public boolean isAnnotationTypeDeclaration() {
        return this.classType==ClassType.ANNOTATION_TYPE_DECL;
        

    }

    /**
     * Add an annotationType Declaration to this package
     * @param name
     *      Name of the annotation Type declaration to be added to this package
     * @return
     *      newly created Annotation Type Declaration
     * @exception ClassAlreadyExistsException
     *      When the specified class/interface was already created.
     
     */
    public DefinedClass _annotationTypeDeclaration(String name) throws ClassAlreadyExistsException {
    	return _class (Modifier.PUBLIC,name,ClassType.ANNOTATION_TYPE_DECL);
    }
   
    /**
     * Add a public enum to this package
     * @param name
     *      Name of the enum to be added to this package
     * @return
     *      newly created Enum
     * @exception ClassAlreadyExistsException
     *      When the specified class/interface was already created.
     
     */
    public DefinedClass _enum (String name) throws ClassAlreadyExistsException {
    	return _class (Modifier.PUBLIC,name,ClassType.ENUM);
    }
    
    /**
     * Add a public enum to this package
     * @param name
     *      Name of the enum to be added to this package
     * @param mods
     * 		Modifiers for this enum declaration
     * @return
     *      newly created Enum
     * @exception ClassAlreadyExistsException
     *      When the specified class/interface was already created.
     
     */
    public DefinedClass _enum (int mods,String name) throws ClassAlreadyExistsException {
    	return _class (mods,name,ClassType.ENUM);
    }
    
    

    

    public ClassType getClassType(){
        return this.classType;
    }
    
    public FieldVariable field(
        int mods,
        Class<?> type,
        String name,
        Expression init) {
        return field(mods, owner()._ref(type), name, init);
    }

    /**
     * Returns all the fields declred in this class.
     * The returned {@link java.util.Map} is a read-only live view.
     *
     * @return always non-null.
     */
    public Map<String,FieldVariable> fields() {
        return Collections.unmodifiableMap(fields);
    }

    /**
     * Removes a {@link FieldVariable} from this class.
     *
     * @throws IllegalArgumentException
     *      if the given field is not a field on this class. 
     */
    public void removeField(FieldVariable field) {
        if(fields.remove(field.name())!=field)
            throw new IllegalArgumentException();
    }

    /**
     * Creates, if necessary, and returns the static initializer
     * for this class.
     *
     * @return JBlock containing initialization statements for this class
     */
    public Block init() {
        if (init == null)
            init = new Block();
        return init;
    }

    /**
     * Adds a constructor to this class.
     *
     * @param mods
     *        Modifiers for this constructor
     */
    public Method constructor(int mods) {
        Method c = new Method(mods, this);
        constructors.add(c);
        return c;
    }

    /**
     * Returns an iterator that walks the constructors defined in this class.
     */
    public Iterator<Method> constructors() {
        return constructors.iterator();
    }

    /**
     * Looks for a method that has the specified method signature
     * and return it.
     * 
     * @return
     *      null if not found.
     */
    public Method getConstructor(Type[] argTypes) {
        for (Method m : constructors) {
            if (m.hasSignature(argTypes))
                return m;
        }
        return null;
    }

    /**
     * Add a method to the list of method members of this DefinedClass instance.
     *
     * @param mods
     *        Modifiers for this method
     *
     * @param type
     *        Return type for this method
     *
     * @param name
     *        Name of the method
     *
     * @return Newly generated Method
     */
    public Method method(int mods, Type type, String name) {
        // XXX problems caught in M constructor
        Method m = new Method(this, mods, type, name);
        methods.add(m);
        return m;
    }

    public Method method(int mods, Class<?> type, String name) {
        return method(mods, owner()._ref(type), name);
    }

    public Method method(int mods, Class<?> type, Class<?> narrowedType, String name) {
        return method(mods, owner()._ref(type).boxify().narrow(narrowedType), name);
    }

    /**
     * Returns the set of methods defined in this class.
     */
    public Collection<Method> methods() {
        return methods;
    }

    /**
     * Looks for a method that has the specified method signature
     * and return it.
     * 
     * @return
     *      null if not found.
     */
    public Method getMethod(String name, Type[] argTypes) {
        for (Method m : methods) {
            if (!m.name().equals(name))
                continue;

            if (m.hasSignature(argTypes))
                return m;
        }
        return null;
    }

    public boolean isClass() {
        return true;
    }
    public boolean isPackage() {
        return false;
    }
    public JPackage getPackage() { return parentContainer().getPackage(); }

    /**
     * Add a new nested class to this class.
     *
     * @param mods
     *        Modifiers for this class declaration
     *
     * @param name
     *        Name of class to be added to this package
     *
     * @return Newly generated class
     */
    public DefinedClass _class(int mods, String name)
        throws ClassAlreadyExistsException {
        return _class(mods, name, ClassType.CLASS);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated
     */
    public DefinedClass _class(int mods, String name, boolean isInterface) throws ClassAlreadyExistsException {
    	return _class(mods,name,isInterface?ClassType.INTERFACE:ClassType.CLASS);
    }

    public DefinedClass _class(int mods, String name, ClassType classTypeVal)
        throws ClassAlreadyExistsException {

        String NAME;
        if (CodeModel.isCaseSensitiveFileSystem)
            NAME = name.toUpperCase();
        else
            NAME = name;

        if (getClasses().containsKey(NAME))
            throw new ClassAlreadyExistsException(getClasses().get(NAME));
        else {
            // XXX problems caught in the NC constructor
            DefinedClass c = new DefinedClass(this, mods, name, classTypeVal);
            getClasses().put(NAME,c);
            return c;
        }
    }

    /**
     * Add a new public nested class to this class.
     */
    public DefinedClass _class(String name)
        throws ClassAlreadyExistsException {
        return _class(Modifier.PUBLIC, name);
    }

    /**
     * Add an interface to this package.
     *
     * @param mods
     *        Modifiers for this interface declaration
     *
     * @param name
     *        Name of interface to be added to this package
     *
     * @return Newly generated interface
     */
    public DefinedClass _interface(int mods, String name)
        throws ClassAlreadyExistsException {
        return _class(mods, name, ClassType.INTERFACE);
    }

    /**
     * Adds a public interface to this package.
     */
    public DefinedClass _interface(String name)
        throws ClassAlreadyExistsException {
        return _interface(Modifier.PUBLIC, name);
    }

    /**
     * Creates, if necessary, and returns the class javadoc for this
     * DefinedClass
     *
     * @return JDocComment containing javadocs for this class
     */
    public DocComment javadoc() {
        if (jdoc == null)
            jdoc = new DocComment(owner());
        return jdoc;
    }

    /**
     * Mark this file as hidden, so that this file won't be
     * generated.
     * 
     * <p>
     * This feature could be used to generate code that refers
     * to class X, without actually generating X.java.
     */
    public void hide() {
        hideFile = true;
    }

    public boolean isHidden() {
        return hideFile;
    }

    /**
     * Returns an iterator that walks the nested classes defined in this
     * class.
     */
    public final Iterator<DefinedClass> classes() {
        if(classes==null)
            return Collections.<DefinedClass>emptyList().iterator();
        else
            return classes.values().iterator();
    }

    private Map<String,DefinedClass> getClasses() {
        if(classes==null)
            classes = new TreeMap<String,DefinedClass>();
        return classes;
    }


    /**
     * Returns all the nested classes defined in this class.
     */
    public final JClass[] listClasses() {
        if(classes==null)
            return new JClass[0];
        else
            return classes.values().toArray(new JClass[classes.values().size()]);
    }

    @Override
    public JClass outer() {
        if (outer.isClass())
            return (JClass) outer;
        else
            return null;
    }

    public void declare(Formatter f) {
        if (jdoc != null)
            f.nl().g(jdoc);

        if (annotations != null){
            for (AnnotationUse annotation : annotations)
                f.g(annotation).nl();
        }

        f.g(mods).p(classType.declarationToken).id(name).d(generifiable);

        if (superClass != null && superClass != owner().ref(Object.class))
            f.nl().i().p("extends").g(superClass).nl().o();

        if (!interfaces.isEmpty()) {
            if (superClass == null)
                f.nl();
            f.i().p(classType==ClassType.INTERFACE ? "extends" : "implements");
            f.g(interfaces);
            f.nl().o();
        }
        declareBody(f);
    }

    /**
     * prints the body of a class.
     */
    protected void declareBody(Formatter f) {
        f.p('{').nl().nl().i();
        boolean first = true;

        if (!enumConstantsByName.isEmpty()) {
            for (EnumConstant c : enumConstantsByName.values()) {
                if (!first) f.p(',').nl();
                f.d(c);
                first = false;
            }
        	f.p(';').nl();
        }

        for( FieldVariable field : fields.values() )
            f.d(field);
        if (init != null)
            f.nl().p("static").s(init);
        for (Method m : constructors) {
            f.nl().d(m);
        }
        for (Method m : methods) {
            f.nl().d(m);
        }
        if(classes!=null)
            for (DefinedClass dc : classes.values())
                f.nl().d(dc);

        
        if (directBlock != null)
            f.p(directBlock);
        f.nl().o().p('}').nl();
    }

    /**
     * Places the given string directly inside the generated class.
     * 
     * This method can be used to add methods/fields that are not
     * generated by CodeModel.
     * This method should be used only as the last resort.
     */
    public void direct(String string) {
        if (directBlock == null)
            directBlock = string;
        else
            directBlock += string;
    }

    public final JPackage _package() {
        ClassContainer p = outer;
        while (!(p instanceof JPackage))
            p = p.parentContainer();
        return (JPackage) p;
    }

    public final ClassContainer parentContainer() {
        return outer;
    }

    public TypeVariable generify(String name) {
        return generifiable.generify(name);
    }
    public TypeVariable generify(String name, Class<?> bound) {
        return generifiable.generify(name, bound);
    }
    public TypeVariable generify(String name, JClass bound) {
        return generifiable.generify(name, bound);
    }
    @Override
    public TypeVariable[] typeParams() {
        return generifiable.typeParams();
    }

    protected JClass substituteParams(
        TypeVariable[] variables,
        List<JClass> bindings) {
        return this;
    }

    /** Adding ability to annotate a class
     * @param clazz
     *          The annotation class to annotate the class with
     */
    public AnnotationUse annotate(Class <? extends Annotation> clazz){
        return annotate(owner().ref(clazz));
    }

    /** Adding ability to annotate a class
      * @param clazz
      *          The annotation class to annotate the class with
      */
     public AnnotationUse annotate(JClass clazz){
        if(annotations==null)
           annotations = new ArrayList<AnnotationUse>();
        AnnotationUse a = new AnnotationUse(clazz);
        annotations.add(a);
        return a;
    }

    public <W extends AnnotationWriter> W annotate2(Class<W> clazz) {
        return TypedAnnotationWriter.create(clazz,this);
    }

    /**
     * {@link Annotable#annotations()}
     */
    public Collection<AnnotationUse> annotations() {
        if (annotations == null)
            annotations = new ArrayList<AnnotationUse>();
        return Collections.unmodifiableCollection(annotations);
    }

    /**
     * @return
     *      the current modifiers of this class.
     *      Always return non-null valid object.
     */
    public Modifiers mods() {
        return mods;
    }
}
