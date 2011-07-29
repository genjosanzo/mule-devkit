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

package org.mule.devkit.it;

import java.io.File;

public class InterceptingProcessorIT extends AbstractMavenIT
{

    protected String getArtifactVersion()
    {
        return "1.0";
    }

    protected String getArtifactId()
    {
        return "intercepting-processor-integration-test";
    }

    protected String getGroupId()
    {
        return "org.mule.devkit.it";
    }

    protected File getRoot()
    {
        return new File("target/integration-tests/" + getArtifactId());
    }
}
