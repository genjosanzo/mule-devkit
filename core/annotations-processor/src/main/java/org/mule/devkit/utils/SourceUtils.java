/**
 * Mule Development Kit
 * Copyright 2010-2011 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mule.devkit.utils;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

public class SourceUtils {
    private Trees trees;

    public SourceUtils(ProcessingEnvironment env) {
        this.trees = Trees.instance(env);
    }

    public String getPath(TypeElement type) {
        TreePath path = trees.getPath(type);
        JavaFileObject source = path.getCompilationUnit().getSourceFile();
        return source.toUri().getPath();
    }
}
