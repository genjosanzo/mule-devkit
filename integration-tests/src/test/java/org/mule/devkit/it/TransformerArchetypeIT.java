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

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TransformerArchetypeIT {

    private static final File ROOT = new File("target/integration-tests/");
    private static final String ARCHETYPE_PROPERTIES = "/transformer-archetype.properties";

    private Properties archetypeProperties;
    private Properties verifierProperties;

    @Before
    public void setUp() throws VerificationException, IOException {
        InputStream stream = getClass().getResourceAsStream(ARCHETYPE_PROPERTIES);
        archetypeProperties = new Properties();
        archetypeProperties.load(stream);

        verifierProperties = new Properties();
        verifierProperties.setProperty("use.mavenRepoLocal", "true");

        Verifier verifier = new Verifier(ROOT.getAbsolutePath());

        // deleting a former created artifact from the archetype to be tested
        verifier.deleteArtifact(getGroupId(), getArtifactId(), getVersion(), null);

        // delete the created maven project
        verifier.deleteDirectory(getArtifactId());
    }

    private String getVersion() {
        return archetypeProperties.getProperty("version");
    }

    private String getArtifactId() {
        return archetypeProperties.getProperty("artifactId");
    }

    private String getGroupId() {
        return archetypeProperties.getProperty("groupId");
    }

    @Test
    public void testGenerateArchetype() throws VerificationException {
        Verifier verifier = new Verifier(ROOT.getAbsolutePath());
        verifier.setSystemProperties(archetypeProperties);
        verifier.setVerifierProperties(verifierProperties);
        verifier.setAutoclean(false);

        verifier.executeGoal("archetype:generate");

        verifier.verifyErrorFreeLog();

        verifier = new Verifier(ROOT.getAbsolutePath() + "/" + getArtifactId());
        verifier.setAutoclean(true);
        verifier.executeGoal("package");

        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog("generate-sources");
    }
}
