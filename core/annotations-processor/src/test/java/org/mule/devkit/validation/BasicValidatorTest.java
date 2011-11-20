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
package org.mule.devkit.validation;

import org.junit.Test;
import org.mule.devkit.tests.ArrayConfigurableField;
import org.mule.devkit.tests.DefaultNonOptionalConfigurableField;
import org.mule.devkit.tests.FinalConfigurableField;
import org.mule.devkit.tests.InterfaceModule;
import org.mule.devkit.tests.OptionalPrimitiveNonDefaultConfigurableField;
import org.mule.devkit.tests.ParametrizedModule;
import org.mule.devkit.tests.StaticConfigurableField;

public class BasicValidatorTest extends ModuleAnnotationProcessorTest {

    @Override
    protected Validator getValidatorToTest() {
        return new BasicValidator();
    }

    @Test
    public void interfaceModule() throws Exception {
        compileTestCase(InterfaceModule.class);
        assertCompilationFailed();
    }

    @Test
    public void parametrizedModule() throws Exception {
        compileTestCase(ParametrizedModule.class);
        assertCompilationFailed();
    }

    @Test
    public void nonPublicModule() throws Exception {
        compileTestCase(Class.forName("org.mule.devkit.tests.NonPublicModule"));
        assertCompilationFailed();
    }

    @Test
    public void finalConfigurableField() throws Exception {
        compileTestCase(FinalConfigurableField.class);
        assertCompilationFailed();
    }

    @Test
    public void staticConfigurableField() throws Exception {
        compileTestCase(StaticConfigurableField.class);
        assertCompilationFailed();
    }

    @Test
    public void arrayConfigurableField() throws Exception {
        compileTestCase(ArrayConfigurableField.class);
        assertCompilationFailed();
    }

    @Test
    public void defaultNonOptionalConfigurableField() throws Exception {
        compileTestCase(DefaultNonOptionalConfigurableField.class);
        assertCompilationFailed();
    }

    @Test
    public void optionalPrimitiveNonDefaultConfigurableField() throws Exception {
        compileTestCase(OptionalPrimitiveNonDefaultConfigurableField.class);
        assertCompilationFailed();
    }
}