/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule.devkit.doclet;

import com.google.clearsilver.jsilver.data.Data;
import com.sun.javadoc.ClassDoc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

public class ClassInfo extends DocInfo implements ContainerInfo, Comparable, Scoped {
    public static final Comparator<ClassInfo> comparator = new Comparator<ClassInfo>() {
        public int compare(ClassInfo a, ClassInfo b) {
            return a.name().compareTo(b.name());
        }
    };

    public static final Comparator<ClassInfo> qualifiedComparator = new Comparator<ClassInfo>() {
        public int compare(ClassInfo a, ClassInfo b) {
            return a.qualifiedName().compareTo(b.qualifiedName());
        }
    };

    /**
     * Constructs a stub representation of a class.
     */
    public ClassInfo(String qualifiedName) {
        super("", SourcePositionInfo.UNKNOWN);

        mQualifiedName = qualifiedName;
        int pos = qualifiedName.lastIndexOf('.');
        if (pos != -1) {
            mName = qualifiedName.substring(pos + 1);
        } else {
            mName = qualifiedName;
        }
    }

    public ClassInfo(ClassDoc cl, String rawCommentText, SourcePositionInfo position,
                     boolean isPublic, boolean isProtected, boolean isPackagePrivate, boolean isPrivate,
                     boolean isStatic, boolean isInterface, boolean isAbstract, boolean isOrdinaryClass,
                     boolean isException, boolean isError, boolean isEnum, boolean isAnnotation, boolean isFinal,
                     boolean isIncluded, String name, String qualifiedName, String qualifiedTypeName,
                     boolean isPrimitive) {
        super(rawCommentText, position);

        mClass = cl;
        mIsPublic = isPublic;
        mIsProtected = isProtected;
        mIsPackagePrivate = isPackagePrivate;
        mIsPrivate = isPrivate;
        mIsStatic = isStatic;
        mIsInterface = isInterface;
        mIsAbstract = isAbstract;
        mIsOrdinaryClass = isOrdinaryClass;
        mIsException = isException;
        mIsError = isError;
        mIsEnum = isEnum;
        mIsAnnotation = isAnnotation;
        mIsFinal = isFinal;
        mIsIncluded = isIncluded;
        mName = name;
        mQualifiedName = qualifiedName;
        mQualifiedTypeName = qualifiedTypeName;
        mIsPrimitive = isPrimitive;
        mNameParts = name.split("\\.");
    }

    public void init(TypeInfo typeInfo, ClassInfo[] interfaces, TypeInfo[] interfaceTypes,
                     ClassInfo[] innerClasses, MethodInfo[] constructors, MethodInfo[] methods,
                     MethodInfo[] annotationElements, FieldInfo[] fields, FieldInfo[] enumConstants,
                     PackageInfo containingPackage, ClassInfo containingClass, ClassInfo superclass,
                     TypeInfo superclassType, AnnotationInstanceInfo[] annotations) {
        mTypeInfo = typeInfo;
        mRealInterfaces = new ArrayList<ClassInfo>();
        for (ClassInfo cl : interfaces) {
            mRealInterfaces.add(cl);
        }
        mRealInterfaceTypes = interfaceTypes;
        mInnerClasses = innerClasses;
        mAllConstructors = constructors;
        mAllSelfMethods = methods;
        mAnnotationElements = annotationElements;
        mAllSelfFields = fields;
        mEnumConstants = enumConstants;
        mContainingPackage = containingPackage;
        mContainingClass = containingClass;
        mRealSuperclass = superclass;
        mRealSuperclassType = superclassType;
        mAnnotations = annotations;

        // after providing new methods and new superclass info,clear any cached
        // lists of self + superclass methods, ctors, etc.
        mSuperclassInit = false;
        mConstructors = null;
        mMethods = null;
        mSelfMethods = null;
        mFields = null;
        mSelfFields = null;
        mSelfAttributes = null;
        mDeprecatedKnown = false;

        Arrays.sort(mEnumConstants, FieldInfo.comparator);
        Arrays.sort(mInnerClasses, ClassInfo.comparator);
    }

    public void init2() {
        // calling this here forces the AttrTagInfo objects to be linked to the AttribtueInfo
        // objects
        selfAttributes();
    }

    public void init3(TypeInfo[] types, ClassInfo[] realInnerClasses) {
        mTypeParameters = types;
        mRealInnerClasses = realInnerClasses;
    }

    public ClassInfo[] getRealInnerClasses() {
        return mRealInnerClasses;
    }

    public TypeInfo[] getTypeParameters() {
        return mTypeParameters;
    }

    public boolean checkLevel() {
        int val = mCheckLevel;
        if (val >= 0) {
            return val != 0;
        } else {
            boolean v =
                    Doclava.checkLevel(mIsPublic, mIsProtected, mIsPackagePrivate, mIsPrivate, isHidden());
            mCheckLevel = v ? 1 : 0;
            return v;
        }
    }

    public int compareTo(Object that) {
        if (that instanceof ClassInfo) {
            return mQualifiedName.compareTo(((ClassInfo) that).mQualifiedName);
        } else {
            return this.hashCode() - that.hashCode();
        }
    }

    @Override
    public ContainerInfo parent() {
        return this;
    }

    public boolean isPublic() {
        return mIsPublic;
    }

    public boolean isProtected() {
        return mIsProtected;
    }

    public boolean isPackagePrivate() {
        return mIsPackagePrivate;
    }

    public boolean isPrivate() {
        return mIsPrivate;
    }

    public boolean isStatic() {
        return mIsStatic;
    }

    public boolean isInterface() {
        return mIsInterface;
    }

    public boolean isAbstract() {
        return mIsAbstract;
    }

    public PackageInfo containingPackage() {
        return mContainingPackage;
    }

    public ClassInfo containingClass() {
        return mContainingClass;
    }

    public boolean isOrdinaryClass() {
        return mIsOrdinaryClass;
    }

    public boolean isException() {
        return mIsException;
    }

    public boolean isError() {
        return mIsError;
    }

    public boolean isEnum() {
        return mIsEnum;
    }

    public boolean isAnnotation() {
        return mIsAnnotation;
    }

    public boolean isFinal() {
        return mIsFinal;
    }

    /**
     * Returns true if the class represented by this object is defined
     * locally, and thus will be included in local documentation.
     */
    public boolean isDefinedLocally() {
        return mIsIncluded;
    }

    public HashSet<String> typeVariables() {
        HashSet<String> result = TypeInfo.typeVariables(mTypeInfo.typeArguments());
        ClassInfo cl = containingClass();
        while (cl != null) {
            TypeInfo[] types = cl.asTypeInfo().typeArguments();
            if (types != null) {
                TypeInfo.typeVariables(types, result);
            }
            cl = cl.containingClass();
        }
        return result;
    }

    private static void gatherHiddenInterfaces(ClassInfo cl, HashSet<ClassInfo> interfaces) {
        for (ClassInfo iface : cl.mRealInterfaces) {
            if (iface.checkLevel()) {
                interfaces.add(iface);
            } else {
                gatherHiddenInterfaces(iface, interfaces);
            }
        }
    }

