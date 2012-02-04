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

import org.apache.commons.io.IOUtils;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractMavenIT {

    private static final boolean DEBUG = false;

    protected abstract String getArtifactVersion();

    protected abstract String getArtifactId();

    protected abstract String getGroupId();

    protected abstract File getRoot();

    @Before
    public void setUp() throws VerificationException, IOException {
        Verifier verifier = new Verifier(getRoot().getAbsolutePath());

        // Deleting a former created artefact from the archetype to be tested
        verifier.deleteArtifact(getGroupId(), getArtifactId(), getArtifactVersion(), null);

        // Delete the created maven project
        verifier.deleteDirectory(getArtifactId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void buildExecutable() throws Exception {
        try {
            Verifier verifier = new Verifier(getRoot().getAbsolutePath(), null, DEBUG, true);
            verifier.setAutoclean(true);

            setSystemProperties(verifier);

            Map<String, String> envVars = new HashMap<String, String>();
            envVars.put("MAVEN_OPTS", "-Xmx512m -XX:MaxPermSize=256m");

            verifier.executeGoal("package", envVars);

            verifier.verifyErrorFreeLog();
        } catch (IOException ioe) {
            throw new VerificationException(ioe);
        }
    }

    protected void setSystemProperties(Verifier verifier) throws IOException {
        InputStream systemPropertiesStream = null;
        try {
            systemPropertiesStream = getClass().getClassLoader().getResourceAsStream("maven.properties");
            Properties systemProperties = new Properties();
            systemProperties.load(systemPropertiesStream);
            verifier.setSystemProperties(systemProperties);
        } finally {
            IOUtils.closeQuietly(systemPropertiesStream);
        }
    }
}