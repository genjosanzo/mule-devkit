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
import org.mule.devkit.generation.mule.studio.editor.EnumElementComparator;
import org.mule.devkit.model.studio.EnumElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class EnumElementComparatorTest {

    @Mock
    private EnumElement enumElement1;
    @Mock
    private EnumElement enumElement2;
    private List<EnumElement> enumElements;

    @Before
    public void setUpTests() throws Exception {
        MockitoAnnotations.initMocks(this);
        enumElements = new ArrayList<EnumElement>(2);
        enumElements.add(enumElement1);
        enumElements.add(enumElement2);
    }

    @Test
    public void collectionOrdered() throws Exception {
        when(enumElement1.getValue()).thenReturn("a");
        when(enumElement2.getValue()).thenReturn("b");
        Collections.sort(enumElements, new EnumElementComparator());
        assertEquals(enumElement1, enumElements.get(0));
    }

    @Test
    public void collectionNotOrdered() throws Exception {
        when(enumElement1.getValue()).thenReturn("b");
        when(enumElement2.getValue()).thenReturn("a");
        Collections.sort(enumElements, new EnumElementComparator());
        assertEquals(enumElement2, enumElements.get(0));
    }
}