    public ClassInfo[] getInterfaces() {
        if (mInterfaces == null) {
            if (checkLevel()) {
                HashSet<ClassInfo> interfaces = new HashSet<ClassInfo>();
                ClassInfo superclass = mRealSuperclass;
                while (superclass != null && !superclass.checkLevel()) {
                    gatherHiddenInterfaces(superclass, interfaces);
                    superclass = superclass.mRealSuperclass;
                }
                gatherHiddenInterfaces(this, interfaces);
                mInterfaces = interfaces.toArray(new ClassInfo[interfaces.size()]);
            } else {
                // put something here in case someone uses it
                mInterfaces = new ClassInfo[mRealInterfaces.size()];
                mRealInterfaces.toArray(mInterfaces);
            }
            Arrays.sort(mInterfaces, ClassInfo.qualifiedComparator);
        }
        return mInterfaces;
    }

    public ClassInfo[] realInterfaces() {
        return mRealInterfaces.toArray(new ClassInfo[mRealInterfaces.size()]);
    }

    TypeInfo[] realInterfaceTypes() {
        return mRealInterfaceTypes;
    }

    public String name() {
        return mName;
    }

    public String[] nameParts() {
        return mNameParts;
    }

    public String leafName() {
        return mNameParts[mNameParts.length - 1];
    }

    public String qualifiedName() {
        return mQualifiedName;
    }

    public String qualifiedTypeName() {
        return mQualifiedTypeName;
    }

    public boolean isPrimitive() {
        return mIsPrimitive;
    }

    public MethodInfo[] allConstructors() {
        return mAllConstructors;
    }

    public MethodInfo[] constructors() {
        if (mConstructors == null) {
            MethodInfo[] methods = mAllConstructors;
            ArrayList<MethodInfo> ctors = new ArrayList<MethodInfo>();
            for (int i = 0; i < methods.length; i++) {
                MethodInfo m = methods[i];
                if (!m.isHidden()) {
                    ctors.add(m);
                }
            }
            mConstructors = ctors.toArray(new MethodInfo[ctors.size()]);
            Arrays.sort(mConstructors, MethodInfo.comparator);
        }
        return mConstructors;
    }

    public ClassInfo[] innerClasses() {
        return mInnerClasses;
    }

    public TagInfo[] inlineTags() {
        return comment().tags();
    }

    public TagInfo[] firstSentenceTags() {
        return comment().briefTags();
    }

    public boolean isDeprecated() {
        if (!mDeprecatedKnown) {
            boolean commentDeprecated = comment().isDeprecated();
            boolean annotationDeprecated = false;
            for (AnnotationInstanceInfo annotation : annotations()) {
                if (annotation.type().qualifiedName().equals("java.lang.Deprecated")) {
                    annotationDeprecated = true;
                    break;
                }
            }

            if (commentDeprecated != annotationDeprecated) {
                Errors.error(Errors.DEPRECATION_MISMATCH, position(), "Class " + qualifiedName()
                        + ": @Deprecated annotation and @deprecated comment do not match");
            }

            mIsDeprecated = commentDeprecated | annotationDeprecated;
            mDeprecatedKnown = true;
        }
        return mIsDeprecated;
    }

    public TagInfo[] deprecatedTags() {
        // Should we also do the interfaces?
        return comment().deprecatedTags();
    }

    public MethodInfo[] methods() {
        if (mMethods == null) {
            TreeMap<String, MethodInfo> all = new TreeMap<String, MethodInfo>();

            ClassInfo[] ifaces = getInterfaces();
            for (ClassInfo iface : ifaces) {
                if (iface != null) {
                    MethodInfo[] inhereted = iface.methods();
                    for (MethodInfo method : inhereted) {
                        String key = method.getHashableName();
                        all.put(key, method);
                    }
                }
            }

            ClassInfo superclass = superclass();
            if (superclass != null) {
                MethodInfo[] inhereted = superclass.methods();
                for (MethodInfo method : inhereted) {
                    String key = method.getHashableName();
                    all.put(key, method);
                }
            }

            MethodInfo[] methods = selfMethods();
            for (MethodInfo method : methods) {
                String key = method.getHashableName();
                all.put(key, method);
            }

            mMethods = all.values().toArray(new MethodInfo[all.size()]);
            Arrays.sort(mMethods, MethodInfo.comparator);
        }
        return mMethods;
    }

    public MethodInfo[] annotationElements() {
        return mAnnotationElements;
    }

    public AnnotationInstanceInfo[] annotations() {
        return mAnnotations;
    }

    private static void addFields(ClassInfo cl, TreeMap<String, FieldInfo> all) {
        FieldInfo[] fields = cl.fields();
        int N = fields.length;
        for (int i = 0; i < N; i++) {
            FieldInfo f = fields[i];
            all.put(f.name(), f);
        }
    }

    public FieldInfo[] fields() {
        if (mFields == null) {
            int N;
            TreeMap<String, FieldInfo> all = new TreeMap<String, FieldInfo>();

            ClassInfo[] interfaces = getInterfaces();
            N = interfaces.length;
            for (int i = 0; i < N; i++) {
                addFields(interfaces[i], all);
            }

            ClassInfo superclass = superclass();
            if (superclass != null) {
                addFields(superclass, all);
            }

            FieldInfo[] fields = selfFields();
            N = fields.length;
            for (int i = 0; i < N; i++) {
                FieldInfo f = fields[i];
                if (!f.isHidden()) {
                    String key = f.name();
                    all.put(key, f);
                }
            }

            mFields = all.values().toArray(new FieldInfo[0]);
        }
        return mFields;
    }

    public void gatherFields(ClassInfo owner, ClassInfo cl, HashMap<String, FieldInfo> fields) {
        gatherFields(owner, cl, fields, false);
    }
    public void gatherFields(ClassInfo owner, ClassInfo cl, HashMap<String, FieldInfo> fields, boolean ignoreVisibility) {
        FieldInfo[] flds = cl.selfFields();
        for (FieldInfo f : flds) {
            if( ignoreVisibility ) {
                fields.put(f.name(), f.cloneForClass(owner));
            } else {
                if (f.checkLevel()) {
                    fields.put(f.name(), f.cloneForClass(owner));
                }
            }
        }
    }

    public FieldInfo[] selfFields() {
        if (mSelfFields == null) {
            HashMap<String, FieldInfo> fields = new HashMap<String, FieldInfo>();
            // our hidden parents
            if (mRealSuperclass != null && !mRealSuperclass.checkLevel()) {
                gatherFields(this, mRealSuperclass, fields);
            }
            for (ClassInfo iface : mRealInterfaces) {
                if (!iface.checkLevel()) {
                    gatherFields(this, iface, fields);
                }
            }
            // mine
            FieldInfo[] selfFields = mAllSelfFields;
            for (int i = 0; i < selfFields.length; i++) {
                FieldInfo f = selfFields[i];
                if (!f.isHidden()) {
                    fields.put(f.name(), f);
                }
            }
            // combine and return in
            mSelfFields = fields.values().toArray(new FieldInfo[fields.size()]);
            Arrays.sort(mSelfFields, FieldInfo.comparator);
        }
        return mSelfFields;
    }

