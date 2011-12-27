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

import org.mule.devkit.model.studio.Group;

import java.util.Comparator;

public class GroupComparator implements Comparator<Group> {

    private static final int GROUP1_FIRST = -1;
    private static final int GROUP2_FIRST = 1;

    @Override
    public int compare(Group group1, Group group2) {
        if (group1.getCaption().equals(BaseStudioXmlBuilder.GENERAL_GROUP_NAME)) {
            return GROUP1_FIRST;
        }
        if (group2.getCaption().equals(BaseStudioXmlBuilder.GENERAL_GROUP_NAME)) {
            return GROUP2_FIRST;
        }
        return group1.getCaption().compareTo(group2.getCaption());
    }
}