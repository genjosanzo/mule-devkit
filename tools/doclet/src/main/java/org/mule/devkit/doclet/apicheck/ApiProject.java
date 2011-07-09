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

import org.mule.devkit.doclet.TypeInfo;

import java.util.HashMap;
import java.util.Map;

public final class ApiProject {

  private final Map<String, TypeInfo> cache = new HashMap<String, TypeInfo>();

  public TypeInfo obtainTypeFromString(String name) {
    TypeInfo result = cache.get(name);
    if (result == null) {
      result = new TypeInfo(name);
      cache.put(name, result);
    }
    return result;
  }
}
