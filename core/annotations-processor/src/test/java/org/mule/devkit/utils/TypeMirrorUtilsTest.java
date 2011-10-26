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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class TypeMirrorUtilsTest {

    @Mock
    private Types types;
    @Mock
    private TypeMirror typeMirror;

    @Before
    public void setUpTests() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testIsArrayOrListForByteArray() throws Exception {
        when(typeMirror.toString()).thenReturn("byte[]");
        assertFalse(new TypeMirrorUtils(types).isArrayOrList(typeMirror));
    }

    @Test
    public void testIsArrayOrListForArray() throws Exception {
        when(typeMirror.getKind()).thenReturn(TypeKind.ARRAY);
        assertTrue(new TypeMirrorUtils(types).isArrayOrList(typeMirror));
    }

    @Test
    public void testIsArrayOrListForMapOfLists() throws Exception {
        when(typeMirror.toString()).thenReturn("java.util.Map<java.lang.String,java.util.List<java.lang.String>>");
        assertFalse(new TypeMirrorUtils(types).isArrayOrList(typeMirror));
    }

    @Test
    public void testIsArrayOrListForListOfMaps() throws Exception {
        when(typeMirror.toString()).thenReturn("java.util.List<java.util.Map<java.lang.String,java.lang.String>>");
        assertTrue(new TypeMirrorUtils(types).isArrayOrList(typeMirror));
    }

    @Test
    public void testIsMapForListOfMaps() throws Exception {
        when(typeMirror.toString()).thenReturn("java.util.List<java.util.Map<java.lang.String,java.lang.String>>");
        assertFalse(new TypeMirrorUtils(types).isMap(typeMirror));
    }
}