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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.jfrog.maven.annomojo.annotations.MojoComponent;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.jfrog.maven.annomojo.annotations.MojoRequiresDependencyResolution;

import java.io.File;
import java.io.FilenameFilter;

@MojoGoal("attach-schema")
@MojoRequiresDependencyResolution(value = "compile")
@MojoPhase("package")
public class AttachSchemaMojo extends AbstractMojo {
    @MojoComponent
    protected MavenProjectHelper projectHelper;

    @MojoParameter(expression = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @MojoParameter(required = false, defaultValue = "target/generated-sources/mule/META-INF")
    protected File targetDirectory;

    public void execute() throws MojoExecutionException, MojoFailureException {
        for (File file : targetDirectory.listFiles(new SchemaFilenameFilter())) {
            projectHelper.attachArtifact(project, "xsd", file);
        }
    }

    private class SchemaFilenameFilter implements FilenameFilter {
        public boolean accept(File file, String s) {
            return s.startsWith("mule-") && s.endsWith("xsd");
        }
    }

}