    public FieldInfo[] allSelfFields() {
        return mAllSelfFields;
    }

    private void gatherMethods(ClassInfo owner, ClassInfo cl, HashMap<String, MethodInfo> methods) {
        MethodInfo[] meth = cl.selfMethods();
        for (MethodInfo m : meth) {
            if (m.checkLevel()) {
                methods.put(m.name() + m.signature(), m.cloneForClass(owner));
            }
        }
    }

    public MethodInfo[] selfMethods() {
        if (mSelfMethods == null) {
            HashMap<String, MethodInfo> methods = new HashMap<String, MethodInfo>();
            // our hidden parents
            if (mRealSuperclass != null && !mRealSuperclass.checkLevel()) {
                gatherMethods(this, mRealSuperclass, methods);
            }
            for (ClassInfo iface : mRealInterfaces) {
                if (!iface.checkLevel()) {
                    gatherMethods(this, iface, methods);
                }
            }
            // mine
            if (mAllSelfMethods != null) {
                for (MethodInfo m : mAllSelfMethods) {
                    if (m.checkLevel()) {
                        methods.put(m.name() + m.signature(), m);
                    }
                }
            }

            // combine and return it
            mSelfMethods = methods.values().toArray(new MethodInfo[methods.size()]);
            Arrays.sort(mSelfMethods, MethodInfo.comparator);
        }
        return mSelfMethods;
    }

    public MethodInfo[] allSelfMethods() {
        return mAllSelfMethods;
    }

    public void addMethod(MethodInfo method) {
        mApiCheckMethods.put(method.getHashableName(), method);

        if (mAllSelfMethods == null) {
            mAllSelfMethods = new MethodInfo[]{method};
            return;
        }

        MethodInfo[] methods = new MethodInfo[mAllSelfMethods.length + 1];
        int i = 0;
        for (MethodInfo m : mAllSelfMethods) {
            methods[i++] = m;
        }
        methods[i] = method;
        mAllSelfMethods = methods;
    }

    public void setContainingPackage(PackageInfo pkg) {
        mContainingPackage = pkg;
    }

    public AttributeInfo[] selfAttributes() {
        if (mSelfAttributes == null) {
            TreeMap<FieldInfo, AttributeInfo> attrs = new TreeMap<FieldInfo, AttributeInfo>();

            // the ones in the class comment won't have any methods
            for (AttrTagInfo tag : comment().attrTags()) {
                FieldInfo field = tag.reference();
                if (field != null) {
                    AttributeInfo attr = attrs.get(field);
                    if (attr == null) {
                        attr = new AttributeInfo(this, field);
                        attrs.put(field, attr);
                    }
                    tag.setAttribute(attr);
                }
            }

            // in the methods
            for (MethodInfo m : selfMethods()) {
                for (AttrTagInfo tag : m.comment().attrTags()) {
                    FieldInfo field = tag.reference();
                    if (field != null) {
                        AttributeInfo attr = attrs.get(field);
                        if (attr == null) {
                            attr = new AttributeInfo(this, field);
                            attrs.put(field, attr);
                        }
                        tag.setAttribute(attr);
                        attr.methods.add(m);
                    }
                }
            }

            // constructors too
            for (MethodInfo m : constructors()) {
                for (AttrTagInfo tag : m.comment().attrTags()) {
                    FieldInfo field = tag.reference();
                    if (field != null) {
                        AttributeInfo attr = attrs.get(field);
                        if (attr == null) {
                            attr = new AttributeInfo(this, field);
                            attrs.put(field, attr);
                        }
                        tag.setAttribute(attr);
                        attr.methods.add(m);
                    }
                }
            }

            mSelfAttributes = attrs.values().toArray(new AttributeInfo[attrs.size()]);
            Arrays.sort(mSelfAttributes, AttributeInfo.comparator);
        }
        return mSelfAttributes;
    }

    public FieldInfo[] enumConstants() {
        return mEnumConstants;
    }

    public ClassInfo superclass() {
        if (!mSuperclassInit) {
            if (this.checkLevel()) {
                // rearrange our little inheritance hierarchy, because we need to hide classes that
                // don't pass checkLevel
                ClassInfo superclass = mRealSuperclass;
                while (superclass != null && !superclass.checkLevel()) {
                    superclass = superclass.mRealSuperclass;
                }
                mSuperclass = superclass;
            } else {
                mSuperclass = mRealSuperclass;
            }
        }
        return mSuperclass;
    }

    public ClassInfo realSuperclass() {
        return mRealSuperclass;
    }

    /**
     * always the real superclass, not the collapsed one we get through superclass(), also has the
     * type parameter info if it's generic.
     */
    public TypeInfo superclassType() {
        return mRealSuperclassType;
    }

    public TypeInfo asTypeInfo() {
        return mTypeInfo;
    }

    TypeInfo[] interfaceTypes() {
        ClassInfo[] infos = getInterfaces();
        int len = infos.length;
        TypeInfo[] types = new TypeInfo[len];
        for (int i = 0; i < len; i++) {
            types[i] = infos[i].asTypeInfo();
        }
        return types;
    }

    public String relativePath() {
        String s = containingPackage().name();
        s = s.replace('.', '/');
        s += '/';
        s += name();
        s += ".html";
        return s;
    }

    public String modulePath() {
        String s = name();
        s += ".html";
        return s;
    }

    public String relativePath(String suffix) {
        String s = containingPackage().name();
        s = s.replace('.', '/');
        s += '/';
        s += name() + suffix;
        s += ".html";
        return s;
    }

