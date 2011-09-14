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
import org.apache.maven.it.util.IOUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public abstract class AbstractMavenIT {

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
    public void buildExecutable() throws VerificationException {
        InputStream systemPropertiesStream = null;
        try {
            Verifier verifier = new Verifier(getRoot().getAbsolutePath(), null, true);
            verifier.setAutoclean(false);
            verifier.setMavenDebug(true);
            verifier.setDebug(true);

            systemPropertiesStream = getClass().getClassLoader().getResourceAsStream("maven.properties");
            Properties systemProperties = new Properties();
            systemProperties.load(systemPropertiesStream);

            verifier.setSystemProperties(systemProperties);
            verifier.getCliOptions().add("-o");

            verifier.executeGoal("clean");
            verifier.executeGoal("package");

            verifier.verifyErrorFreeLog();
        } catch (IOException ioe) {
            throw new VerificationException(ioe);
        } finally {
            IOUtil.close(systemPropertiesStream);
        }
    }
}