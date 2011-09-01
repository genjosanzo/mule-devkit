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
package org.mule.devkit.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.IOUtil;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoRequiresDependencyResolution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@MojoGoal("install")
@MojoRequiresDependencyResolution("runtime")
public class InstallMojo extends AbstractMuleMojo {
    /**
     * If set to <code>true</code> attempt to copy the Mule application zip to $MULE_HOME/apps
     */
    @MojoParameter(alias = "copyToAppsDirectory", expression = "${copyToAppsDirectory}", defaultValue = "false", required = true)
    protected boolean copyToAppsDirectory;

    public void execute() throws MojoExecutionException, MojoFailureException {
        final String packaging = project.getPackaging();

        if (copyToAppsDirectory) {
            if (!"mule".equals(packaging)) {
                throw new MojoExecutionException(
                        String.format("Only 'mule' packaging supports 'copyToAppsDirectory' configuration element, " +
                                "current project's packaging is '%s'", packaging)
                );
            }

            File muleHome = determineMuleHome();
            if (muleHome != null) {
                copyMuleZipToMuleHome(muleHome);
            } else {
                getLog().warn("MULE_HOME is not set, not copying " + finalName + ".zip");
            }
        }
    }

    private File determineMuleHome() throws MojoExecutionException {
        File muleHomeFile = null;

        String muleHome = getMuleHomeEnvVarOrSystemProperty();
        if (muleHome != null) {
            muleHomeFile = new File(muleHome);
            if (muleHomeFile.exists() == false) {
                String message =
                        String.format("MULE_HOME is set to %1s but this directory does not exist.",
                                muleHome);
                throw new MojoExecutionException(message);
            }
            if (muleHomeFile.canWrite() == false) {
                String message =
                        String.format("MULE_HOME is set to %1s but the directory is not writeable.",
                                muleHome);
                throw new MojoExecutionException(message);
            }
        }

        return muleHomeFile;
    }

    private String getMuleHomeEnvVarOrSystemProperty() {
        String muleHome = System.getenv("MULE_HOME");
        if (muleHome == null) {
            // fall back to a system property which is set from the plugin testing framework
            // when invoking the integration tests
            muleHome = System.getProperty("mule.home");
        }
        return muleHome;
    }

    private void copyMuleZipToMuleHome(File muleHome) throws MojoExecutionException {
        try {
            copyMuleZipFileToTempFileInAppsDirectory(muleHome);
            renameMuleZipFileToFinalName(muleHome);
        } catch (IOException iox) {
            throw new MojoExecutionException("Exception while copying to apps directory", iox);
        }
    }

    private void copyMuleZipFileToTempFileInAppsDirectory(File muleHome) throws IOException {
        InputStream muleZipInput = null;
        OutputStream tempOutput = null;
        try {
            File zipFile = getMuleZipFile();
            muleZipInput = new FileInputStream(zipFile);

            File tempFile = tempFileInAppsDirectory(muleHome);
            tempOutput = new FileOutputStream(tempFile);

            IOUtil.copy(muleZipInput, tempOutput);

            String message = String.format("Copying %1s to %2s", zipFile.getAbsolutePath(),
                    tempFile.getAbsolutePath());
            getLog().info(message);
        } finally {
            IOUtil.close(muleZipInput);
            IOUtil.close(tempOutput);
        }
    }

    private void renameMuleZipFileToFinalName(File muleHome) throws MojoExecutionException {
        File sourceFile = tempFileInAppsDirectory(muleHome);

        File appsDirectory = muleAppsDirectory(muleHome);
        File targetFile = new File(appsDirectory, finalName + ".zip");

        if (sourceFile.renameTo(targetFile) == false) {
            String message = String.format("Could not rename %1s to %2s",
                    sourceFile.getAbsolutePath(), targetFile.getAbsolutePath());
            throw new MojoExecutionException(message);
        }

        String message = String.format("Renaming %1s to %2s", sourceFile.getAbsolutePath(),
                targetFile.getAbsolutePath());
        getLog().info(message);
    }

    private File tempFileInAppsDirectory(File muleHome) {
        File appsDirectory = muleAppsDirectory(muleHome);
        return new File(appsDirectory, finalName + ".temp");
    }

    private File muleAppsDirectory(File muleHome) {
        return new File(muleHome, "apps");
    }
}
