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
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.artifact.AttachedArtifact;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.jfrog.maven.annomojo.annotations.MojoComponent;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.jfrog.maven.annomojo.annotations.MojoRequiresDependencyResolution;
import org.mule.devkit.generation.mule.studio.MuleStudioPluginGenerator;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Build a Mule plugin archive.
 */
@MojoPhase("package")
@MojoGoal("studio-package")
@MojoRequiresDependencyResolution("runtime")
public class StudioPackageMojo extends AbstractMuleMojo {

    public static final String STUDIO_PACKAGE_SUFFIX = "-studio.zip";
    @MojoComponent
    private MavenProjectHelper projectHelper;

    /**
     * Directory containing the classes.
     */
    @MojoParameter(expression = "${project.build.outputDirectory}", required = true)
    private File classesDirectory;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipStudioPluginPackage) {
            return;
        }

        File studioPlugin = new File(outputDirectory, finalName + STUDIO_PACKAGE_SUFFIX);
        try {
            createStudioPlugin(studioPlugin);
        } catch (ArchiverException e) {
            throw new MojoExecutionException("Exception creating the Mule Plugin", e);
        }

        projectHelper.attachArtifact(project, "zip", studioPlugin);

    }

    protected void createStudioPlugin(File studioPlugin) throws MojoExecutionException, ArchiverException {
        ModuleArchiver archiver = new ModuleArchiver();
        List attachedArtifacts = project.getAttachedArtifacts();

        AttachedArtifact mulePluginZipArtifact = null;
        for (Object object : attachedArtifacts) {
            AttachedArtifact attachedArtifact = (AttachedArtifact) object;
            if (attachedArtifact.getFile().getName().equals(finalName + ".zip")) {
                mulePluginZipArtifact = attachedArtifact;
            }
        }

        if (mulePluginZipArtifact == null) {
            throw new MojoExecutionException("Mule Plugin zip file not available");
        }

        archiver.addFile(mulePluginZipArtifact.getFile(), mulePluginZipArtifact.getFile().getName());

        addArchivedClasses(archiver, File.separator);

        for (String fileName : MuleStudioPluginGenerator.GENERATED_FILES) {
            File file = new File(classesDirectory, fileName);
            if (!file.exists()) {
                throw new MojoExecutionException("Error while packagin Mule Studio plugin: " + file.getName() + " does not exist");
            }
            archiver.addFile(file, file.getPath().substring(file.getPath().indexOf(classesDirectory.getPath()) + classesDirectory.getPath().length()));
        }

        archiver.setDestFile(studioPlugin);

        try {
            studioPlugin.delete();
            archiver.createArchive();
        } catch (IOException e) {
            throw new MojoExecutionException("Error while packaging Studio plugin", e);
        }
    }

    private void addArchivedClasses(ModuleArchiver archiver, String directory) throws ArchiverException, MojoExecutionException {
        if (!classesDirectory.exists()) {
            getLog().info(classesDirectory + " does not exist, skipping");
            return;
        }

        getLog().info("Copying classes as a jar");

        final JarArchiver jarArchiver = new JarArchiver();
        jarArchiver.addDirectory(classesDirectory, null, null);
        final File jar = new File(outputDirectory, finalName + ".jar");
        jarArchiver.setDestFile(jar);
        try {
            jarArchiver.createArchive();
            if (!directory.endsWith(File.separator)) {
                directory += File.separator;
            }
            archiver.addFile(jar, directory + jar.getName());
        } catch (IOException e) {
            final String message = "Cannot create project jar";
            getLog().error(message, e);
            throw new MojoExecutionException(message, e);
        }
    }
}