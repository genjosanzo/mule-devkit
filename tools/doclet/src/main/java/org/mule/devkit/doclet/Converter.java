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

import com.sun.javadoc.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Converter {
  private static RootDoc root;

  public static void makeInfo(RootDoc r) {
    root = r;

    int N, i;

    // create the objects
    ClassDoc[] classDocs = r.classes();
    N = classDocs.length;
    for (i = 0; i < N; i++) {
      Converter.obtainClass(classDocs[i]);
    }
    ArrayList<ClassInfo> classesNeedingInit2 = new ArrayList<ClassInfo>();
    // fill in the fields that reference other classes
    while (mClassesNeedingInit.size() > 0) {
      i = mClassesNeedingInit.size() - 1;
      ClassNeedingInit clni = mClassesNeedingInit.get(i);
      mClassesNeedingInit.remove(i);

      initClass(clni.c, clni.cl);
      classesNeedingInit2.add(clni.cl);
    }
    mClassesNeedingInit = null;
    for (ClassInfo cl : classesNeedingInit2) {
      cl.init2();
    }

    finishAnnotationValueInit();

    // fill in the "root" stuff
    mRootClasses = Converter.convertClasses(r.classes());
  }

  private static ClassInfo[] mRootClasses;

  public static ClassInfo[] rootClasses() {
    return mRootClasses;
  }

  public static ClassInfo[] allClasses() {
    return (ClassInfo[]) mClasses.all();
  }

  private static void initClass(ClassDoc c, ClassInfo cl) {
    MethodDoc[] annotationElements;
    if (c instanceof AnnotationTypeDoc) {
      annotationElements = ((AnnotationTypeDoc) c).elements();
    } else {
      annotationElements = new MethodDoc[0];
    }
    cl.init(Converter.obtainType(c), Converter.convertClasses(c.interfaces()), Converter
        .convertTypes(c.interfaceTypes()), Converter.convertClasses(c.innerClasses()), Converter
        .convertMethods(c.constructors(false)), Converter.convertMethods(c.methods(false)),
        Converter.convertMethods(annotationElements), Converter.convertFields(c.fields(false)),
        Converter.convertFields(c.enumConstants()), Converter.obtainPackage(c.containingPackage()),
        Converter.obtainClass(c.containingClass()), Converter.obtainClass(c.superclass()),
        Converter.obtainType(c.superclassType()), Converter.convertAnnotationInstances(c
            .annotations()));
    cl.setHiddenMethods(Converter.getHiddenMethods(c.methods(false)));
    cl.setNonWrittenConstructors(Converter.convertNonWrittenConstructors(c.constructors(false)));
    cl.init3(Converter.convertTypes(c.typeParameters()), Converter.convertClasses(c
        .innerClasses(false)));
  }

  public static ClassInfo obtainClass(String className) {
    return Converter.obtainClass(root.classNamed(className));
  }

  public static PackageInfo obtainPackage(String packageName) {
    return Converter.obtainPackage(root.packageNamed(packageName));
  }

  private static TagInfo convertTag(Tag tag) {
    return new TextTagInfo(tag.name(), tag.kind(), tag.text(), Converter.convertSourcePosition(tag
        .position()));
  }

  private static ThrowsTagInfo convertThrowsTag(ThrowsTag tag, ContainerInfo base) {
    return new ThrowsTagInfo(tag.name(), tag.text(), tag.kind(), Converter.obtainClass(tag
        .exception()), tag.exceptionComment(), base, Converter
        .convertSourcePosition(tag.position()));
  }

  private static ParamTagInfo convertParamTag(ParamTag tag, ContainerInfo base) {
    return new ParamTagInfo(tag.name(), tag.kind(), tag.text(), tag.isTypeParameter(), tag
        .parameterComment(), tag.parameterName(), base, Converter.convertSourcePosition(tag
        .position()));
  }

  private static SeeTagInfo convertSeeTag(SeeTag tag, ContainerInfo base) {
    return new SeeTagInfo(tag.name(), tag.kind(), tag.text(), base, Converter
        .convertSourcePosition(tag.position()));
  }

  private static SourcePositionInfo convertSourcePosition(SourcePosition sp) {
    if (sp == null) {
      return null;
    }
    return new SourcePositionInfo(sp.file().toString(), sp.line(), sp.column());
  }

  public static TagInfo[] convertTags(Tag[] tags, ContainerInfo base) {
    int len = tags.length;
    TagInfo[] out = new TagInfo[len];
    for (int i = 0; i < len; i++) {
      Tag t = tags[i];
      /*
       * System.out.println("Tag name='" + t.name() + "' kind='" + t.kind() + "'");
       */
      if (t instanceof SeeTag) {
        out[i] = Converter.convertSeeTag((SeeTag) t, base);
      } else if (t instanceof ThrowsTag) {
        out[i] = Converter.convertThrowsTag((ThrowsTag) t, base);
      } else if (t instanceof ParamTag) {
        out[i] = Converter.convertParamTag((ParamTag) t, base);
      } else {
        out[i] = Converter.convertTag(t);
      }
    }
    return out;
  }
  
  public static ClassInfo[] convertClasses(ClassDoc[] classes) {
    if (classes == null) {
      return null;
    }
    int N = classes.length;
    ClassInfo[] result = new ClassInfo[N];
    for (int i = 0; i < N; i++) {
      result[i] = Converter.obtainClass(classes[i]);
    }
    return result;
  }

  private static ParameterInfo convertParameter(Parameter p, SourcePosition pos, boolean isVarArg) {
    if (p == null) return null;
    ParameterInfo pi =
        new ParameterInfo(p.name(), p.typeName(), Converter.obtainType(p.type()), isVarArg,
          Converter.convertSourcePosition(pos), Converter.convertAnnotationInstances(p.annotations()));
    return pi;
  }

  private static ParameterInfo[] convertParameters(Parameter[] p, ExecutableMemberDoc m) {
    SourcePosition pos = m.position();
    int len = p.length;
    ParameterInfo[] q = new ParameterInfo[len];
    for (int i = 0; i < len; i++) {
      boolean isVarArg = (m.isVarArgs() && i == len - 1);
      q[i] = Converter.convertParameter(p[i], pos, isVarArg);
    }
    return q;
  }

  private static TypeInfo[] convertTypes(Type[] p) {
    if (p == null) return null;
    int len = p.length;
    TypeInfo[] q = new TypeInfo[len];
    for (int i = 0; i < len; i++) {
      q[i] = Converter.obtainType(p[i]);
    }
    return q;
  }

  private Converter() {}

  private static class ClassNeedingInit {
    ClassNeedingInit(ClassDoc c, ClassInfo cl) {
      this.c = c;
      this.cl = cl;
    }

    ClassDoc c;
    ClassInfo cl;
  }

  private static ArrayList<ClassNeedingInit> mClassesNeedingInit =
      new ArrayList<ClassNeedingInit>();

  static ClassInfo obtainClass(ClassDoc o) {
    return mClasses.obtain(o);
  }

  private static Cache<ClassDoc, ClassInfo> mClasses = new Cache<ClassDoc, ClassInfo>() {
    @Override
    protected ClassInfo make(ClassDoc input) {
      // A bug while generating OpenJDK documentation reports some classes with no name.
      if (input.name() == null || input.name().equals("")) {
         return null;
      }
      ClassInfo cl =
          new ClassInfo(input, input.getRawCommentText(), Converter.convertSourcePosition(input.position()), input
              .isPublic(), input.isProtected(), input.isPackagePrivate(), input.isPrivate(), input.isStatic(), input
              .isInterface(), input.isAbstract(), input.isOrdinaryClass(), input.isException(), input.isError(), input
              .isEnum(), (input instanceof AnnotationTypeDoc), input.isFinal(), input.isIncluded(), input.name(), input
              .qualifiedName(), input.qualifiedTypeName(), input.isPrimitive());
      if (mClassesNeedingInit != null) {
        mClassesNeedingInit.add(new ClassNeedingInit(input, cl));
      }
      return cl;
    }

    @Override
    protected void made(ClassDoc input, ClassInfo output) {
      if (mClassesNeedingInit == null) {
        initClass(input, output);
        output.init2();
      }
    }

    @Override
    ClassInfo[] all() {
      return mCache.values().toArray(new ClassInfo[mCache.size()]);
    }
  };

  private static MethodInfo[] getHiddenMethods(MethodDoc[] methods) {
    if (methods == null) return null;
    ArrayList<MethodInfo> out = new ArrayList<MethodInfo>();
    int N = methods.length;
    for (int i = 0; i < N; i++) {
      MethodInfo m = Converter.obtainMethod(methods[i]);
      // System.out.println(m.toString() + ": ");
      // for (TypeInfo ti : m.getTypeParameters()){
      // if (ti.asClassInfo() != null){
      // System.out.println(" " +ti.asClassInfo().toString());
      // } else {
      // System.out.println(" null");
      // }
      // }
      if (m.isHidden()) {
        out.add(m);
      }
    }
    return out.toArray(new MethodInfo[out.size()]);
  }

  /**
   * Convert MethodDoc[] into MethodInfo[]. Also filters according to the -private, -public option,
   * because the filtering doesn't seem to be working in the ClassDoc.constructors(boolean) call.
   */
  private static MethodInfo[] convertMethods(MethodDoc[] methods) {
    if (methods == null) return null;
    ArrayList<MethodInfo> out = new ArrayList<MethodInfo>();
    int N = methods.length;
    for (int i = 0; i < N; i++) {
      MethodInfo m = Converter.obtainMethod(methods[i]);
      // System.out.println(m.toString() + ": ");
      // for (TypeInfo ti : m.getTypeParameters()){
      // if (ti.asClassInfo() != null){
      // System.out.println(" " +ti.asClassInfo().toString());
      // } else {
      // System.out.println(" null");
      // }
      // }
      if (m.checkLevel()) {
        out.add(m);
      }
    }
    return out.toArray(new MethodInfo[out.size()]);
  }

  private static MethodInfo[] convertMethods(ConstructorDoc[] methods) {
    if (methods == null) return null;
    ArrayList<MethodInfo> out = new ArrayList<MethodInfo>();
    int N = methods.length;
    for (int i = 0; i < N; i++) {
      MethodInfo m = Converter.obtainMethod(methods[i]);
      if (m.checkLevel()) {
        out.add(m);
      }
    }
    return out.toArray(new MethodInfo[out.size()]);
  }

  private static MethodInfo[] convertNonWrittenConstructors(ConstructorDoc[] methods) {
    if (methods == null) return null;
    ArrayList<MethodInfo> out = new ArrayList<MethodInfo>();
    int N = methods.length;
    for (int i = 0; i < N; i++) {
      MethodInfo m = Converter.obtainMethod(methods[i]);
      if (!m.checkLevel()) {
        out.add(m);
      }
    }
    return out.toArray(new MethodInfo[out.size()]);
  }

  private static MethodInfo obtainMethod(MethodDoc o) {
    return mMethods.obtain(o);
  }

  private static MethodInfo obtainMethod(ConstructorDoc o) {
    return mMethods.obtain(o);
  }

  private static Cache<ExecutableMemberDoc, MethodInfo> mMethods
      = new Cache<ExecutableMemberDoc, MethodInfo>() {
    @Override
    protected MethodInfo make(ExecutableMemberDoc o) {
      if (o instanceof AnnotationTypeElementDoc) {
        AnnotationTypeElementDoc m = (AnnotationTypeElementDoc) o;
        MethodInfo result =
            new MethodInfo(m.getRawCommentText(), Converter.convertTypes(m.typeParameters()), m
                .name(), m.signature(), Converter.obtainClass(m.containingClass()), Converter
                .obtainClass(m.containingClass()), m.isPublic(), m.isProtected(), m
                .isPackagePrivate(), m.isPrivate(), m.isFinal(), m.isStatic(), m.isSynthetic(), m
                .isAbstract(), m.isSynchronized(), m.isNative(), true, "annotationElement", m
                .flatSignature(), Converter.obtainMethod(m.overriddenMethod()), Converter
                .obtainType(m.returnType()), Converter.convertParameters(m.parameters(), m),
                Converter.convertClasses(m.thrownExceptions()), Converter.convertSourcePosition(m
                    .position()), Converter.convertAnnotationInstances(m.annotations()));
        result.setVarargs(m.isVarArgs());
        result.init(Converter.obtainAnnotationValue(m.defaultValue(), result));
        return result;
      } else if (o instanceof MethodDoc) {
        MethodDoc m = (MethodDoc) o;
        MethodInfo result =
            new MethodInfo(m.getRawCommentText(), Converter.convertTypes(m.typeParameters()), m
                .name(), m.signature(), Converter.obtainClass(m.containingClass()), Converter
                .obtainClass(m.containingClass()), m.isPublic(), m.isProtected(), m
                .isPackagePrivate(), m.isPrivate(), m.isFinal(), m.isStatic(), m.isSynthetic(), m
                .isAbstract(), m.isSynchronized(), m.isNative(), false, "method",
                m.flatSignature(), Converter.obtainMethod(m.overriddenMethod()), Converter
                    .obtainType(m.returnType()), Converter.convertParameters(m.parameters(), m),
                Converter.convertClasses(m.thrownExceptions()), Converter.convertSourcePosition(m
                    .position()), Converter.convertAnnotationInstances(m.annotations()));
        result.setVarargs(m.isVarArgs());
        result.init(null);
        return result;
      } else {
        ConstructorDoc m = (ConstructorDoc) o;
        MethodInfo result =
            new MethodInfo(m.getRawCommentText(), Converter.convertTypes(m.typeParameters()), m
                .name(), m.signature(), Converter.obtainClass(m.containingClass()), Converter
                .obtainClass(m.containingClass()), m.isPublic(), m.isProtected(), m
                .isPackagePrivate(), m.isPrivate(), m.isFinal(), m.isStatic(), m.isSynthetic(),
                false, m.isSynchronized(), m.isNative(), false, "constructor", m.flatSignature(),
                null, null, Converter.convertParameters(m.parameters(), m), Converter
                    .convertClasses(m.thrownExceptions()), Converter.convertSourcePosition(m
                    .position()), Converter.convertAnnotationInstances(m.annotations()));
        result.setVarargs(m.isVarArgs());
        result.init(null);
        return result;
      }
    }
  };


  private static FieldInfo[] convertFields(FieldDoc[] fields) {
    if (fields == null) return null;
    ArrayList<FieldInfo> out = new ArrayList<FieldInfo>();
    int N = fields.length;
    for (int i = 0; i < N; i++) {
      FieldInfo f = Converter.obtainField(fields[i]);
      //if (f.checkLevel()) {
        out.add(f);
      //}
    }
    return out.toArray(new FieldInfo[out.size()]);
  }

  private static FieldInfo obtainField(FieldDoc o) {
    return mFields.obtain(o);
  }

  private static Cache<FieldDoc, FieldInfo> mFields = new Cache<FieldDoc, FieldInfo>() {
    @Override
    protected FieldInfo make(FieldDoc f) {
      return new FieldInfo(f.name(), Converter.obtainClass(f.containingClass()), Converter
          .obtainClass(f.containingClass()), f.isPublic(), f.isProtected(), f.isPackagePrivate(), f
          .isPrivate(), f.isFinal(), f.isStatic(), f.isTransient(), f.isVolatile(),
          f.isSynthetic(), Converter.obtainType(f.type()), f.getRawCommentText(),
          f.constantValue(), Converter.convertSourcePosition(f.position()), Converter
              .convertAnnotationInstances(f.annotations()));
    }
  };

  private static PackageInfo obtainPackage(PackageDoc o) {
    return mPackages.obtain(o);
  }

  private static Cache<PackageDoc, PackageInfo> mPackages = new Cache<PackageDoc, PackageInfo>() {
    @Override
    protected PackageInfo make(PackageDoc p) {
      return new PackageInfo(p, p.name(), Converter.convertSourcePosition(p.position()));
    }
  };

  private static TypeInfo obtainType(Type o) {
    return mTypes.obtain(o);
  }

  private static Cache<Type, TypeInfo> mTypes = new Cache<Type, TypeInfo>() {
    @Override
    protected TypeInfo make(Type t) {
      String simpleTypeName;
      if (t instanceof ClassDoc) {
        simpleTypeName = ((ClassDoc) t).name();
      } else {
        simpleTypeName = t.simpleTypeName();
      }
      TypeInfo ti =
          new TypeInfo(t.isPrimitive(), t.dimension(), simpleTypeName, t.qualifiedTypeName(),
              Converter.obtainClass(t.asClassDoc()));
      return ti;
    }

    @Override
    protected void made(Type t, TypeInfo ti) {
      if (t.asParameterizedType() != null) {
        ti.setTypeArguments(Converter.convertTypes(t.asParameterizedType().typeArguments()));
      } else if (t instanceof ClassDoc) {
        ti.setTypeArguments(Converter.convertTypes(((ClassDoc) t).typeParameters()));
      } else if (t.asTypeVariable() != null) {
        ti.setBounds(null, Converter.convertTypes((t.asTypeVariable().bounds())));
        ti.setIsTypeVariable(true);
      } else if (t.asWildcardType() != null) {
        ti.setIsWildcard(true);
        ti.setBounds(Converter.convertTypes(t.asWildcardType().superBounds()), Converter
            .convertTypes(t.asWildcardType().extendsBounds()));
      }
    }

    @Override
    protected Object keyFor(Type t) {
      StringBuilder result = new StringBuilder();
      result.append(t.getClass().getName()).append("/").append(t).append("/");
      if (t.asParameterizedType() != null) {
        result.append(t.asParameterizedType()).append("/");
        if (t.asParameterizedType().typeArguments() != null) {
          for (Type ty : t.asParameterizedType().typeArguments()) {
            result.append(ty).append("/");
          }
        }
      } else {
        result.append("NoParameterizedType//");
      }
      if (t.asTypeVariable() != null) {
        result.append(t.asTypeVariable()).append("/");
        if (t.asTypeVariable().bounds() != null) {
          for (Type ty : t.asTypeVariable().bounds()) {
            result.append(ty).append("/");
          }
        }
      } else {
        result.append("NoTypeVariable//");
      }
      if (t.asWildcardType() != null) {
        result.append(t.asWildcardType()).append("/");
        if (t.asWildcardType().superBounds() != null) {
          for (Type ty : t.asWildcardType().superBounds()) {
            result.append(ty).append("/");
          }
        }
        if (t.asWildcardType().extendsBounds() != null) {
          for (Type ty : t.asWildcardType().extendsBounds()) {
            result.append(ty).append("/");
          }
        }
      } else {
        result.append("NoWildCardType//");
      }

      return result.toString();
    }
  };
  
  public static TypeInfo obtainTypeFromString(String type) {
    return mTypesFromString.obtain(type);
  }
  
  private static final Cache<String, TypeInfo> mTypesFromString = new Cache<String, TypeInfo>() {
    @Override
    protected TypeInfo make(String name) {
      return new TypeInfo(name);
    }
  };

  private static AnnotationInstanceInfo[] convertAnnotationInstances(AnnotationDesc[] orig) {
    int len = orig.length;
    AnnotationInstanceInfo[] out = new AnnotationInstanceInfo[len];
    for (int i = 0; i < len; i++) {
      out[i] = Converter.obtainAnnotationInstance(orig[i]);
    }
    return out;
  }


  private static AnnotationInstanceInfo obtainAnnotationInstance(AnnotationDesc o) {
    return mAnnotationInstances.obtain(o);
  }

  private static Cache<AnnotationDesc, AnnotationInstanceInfo> mAnnotationInstances
      = new Cache<AnnotationDesc, AnnotationInstanceInfo>() {
    @Override
    protected AnnotationInstanceInfo make(AnnotationDesc a) {
      ClassInfo annotationType;
      try {
    	  annotationType = Converter.obtainClass(a.annotationType());
      } catch (ClassCastException e) {
    	  throw new IllegalArgumentException("Could not find annotation " + a.toString() + " on classpath.", e);
      }
      AnnotationDesc.ElementValuePair[] ev = a.elementValues();
      AnnotationValueInfo[] elementValues = new AnnotationValueInfo[ev.length];
      for (int i = 0; i < ev.length; i++) {
        elementValues[i] =
            obtainAnnotationValue(ev[i].value(), Converter.obtainMethod(ev[i].element()));
      }
      return new AnnotationInstanceInfo(annotationType, elementValues);
    }
  };


  private abstract static class Cache<K, V> {
    V obtain(K input) {
      if (input == null) {
        return null;
      }
      Object key = keyFor(input);
      V value = mCache.get(key);
      if (value == null) {
        value = make(input);
        if (value != null) {
          mCache.put(key, value);
          made(input, value);
        }
      }
      return value;
    }

    protected final HashMap<Object, V> mCache = new HashMap<Object, V>();

    protected abstract V make(K input);

    protected void made(K input, V value) {}

    protected Object keyFor(K key) {
      return key;
    }

    Object[] all() {
      return null;
    }
  }

  // annotation values
  private static HashMap<AnnotationValue, AnnotationValueInfo> mAnnotationValues =
      new HashMap<AnnotationValue, AnnotationValueInfo>();
  private static HashSet<AnnotationValue> mAnnotationValuesNeedingInit =
      new HashSet<AnnotationValue>();

  private static AnnotationValueInfo obtainAnnotationValue(AnnotationValue o, MethodInfo element) {
    if (o == null) {
      return null;
    }
    AnnotationValueInfo v = mAnnotationValues.get(o);
    if (v != null) return v;
    v = new AnnotationValueInfo(element);
    mAnnotationValues.put(o, v);
    if (mAnnotationValuesNeedingInit != null) {
      mAnnotationValuesNeedingInit.add(o);
    } else {
      initAnnotationValue(o, v);
    }
    return v;
  }

  private static void initAnnotationValue(AnnotationValue o, AnnotationValueInfo v) {
    Object orig = o.value();
    Object converted;
    if (orig instanceof Type) {
      // class literal
      converted = Converter.obtainType((Type) orig);
    } else if (orig instanceof FieldDoc) {
      // enum constant
      converted = Converter.obtainField((FieldDoc) orig);
    } else if (orig instanceof AnnotationDesc) {
      // annotation instance
      converted = Converter.obtainAnnotationInstance((AnnotationDesc) orig);
    } else if (orig instanceof AnnotationValue[]) {
      AnnotationValue[] old = (AnnotationValue[]) orig;
      AnnotationValueInfo[] array = new AnnotationValueInfo[old.length];
      for (int i = 0; i < array.length; i++) {
        array[i] = Converter.obtainAnnotationValue(old[i], null);
      }
      converted = array;
    } else {
      converted = orig;
    }
    v.init(converted);
  }

  private static void finishAnnotationValueInit() {
    while (mAnnotationValuesNeedingInit.size() > 0) {
      HashSet<AnnotationValue> set = mAnnotationValuesNeedingInit;
      mAnnotationValuesNeedingInit = new HashSet<AnnotationValue>();
      for (AnnotationValue o : set) {
        AnnotationValueInfo v = mAnnotationValues.get(o);
        initAnnotationValue(o, v);
      }
    }
    mAnnotationValuesNeedingInit = null;
  }
}
