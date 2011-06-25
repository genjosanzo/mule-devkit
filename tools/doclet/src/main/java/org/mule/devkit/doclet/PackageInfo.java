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

import com.sun.javadoc.*;
import java.util.*;

public class PackageInfo extends DocInfo implements ContainerInfo {
  public static final String DEFAULT_PACKAGE = "default package";
  
  public static final Comparator<PackageInfo> comparator = new Comparator<PackageInfo>() {
    public int compare(PackageInfo a, PackageInfo b) {
      return a.name().compareTo(b.name());
    }
  };

  public PackageInfo(PackageDoc pkg, String name, SourcePositionInfo position) {
    super(pkg.getRawCommentText(), position);
    if (name.isEmpty()) {
      mName = DEFAULT_PACKAGE;
    } else {
      mName = name;
    }

    mPackage = pkg;
  }

  public PackageInfo(String name) {
    super("", null);
    mName = name;
  }

  public PackageInfo(String name, SourcePositionInfo position) {
    super("", position);
    
    if (name.isEmpty()) {
      mName = "default package";
    } else {
      mName = name;
    }
  }

  public boolean isDefinedLocally() {
    return true;
  }

  public String relativePath() {
    String s = mName;
    s = s.replace('.', '/');
    s += "/package-summary.html";
    return s;
  }

  public String fullDescriptionFile() {
    String s = mName;
    s = s.replace('.', '/');
    s += "/package-descr.html";
    return s;
  }
  
  public String fullDescriptionHtmlPage() {
    return htmlPage().replace("/package-summary.html", "/package-descr.html");
  }

  @Override
  public ContainerInfo parent() {
    return null;
  }

  @Override
  public boolean isHidden() {
    return comment().isHidden();
  }

  public boolean checkLevel() {
    // TODO should return false if all classes are hidden but the package isn't.
    // We don't have this so I'm not doing it now.
    return !isHidden();
  }

  public String name() {
    return mName;
  }

  public String qualifiedName() {
    return mName;
  }

  public TagInfo[] inlineTags() {
    return comment().tags();
  }

  public TagInfo[] firstSentenceTags() {
    return comment().briefTags();
  }

  public static ClassInfo[] filterHidden(ClassInfo[] classes) {
    ArrayList<ClassInfo> out = new ArrayList<ClassInfo>();

    for (ClassInfo cl : classes) {
      if (!cl.isHidden()) {
        out.add(cl);
      }
    }

    return out.toArray(new ClassInfo[0]);
  }

  public void makeLink(Data data, String base) {
    if (checkLevel()) {
      data.setValue(base + ".link", htmlPage());
    }
    data.setValue(base + ".name", name());
    data.setValue(base + ".since.key", SinceTagger.keyForName(getSince()));
    data.setValue(base + ".since.name", getSince());
  }

  public void makeClassLinkListHDF(Data data, String base) {
    makeLink(data, base);
    ClassInfo.makeLinkListHDF(data, base + ".annotations", getAnnotations());
    ClassInfo.makeLinkListHDF(data, base + ".interfaces", getInterfaces());
    ClassInfo.makeLinkListHDF(data, base + ".classes", ordinaryClasses());
    ClassInfo.makeLinkListHDF(data, base + ".enums", enums());
    ClassInfo.makeLinkListHDF(data, base + ".exceptions", exceptions());
    ClassInfo.makeLinkListHDF(data, base + ".errors", errors());
    data.setValue(base + ".since.key", SinceTagger.keyForName(getSince()));
    data.setValue(base + ".since.name", getSince());
  }

  /**
   * Returns the list of annotations defined in this package.
   * @return
   */
  public ClassInfo[] getAnnotations() {
    if (mAnnotations == null) {
    	mAnnotations = ClassInfo.sortByName(
    	    filterHidden(Converter.convertClasses(mPackage.annotationTypes())));
    }
    return mAnnotations;
  }
  
  public ClassInfo[] getInterfaces() {
    if (mInterfaces == null) {
      mInterfaces =
          ClassInfo.sortByName(filterHidden(Converter.convertClasses(mPackage.interfaces())));
    }
    return mInterfaces;
  }

  public ClassInfo[] ordinaryClasses() {
    if (mOrdinaryClasses == null) {
      mOrdinaryClasses =
          ClassInfo.sortByName(filterHidden(Converter.convertClasses(mPackage.ordinaryClasses())));
    }
    return mOrdinaryClasses;
  }

  public ClassInfo[] enums() {
    if (mEnums == null) {
      mEnums = ClassInfo.sortByName(filterHidden(Converter.convertClasses(mPackage.enums())));
    }
    return mEnums;
  }

  public ClassInfo[] exceptions() {
    if (mExceptions == null) {
      mExceptions =
          ClassInfo.sortByName(filterHidden(Converter.convertClasses(mPackage.exceptions())));
    }
    return mExceptions;
  }

  public ClassInfo[] errors() {
    if (mErrors == null) {
      mErrors = ClassInfo.sortByName(filterHidden(Converter.convertClasses(mPackage.errors())));
    }
    return mErrors;
  }

  // in hashed containers, treat the name as the key
  @Override
  public int hashCode() {
    return mName.hashCode();
  }

  private String mName;
  private PackageDoc mPackage;
  private ClassInfo[] mAnnotations;
  private ClassInfo[] mInterfaces;
  private ClassInfo[] mOrdinaryClasses;
  private ClassInfo[] mEnums;
  private ClassInfo[] mExceptions;
  private ClassInfo[] mErrors;
  
  // TODO: Leftovers from ApiCheck that should be better merged.
  private HashMap<String, ClassInfo> mClasses = new HashMap<String, ClassInfo>();
  
  public void addClass(ClassInfo cl) {
    mClasses.put(cl.name(), cl);
  }

  public HashMap<String, ClassInfo> allClasses() {
    return mClasses;
  }
  
  public boolean isConsistent(PackageInfo pInfo) {
    boolean consistent = true;
    for (ClassInfo cInfo : mClasses.values()) {
      if (pInfo.mClasses.containsKey(cInfo.name())) {
        if (!cInfo.isConsistent(pInfo.mClasses.get(cInfo.name()))) {
          consistent = false;
        }
      } else {
        Errors.error(Errors.REMOVED_CLASS, cInfo.position(), "Removed public class "
            + cInfo.qualifiedName());
        consistent = false;
      }
    }
    for (ClassInfo cInfo : pInfo.mClasses.values()) {
      if (!mClasses.containsKey(cInfo.name())) {
        Errors.error(Errors.ADDED_CLASS, cInfo.position(), "Added class " + cInfo.name()
            + " to package " + pInfo.name());
        consistent = false;
      }
    }
    return consistent;
  }
}
