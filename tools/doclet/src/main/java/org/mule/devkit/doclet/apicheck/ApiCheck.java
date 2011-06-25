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

package org.mule.devkit.doclet.apicheck;

import org.mule.devkit.doclet.AnnotationInstanceInfo;
import org.mule.devkit.doclet.ClassInfo;
import org.mule.devkit.doclet.ConstructorInfo;
import org.mule.devkit.doclet.Converter;
import org.mule.devkit.doclet.ErrorReport;
import org.mule.devkit.doclet.Errors;
import org.mule.devkit.doclet.FieldInfo;
import org.mule.devkit.doclet.MethodInfo;
import org.mule.devkit.doclet.PackageInfo;
import org.mule.devkit.doclet.ParameterInfo;
import org.mule.devkit.doclet.SourcePositionInfo;
import org.mule.devkit.doclet.TypeInfo;
import com.sun.javadoc.ClassDoc;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Stack;

import org.mule.devkit.doclet.AnnotationInstanceInfo;
import org.mule.devkit.doclet.ClassInfo;
import org.mule.devkit.doclet.ConstructorInfo;
import org.mule.devkit.doclet.Converter;
import org.mule.devkit.doclet.ErrorReport;
import org.mule.devkit.doclet.Errors;
import org.mule.devkit.doclet.MethodInfo;
import org.mule.devkit.doclet.PackageInfo;
import org.mule.devkit.doclet.ParameterInfo;
import org.mule.devkit.doclet.SourcePositionInfo;
import org.mule.devkit.doclet.TypeInfo;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class ApiCheck {
  // parse out and consume the -whatever command line flags
  private static ArrayList<String[]> parseFlags(ArrayList<String> allArgs) {
    ArrayList<String[]> ret = new ArrayList<String[]>();

    int i;
    for (i = 0; i < allArgs.size(); i++) {
      // flags with one value attached
      String flag = allArgs.get(i);
      if (flag.equals("-error") || flag.equals("-warning") || flag.equals("-hide")) {
        String[] arg = new String[2];
        arg[0] = flag;
        arg[1] = allArgs.get(++i);
        ret.add(arg);
      } else {
        // we've consumed all of the -whatever args, so we're done
        break;
      }
    }

    // i now points to the first non-flag arg; strip what came before
    for (; i > 0; i--) {
      allArgs.remove(0);
    }
    return ret;
  }

  public static void main(String[] originalArgs) {
    ApiCheck acheck = new ApiCheck();
    ErrorReport report = acheck.checkApi(originalArgs);
   
    Errors.printErrors(report.getErrors());
    System.exit(report.getCode());
  }
  
  /**
   * Compares two api xml files for consistency.
   */
  public ErrorReport checkApi(String[] originalArgs) {
    // translate to an ArrayList<String> for munging
    ArrayList<String> args = new ArrayList<String>(originalArgs.length);
    for (String a : originalArgs) {
      args.add(a);
    }

    ArrayList<String[]> flags = ApiCheck.parseFlags(args);
    for (String[] a : flags) {
      if (a[0].equals("-error") || a[0].equals("-warning") || a[0].equals("-hide")) {
        try {
          int level = -1;
          if (a[0].equals("-error")) {
            level = Errors.ERROR;
          } else if (a[0].equals("-warning")) {
            level = Errors.WARNING;
          } else if (a[0].equals("-hide")) {
            level = Errors.HIDDEN;
          }
          Errors.setErrorLevel(Integer.parseInt(a[1]), level);
        } catch (NumberFormatException e) {
          System.err.println("Bad argument: " + a[0] + " " + a[1]);
          return new ErrorReport(Errors.EXIT_BAD_ARGUMENTS, Errors.getErrors());
        }
      }
    }

    ApiInfo oldApi;
    ApiInfo newApi;
    
    try {
      oldApi = parseApi(args.get(0));
      newApi = parseApi(args.get(1));
    } catch (ApiParseException e) {
      e.printStackTrace();
      System.err.println("Error parsing API");
      return new ErrorReport(Errors.EXIT_PARSE_ERROR, Errors.getErrors());
    }

    // only run the consistency check if we haven't had XML parse errors
    if (!Errors.hadError) {
      oldApi.isConsistent(newApi);
    }

    return new ErrorReport(Errors.hadError ? Errors.EXIT_ERROR : Errors.EXIT_NORMAL,
        Errors.getErrors());
  }

  private InputStream getInputStreamForFile(String filename) throws IOException {
    try {
      URL url = new URL(filename);
      return url.openStream();
    } catch (MalformedURLException e) {
      return new FileInputStream(filename);
    }
  }
  
  public ApiInfo parseApi(String xmlFile) throws ApiParseException {
    InputStream inStream = null;
    try {
      inStream = getInputStreamForFile(xmlFile);
      return parseApi(inStream);
    } catch (IOException e) {
      throw new ApiParseException("Error parsing xml: " + xmlFile, e);
    } finally {
      if (inStream != null) {
        try {
          inStream.close();
        } catch (IOException ignored) {}
      }
    }
  }
  
  public ApiInfo parseApi(URL xmlURL) throws ApiParseException {
    InputStream xmlStream = null;
    try {
      xmlStream = xmlURL.openStream();
      return parseApi(xmlStream);
    } catch (IOException e) {
      throw new ApiParseException("Could not open stream for parsing: " + xmlURL,e);
    } finally {
      if (xmlStream != null) {
        try {
          xmlStream.close();
        } catch (IOException ignored) {}
      }
    }
  }
  
  public ApiInfo parseApi(InputStream xmlStream) throws ApiParseException {
    try {
      XMLReader xmlreader = XMLReaderFactory.createXMLReader();
      MakeHandler handler = new MakeHandler();
      xmlreader.setContentHandler(handler);
      xmlreader.setErrorHandler(handler);
      xmlreader.parse(new InputSource(xmlStream));
      ApiInfo apiInfo = handler.getApi();
      apiInfo.resolveSuperclasses();
      apiInfo.resolveInterfaces();
      return apiInfo;
    } catch (Exception e) {
      throw new ApiParseException("Error parsing API", e);
    }
  }
  
  private class MakeHandler extends DefaultHandler {

    private ApiInfo mApi;
    private PackageInfo mCurrentPackage;
    private ClassInfo mCurrentClass;
    private AbstractMethodInfo mCurrentMethod;
    private Stack<ClassInfo> mClassScope = new Stack<ClassInfo>();
    

    public MakeHandler() {
      super();
      mApi = new ApiInfo();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
      if (qName.equals("package")) {
        mCurrentPackage =
            new PackageInfo(attributes.getValue("name"), SourcePositionInfo.fromXml(attributes
                .getValue("source")));
      } else if (qName.equals("class") || qName.equals("interface")) {
        // push the old outer scope for later recovery, then set
        // up the new current class object
        mClassScope.push(mCurrentClass);
        
        ClassDoc classDoc = null;
        String rawCommentText = "";
        SourcePositionInfo position = SourcePositionInfo.fromXml(attributes.getValue("source"));
        String visibility = attributes.getValue("visibility");
        boolean isPublic = "public".equals(visibility);
        boolean isProtected = "protected".equals(visibility);
        boolean isPrivate = "private".equals(visibility); 
        boolean isPackagePrivate = !isPublic && !isPrivate && !isProtected;
        boolean isStatic = Boolean.valueOf(attributes.getValue("static"));
        boolean isInterface = qName.equals("interface");
        boolean isAbstract = Boolean.valueOf(attributes.getValue("abstract"));
        boolean isOrdinaryClass = qName.equals("class");
        boolean isException = false; // TODO: check hierarchy for java.lang.Exception
        boolean isError = false; // TODO: not sure.
        boolean isEnum = false; // TODO: not sure.
        boolean isAnnotation = false; // TODO: not sure.
        boolean isFinal = Boolean.valueOf(attributes.getValue("final"));
        boolean isIncluded = false;
        String name = attributes.getValue("name");
        String qualifiedName = qualifiedName(mCurrentPackage.name(), name, mCurrentClass);
        String qualifiedTypeName = null; // TODO: not sure
        boolean isPrimitive = false;
        
        mCurrentClass =
            new ClassInfo(classDoc, rawCommentText, position, isPublic, isProtected, 
            isPackagePrivate, isPrivate, isStatic, isInterface, isAbstract, isOrdinaryClass, 
            isException, isError, isEnum, isAnnotation, isFinal, isIncluded, name, qualifiedName,
            qualifiedTypeName, isPrimitive);
        
        mCurrentClass.setContainingPackage(mCurrentPackage);
        String superclass = attributes.getValue("extends");
        if (superclass == null && !isInterface && !"java.lang.Object".equals(qualifiedName)) {
          throw new AssertionError("no superclass known for class " + name);
        }
        
        // Resolve superclass after .xml completely parsed.
        mApi.mapClassToSuper(mCurrentClass, superclass);
        
        TypeInfo typeInfo = Converter.obtainTypeFromString(qualifiedName) ;
        mCurrentClass.setTypeInfo(typeInfo);
        mCurrentClass.setAnnotations(new AnnotationInstanceInfo[] {});
      } else if (qName.equals("method")) {
        String rawCommentText = "";
        TypeInfo[] typeParameters = new TypeInfo[0];
        String name = attributes.getValue("name");
        String signature = null; // TODO
        ClassInfo containingClass = mCurrentClass;
        ClassInfo realContainingClass = mCurrentClass;
        String visibility = attributes.getValue("visibility");
        boolean isPublic = "public".equals(visibility);
        boolean isProtected = "protected".equals(visibility);
        boolean isPrivate = "private".equals(visibility); 
        boolean isPackagePrivate = !isPublic && !isPrivate && !isProtected;
        boolean isFinal = Boolean.valueOf(attributes.getValue("final"));
        boolean isStatic = Boolean.valueOf(attributes.getValue("static"));
        boolean isSynthetic = false; // TODO
        boolean isAbstract = Boolean.valueOf(attributes.getValue("abstract"));
        boolean isSynchronized = Boolean.valueOf(attributes.getValue("synchronized"));
        boolean isNative = Boolean.valueOf(attributes.getValue("native"));
        boolean isAnnotationElement = false; // TODO
        String kind = qName;
        String flatSignature = null; // TODO
        MethodInfo overriddenMethod = null; // TODO
        TypeInfo returnType = Converter.obtainTypeFromString(attributes.getValue("return"));
        ParameterInfo[] parameters = new ParameterInfo[0];
        ClassInfo[] thrownExceptions = new ClassInfo[0];
        SourcePositionInfo position = SourcePositionInfo.fromXml(attributes.getValue("source"));
        AnnotationInstanceInfo[] annotations = new AnnotationInstanceInfo[] {}; // TODO
        
        mCurrentMethod = 
            new MethodInfo(rawCommentText, typeParameters, name, signature, containingClass,
            realContainingClass, isPublic, isProtected, isPackagePrivate, isPrivate, isFinal,
            isStatic, isSynthetic, isAbstract, isSynchronized, isNative, isAnnotationElement, kind,
            flatSignature, overriddenMethod, returnType, parameters, thrownExceptions, position,
            annotations);
        
        mCurrentMethod.setDeprecated("deprecated".equals(attributes.getValue("deprecated")));
      } else if (qName.equals("constructor")) {
        mCurrentMethod =
            new ConstructorInfo(attributes.getValue("name"), attributes.getValue("type"), Boolean
                .valueOf(attributes.getValue("static")), Boolean.valueOf(attributes
                .getValue("final")), attributes.getValue("deprecated"), attributes
                .getValue("visibility"), SourcePositionInfo.fromXml(attributes.getValue("source")),
                mCurrentClass);
      } else if (qName.equals("field")) {
        String visibility = attributes.getValue("visibility");
        boolean isPublic = visibility.equals("public");
        boolean isProtected = visibility.equals("protected");
        boolean isPrivate = visibility.equals("private");
        boolean isPackagePrivate = visibility.equals("");
        String typeName = attributes.getValue("type");
        TypeInfo type = Converter.obtainTypeFromString(typeName);
        
        FieldInfo fInfo =
            new FieldInfo(attributes.getValue("name"), mCurrentClass, mCurrentClass, isPublic,
            isProtected, isPackagePrivate, isPrivate, Boolean.valueOf(attributes.getValue("final")),
            Boolean.valueOf(attributes.getValue("static")), Boolean.valueOf(attributes.
            getValue("transient")), Boolean.valueOf(attributes.getValue("volatile")), false,
            type, "", attributes.getValue("value"), SourcePositionInfo
            .fromXml(attributes.getValue("source")), new AnnotationInstanceInfo[] {});
        
        fInfo.setDeprecated("deprecated".equals(attributes.getValue("deprecated")));
        mCurrentClass.addField(fInfo);
      } else if (qName.equals("parameter")) {
        String name = attributes.getValue("name");
        String typeName = attributes.getValue("type");
        TypeInfo type = Converter.obtainTypeFromString(typeName);
        boolean isVarArg = typeName.endsWith("...");
        SourcePositionInfo position = null;
        
        mCurrentMethod.addParameter(new ParameterInfo(name, typeName, type, isVarArg, position));
        mCurrentMethod.setVarargs(isVarArg);
      } else if (qName.equals("exception")) {
        mCurrentMethod.addException(attributes.getValue("type"));
      } else if (qName.equals("implements")) {
        // Resolve interfaces after .xml completely parsed.
        mApi.mapClassToInterface(mCurrentClass, attributes.getValue("name"));
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
      if (qName.equals("method")) {
        mCurrentClass.addMethod((MethodInfo) mCurrentMethod);
      } else if (qName.equals("constructor")) {
        mCurrentClass.addConstructor((ConstructorInfo) mCurrentMethod);
      } else if (qName.equals("class") || qName.equals("interface")) {
        mCurrentPackage.addClass(mCurrentClass);
        mCurrentClass = mClassScope.pop();
      } else if (qName.equals("package")) {
        mApi.addPackage(mCurrentPackage);
      }
    }

    public ApiInfo getApi() {
      return mApi;
    }
    
    private String qualifiedName(String pkg, String className, ClassInfo parent) {
      String parentQName = (parent != null) ? (parent.qualifiedName() + ".") : "";
      return pkg + "." + parentQName + className;
    }
  }
}
