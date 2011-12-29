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
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Placement;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.mule.studio.editor.VariableComparator;
import org.mule.devkit.utils.TypeMirrorUtils;

import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
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
    private VariableElement unknownTypeVariable;
    @Mock
    private GeneratorContext context;
    @Mock
    private TypeMirrorUtils typeMirrorUtils;

    @Before
    public void setUpTests() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(context.getTypeMirrorUtils()).thenReturn(typeMirrorUtils);
        when(typeMirrorUtils.isString(stringVariable)).thenReturn(true);
        when(typeMirrorUtils.isInteger(intVariable)).thenReturn(true);
        when(typeMirrorUtils.isEnum(enumVariable)).thenReturn(true);
        when(typeMirrorUtils.isCollection(mapVariable)).thenReturn(true);
        when(typeMirrorUtils.isBoolean(booleanVariable)).thenReturn(true);
    }

    @Test
    public void compareByType() throws Exception {
        List<VariableElement> variables = new ArrayList<VariableElement>();
        variables.add(mapVariable);
        variables.add(enumVariable);
        variables.add(booleanVariable);
        variables.add(intVariable);
        variables.add(stringVariable);

        setName(mapVariable, "a");
        setName(enumVariable, "b");
        setName(booleanVariable, "c");
        setName(intVariable, "d");
        setName(stringVariable, "e");

        Collections.sort(variables, new VariableComparator(context));

        assertEquals(intVariable, variables.get(0));
        assertEquals(stringVariable, variables.get(1));
        assertEquals(enumVariable, variables.get(2));
        assertEquals(booleanVariable, variables.get(3));
        assertEquals(mapVariable, variables.get(4));
    }

    @Test
    public void testCompareUnknownType() throws Exception {
        setName(unknownTypeVariable, "a");
        setName(booleanVariable, "b");

        List<VariableElement> variables = new ArrayList<VariableElement>();
        variables.add(unknownTypeVariable);
        variables.add(booleanVariable);

        Collections.sort(variables, new VariableComparator(context));

        assertEquals(unknownTypeVariable, variables.get(0));
        assertEquals(booleanVariable, variables.get(1));
    }

    private void setName(VariableElement variableElement, String name) {
        Name simpleName = mock(Name.class);
        when(variableElement.getSimpleName()).thenReturn(simpleName);
        when(simpleName.toString()).thenReturn(name);
    }

    @Test
    public void testCompareByNameAndType() throws Exception {
        Name a = mockName("a");
        Name b = mockName("b");

        when(intVariable.getSimpleName()).thenReturn(a);
        when(stringVariable.getSimpleName()).thenReturn(b);

        List<VariableElement> variables = new ArrayList<VariableElement>();
        variables.add(stringVariable);
        variables.add(intVariable);

        Collections.sort(variables, new VariableComparator(context));

        assertEquals(intVariable, variables.get(0));
        assertEquals(stringVariable, variables.get(1));
    }

    @Test
    public void testCompareByOrder() throws Exception {
        Placement mapVariablePlacement = mock(Placement.class);
        Placement intVariablePlacement = mock(Placement.class);

        when(mapVariable.getAnnotation(Placement.class)).thenReturn(mapVariablePlacement);
        when(intVariable.getAnnotation(Placement.class)).thenReturn(intVariablePlacement);

        when(mapVariablePlacement.order()).thenReturn(4);
        when(intVariablePlacement.order()).thenReturn(5);

        List<VariableElement> variables = new ArrayList<VariableElement>();
        variables.add(intVariable);
        variables.add(mapVariable);

        Collections.sort(variables, new VariableComparator(context));

        assertEquals(mapVariable, variables.get(0));
        assertEquals(intVariable, variables.get(1));
    }

    @Test
    public void testCompareBothWithFriendlyNames() throws Exception {
        VariableElement stringVariable2 = mock(VariableElement.class);
        when(typeMirrorUtils.isString(stringVariable2)).thenReturn(true);

        FriendlyName friendlyName1 = mock(FriendlyName.class);
        FriendlyName friendlyName2 = mock(FriendlyName.class);

        when(stringVariable.getAnnotation(FriendlyName.class)).thenReturn(friendlyName1);
        when(stringVariable2.getAnnotation(FriendlyName.class)).thenReturn(friendlyName2);

        when(friendlyName1.value()).thenReturn("b");
        when(friendlyName2.value()).thenReturn("a");

        List<VariableElement> variables = new ArrayList<VariableElement>();
        variables.add(stringVariable);
        variables.add(stringVariable2);

        Collections.sort(variables, new VariableComparator(context));

        assertEquals(stringVariable2, variables.get(0));
        assertEquals(stringVariable, variables.get(1));
    }

    @Test
    public void testCompareFriendlyName() throws Exception {
        VariableElement stringVariable2 = mock(VariableElement.class);
        when(typeMirrorUtils.isString(stringVariable2)).thenReturn(true);

        FriendlyName friendlyName1 = mock(FriendlyName.class);
        when(friendlyName1.value()).thenReturn("b");

        Name a = mockName("a");

        when(stringVariable.getAnnotation(FriendlyName.class)).thenReturn(friendlyName1);
        when(stringVariable2.getSimpleName()).thenReturn(a);


        List<VariableElement> variables = new ArrayList<VariableElement>();
        variables.add(stringVariable);
        variables.add(stringVariable2);

        Collections.sort(variables, new VariableComparator(context));

        assertEquals(stringVariable2, variables.get(0));
        assertEquals(stringVariable, variables.get(1));
    }

    private Name mockName(String a) {
        Name name = mock(Name.class);
        when(name.toString()).thenReturn(a);
        return name;
    }
}