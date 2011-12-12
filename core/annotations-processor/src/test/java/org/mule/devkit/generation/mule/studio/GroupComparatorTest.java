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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mule.devkit.model.studio.Group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class GroupComparatorTest {
    @Mock
    private Group group1;
    @Mock
    private Group group2;
    private List<Group> groups;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        groups = new ArrayList<Group>();
        groups.add(group1);
        groups.add(group2);
    }

    @Test
    public void compareGroup1IsDefault() throws Exception {
        when(group1.getCaption()).thenReturn(BaseStudioXmlBuilder.GENERAL_GROUP_NAME);
        when(group2.getCaption()).thenReturn("abc");
        Collections.sort(groups, new GroupComparator());
        assertEquals(group1, groups.get(0));
    }

    @Test
    public void compareGroup2IsDefault() throws Exception {
        when(group1.getCaption()).thenReturn("abc");
        when(group2.getCaption()).thenReturn(BaseStudioXmlBuilder.GENERAL_GROUP_NAME);
        Collections.sort(groups, new GroupComparator());
        assertEquals(group2, groups.get(0));
    }

    @Test
    public void compareGroup1FirstAlphabetically() throws Exception {
        when(group1.getCaption()).thenReturn("abc");
        when(group2.getCaption()).thenReturn("bcd");
        Collections.sort(groups, new GroupComparator());
        assertEquals(group1, groups.get(0));
    }

    @Test
    public void compareGroup2FirstAlphabetically() throws Exception {
        when(group1.getCaption()).thenReturn("bcd");
        when(group2.getCaption()).thenReturn("abc");
        Collections.sort(groups, new GroupComparator());
        assertEquals(group2, groups.get(0));
    }
}
