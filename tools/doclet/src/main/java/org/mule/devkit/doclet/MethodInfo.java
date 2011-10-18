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
import org.mule.devkit.doclet.apicheck.AbstractMethodInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class MethodInfo extends MemberInfo implements AbstractMethodInfo {
    public static final Comparator<MethodInfo> comparator = new Comparator<MethodInfo>() {
        public int compare(MethodInfo a, MethodInfo b) {
            return a.name().compareTo(b.name());
        }
    };

    private class InlineTags implements InheritedTags {
        public TagInfo[] tags() {
            return comment().tags();
        }

        public InheritedTags inherited() {
            MethodInfo m = findOverriddenMethod(name(), signature());
            if (m != null) {
                return m.inlineTags();
            } else {
                return null;
            }
        }
    }

    public String relativeModulePath() {
        String s = mContainingClass.moduleName();
        s += ".html";
        s += "#" + elementName();
        return s;
    }

    private static void addInterfaces(ClassInfo[] ifaces, ArrayList<ClassInfo> queue) {
        for (ClassInfo i : ifaces) {
            queue.add(i);
        }
        for (ClassInfo i : ifaces) {
            addInterfaces(i.getInterfaces(), queue);
        }
    }

    // first looks for a superclass, and then does a breadth first search to
    // find the least far away match
    public MethodInfo findOverriddenMethod(String name, String signature) {
        if (mReturnType == null) {
            // ctor
            return null;
        }
        if (mOverriddenMethod != null) {
            return mOverriddenMethod;
        }

        ArrayList<ClassInfo> queue = new ArrayList<ClassInfo>();
        addInterfaces(containingClass().getInterfaces(), queue);
        for (ClassInfo iface : queue) {
            for (MethodInfo me : iface.methods()) {
                if (me.name().equals(name) && me.signature().equals(signature)
                        && me.inlineTags().tags() != null && me.inlineTags().tags().length > 0) {
                    return me;
                }
            }
        }
        return null;
    }

    private static void addRealInterfaces(ClassInfo[] ifaces, ArrayList<ClassInfo> queue) {
        for (ClassInfo i : ifaces) {
            queue.add(i);
            if (i.realSuperclass() != null && i.realSuperclass().isAbstract()) {
                queue.add(i.superclass());
            }
        }
        for (ClassInfo i : ifaces) {
            addInterfaces(i.realInterfaces(), queue);
        }
    }

    public MethodInfo findRealOverriddenMethod(MethodInfo other, HashSet<ClassInfo> notStrippable) {
        String name = other.name();
        String signature = other.signature();

        if (mReturnType == null) {
            // ctor
            return null;
        }
        if (mOverriddenMethod != null) {
            return mOverriddenMethod;
        }

        ArrayList<ClassInfo> queue = new ArrayList<ClassInfo>();
        if (containingClass().realSuperclass() != null
                && containingClass().realSuperclass().isAbstract()) {
            queue.add(containingClass());
        }
        addInterfaces(containingClass().realInterfaces(), queue);
        for (ClassInfo iface : queue) {
            for (MethodInfo me : iface.methods()) {
                if (me.name().equals(name) && me.signature().equals(signature)
                        && me.inlineTags().tags() != null && me.inlineTags().tags().length > 0
                        && notStrippable.contains(me.containingClass())) {
                    return me;
                }
            }
        }
        return null;
    }

    public MethodInfo findSuperclassImplementation(HashSet notStrippable) {
        if (mReturnType == null) {
            // ctor
            return null;
        }
        if (mOverriddenMethod != null) {
            // Even if we're told outright that this was the overridden method, we want to
            // be conservative and ignore mismatches of parameter types -- they arise from
            // extending generic specializations, and we want to consider the derived-class
            // method to be a non-override.
            if (this.signature().equals(mOverriddenMethod.signature())) {
                return mOverriddenMethod;
            }
        }

        ArrayList<ClassInfo> queue = new ArrayList<ClassInfo>();
        if (containingClass().realSuperclass() != null
                && containingClass().realSuperclass().isAbstract()) {
            queue.add(containingClass());
        }
        addInterfaces(containingClass().realInterfaces(), queue);
        for (ClassInfo iface : queue) {
            for (MethodInfo me : iface.methods()) {
                if (me.name().equals(this.name()) && me.signature().equals(this.signature())
                        && notStrippable.contains(me.containingClass())) {
                    return me;
                }
            }
        }
        return null;
    }

    public ClassInfo findRealOverriddenClass(String name, String signature) {
        if (mReturnType == null) {
            // ctor
            return null;
        }
        if (mOverriddenMethod != null) {
            return mOverriddenMethod.mRealContainingClass;
        }

        ArrayList<ClassInfo> queue = new ArrayList<ClassInfo>();
        if (containingClass().realSuperclass() != null
                && containingClass().realSuperclass().isAbstract()) {
            queue.add(containingClass());
        }
        addInterfaces(containingClass().realInterfaces(), queue);
        for (ClassInfo iface : queue) {
            for (MethodInfo me : iface.methods()) {
                if (me.name().equals(name) && me.signature().equals(signature)
                        && me.inlineTags().tags() != null && me.inlineTags().tags().length > 0) {
                    return iface;
                }
            }
        }
        return null;
    }

    private class FirstSentenceTags implements InheritedTags {
        public TagInfo[] tags() {
            return comment().briefTags();
        }

        public InheritedTags inherited() {
            MethodInfo m = findOverriddenMethod(name(), signature());
            if (m != null) {
                return m.firstSentenceTags();
            } else {
                return null;
            }
        }
    }

    private class ReturnTags implements InheritedTags {
        public TagInfo[] tags() {
            return comment().returnTags();
        }

        public InheritedTags inherited() {
            MethodInfo m = findOverriddenMethod(name(), signature());
            if (m != null) {
                return m.returnTags();
            } else {
                return null;
            }
        }
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
                Errors.error(Errors.DEPRECATION_MISMATCH, position(), "Method "
                        + mContainingClass.qualifiedName() + "." + name()
                        + ": @Deprecated annotation and @deprecated doc tag do not match");
            }

            mIsDeprecated = commentDeprecated | annotationDeprecated;
            mDeprecatedKnown = true;
        }
        return mIsDeprecated;
    }

    public String uncamel(String camelCaseName) {
        String result = "";
        String[] parts = camelCaseName.split("(?<!^)(?=[A-Z])");

        for (int i = 0; i < parts.length; i++) {
            result += parts[i].toLowerCase() + (i < parts.length - 1 ? "-" : "");
        }

        return result;
    }


    public boolean isProcessor() {
        if (!mProcessorKnown) {
            boolean annotationPresent = false;
            for (AnnotationInstanceInfo annotation : annotations()) {
                if (annotation.type().qualifiedName().equals("org.mule.api.annotations.Processor")) {
                    mElementName = uncamel(name());
                    for (AnnotationValueInfo value : annotation.elementValues()) {
                        if ("name".equals(value.element().name())) {
                            mElementName = value.valueString().replace("\"", "");
                        }
                    }
                    annotationPresent = true;
                    break;
                }
            }
            mIsProcessor = annotationPresent;
            mProcessorKnown = true;
        }
        return mIsProcessor;
    }

    public boolean isSource() {
        if (!mSourceKnown) {
            boolean annotationPresent = false;
            for (AnnotationInstanceInfo annotation : annotations()) {
                if (annotation.type().qualifiedName().equals("org.mule.api.annotations.Source")) {
                    mElementName = uncamel(name());
                    for (AnnotationValueInfo value : annotation.elementValues()) {
                        if ("name".equals(value.element().name())) {
                            mElementName = value.valueString().replace("\"", "");
                        }
                    }
                    annotationPresent = true;
                    break;
                }
            }
            mIsSource = annotationPresent;
            mSourceKnown = true;
        }
        return mIsSource;
    }

    public boolean isTransformer() {
        if (!mTransformerKnown) {
            boolean annotationPresent = false;
            for (AnnotationInstanceInfo annotation : annotations()) {
                if (annotation.type().qualifiedName().equals("org.mule.api.annotations.Transformer")) {
                    mElementName = uncamel(name());
                    for (AnnotationValueInfo value : annotation.elementValues()) {
                        if ("name".equals(value.element().name())) {
                            mElementName = value.valueString().replace("\"", "");
                        }
                    }
                    annotationPresent = true;
                    break;
                }
            }
            mIsTransformer = annotationPresent;
            mTransformerKnown = true;
        }
        return mIsTransformer;
    }

    public void setDeprecated(boolean deprecated) {
        mDeprecatedKnown = true;
        mIsDeprecated = deprecated;
    }

    public TypeInfo[] getTypeParameters() {
        return mTypeParameters;
    }

    public MethodInfo cloneForClass(ClassInfo newContainingClass) {
        MethodInfo result =
                new MethodInfo(getRawCommentText(), mTypeParameters, name(), signature(),
                        newContainingClass, realContainingClass(), isPublic(), isProtected(),
                        isPackagePrivate(), isPrivate(), isFinal(), isStatic(), isSynthetic(), mIsAbstract,
                        mIsSynchronized, mIsNative, mIsAnnotationElement, kind(), mFlatSignature,
                        mOverriddenMethod, mReturnType, mParameters, mThrownExceptions, position(),
                        annotations());
        result.init(mDefaultAnnotationElementValue);
        return result;
    }

    public MethodInfo(String rawCommentText, TypeInfo[] typeParameters, String name,
                      String signature, ClassInfo containingClass, ClassInfo realContainingClass, boolean isPublic,
                      boolean isProtected, boolean isPackagePrivate, boolean isPrivate, boolean isFinal,
                      boolean isStatic, boolean isSynthetic, boolean isAbstract, boolean isSynchronized,
                      boolean isNative, boolean isAnnotationElement, String kind, String flatSignature,
                      MethodInfo overriddenMethod, TypeInfo returnType, ParameterInfo[] parameters,
                      ClassInfo[] thrownExceptions, SourcePositionInfo position,
                      AnnotationInstanceInfo[] annotations) {
        // Explicitly coerce 'final' state of Java6-compiled enum values() method, to match
        // the Java5-emitted base API description.
        super(rawCommentText, name, signature, containingClass, realContainingClass, isPublic,
                isProtected, isPackagePrivate, isPrivate, ((name.equals("values") && containingClass
                .isEnum()) ? true : isFinal), isStatic, isSynthetic, kind, position, annotations);

        // The underlying MethodDoc for an interface's declared methods winds up being marked
        // non-abstract. Correct that here by looking at the immediate-parent class, and marking
        // this method abstract if it is an unimplemented interface method.
        if (containingClass.isInterface()) {
            isAbstract = true;
        }

        mReasonOpened = "0:0";
        mIsAnnotationElement = isAnnotationElement;
        mTypeParameters = typeParameters;
        mIsAbstract = isAbstract;
        mIsSynchronized = isSynchronized;
        mIsNative = isNative;
        mFlatSignature = flatSignature;
        mOverriddenMethod = overriddenMethod;
        mReturnType = returnType;
        mParameters = parameters;
        mThrownExceptions = thrownExceptions;
    }

    public void init(AnnotationValueInfo defaultAnnotationElementValue) {
        mDefaultAnnotationElementValue = defaultAnnotationElementValue;
    }

    public boolean isAbstract() {
        return mIsAbstract;
    }

    public boolean isSynchronized() {
        return mIsSynchronized;
    }

    public boolean isNative() {
        return mIsNative;
    }

    public String flatSignature() {
        return mFlatSignature;
    }

    public String elementName() {
        return mElementName;
    }

    public InheritedTags inlineTags() {
        return new InlineTags();
    }

    public InheritedTags firstSentenceTags() {
        return new FirstSentenceTags();
    }

    public InheritedTags returnTags() {
        return new ReturnTags();
    }

    public TypeInfo returnType() {
        return mReturnType;
    }

    public String prettySignature() {
        return name() + prettyParameters();
    }

    /**
     * Returns a printable version of the parameters of this method's signature.
     */
    public String prettyParameters() {
        StringBuilder params = new StringBuilder("(");
        for (ParameterInfo pInfo : mParameters) {
            if (params.length() > 1) {
                params.append(",");
            }
            params.append(pInfo.type().simpleTypeName());
        }

        params.append(")");
        return params.toString();
    }

    /**
     * Returns a name consistent with the {@link MethodInfo#getHashableName()}.
     */
    public String getHashableName() {
        StringBuilder result = new StringBuilder();
        result.append(name());
        for (int p = 0; p < mParameters.length; p++) {
            result.append(":");
            if (p == mParameters.length - 1 && isVarArgs()) {
                // TODO: note that this does not attempt to handle hypothetical
                // vararg methods whose last parameter is a list of arrays, e.g.
                // "Object[]...".
                result.append(mParameters[p].type().fullNameNoDimension(typeVariables())).append("...");
            } else {
                result.append(mParameters[p].type().fullName(typeVariables()));
            }
        }
        return result.toString();
    }

    private boolean inList(ClassInfo item, ThrowsTagInfo[] list) {
        int len = list.length;
        String qn = item.qualifiedName();
        for (int i = 0; i < len; i++) {
            ClassInfo ex = list[i].exception();
            if (ex != null && ex.qualifiedName().equals(qn)) {
                return true;
            }
        }
        return false;
    }

    public ThrowsTagInfo[] throwsTags() {
        if (mThrowsTags == null) {
            ThrowsTagInfo[] documented = comment().throwsTags();
            ArrayList<ThrowsTagInfo> rv = new ArrayList<ThrowsTagInfo>();

            int len = documented.length;
            for (int i = 0; i < len; i++) {
                rv.add(documented[i]);
            }

            ClassInfo[] all = mThrownExceptions;
            len = all.length;
            for (int i = 0; i < len; i++) {
                ClassInfo cl = all[i];
                if (documented == null || !inList(cl, documented)) {
                    rv.add(new ThrowsTagInfo("@throws", "@throws", cl.qualifiedName(), cl, "",
                            containingClass(), position()));
                }
            }
            mThrowsTags = rv.toArray(new ThrowsTagInfo[rv.size()]);
        }
        return mThrowsTags;
    }

    private static int indexOfParam(String name, String[] list) {
        final int N = list.length;
        for (int i = 0; i < N; i++) {
            if (name.equals(list[i])) {
                return i;
            }
        }
        return -1;
    }

    public ParamTagInfo[] paramTags() {
        if (mParamTags == null) {
            final int N = mParameters.length;

            String[] names = new String[N];
            boolean[] optional = new boolean[N];
            boolean[] nestedProcessor = new boolean[N];
            String[] defaultValue = new String[N];
            String[] attributeName = new String[N];
            String[] comments = new String[N];
            SourcePositionInfo[] positions = new SourcePositionInfo[N];

            // get the right names so we can handle our names being different from
            // our parent's names.
            for (int i = 0; i < N; i++) {
                attributeName[i] = mParameters[i].name();
                nestedProcessor[i] = false;
                if (mParameters[i].typeName().contains("HttpCallback")) {
                    attributeName[i] = uncamel(attributeName[i]) + "-flow-ref";
                }
                if (mParameters[i].typeName().contains("NestedProcessor")) {
                    nestedProcessor[i] = true;
                }
                optional[i] = false;
                defaultValue[i] = "";
                for (AnnotationInstanceInfo annotation : mParameters[i].annotations()) {
                    if (annotation.type().qualifiedName().equals("org.mule.api.annotations.Parameter")) {
                        for (AnnotationValueInfo value : annotation.elementValues()) {
                            if ("name".equals(value.element().name())) {
                                attributeName[i] = value.valueString().replace("\"", "");
                            }
                        }
                        break;
                    } else if (annotation.type().qualifiedName().equals("org.mule.api.annotations.param.Optional")) {
                        optional[i] = true;
                        break;
                    } else if (annotation.type().qualifiedName().equals("org.mule.api.annotations.param.Default")) {
                        defaultValue[i] = annotation.elementValues()[0].valueString().replace("\"", "");
                        break;
                    }
                }
                names[i] = mParameters[i].name();
                comments[i] = "";
                positions[i] = mParameters[i].position();
            }

            // gather our comments, and complain about misnamed @param tags
            for (ParamTagInfo tag : comment().paramTags()) {
                int index = indexOfParam(tag.parameterName(), names);
                if (index >= 0) {
                    comments[index] = tag.parameterComment();
                    positions[index] = tag.position();
                } else {
                    Errors.error(Errors.UNKNOWN_PARAM_TAG_NAME, tag.position(),
                            "@param tag with name that doesn't match the parameter list: '" + tag.parameterName()
                                    + "'");
                }
            }

            // get our parent's tags to fill in the blanks
            MethodInfo overridden = this.findOverriddenMethod(name(), signature());
            if (overridden != null) {
                ParamTagInfo[] maternal = overridden.paramTags();
                for (int i = 0; i < N; i++) {
                    if (comments[i].equals("")) {
                        comments[i] = maternal[i].parameterComment();
                        positions[i] = maternal[i].position();
                    }
                }
            }

            // construct the results, and cache them for next time
            mParamTags = new ParamTagInfo[N];
            for (int i = 0; i < N; i++) {
                mParamTags[i] =
                        new ParamTagInfo("@param", "@param", names[i] + " " + comments[i], attributeName[i],
                                optional[i], defaultValue[i], nestedProcessor[i], parent(), positions[i]);

                // while we're here, if we find any parameters that are still undocumented at this
                // point, complain. (this warning is off by default, because it's really, really
                // common; but, it's good to be able to enforce it)
                if (comments[i].equals("")) {
                    Errors.error(Errors.UNDOCUMENTED_PARAMETER, positions[i], "Undocumented parameter '"
                            + names[i] + "' on method '" + name() + "'");
                }
            }
        }
        return mParamTags;
    }

    public SeeTagInfo[] seeTags() {
        SeeTagInfo[] result = comment().seeTags();
        if (result == null) {
            if (mOverriddenMethod != null) {
                result = mOverriddenMethod.seeTags();
            }
        }
        return result;
    }

    public TagInfo[] deprecatedTags() {
        TagInfo[] result = comment().deprecatedTags();
        if (result.length == 0) {
            if (comment().undeprecateTags().length == 0) {
                if (mOverriddenMethod != null) {
                    result = mOverriddenMethod.deprecatedTags();
                }
            }
        }
        return result;
    }

    public ParameterInfo[] parameters() {
        return mParameters;
    }


    public boolean matchesParams(String[] params, String[] dimensions, boolean varargs) {
        if (mParamStrings == null) {
            ParameterInfo[] mine = mParameters;
            int len = mine.length;
            if (len != params.length) {
                return false;
            }
            for (int i = 0; i < len; i++) {
                if (!mine[i].matchesDimension(dimensions[i], varargs)) {
                    return false;
                }
                TypeInfo myType = mine[i].type();
                String qualifiedName = myType.qualifiedTypeName();
                String realType = myType.isPrimitive() ? "" : myType.asClassInfo().qualifiedName();
                String s = params[i];

                // Check for a matching generic name or best known type
                if (!matchesType(qualifiedName, s) && !matchesType(realType, s)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns true if a parameter from a method signature is
     * compatible with a parameter given in a {@code @link} tag.
     */
    private boolean matchesType(String signatureParam, String callerParam) {
        int signatureLength = signatureParam.length();
        int callerLength = callerParam.length();
        return signatureParam.equals(callerParam) || ((callerLength + 1) < signatureLength
                && signatureParam.charAt(signatureLength - callerLength - 1) == '.'
                && signatureParam.endsWith(callerParam));
    }

    public void makeHDF(Data data, String base) {
        data.setValue(base + ".kind", kind());
        data.setValue(base + ".name", name());
        data.setValue(base + ".elementName", elementName());
        data.setValue(base + ".href", htmlPage());
        data.setValue(base + ".modhref", relativeModulePath());
        data.setValue(base + ".anchor", anchor());

        if (mReturnType != null) {
            returnType().makeHDF(data, base + ".returnType", false, typeVariables());
            data.setValue(base + ".abstract", mIsAbstract ? "abstract" : "");
        }

        data.setValue(base + ".synchronized", mIsSynchronized ? "synchronized" : "");
        data.setValue(base + ".final", isFinal() ? "final" : "");
        data.setValue(base + ".static", isStatic() ? "static" : "");

        TagInfo.makeHDF(data, base + ".shortDescr", firstSentenceTags());
        TagInfo.makeHDF(data, base + ".descr", inlineTags());
        TagInfo.makeHDF(data, base + ".deprecated", deprecatedTags());
        TagInfo.makeHDF(data, base + ".seeAlso", seeTags());
        data.setValue(base + ".since.key", SinceTagger.keyForName(getSince()));
        data.setValue(base + ".since.name", getSince());
        ParamTagInfo.makeHDF(data, base + ".paramTags", paramTags());
        if( containingClass().hasConnectionManager() ) {
            ParamTagInfo.makeHDF(data, base + ".connectionTags", containingClass().connectionTags());
        }
        AttrTagInfo.makeReferenceHDF(data, base + ".attrRefs", comment().attrTags());
        ThrowsTagInfo.makeHDF(data, base + ".throws", throwsTags());
        ParameterInfo.makeHDF(data, base + ".params", parameters(), isVarArgs(), typeVariables());
        if (isProtected()) {
            data.setValue(base + ".scope", "protected");
        } else if (isPublic()) {
            data.setValue(base + ".scope", "public");
        }
        TagInfo.makeHDF(data, base + ".returns", returnTags());

        if (mTypeParameters != null) {
            TypeInfo.makeHDF(data, base + ".generic.typeArguments", mTypeParameters, false);
        }

        setFederatedReferences(data, base);
    }

    public HashSet<String> typeVariables() {
        HashSet<String> result = TypeInfo.typeVariables(mTypeParameters);
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

    @Override
    public boolean isExecutable() {
        return true;
    }

    public ClassInfo[] thrownExceptions() {
        return mThrownExceptions;
    }

    public String typeArgumentsName(HashSet<String> typeVars) {
        if (mTypeParameters == null || mTypeParameters.length == 0) {
            return "";
        } else {
            return TypeInfo.typeArgumentsName(mTypeParameters, typeVars);
        }
    }

    public boolean isAnnotationElement() {
        return mIsAnnotationElement;
    }

    public AnnotationValueInfo defaultAnnotationElementValue() {
        return mDefaultAnnotationElementValue;
    }

    public void setVarargs(boolean set) {
        mIsVarargs = set;
    }

    public boolean isVarArgs() {
        return mIsVarargs;
    }

    @Override
    public String toString() {
        return this.name();
    }

    public void setReason(String reason) {
        mReasonOpened = reason;
    }

    public String getReason() {
        return mReasonOpened;
    }

    public void addException(String exec) {
        ClassInfo exceptionClass = new ClassInfo(exec);
        List<ClassInfo> exceptions = new ArrayList<ClassInfo>(mThrownExceptions.length + 1);
        exceptions.addAll(Arrays.asList(mThrownExceptions));
        exceptions.add(exceptionClass);
        mThrownExceptions = new ClassInfo[exceptions.size()];
        exceptions.toArray(mThrownExceptions);
    }

    public void addParameter(ParameterInfo p) {
        // Name information
        ParameterInfo[] newParams;
        int i = 0;

        if (mParameters == null) {
            newParams = new ParameterInfo[1];
        } else {
            newParams = new ParameterInfo[mParameters.length + 1];
            for (ParameterInfo info : mParameters) {
                newParams[i++] = info;
            }
        }
        newParams[i] = p;
        mParameters = newParams;

        // Type information
        TypeInfo[] newTypes;
        i = 0;

        if (mTypeParameters == null) {
            newTypes = new TypeInfo[1];
        } else {
            newTypes = new TypeInfo[mTypeParameters.length + 1];
            for (TypeInfo info : mTypeParameters) {
                newTypes[i++] = info;
            }
        }
        newTypes[i] = p.mType;
        mTypeParameters = newTypes;
    }

    private String mFlatSignature;
    private MethodInfo mOverriddenMethod;
    private TypeInfo mReturnType;
    private boolean mIsAnnotationElement;
    private boolean mIsAbstract;
    private boolean mIsSynchronized;
    private boolean mIsNative;
    private boolean mIsVarargs;
    private boolean mDeprecatedKnown;
    private boolean mIsDeprecated;
    private boolean mProcessorKnown;
    private boolean mIsProcessor;
    private boolean mSourceKnown;
    private boolean mIsSource;
    private boolean mTransformerKnown;
    private boolean mIsTransformer;
    private String mElementName;
    private ParameterInfo[] mParameters;
    private ClassInfo[] mThrownExceptions;
    private String[] mParamStrings;
    ThrowsTagInfo[] mThrowsTags;
    private ParamTagInfo[] mParamTags;
    private TypeInfo[] mTypeParameters;
    private AnnotationValueInfo mDefaultAnnotationElementValue;
    private String mReasonOpened;

    // TODO: merge with droiddoc version (above)
    public String qualifiedName() {
        String parentQName = (containingClass() != null)
                ? (containingClass().qualifiedName() + ".") : "";
        return parentQName + name();
    }

    @Override
    public String signature() {
        if (mSignature == null) {
            StringBuilder params = new StringBuilder("(");
            for (ParameterInfo pInfo : mParameters) {
                if (params.length() > 1) {
                    params.append(", ");
                }
                params.append(pInfo.type().fullName());
            }

            params.append(")");
            mSignature = params.toString();
        }
        return mSignature;
    }

    public boolean matches(MethodInfo other) {
        return prettySignature().equals(other.prettySignature());
    }

    public boolean throwsException(ClassInfo exception) {
        for (ClassInfo e : mThrownExceptions) {
            if (e.qualifiedName().equals(exception.qualifiedName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isConsistent(MethodInfo mInfo) {
        boolean consistent = true;
        if (!this.mReturnType.equals(mInfo.mReturnType)) {
            consistent = false;
            Errors.error(Errors.CHANGED_TYPE, mInfo.position(), "Method " + mInfo.qualifiedName()
                    + " has changed return type from " + mReturnType + " to " + mInfo.mReturnType);
        }

        if (mIsAbstract != mInfo.mIsAbstract) {
            consistent = false;
            Errors.error(Errors.CHANGED_ABSTRACT, mInfo.position(), "Method " + mInfo.qualifiedName()
                    + " has changed 'abstract' qualifier");
        }

        if (mIsNative != mInfo.mIsNative) {
            consistent = false;
            Errors.error(Errors.CHANGED_NATIVE, mInfo.position(), "Method " + mInfo.qualifiedName()
                    + " has changed 'native' qualifier");
        }

        if (mIsFinal != mInfo.mIsFinal) {
            // Compiler-generated methods vary in their 'final' qual between versions of
            // the compiler, so this check needs to be quite narrow. A change in 'final'
            // status of a method is only relevant if (a) the method is not declared 'static'
            // and (b) the method's class is not itself 'final'.
            if (!mIsStatic) {
                if ((containingClass() == null) || (!containingClass().isFinal())) {
                    consistent = false;
                    Errors.error(Errors.CHANGED_FINAL, mInfo.position(), "Method " + mInfo.qualifiedName()
                            + " has changed 'final' qualifier");
                }
            }
        }

        if (mIsStatic != mInfo.mIsStatic) {
            consistent = false;
            Errors.error(Errors.CHANGED_STATIC, mInfo.position(), "Method " + mInfo.qualifiedName()
                    + " has changed 'static' qualifier");
        }

        if (!scope().equals(mInfo.scope())) {
            consistent = false;
            Errors.error(Errors.CHANGED_SCOPE, mInfo.position(), "Method " + mInfo.qualifiedName()
                    + " changed scope from " + scope() + " to " + mInfo.scope());
        }

        if (!isDeprecated() == mInfo.isDeprecated()) {
            Errors.error(Errors.CHANGED_DEPRECATED, mInfo.position(), "Method " + mInfo.qualifiedName()
                    + " has changed deprecation state");
            consistent = false;
        }

        if (mIsSynchronized != mInfo.mIsSynchronized) {
            Errors.error(Errors.CHANGED_SYNCHRONIZED, mInfo.position(), "Method " + mInfo.qualifiedName()
                    + " has changed 'synchronized' qualifier from " + mIsSynchronized + " to "
                    + mInfo.mIsSynchronized);
            consistent = false;
        }

        for (ClassInfo exception : thrownExceptions()) {
            if (!mInfo.throwsException(exception)) {
                // exclude 'throws' changes to finalize() overrides with no arguments
                if (!name().equals("finalize") || (mParameters.length > 0)) {
                    Errors.error(Errors.CHANGED_THROWS, mInfo.position(), "Method " + mInfo.qualifiedName()
                            + " no longer throws exception " + exception.qualifiedName());
                    consistent = false;
                }
            }
        }

        for (ClassInfo exec : mInfo.thrownExceptions()) {
            // exclude 'throws' changes to finalize() overrides with no arguments
            if (!throwsException(exec)) {
                if (!name().equals("finalize") || (mParameters.length > 0)) {
                    Errors.error(Errors.CHANGED_THROWS, mInfo.position(), "Method " + mInfo.qualifiedName()
                            + " added thrown exception " + exec.qualifiedName());
                    consistent = false;
                }
            }
        }

        return consistent;
    }
}
