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

import java.util.Set;
import java.util.TreeSet;

public class Errors {
  public static boolean hadError = false;
  private static boolean warningsAreErrors = false;
  private static TreeSet<ErrorMessage> allErrors = new TreeSet<ErrorMessage>();

  public static void error(ErrorCode error, SourcePositionInfo where, String text) {
    if (error.getLevel() == HIDDEN) {
      return;
    }

    int level = (!warningsAreErrors && error.getLevel() == WARNING) ? WARNING : ERROR;
    String which = level == WARNING ? " warning " : " error ";
    String message = which + error.getCode() + ": " + text;

    if (where == null) {
      where = SourcePositionInfo.UNKNOWN;
    }

    allErrors.add(new ErrorMessage(error, where, message));

    if (error.getLevel() == ERROR || (warningsAreErrors && error.getLevel() == WARNING)) {
      hadError = true;
    }
  }
  
  public static void clearErrors() {
    hadError = false;
    allErrors.clear();
  }

  public static void printErrors() {
    printErrors(allErrors);
  }
  
  public static void printErrors(Set<ErrorMessage> errors) {
    for (ErrorMessage m : errors) {
      if (m.getError().getLevel() == WARNING) {
        System.err.println(m.toString());
      }
    }
    for (ErrorMessage m : errors) {
      if (m.getError().getLevel() == ERROR) {
        System.err.println(m.toString());
      }
    }
  }
  
  public static Set<ErrorMessage> getErrors() {
    return allErrors;
  }

  public static int HIDDEN = 0;
  public static int WARNING = 1;
  public static int ERROR = 2;

  public static void setWarningsAreErrors(boolean val) {
    warningsAreErrors = val;
  }

  // Exit status codes
  public static final int EXIT_NORMAL = 0;
  public static final int EXIT_ERROR = 1;
  public static final int EXIT_BAD_ARGUMENTS = 2;
  public static final int EXIT_PARSE_ERROR = 3;

  // Errors for API verification
  public static final ErrorCode PARSE_ERROR = new ErrorCode(1, ERROR);
  public static final ErrorCode ADDED_PACKAGE = new ErrorCode(2, WARNING);
  public static final ErrorCode ADDED_CLASS = new ErrorCode(3, WARNING);
  public static final ErrorCode ADDED_METHOD = new ErrorCode(4, WARNING);
  public static final ErrorCode ADDED_FIELD = new ErrorCode(5, WARNING);
  public static final ErrorCode ADDED_INTERFACE = new ErrorCode(6, WARNING);
  public static final ErrorCode REMOVED_PACKAGE = new ErrorCode(7, WARNING);
  public static final ErrorCode REMOVED_CLASS = new ErrorCode(8, WARNING);
  public static final ErrorCode REMOVED_METHOD = new ErrorCode(9, WARNING);
  public static final ErrorCode REMOVED_FIELD = new ErrorCode(10, WARNING);
  public static final ErrorCode REMOVED_INTERFACE = new ErrorCode(11, WARNING);
  public static final ErrorCode CHANGED_STATIC = new ErrorCode(12, WARNING);
  public static final ErrorCode CHANGED_FINAL = new ErrorCode(13, WARNING);
  public static final ErrorCode CHANGED_TRANSIENT = new ErrorCode(14, WARNING);
  public static final ErrorCode CHANGED_VOLATILE = new ErrorCode(15, WARNING);
  public static final ErrorCode CHANGED_TYPE = new ErrorCode(16, WARNING);
  public static final ErrorCode CHANGED_VALUE = new ErrorCode(17, WARNING);
  public static final ErrorCode CHANGED_SUPERCLASS = new ErrorCode(18, WARNING);
  public static final ErrorCode CHANGED_SCOPE = new ErrorCode(19, WARNING);
  public static final ErrorCode CHANGED_ABSTRACT = new ErrorCode(20, WARNING);
  public static final ErrorCode CHANGED_THROWS = new ErrorCode(21, WARNING);
  public static final ErrorCode CHANGED_NATIVE = new ErrorCode(22, HIDDEN);
  public static final ErrorCode CHANGED_CLASS = new ErrorCode(23, WARNING);
  public static final ErrorCode CHANGED_DEPRECATED = new ErrorCode(24, WARNING);
  public static final ErrorCode CHANGED_SYNCHRONIZED = new ErrorCode(25, ERROR);

