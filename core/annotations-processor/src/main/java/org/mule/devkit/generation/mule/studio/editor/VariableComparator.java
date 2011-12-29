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

package org.mule.devkit.generation.mule.studio.editor;

import org.apache.commons.lang.StringUtils;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Placement;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.utils.TypeMirrorUtils;

import javax.lang.model.element.VariableElement;
import java.util.Comparator;

public class VariableComparator implements Comparator<VariableElement> {

    private static final int VARIABLE1_FIRST = -1;
    private static final int VARIABLE2_FIRST = 1;
    private TypeMirrorUtils typeMirrorUtils;

    public VariableComparator(GeneratorContext context) {
        typeMirrorUtils = context.getTypeMirrorUtils();
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

        Placement placementVar1 = variable1.getAnnotation(Placement.class);
        Placement placementVar2 = variable2.getAnnotation(Placement.class);

        if (!sameGroup(placementVar1, placementVar2)) {
            return 0;
        }

        if (placementVar1 != null && placementVar2 != null) {
            return new Integer(placementVar1.order()).compareTo(placementVar2.order());
        } else if (placementVar1 != null) {
            return new Integer(placementVar1.order()).compareTo(Placement.DEFAULT_ORDER);
        } else if (placementVar2 != null) {
            return new Integer(Placement.DEFAULT_ORDER).compareTo(placementVar2.order());
        }

        if (bothOfSameType(variable1, variable2)) {
            return compareByName(variable1, variable2);
        }

        if (typeMirrorUtils.isCollection(variable1)) {
            return VARIABLE2_FIRST;
        }
        if (typeMirrorUtils.isCollection(variable2)) {
            return VARIABLE1_FIRST;
        }

        if (typeMirrorUtils.isBoolean(variable1)) {
            return VARIABLE2_FIRST;
        }
        if (typeMirrorUtils.isBoolean(variable2)) {
            return VARIABLE1_FIRST;
        }

        if (typeMirrorUtils.isEnum(variable1)) {
            return VARIABLE2_FIRST;
        }
        if (typeMirrorUtils.isEnum(variable2)) {
            return VARIABLE1_FIRST;
        }

        return 0;
    }

    private boolean sameGroup(Placement placementVar1, Placement placementVar2) {
        String group1 = extractGroup(placementVar1);
        String group2 = extractGroup(placementVar2);
        return group1.equals(group2);
    }

    private String extractGroup(Placement placement) {
        if (placement != null && StringUtils.isNotBlank(placement.group())) {
            return placement.group();
        }
        return BaseStudioXmlBuilder.GENERAL_GROUP_NAME;
    }

    private boolean bothOfSameType(VariableElement variable1, VariableElement variable2) {
        return typeMirrorUtils.isString(variable1) && typeMirrorUtils.isString(variable2) ||
                typeMirrorUtils.isInteger(variable1) && typeMirrorUtils.isInteger(variable2) ||
                typeMirrorUtils.isEnum(variable1) && typeMirrorUtils.isEnum(variable2) ||
                typeMirrorUtils.isBoolean(variable1) && typeMirrorUtils.isBoolean(variable2) ||
                typeMirrorUtils.isCollection(variable1) && typeMirrorUtils.isCollection(variable2);
    }

    private int compareByName(VariableElement variable1, VariableElement variable2) {
        String name1 = extractName(variable1);
        String name2 = extractName(variable2);
        return name1.compareTo(name2);
    }

    private String extractName(VariableElement variableElement) {
        if (variableElement.getAnnotation(FriendlyName.class) != null) {
            return variableElement.getAnnotation(FriendlyName.class).value();
        } else {
            return variableElement.getSimpleName().toString();
        }
    }
}