    /**
     * Even indirectly
     */
    public boolean isDerivedFrom(ClassInfo cl) {
        ClassInfo dad = this.superclass();
        if (dad != null) {
            if (dad.equals(cl)) {
                return true;
            } else {
                if (dad.isDerivedFrom(cl)) {
                    return true;
                }
            }
        }
        for (ClassInfo iface : getInterfaces()) {
            if (iface.equals(cl)) {
                return true;
            } else {
                if (iface.isDerivedFrom(cl)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void makeKeywordEntries(List<KeywordEntry> keywords) {
        if (!checkLevel()) {
            return;
        }

        String htmlPage = htmlPage();
        String qualifiedName = qualifiedName();

        keywords.add(new KeywordEntry(name(), htmlPage, "class in " + containingPackage().name()));

        FieldInfo[] fields = selfFields();
        MethodInfo[] ctors = constructors();
        MethodInfo[] methods = selfMethods();

        // enum constants
        for (FieldInfo field : enumConstants()) {
            if (field.checkLevel()) {
                keywords.add(new KeywordEntry(field.name(), htmlPage + "#" + field.anchor(),
                        "enum constant in " + qualifiedName));
            }
        }

        // constants
        for (FieldInfo field : fields) {
            if (field.isConstant() && field.checkLevel()) {
                keywords.add(new KeywordEntry(field.name(), htmlPage + "#" + field.anchor(), "constant in "
                        + qualifiedName));
            }
        }

        // fields
        for (FieldInfo field : fields) {
            if (!field.isConstant() && field.checkLevel()) {
                keywords.add(new KeywordEntry(field.name(), htmlPage + "#" + field.anchor(), "field in "
                        + qualifiedName));
            }
        }

        // public constructors
        for (MethodInfo m : ctors) {
            if (m.isPublic() && m.checkLevel()) {
                keywords.add(new KeywordEntry(m.prettySignature(), htmlPage + "#" + m.anchor(),
                        "constructor in " + qualifiedName));
            }
        }

        // protected constructors
        if (Doclava.checkLevel(Doclava.SHOW_PROTECTED)) {
            for (MethodInfo m : ctors) {
                if (m.isProtected() && m.checkLevel()) {
                    keywords.add(new KeywordEntry(m.prettySignature(),
                            htmlPage + "#" + m.anchor(), "constructor in " + qualifiedName));
                }
            }
        }

        // package private constructors
        if (Doclava.checkLevel(Doclava.SHOW_PACKAGE)) {
            for (MethodInfo m : ctors) {
                if (m.isPackagePrivate() && m.checkLevel()) {
                    keywords.add(new KeywordEntry(m.prettySignature(),
                            htmlPage + "#" + m.anchor(), "constructor in " + qualifiedName));
                }
            }
        }

        // private constructors
        if (Doclava.checkLevel(Doclava.SHOW_PRIVATE)) {
            for (MethodInfo m : ctors) {
                if (m.isPrivate() && m.checkLevel()) {
                    keywords.add(new KeywordEntry(m.name() + m.prettySignature(),
                            htmlPage + "#" + m.anchor(), "constructor in " + qualifiedName));
                }
            }
        }

        // public methods
        for (MethodInfo m : methods) {
            if (m.isPublic() && m.checkLevel()) {
                keywords.add(new KeywordEntry(m.name() + m.prettySignature(), htmlPage + "#" + m.anchor(),
                        "method in " + qualifiedName));
            }
        }

        // protected methods
        if (Doclava.checkLevel(Doclava.SHOW_PROTECTED)) {
            for (MethodInfo m : methods) {
                if (m.isProtected() && m.checkLevel()) {
                    keywords.add(new KeywordEntry(m.name() + m.prettySignature(),
                            htmlPage + "#" + m.anchor(), "method in " + qualifiedName));
                }
            }
        }

        // package private methods
        if (Doclava.checkLevel(Doclava.SHOW_PACKAGE)) {
            for (MethodInfo m : methods) {
                if (m.isPackagePrivate() && m.checkLevel()) {
                    keywords.add(new KeywordEntry(m.name() + m.prettySignature(),
                            htmlPage + "#" + m.anchor(), "method in " + qualifiedName));
                }
            }
        }

        // private methods
        if (Doclava.checkLevel(Doclava.SHOW_PRIVATE)) {
            for (MethodInfo m : methods) {
                if (m.isPrivate() && m.checkLevel()) {
                    keywords.add(new KeywordEntry(m.name() + m.prettySignature(),
                            htmlPage + "#" + m.anchor(), "method in " + qualifiedName));
                }
            }
        }
    }

    public static void makeLinkListHDF(Data data, String base, ClassInfo[] classes) {
        final int N = classes.length;
        for (int i = 0; i < N; i++) {
            ClassInfo cl = classes[i];
            if (cl.checkLevel()) {
                cl.asTypeInfo().makeHDF(data, base + "." + i);
            }
        }
    }

    /**
     * Used in lists of this class (packages, nested classes, known subclasses)
     */
    public void makeShortDescrHDF(Data data, String base) {
        mTypeInfo.makeHDF(data, base + ".type");
        data.setValue(base + ".kind", this.kind());
        TagInfo.makeHDF(data, base + ".shortDescr", this.firstSentenceTags());
        TagInfo.makeHDF(data, base + ".deprecated", deprecatedTags());
        data.setValue(base + ".since.key", SinceTagger.keyForName(getSince()));
        data.setValue(base + ".since.name", getSince());
        setFederatedReferences(data, base);
    }

    public void makeModuleShortDescrHDF(Data data, String base) {
        mTypeInfo.makeHDF(data, base + ".type");
        data.setValue(base + ".kind", this.kind());
        data.setValue(base + ".name", this.moduleName());
        data.setValue(base + ".version", this.moduleVersion());
        data.setValue(base + ".namespace", this.moduleNamespace());
        data.setValue(base + ".schemaloc", this.moduleSchemaLocation());
        data.setValue(base + ".link", this.modulePath());
        TagInfo.makeHDF(data, base + ".shortDescr", this.firstSentenceTags());
        TagInfo.makeHDF(data, base + ".deprecated", deprecatedTags());
        data.setValue(base + ".since.key", SinceTagger.keyForName(getSince()));
        data.setValue(base + ".since.name", getSince());
        setFederatedReferences(data, base);
    }

    /**
     * Turns into the main class page
     */
    public void makeHDF(Data data) {
        int i, j, n;
        String name = name();
        String qualified = qualifiedName();
        AttributeInfo[] selfAttributes = selfAttributes();
        MethodInfo[] methods = selfMethods();
        FieldInfo[] fields = selfFields();
        FieldInfo[] enumConstants = enumConstants();
        MethodInfo[] ctors = constructors();
        ClassInfo[] inners = innerClasses();

        // class name
        mTypeInfo.makeHDF(data, "class.type");
        mTypeInfo.makeQualifiedHDF(data, "class.qualifiedType");
        data.setValue("class.name", name);
        data.setValue("class.qualified", qualified);
        if (isProtected()) {
            data.setValue("class.scope", "protected");
        } else if (isPublic()) {
            data.setValue("class.scope", "public");
        }
        if (isStatic()) {
            data.setValue("class.static", "static");
        }
        if (isFinal()) {
            data.setValue("class.final", "final");
        }
        if (isAbstract() && !isInterface()) {
            data.setValue("class.abstract", "abstract");
        }

        // module info
        data.setValue("class.moduleName", this.moduleName());
        data.setValue("class.moduleNamespace", this.moduleNamespace());
        data.setValue("class.moduleSchemaLocation", this.moduleSchemaLocation());
        data.setValue("class.moduleVersion", this.moduleVersion());

        // class info
        String kind = kind();
        if (kind != null) {
            data.setValue("class.kind", kind);
        }
        data.setValue("class.since.key", SinceTagger.keyForName(getSince()));
        data.setValue("class.since.name", getSince());
        setFederatedReferences(data, "class");

        // the containing package -- note that this can be passed to type_link,
        // but it also contains the list of all of the packages
        containingPackage().makeClassLinkListHDF(data, "class.package");

        // inheritance hierarchy
        Vector<ClassInfo> superClasses = new Vector<ClassInfo>();
        superClasses.add(this);
        ClassInfo supr = superclass();
        while (supr != null) {
            superClasses.add(supr);
            supr = supr.superclass();
        }
        n = superClasses.size();
        for (i = 0; i < n; i++) {
            supr = superClasses.elementAt(n - i - 1);

            supr.asTypeInfo().makeQualifiedHDF(data, "class.inheritance." + i + ".class");
            supr.asTypeInfo().makeHDF(data, "class.inheritance." + i + ".short_class");
            j = 0;
            for (TypeInfo t : supr.interfaceTypes()) {
                t.makeHDF(data, "class.inheritance." + i + ".interfaces." + j);
                j++;
            }
        }

        // class description
        TagInfo.makeHDF(data, "class.descr", inlineTags());
        TagInfo.makeHDF(data, "class.seeAlso", comment().seeTags());
        TagInfo.makeHDF(data, "class.deprecated", deprecatedTags());

        // known subclasses
        TreeMap<String, ClassInfo> direct = new TreeMap<String, ClassInfo>();
        TreeMap<String, ClassInfo> indirect = new TreeMap<String, ClassInfo>();
        ClassInfo[] all = Converter.rootClasses();
        for (ClassInfo cl : all) {
            if (cl.superclass() != null && cl.superclass().equals(this)) {
                direct.put(cl.name(), cl);
            } else if (cl.isDerivedFrom(this)) {
                indirect.put(cl.name(), cl);
            }
        }
        // direct
        i = 0;
        for (ClassInfo cl : direct.values()) {
            if (cl.checkLevel()) {
                cl.makeShortDescrHDF(data, "class.subclasses.direct." + i);
            }
            i++;
        }
        // indirect
        i = 0;
        for (ClassInfo cl : indirect.values()) {
            if (cl.checkLevel()) {
                cl.makeShortDescrHDF(data, "class.subclasses.indirect." + i);
            }
            i++;
        }

        // hide special cases
        if ("java.lang.Object".equals(qualified) || "java.io.Serializable".equals(qualified)) {
            data.setValue("class.subclasses.hidden", "1");
        } else {
            data.setValue("class.subclasses.hidden", "0");
        }

        // nested classes
        i = 0;
        for (ClassInfo inner : inners) {
            if (inner.checkLevel()) {
                inner.makeShortDescrHDF(data, "class.inners." + i);
            }
            i++;
        }

        // enum constants
        i = 0;
        for (FieldInfo field : enumConstants) {
            field.makeHDF(data, "class.enumConstants." + i);
            i++;
        }

        // constants
        i = 0;
        for (FieldInfo field : fields) {
            if (field.isConstant()) {
                field.makeHDF(data, "class.constants." + i);
                i++;
            }
        }

        // fields
        i = 0;
        for (FieldInfo field : fields) {
            if (!field.isConstant()) {
                field.makeHDF(data, "class.fields." + i);
                i++;
            }
        }

        // constants
        i = 0;
        for (FieldInfo field : mAllSelfFields) {
            if (field.isConfigurable()) {
                field.makeHDF(data, "class.config." + i);
                i++;
            }
        }


        // public constructors
        i = 0;
        for (MethodInfo ctor : ctors) {
            if (ctor.isPublic()) {
                ctor.makeHDF(data, "class.ctors.public." + i);
                i++;
            }
        }

        // protected constructors
        if (Doclava.checkLevel(Doclava.SHOW_PROTECTED)) {
            i = 0;
            for (MethodInfo ctor : ctors) {
                if (ctor.isProtected()) {
                    ctor.makeHDF(data, "class.ctors.protected." + i);
                    i++;
                }
            }
        }

        // package private constructors
        if (Doclava.checkLevel(Doclava.SHOW_PACKAGE)) {
            i = 0;
            for (MethodInfo ctor : ctors) {
                if (ctor.isPackagePrivate()) {
                    ctor.makeHDF(data, "class.ctors.package." + i);
                    i++;
                }
            }
        }

        // private constructors
        if (Doclava.checkLevel(Doclava.SHOW_PRIVATE)) {
            i = 0;
            for (MethodInfo ctor : ctors) {
                if (ctor.isPrivate()) {
                    ctor.makeHDF(data, "class.ctors.private." + i);
                    i++;
                }
            }
        }

        // public methods
        i = 0;
        for (MethodInfo method : methods) {
            if (method.isPublic()) {
                method.makeHDF(data, "class.methods.public." + i);
                i++;
            }
        }

        // protected methods
        if (Doclava.checkLevel(Doclava.SHOW_PROTECTED)) {
            i = 0;
            for (MethodInfo method : methods) {
                if (method.isProtected()) {
                    method.makeHDF(data, "class.methods.protected." + i);
                    i++;
                }
            }
        }

        // package private methods
        if (Doclava.checkLevel(Doclava.SHOW_PACKAGE)) {
            i = 0;
            for (MethodInfo method : methods) {
                if (method.isPackagePrivate()) {
                    method.makeHDF(data, "class.methods.package." + i);
                    i++;
                }
            }
        }

        // private methods
        if (Doclava.checkLevel(Doclava.SHOW_PRIVATE)) {
            i = 0;
            for (MethodInfo method : methods) {
                if (method.isPrivate()) {
                    method.makeHDF(data, "class.methods.private." + i);
                    i++;
                }
            }
        }

        // processors
        i = 0;
        for (MethodInfo method : methods) {
            if (method.isProcessor()) {
                method.makeHDF(data, "class.methods.processor." + i);
                i++;

            }
        }

        // source
        i = 0;
        for (MethodInfo method : methods) {
            if (method.isSource()) {
                method.makeHDF(data, "class.methods.source." + i);
                i++;
            }
        }

        // transformer
        i = 0;
        for (MethodInfo method : methods) {
            if (method.isTransformer()) {
                method.makeHDF(data, "class.methods.transformer." + i);
                i++;
            }
        }

        // xml attributes
        i = 0;
        for (AttributeInfo attr : selfAttributes) {
            if (attr.checkLevel()) {
                attr.makeHDF(data, "class.attrs." + i);
                i++;
            }
        }

        // inherited methods
        Set<ClassInfo> interfaces = new TreeSet<ClassInfo>();
        addInterfaces(getInterfaces(), interfaces);
        ClassInfo cl = superclass();
        i = 0;
        while (cl != null) {
            addInterfaces(cl.getInterfaces(), interfaces);
            makeInheritedHDF(data, i, cl);
            cl = cl.superclass();
            i++;
        }
        for (ClassInfo iface : interfaces) {
            makeInheritedHDF(data, i, iface);
            i++;
        }
    }

    private static void addInterfaces(ClassInfo[] ifaces, Set<ClassInfo> out) {
        for (ClassInfo cl : ifaces) {
            out.add(cl);
            addInterfaces(cl.getInterfaces(), out);
        }
    }

    private static void makeInheritedHDF(Data data, int index, ClassInfo cl) {
        int i;

        String base = "class.inherited." + index;
        data.setValue(base + ".qualified", cl.qualifiedName());
        if (cl.checkLevel()) {
            data.setValue(base + ".link", cl.htmlPage());
        }
        String kind = cl.kind();
        if (kind != null) {
            data.setValue(base + ".kind", kind);
        }

        if (cl.isDefinedLocally()) {
            data.setValue(base + ".included", "true");
        } else {
            Doclava.federationTagger.tagAll(new ClassInfo[]{cl});
            if (!cl.getFederatedReferences().isEmpty()) {
                FederatedSite site = cl.getFederatedReferences().iterator().next();
                data.setValue(base + ".link", site.linkFor(cl.relativePath()));
                data.setValue(base + ".federated", site.name());
            }
        }

        // xml attributes
        i = 0;
        for (AttributeInfo attr : cl.selfAttributes()) {
            attr.makeHDF(data, base + ".attrs." + i);
            i++;
        }

        // methods
        i = 0;
        for (MethodInfo method : cl.selfMethods()) {
            method.makeHDF(data, base + ".methods." + i);
            i++;
        }

        // fields
        i = 0;
        for (FieldInfo field : cl.selfFields()) {
            if (!field.isConstant()) {
                field.makeHDF(data, base + ".fields." + i);
                i++;
            }
        }

        // constants
        i = 0;
        for (FieldInfo field : cl.selfFields()) {
            if (field.isConstant()) {
                field.makeHDF(data, base + ".constants." + i);
                i++;
            }
        }
    }

    @Override
    public boolean isHidden() {
        int val = mHidden;
        if (val >= 0) {
            return val != 0;
        } else {
            boolean v = isHiddenImpl();
            mHidden = v ? 1 : 0;
            return v;
        }
    }

    public boolean isHiddenImpl() {
        ClassInfo cl = this;
        while (cl != null) {
            PackageInfo pkg = cl.containingPackage();
            if (pkg != null && pkg.isHidden()) {
                return true;
            }
            if (cl.comment().isHidden()) {
                return true;
            }
            cl = cl.containingClass();
        }
        return false;
    }

    private MethodInfo matchMethod(MethodInfo[] methods, String name, String[] params,
                                   String[] dimensions, boolean varargs) {
        int len = methods.length;
        for (int i = 0; i < len; i++) {
            MethodInfo method = methods[i];
            if (method.name().equals(name)) {
                if (params == null) {
                    return method;
                } else {
                    if (method.matchesParams(params, dimensions, varargs)) {
                        return method;
                    }
                }
            }
        }
        return null;
    }

    public MethodInfo findMethod(String name, String[] params, String[] dimensions, boolean varargs) {
        // first look on our class, and our superclasses
        MethodInfo rv;

        // for methods
        if (mAllSelfMethods != null) {
            rv = matchMethod(methods(), name, params, dimensions, varargs);

            if (rv != null) {
                return rv;
            }
        }

        // for constructors
        if (mAllConstructors != null) {
            rv = matchMethod(constructors(), name, params, dimensions, varargs);
            if (rv != null) {
                return rv;
            }
        }

        // then recursively look at our containing class
        ClassInfo containing = containingClass();
        if (containing != null) {
            return containing.findMethod(name, params, dimensions, varargs);
        }

        return null;
    }

    /**
     * Returns true if the given method's signature is available in this class,
     * either directly or via inheritance.
     */
    public boolean containsMethod(MethodInfo method) {
        for (MethodInfo m : methods()) {
            if (m.getHashableName().equals(method.getHashableName())) {
                return true;
            }
        }
        return false;
    }

    private ClassInfo searchInnerClasses(String[] nameParts, int index) {
        String part = nameParts[index];

        ClassInfo[] inners = mInnerClasses;
        for (ClassInfo in : inners) {
            String[] innerParts = in.nameParts();
            if (part.equals(innerParts[innerParts.length - 1])) {
                if (index == nameParts.length - 1) {
                    return in;
                } else {
                    return in.searchInnerClasses(nameParts, index + 1);
                }
            }
        }
        return null;
    }

    public ClassInfo extendedFindClass(String className) {
        // ClassDoc.findClass has this bug that we're working around here:
        // If you have a class PackageManager with an inner class PackageInfo
        // and you call it with "PackageInfo" it doesn't find it.
        return searchInnerClasses(className.split("\\."), 0);
    }

    public ClassInfo findClass(String className) {
        return Converter.obtainClass(mClass.findClass(className));
    }

    public ClassInfo findInnerClass(String className) {
        // ClassDoc.findClass won't find inner classes. To deal with that,
        // we try what they gave us first, but if that didn't work, then
        // we see if there are any periods in className, and start searching
        // from there.
        String[] nodes = className.split("\\.");
        ClassDoc cl = mClass;
        for (String n : nodes) {
            cl = cl.findClass(n);
            if (cl == null) {
                return null;
            }
        }
        return Converter.obtainClass(cl);
    }

    public FieldInfo findField(String name) {
        // first look on our class, and our superclasses
        for (FieldInfo f : fields()) {
            if (f.name().equals(name)) {
                return f;
            }
        }

        // then look at our enum constants (these are really fields, maybe
        // they should be mixed into fields(). not sure)
        for (FieldInfo f : enumConstants()) {
            if (f.name().equals(name)) {
                return f;
            }
        }

        // then recursively look at our containing class
        ClassInfo containing = containingClass();
        if (containing != null) {
            return containing.findField(name);
        }

        return null;
    }

    public static ClassInfo[] sortByName(ClassInfo[] classes) {
        int i;
        Sorter[] sorted = new Sorter[classes.length];
        for (i = 0; i < sorted.length; i++) {
            ClassInfo cl = classes[i];
            sorted[i] = new Sorter(cl.name(), cl);
        }

        Arrays.sort(sorted);

        ClassInfo[] rv = new ClassInfo[classes.length];
        for (i = 0; i < rv.length; i++) {
            rv[i] = (ClassInfo) sorted[i].data;
        }

        return rv;
    }

    public boolean equals(ClassInfo that) {
        if (that != null) {
            return this.qualifiedName().equals(that.qualifiedName());
        } else {
            return false;
        }
    }

    public void setNonWrittenConstructors(MethodInfo[] nonWritten) {
        mNonWrittenConstructors = nonWritten;
    }

    public MethodInfo[] getNonWrittenConstructors() {
        return mNonWrittenConstructors;
    }

    public String kind() {
        //if (isModule()) {
        //    return "module";
        // else
        if (isOrdinaryClass()) {
            return "class";
        } else if (isInterface()) {
            return "interface";
        } else if (isEnum()) {
            return "enum";
        } else if (isError()) {
            return "class";
        } else if (isException()) {
            return "class";
        } else if (isAnnotation()) {
            return "@interface";
        }
        return null;
    }

    public String scope() {
        if (isPublic()) {
            return "public";
        } else if (isProtected()) {
            return "protected";
        } else if (isPackagePrivate()) {
            return "";
        } else if (isPrivate()) {
            return "private";
        } else {
            throw new RuntimeException("invalid scope for object " + this);
        }
    }

    public void setHiddenMethods(MethodInfo[] mInfo) {
        mHiddenMethods = mInfo;
    }

    public MethodInfo[] getHiddenMethods() {
        return mHiddenMethods;
    }

    @Override
    public String toString() {
        return this.qualifiedName();
    }

    public void setReasonIncluded(String reason) {
        mReasonIncluded = reason;
    }

    public String getReasonIncluded() {
        return mReasonIncluded;
    }

    private ClassDoc mClass;

    // ctor
    private boolean mIsPublic;
    private boolean mIsProtected;
    private boolean mIsPackagePrivate;
    private boolean mIsPrivate;
    private boolean mIsStatic;
    private boolean mIsInterface;
    private boolean mIsAbstract;
    private boolean mIsOrdinaryClass;
    private boolean mIsException;
    private boolean mIsError;
    private boolean mIsEnum;
    private boolean mIsAnnotation;
    private boolean mIsFinal;
    private boolean mIsIncluded;
    private boolean mIsModule;
    private boolean mModuleKnown;
    private String mModuleName;
    private String mModuleVersion;
    private String mModuleNamespace;
    private String mModuleSchemaLocation;
    private String mName;
    private String mQualifiedName;
    private String mQualifiedTypeName;
    private boolean mIsPrimitive;
    private TypeInfo mTypeInfo;
    private String[] mNameParts;

    // init
    private List<ClassInfo> mRealInterfaces = new ArrayList<ClassInfo>();
    private ClassInfo[] mInterfaces;
    private TypeInfo[] mRealInterfaceTypes;
    private ClassInfo[] mInnerClasses;
    private MethodInfo[] mAllConstructors;
    private MethodInfo[] mAllSelfMethods;
    private MethodInfo[] mAnnotationElements; // if this class is an annotation
    private FieldInfo[] mAllSelfFields;
    private FieldInfo[] mEnumConstants;
    private PackageInfo mContainingPackage;
    private ClassInfo mContainingClass;
    private ClassInfo mRealSuperclass;
    private TypeInfo mRealSuperclassType;
    private ClassInfo mSuperclass;
    private AnnotationInstanceInfo[] mAnnotations;
    private boolean mSuperclassInit;
    private boolean mDeprecatedKnown;

    // lazy
    private MethodInfo[] mConstructors;
    private ClassInfo[] mRealInnerClasses;
    private MethodInfo[] mSelfMethods;
    private FieldInfo[] mSelfFields;
    private AttributeInfo[] mSelfAttributes;
    private MethodInfo[] mMethods;
    private FieldInfo[] mFields;
    private TypeInfo[] mTypeParameters;
    private MethodInfo[] mHiddenMethods;
    private int mHidden = -1;
    private int mCheckLevel = -1;
    private String mReasonIncluded;
    private MethodInfo[] mNonWrittenConstructors;
    private boolean mIsDeprecated;

    // TODO: Temporary members from apicheck migration.
    private HashMap<String, MethodInfo> mApiCheckMethods = new HashMap<String, MethodInfo>();
    private HashMap<String, FieldInfo> mApiCheckFields = new HashMap<String, FieldInfo>();
    private HashMap<String, ConstructorInfo> mApiCheckConstructors
            = new HashMap<String, ConstructorInfo>();

    /**
     * Returns true if {@code cl} implements the interface {@code iface} either by either being that
     * interface, implementing that interface or extending a type that implements the interface.
     */
    private boolean implementsInterface(ClassInfo cl, String iface) {
        if (cl.qualifiedName().equals(iface)) {
            return true;
        }
        for (ClassInfo clImplements : cl.getInterfaces()) {
            if (implementsInterface(clImplements, iface)) {
                return true;
            }
        }
        if (cl.mSuperclass != null && implementsInterface(cl.mSuperclass, iface)) {
            return true;
        }
        return false;
    }


    public void addInterface(ClassInfo iface) {
        mRealInterfaces.add(iface);
    }

    public void addConstructor(ConstructorInfo cInfo) {
        mApiCheckConstructors.put(cInfo.getHashableName(), cInfo);

    }

    public void addField(FieldInfo fInfo) {
        mApiCheckFields.put(fInfo.name(), fInfo);

    }

    public void setSuperClass(ClassInfo superclass) {
        mSuperclass = superclass;
    }

    public Map<String, ConstructorInfo> allConstructorsMap() {
        return mApiCheckConstructors;
    }

    public Map<String, FieldInfo> allFields() {
        return mApiCheckFields;
    }

    /**
     * Returns all methods defined directly in this class. For a list of all
     * methods defined by this class and inherited from its supertypes, see
     * {@link #methods()}.
     */
    public Map<String, MethodInfo> allMethods() {
        return mApiCheckMethods;
    }

    /**
     * Returns the class hierarchy for this class, starting with this class.
     */
    public Iterable<ClassInfo> hierarchy() {
        List<ClassInfo> result = new ArrayList<ClassInfo>(4);
        for (ClassInfo c = this; c != null; c = c.mSuperclass) {
            result.add(c);
        }
        return result;
    }

    public String superclassName() {
        if (mSuperclass == null) {
            if (mQualifiedName.equals("java.lang.Object")) {
                return null;
            }
            throw new IllegalStateException("Superclass not set for " + qualifiedName());
        }
        return mSuperclass.mQualifiedName;
    }

    public void setAnnotations(AnnotationInstanceInfo[] annotations) {
        mAnnotations = annotations;
    }

    public boolean isConsistent(ClassInfo cl) {
        boolean consistent = true;

        if (isInterface() != cl.isInterface()) {
            Errors.error(Errors.CHANGED_CLASS, cl.position(), "Class " + cl.qualifiedName()
                    + " changed class/interface declaration");
            consistent = false;
        }
        for (ClassInfo iface : mRealInterfaces) {
            if (!implementsInterface(cl, iface.mQualifiedName)) {
                Errors.error(Errors.REMOVED_INTERFACE, cl.position(), "Class " + qualifiedName()
                        + " no longer implements " + iface);
            }
        }
        for (ClassInfo iface : cl.mRealInterfaces) {
            if (!implementsInterface(this, iface.mQualifiedName)) {
                Errors.error(Errors.ADDED_INTERFACE, cl.position(), "Added interface " + iface
                        + " to class " + qualifiedName());
                consistent = false;
            }
        }

        for (MethodInfo mInfo : mApiCheckMethods.values()) {
            if (cl.mApiCheckMethods.containsKey(mInfo.getHashableName())) {
                if (!mInfo.isConsistent(cl.mApiCheckMethods.get(mInfo.getHashableName()))) {
                    consistent = false;
                }
            } else {
                /*
                * This class formerly provided this method directly, and now does not. Check our ancestry
                * to see if there's an inherited version that still fulfills the API requirement.
                */
                MethodInfo mi = ClassInfo.overriddenMethod(mInfo, cl);
                if (mi == null) {
                    mi = ClassInfo.interfaceMethod(mInfo, cl);
                }
                if (mi == null) {
                    Errors.error(Errors.REMOVED_METHOD, mInfo.position(), "Removed public method "
                            + mInfo.qualifiedName());
                    consistent = false;
                }
            }
        }
        for (MethodInfo mInfo : cl.mApiCheckMethods.values()) {
            if (!mApiCheckMethods.containsKey(mInfo.getHashableName())) {
                /*
                * Similarly to the above, do not fail if this "new" method is really an override of an
                * existing superclass method.
                */
                MethodInfo mi = ClassInfo.overriddenMethod(mInfo, this);
                if (mi == null) {
                    Errors.error(Errors.ADDED_METHOD, mInfo.position(), "Added public method "
                            + mInfo.qualifiedName());
                    consistent = false;
                }
            }
        }

        for (ConstructorInfo mInfo : mApiCheckConstructors.values()) {
            if (cl.mApiCheckConstructors.containsKey(mInfo.getHashableName())) {
                if (!mInfo.isConsistent(cl.mApiCheckConstructors.get(mInfo.getHashableName()))) {
                    consistent = false;
                }
            } else {
                Errors.error(Errors.REMOVED_METHOD, mInfo.position(), "Removed public constructor "
                        + mInfo.prettySignature());
                consistent = false;
            }
        }
        for (ConstructorInfo mInfo : cl.mApiCheckConstructors.values()) {
            if (!mApiCheckConstructors.containsKey(mInfo.getHashableName())) {
                Errors.error(Errors.ADDED_METHOD, mInfo.position(), "Added public constructor "
                        + mInfo.prettySignature());
                consistent = false;
            }
        }

        for (FieldInfo mInfo : mApiCheckFields.values()) {
            if (cl.mApiCheckFields.containsKey(mInfo.name())) {
                if (!mInfo.isConsistent(cl.mApiCheckFields.get(mInfo.name()))) {
                    consistent = false;
                }
            } else {
                Errors.error(Errors.REMOVED_FIELD, mInfo.position(), "Removed field "
                        + mInfo.qualifiedName());
                consistent = false;
            }
        }
        for (FieldInfo mInfo : cl.mApiCheckFields.values()) {
            if (!mApiCheckFields.containsKey(mInfo.name())) {
                Errors.error(Errors.ADDED_FIELD, mInfo.position(), "Added public field "
                        + mInfo.qualifiedName());
                consistent = false;
            }
        }

        if (mIsAbstract != cl.mIsAbstract) {
            consistent = false;
            Errors.error(Errors.CHANGED_ABSTRACT, cl.position(), "Class " + cl.qualifiedName()
                    + " changed abstract qualifier");
        }

        if (mIsFinal != cl.mIsFinal) {
            consistent = false;
            Errors.error(Errors.CHANGED_FINAL, cl.position(), "Class " + cl.qualifiedName()
                    + " changed final qualifier");
        }

        if (mIsStatic != cl.mIsStatic) {
            consistent = false;
            Errors.error(Errors.CHANGED_STATIC, cl.position(), "Class " + cl.qualifiedName()
                    + " changed static qualifier");
        }

        if (!scope().equals(cl.scope())) {
            consistent = false;
            Errors.error(Errors.CHANGED_SCOPE, cl.position(), "Class " + cl.qualifiedName()
                    + " scope changed from " + scope() + " to " + cl.scope());
        }

        if (!isDeprecated() == cl.isDeprecated()) {
            consistent = false;
            Errors.error(Errors.CHANGED_DEPRECATED, cl.position(), "Class " + cl.qualifiedName()
                    + " has changed deprecation state");
        }

        if (superclassName() != null) {
            if (cl.superclassName() == null || !superclassName().equals(cl.superclassName())) {
                consistent = false;
                Errors.error(Errors.CHANGED_SUPERCLASS, cl.position(), "Class " + qualifiedName()
                        + " superclass changed from " + superclassName() + " to " + cl.superclassName());
            }
        } else if (cl.superclassName() != null) {
            consistent = false;
            Errors.error(Errors.CHANGED_SUPERCLASS, cl.position(), "Class " + qualifiedName()
                    + " superclass changed from " + "null to " + cl.superclassName());
        }

        return consistent;
    }

    // Find a superclass implementation of the given method.
    public static MethodInfo overriddenMethod(MethodInfo candidate, ClassInfo newClassObj) {
        if (newClassObj == null) {
            return null;
        }
        for (MethodInfo mi : newClassObj.mApiCheckMethods.values()) {
            if (mi.matches(candidate)) {
                // found it
                return mi;
            }
        }

        // not found here. recursively search ancestors
        return ClassInfo.overriddenMethod(candidate, newClassObj.mSuperclass);
    }

    // Find a superinterface declaration of the given method.
    public static MethodInfo interfaceMethod(MethodInfo candidate, ClassInfo newClassObj) {
        if (newClassObj == null) {
            return null;
        }
        for (ClassInfo interfaceInfo : newClassObj.getInterfaces()) {
            for (MethodInfo mi : interfaceInfo.mApiCheckMethods.values()) {
                if (mi.matches(candidate)) {
                    return mi;
                }
            }
        }
        return ClassInfo.interfaceMethod(candidate, newClassObj.mSuperclass);
    }

    public boolean hasConstructor(MethodInfo constructor) {
        String name = constructor.getHashableName();
        for (ConstructorInfo ctor : mApiCheckConstructors.values()) {
            if (name.equals(ctor.getHashableName())) {
                return true;
            }
        }
        return false;
    }

    public String moduleName() {
        return mModuleName;
    }

    public String moduleVersion() {
        return mModuleVersion;
    }

    public String moduleNamespace() {
        return mModuleNamespace;
    }

    public String moduleSchemaLocation() {
        return mModuleSchemaLocation;
    }

    public void setTypeInfo(TypeInfo typeInfo) {
        mTypeInfo = typeInfo;
    }

    public String getSource() throws IOException {
        StringBuffer stringBuffer = new StringBuffer();
        BufferedReader reader;

        reader = new BufferedReader(new FileReader(mPosition.file));
        String line = null;

        while ((line = reader.readLine()) != null) {
            stringBuffer.append(line)
                    .append(System.getProperty("line.separator"));
        }

        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {

        }

        return stringBuffer.toString();
    }

    public boolean isModule() {
        if (!mModuleKnown) {
            boolean annotationPresent = false;
            for (AnnotationInstanceInfo annotation : annotations()) {
                if (annotation.type().qualifiedName().equals("org.mule.devkit.annotations.Module")) {
                    for (AnnotationValueInfo value : annotation.elementValues()) {
                        if ("name".equals(value.element().name())) {
                            mModuleName = value.valueString().replace("\"", "");
                        }
                    }
                    mModuleVersion = "1.0";
                    for (AnnotationValueInfo value : annotation.elementValues()) {
                        if ("version".equals(value.element().name())) {
                            mModuleVersion = value.valueString().replace("\"", "");
                        }
                    }
                    mModuleNamespace = "http://www.mulesoft.org/schema/mule/" + mModuleName;
                    for (AnnotationValueInfo value : annotation.elementValues()) {
                        if ("namespace".equals(value.element().name())) {
                            mModuleNamespace = value.valueString().replace("\"", "");
                        }
                    }
                    mModuleSchemaLocation = mModuleNamespace + "/" + mModuleVersion + "/mule-" + mModuleName + ".xsd";
                    for (AnnotationValueInfo value : annotation.elementValues()) {
                        if ("schemaLocation".equals(value.element().name())) {
                            mModuleSchemaLocation = value.valueString().replace("\"", "");
                        }
                    }
                    annotationPresent = true;
                    break;
                }
            }
            mIsModule = annotationPresent;
            mModuleKnown = true;
        }
        return mIsModule;
    }

}
