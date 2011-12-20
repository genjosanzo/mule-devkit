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
import org.apache.maven.project.MavenProject;
import org.jfrog.maven.annomojo.annotations.MojoParameter;

import java.io.File;

/**
 * Base Mule Application Mojo
 */
public abstract class AbstractMuleMojo extends AbstractMojo {
    /**
     * Directory containing the generated Mule App.
     */
    @MojoParameter(required = true, expression = "${project.build.directory}")
    protected File outputDirectory;

    /**
     * Name of the generated Mule App.
     */
    @MojoParameter(required = true, expression = "${appName}", alias = "appName", defaultValue = "${project.build.finalName}")
    protected String finalName;

    /**
     * Directory containing the app resources.
     */
    @MojoParameter(required = true, expression = "${basedir}/src/main/app")
    protected File appDirectory;

    /**
     * The Maven project.
     */
    @MojoParameter(required = true, expression = "${project}", readonly = true)
    protected MavenProject project;

    /**
     * Whether to skip the creating of a Mule Studio plugin.
     */
    @MojoParameter(expression = "${devkit.studio.package.skip}", defaultValue = "false")
    protected boolean skipStudioPluginPackage;

    protected File getMuleZipFile() {
        return new File(this.outputDirectory, this.finalName + ".zip");
    }

    protected File getFilteredAppDirectory() {
        return new File(outputDirectory, "app");
    }
}
