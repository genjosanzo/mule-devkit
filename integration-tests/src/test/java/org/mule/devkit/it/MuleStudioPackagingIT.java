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

import org.apache.maven.it.Verifier;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MuleStudioPackagingIT extends AbstractMavenIT {

    private String[] EXPECTED_FILES_IN_STUDIO_PACKAGE = new String[] {"MANIFEST.MF", "Activator.class", "editors.xml",
            "plugin.xml", "studio-packaging-integration-test-1.0.jar", "studio-packaging-integration-test-1.0.zip"};

    @Override
    public void buildExecutable() throws Exception {
        super.buildExecutable();
        File zipFile = new File(getRoot().getAbsolutePath(), "target/studio-plugin.zip");
        assertTrue("Cannot find Mule Studio plugin package in path: " + zipFile.getAbsolutePath(), zipFile.exists());
        for(String expectedFile : EXPECTED_FILES_IN_STUDIO_PACKAGE) {
            assertZipContains(zipFile, expectedFile);
        }
    }

    private void assertZipContains(File zipFile, String fileNameToCheck) throws IOException {
        Enumeration<? extends ZipEntry> entries = new ZipFile(zipFile).entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().endsWith(fileNameToCheck) || entry.getName().endsWith("/" + fileNameToCheck)) {
                return;
            }
        }
        fail("File with name " + fileNameToCheck + " not found in Mule Studio package");
    }

    protected String getArtifactVersion() {
        return "1.0";
    }

    protected String getArtifactId() {
        return "studio-packaging-integration-test";
    }

    protected String getGroupId() {
        return "org.mule.devkit.it";
    }

    protected File getRoot() {
        return new File("target/integration-tests/" + getArtifactId());
    }

    @Override
    protected void setSystemProperties(Verifier verifier) throws IOException {
        super.setSystemProperties(verifier);
        verifier.getSystemProperties().setProperty("devkit.studio.package.skip", "false");
    }
}
