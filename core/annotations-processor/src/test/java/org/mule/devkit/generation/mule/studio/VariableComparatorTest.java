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
import org.mule.devkit.utils.TypeMirrorUtils;

import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class VariableComparatorTest {

    @Mock
    private VariableElement stringVariable;
    @Mock
    private VariableElement intVariable;
    @Mock
    private VariableElement enumVariable;
    @Mock
    private VariableElement mapVariable;
    @Mock
    private VariableElement booleanVariable;
    @Mock
    private TypeMirrorUtils typeMirrorUtils;

    @Before
    public void setUpTests() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(typeMirrorUtils.isString(stringVariable)).thenReturn(true);
        when(typeMirrorUtils.isInteger(intVariable)).thenReturn(true);
        when(typeMirrorUtils.isEnum(enumVariable)).thenReturn(true);
        when(typeMirrorUtils.isCollection(mapVariable)).thenReturn(true);
        when(typeMirrorUtils.isBoolean(booleanVariable)).thenReturn(true);
    }

    @Test
    public void testCompare() throws Exception {
        List<VariableElement> variables = new ArrayList<VariableElement>();
        variables.add(mapVariable);
        variables.add(enumVariable);
        variables.add(booleanVariable);
        variables.add(intVariable);
        variables.add(stringVariable);

        Collections.sort(variables, new VariableComparator(typeMirrorUtils));

        assertEquals(stringVariable, variables.get(0));
        assertEquals(intVariable, variables.get(1));
        assertEquals(booleanVariable, variables.get(2));
        assertEquals(enumVariable, variables.get(3));
        assertEquals(mapVariable, variables.get(4));
    }
}