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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class MethodComparatorTest {

    @Mock
    private ExecutableElement method1;
    @Mock
    private ExecutableElement method2;
    @Mock
    private Name name1;
    @Mock
    private Name name2;
    private List<ExecutableElement> methods;

    @Before
    public void setUpTests() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(method1.getSimpleName()).thenReturn(name1);
        when(method2.getSimpleName()).thenReturn(name2);
        methods = new ArrayList<ExecutableElement>(2);
        methods.add(method1);
        methods.add(method2);
    }

    @After
    public void verifyMockInteraction() {
        verify(method1).getSimpleName();
        verify(method2).getSimpleName();
        verifyNoMoreInteractions(method1);
        verifyNoMoreInteractions(method2);
    }
    @Test
    public void testCompareOrdered() throws Exception {
        when(name1.toString()).thenReturn("a");
        when(name2.toString()).thenReturn("b");
        Collections.sort(methods, new MethodComparator());
        assertEquals(method1, methods.get(0));
        assertEquals(method2, methods.get(1));
    }

    @Test
    public void testCompareNotOrdered() throws Exception {
        when(name1.toString()).thenReturn("b");
        when(name2.toString()).thenReturn("a");
        Collections.sort(methods, new MethodComparator());
        assertEquals(method2, methods.get(0));
        assertEquals(method1, methods.get(1));
    }
}