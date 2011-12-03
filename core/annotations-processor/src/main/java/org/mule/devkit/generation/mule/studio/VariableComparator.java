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

package org.mule.devkit.generation.mule.studio;

import org.mule.devkit.utils.TypeMirrorUtils;

import javax.lang.model.element.VariableElement;
import java.util.Comparator;

public class VariableComparator implements Comparator<VariableElement> {

    private static final int VARIABLE1_FIRST = -1;
    private static final int VARIABLE2_FIRST = 1;
    private TypeMirrorUtils typeMirrorUtils;

    public VariableComparator(TypeMirrorUtils typeMirrorUtils) {
        this.typeMirrorUtils = typeMirrorUtils;
    }

    /**
     * Compares two {@link VariableElement} using the following order:
     * 1) strings
     * 2) integers
     * 3) booleans
     * 4) enums
     * 5) collections and maps
     * For same types or types not listed here, order alphabetically.
     */
    @Override
    public int compare(VariableElement variable1, VariableElement variable2) {
        if (typeMirrorUtils.isString(variable1) && typeMirrorUtils.isString(variable2)) {
            return compareByName(variable1, variable2);
        } else if (typeMirrorUtils.isString(variable1)) {
            return VARIABLE1_FIRST;
        } else if (typeMirrorUtils.isString(variable2)) {
            return VARIABLE2_FIRST;
        }

        if (typeMirrorUtils.isInteger(variable1) && typeMirrorUtils.isInteger(variable2)) {
            return compareByName(variable1, variable2);
        } else if (typeMirrorUtils.isInteger(variable1)) {
            return VARIABLE1_FIRST;
        } else if (typeMirrorUtils.isInteger(variable2)) {
            return VARIABLE2_FIRST;
        }

        if (typeMirrorUtils.isBoolean(variable1) && typeMirrorUtils.isBoolean(variable2)) {
            return compareByName(variable1, variable2);
        } else if (typeMirrorUtils.isBoolean(variable1)) {
            return VARIABLE1_FIRST;
        } else if (typeMirrorUtils.isBoolean(variable2)) {
            return VARIABLE2_FIRST;
        }

        if (typeMirrorUtils.isEnum(variable1) && typeMirrorUtils.isEnum(variable2)) {
            return compareByName(variable1, variable2);
        } else if (typeMirrorUtils.isEnum(variable1)) {
            return VARIABLE1_FIRST;
        } else if (typeMirrorUtils.isEnum(variable2)) {
            return VARIABLE2_FIRST;
        }

        if (typeMirrorUtils.isCollection(variable1) && typeMirrorUtils.isCollection(variable2)) {
            return compareByName(variable1, variable2);
        } else if (typeMirrorUtils.isCollection(variable1)) {
            return VARIABLE1_FIRST;
        } else if (typeMirrorUtils.isCollection(variable2)) {
            return VARIABLE2_FIRST;
        }

        return compareByName(variable1, variable2);
    }

    private int compareByName(VariableElement variable1, VariableElement variable2) {
        return variable1.getSimpleName().toString().compareTo(variable2.getSimpleName().toString());
    }
}