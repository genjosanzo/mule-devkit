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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.jfrog.maven.annomojo.annotations.MojoComponent;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.jfrog.maven.annomojo.annotations.MojoRequiresDependencyResolution;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Build a Mule plugin archive.
 */
@MojoPhase("package")
@MojoGoal("package")
@MojoRequiresDependencyResolution("runtime")
public class PackageMojo extends AbstractMuleMojo {
    @MojoComponent
    private MavenProjectHelper projectHelper;

    /**
     * Directory containing the classes.
     */
    @MojoParameter(expression = "${project.build.outputDirectory}", required = true)
    private File classesDirectory;

    /**
     * Whether a JAR file will be created for the classes in the app. Using this optional
     * configuration parameter will make the generated classes to be archived into a jar file
     * and the classes directory will then be excluded from the app.
     */
    @MojoParameter(expression = "${archiveClasses}", defaultValue = "false")
    private boolean archiveClasses;

    /**
     * List of exclusion elements (having groupId and artifactId children) to exclude from the
     * application archive.
     *
     * @since 1.2
     */
    @MojoParameter
    private List<Exclusion> exclusions;

    /**
     * List of inclusion elements (having groupId and artifactId children) to exclude from the
     * application archive.
     *
     * @since 1.5
     */
    @MojoParameter
    private List<Inclusion> inclusions;

    /**
     * Exclude all artifacts with Mule groupIds. Default is <code>true</code>.
     *
     * @since 1.4
     */
    @MojoParameter(defaultValue = "true")
    private boolean excludeMuleDependencies;

    /**
     * @since 1.7
     */
    @MojoParameter(defaultValue = "false")
    private boolean filterAppDirectory;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File plugin = getMuleZipFile();
        try {
            createMulePlugin(plugin);
        } catch (ArchiverException e) {
            throw new MojoExecutionException("Exception creating the Mule Plugin", e);
        }

        this.projectHelper.attachArtifact(this.project, "zip", plugin);
    }

    protected void createMulePlugin(final File plugin) throws MojoExecutionException, ArchiverException {
        ModuleArchiver archiver = new ModuleArchiver();
        addAppDirectory(archiver);
        addCompiledClasses(archiver);
        addDependencies(archiver);

        archiver.setDestFile(plugin);

        try {
            plugin.delete();
            archiver.createArchive();
        } catch (IOException e) {
            getLog().error("Cannot create archive", e);
        }
    }

    private void addAppDirectory(ModuleArchiver archiver) throws ArchiverException {
        if (filterAppDirectory) {
            if (getFilteredAppDirectory().exists()) {
                archiver.addResources(getFilteredAppDirectory());
            }
        } else {
            if (appDirectory.exists()) {
                archiver.addResources(appDirectory);
            }
        }
    }

    private void addCompiledClasses(ModuleArchiver archiver) throws ArchiverException, MojoExecutionException {
        if (!this.archiveClasses) {
            addClassesFolder(archiver);
        } else {
            addArchivedClasses(archiver);
        }
    }

    private void addClassesFolder(ModuleArchiver archiver) throws ArchiverException {
        if (this.classesDirectory.exists()) {
            getLog().info("Copying classes directly");
            archiver.addClasses(this.classesDirectory, null, null);
        } else {
            getLog().info(this.classesDirectory + " does not exist, skipping");
        }
    }

    private void addArchivedClasses(ModuleArchiver archiver) throws ArchiverException, MojoExecutionException {
        if (!this.classesDirectory.exists()) {
            getLog().info(this.classesDirectory + " does not exist, skipping");
            return;
        }

        getLog().info("Copying classes as a jar");

        final JarArchiver jarArchiver = new JarArchiver();
        jarArchiver.addDirectory(this.classesDirectory, null, null);
        final File jar = new File(this.outputDirectory, this.finalName + ".jar");
        jarArchiver.setDestFile(jar);
        try {
            jarArchiver.createArchive();
            archiver.addLib(jar);
        } catch (IOException e) {
            final String message = "Cannot create project jar";
            getLog().error(message, e);
            throw new MojoExecutionException(message, e);
        }
    }

    private void addDependencies(ModuleArchiver archiver) throws ArchiverException {
        for (Artifact artifact : getArtifactsToArchive()) {
            String message = String.format("Adding <%1s> as a lib", artifact.getId());
            getLog().info(message);

            archiver.addLib(artifact.getFile());
        }
    }

    private Set<Artifact> getArtifactsToArchive() {
        ArtifactFilter filter = new ArtifactFilter(this.project, this.inclusions,
                this.exclusions, this.excludeMuleDependencies);
        return filter.getArtifactsToArchive();
    }
}
