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
import org.mule.devkit.tests.ConnectorWithConnectMethodInvalidReturnType;
import org.mule.devkit.tests.ConnectorWithConnectMethodInvalidThrownType;
import org.mule.devkit.tests.ConnectorWithConnectMethodManyThrownTypes;
import org.mule.devkit.tests.ConnectorWithConnectMethodNotPublic;
import org.mule.devkit.tests.ConnectorWithDisconnectMethodInvalidReturnType;
import org.mule.devkit.tests.ConnectorWithDisconnectMethodNotPublic;
import org.mule.devkit.tests.ConnectorWithDisconnectMethodWithParams;
import org.mule.devkit.tests.ConnectorWithTwoConnectMethods;
import org.mule.devkit.tests.ConnectorWithTwoDisconnectMethods;

public class ConnectorValidatorTest extends ConnectorAnnotationProcessorTest {

    @Override
    protected Validator getValidatorToTest() {
        return new ConnectorValidator();
    }

    @Test
    public void connectorWithTwoConnectMethods() throws Exception {
        compileTestCase(ConnectorWithTwoConnectMethods.class);
        assertCompilationFailed();
    }

    @Test
    public void connectorWithConnectMethodNotPublic() throws Exception {
        compileTestCase(ConnectorWithConnectMethodNotPublic.class);
        assertCompilationFailed();
    }

    @Test
    public void connectorWithConnectMethodManyThrownTypes() throws Exception {
        compileTestCase(ConnectorWithConnectMethodManyThrownTypes.class);
        assertCompilationFailed();
    }

    @Test
    public void connectorWithConnectMethodInvalidThrownType() throws Exception {
        compileTestCase(ConnectorWithConnectMethodInvalidThrownType.class);
        assertCompilationFailed();
    }

    @Test
    public void connectorWithConnectMethodInvalidReturnType() throws Exception {
        compileTestCase(ConnectorWithConnectMethodInvalidReturnType.class);
        assertCompilationFailed();
    }

    @Test
    public void connectorWithDisconnectMethodInvalidReturnType() throws Exception {
        compileTestCase(ConnectorWithDisconnectMethodInvalidReturnType.class);
        assertCompilationFailed();
    }

    @Test
    public void connectorWithDisconnectMethodNotPublic() throws Exception {
        compileTestCase(ConnectorWithDisconnectMethodNotPublic.class);
        assertCompilationFailed();
    }

    @Test
    public void connectorWithDisconnectMethodWithParams() throws Exception {
        compileTestCase(ConnectorWithDisconnectMethodWithParams.class);
        assertCompilationFailed();
    }

    @Test
    public void connectorWithTwoDisconnectMethods() throws Exception {
        compileTestCase(ConnectorWithTwoDisconnectMethods.class);
        assertCompilationFailed();
    }
}