  // Errors in javadoc generation
  public static final ErrorCode UNRESOLVED_LINK = new ErrorCode(101, WARNING);
  public static final ErrorCode BAD_INCLUDE_TAG = new ErrorCode(102, WARNING);
  public static final ErrorCode UNKNOWN_TAG = new ErrorCode(103, WARNING);
  public static final ErrorCode UNKNOWN_PARAM_TAG_NAME = new ErrorCode(104, WARNING);
  public static final ErrorCode UNDOCUMENTED_PARAMETER = new ErrorCode(105, HIDDEN);
  public static final ErrorCode BAD_ATTR_TAG = new ErrorCode(106, ERROR);
  public static final ErrorCode BAD_INHERITDOC = new ErrorCode(107, HIDDEN);
  public static final ErrorCode HIDDEN_LINK = new ErrorCode(108, WARNING);
  public static final ErrorCode HIDDEN_CONSTRUCTOR = new ErrorCode(109, WARNING);
  public static final ErrorCode UNAVAILABLE_SYMBOL = new ErrorCode(110, ERROR);
  public static final ErrorCode HIDDEN_SUPERCLASS = new ErrorCode(111, WARNING);
  public static final ErrorCode DEPRECATED = new ErrorCode(112, HIDDEN);
  public static final ErrorCode DEPRECATION_MISMATCH = new ErrorCode(113, HIDDEN);
  public static final ErrorCode MISSING_COMMENT = new ErrorCode(114, WARNING);
  public static final ErrorCode IO_ERROR = new ErrorCode(115, HIDDEN);
  public static final ErrorCode NO_SINCE_DATA = new ErrorCode(116, HIDDEN);
  public static final ErrorCode NO_FEDERATION_DATA = new ErrorCode(117, WARNING);
  public static final ErrorCode NO_SINCE_FILE = new ErrorCode(118, WARNING);

  public static final ErrorCode[] ERRORS =
      {UNRESOLVED_LINK, BAD_INCLUDE_TAG, UNKNOWN_TAG, UNKNOWN_PARAM_TAG_NAME,
          UNDOCUMENTED_PARAMETER, BAD_ATTR_TAG, BAD_INHERITDOC, HIDDEN_LINK, HIDDEN_CONSTRUCTOR,
          UNAVAILABLE_SYMBOL, HIDDEN_SUPERCLASS, DEPRECATED, DEPRECATION_MISMATCH, MISSING_COMMENT,
          IO_ERROR, NO_SINCE_DATA, NO_FEDERATION_DATA, NO_SINCE_FILE, PARSE_ERROR, ADDED_PACKAGE,
          ADDED_CLASS, ADDED_METHOD, ADDED_FIELD, ADDED_INTERFACE, REMOVED_PACKAGE, REMOVED_CLASS,
          REMOVED_METHOD, REMOVED_FIELD, REMOVED_INTERFACE, CHANGED_STATIC, CHANGED_FINAL,
          CHANGED_TRANSIENT, CHANGED_VOLATILE, CHANGED_TYPE, CHANGED_VALUE, CHANGED_SUPERCLASS,
          CHANGED_SCOPE, CHANGED_ABSTRACT, CHANGED_THROWS, CHANGED_NATIVE, CHANGED_CLASS,
          CHANGED_DEPRECATED, CHANGED_SYNCHRONIZED};

  public static boolean setErrorLevel(int code, int level) {
    for (ErrorCode e : ERRORS) {
      if (e.getCode() == code) {
        e.setLevel(level);
        return true;
      }
    }
    return false;
  }
}
