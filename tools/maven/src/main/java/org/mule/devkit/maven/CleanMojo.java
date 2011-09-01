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
import org.jfrog.maven.annomojo.annotations.MojoExecute;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoRequiresDependencyResolution;

import java.io.File;

/**
 * Clean the build path for a Mule application
 */
@MojoGoal("clean")
@MojoRequiresDependencyResolution(value = "runtime")
@MojoExecute(lifecycle = "mule-package", phase = "package")
public class CleanMojo extends AbstractMuleMojo {
    public void execute() throws MojoExecutionException, MojoFailureException {
        File module = new File(this.outputDirectory, this.finalName);
        if (module.exists()) {
            final boolean success = module.delete();
            if (success) {
                getLog().info("Deleted Mule module: " + module);
            } else {
                getLog().info("Failed to delete Mule module: " + module);
            }
        } else {
            getLog().info("Nothing to clean");
        }
    }
}
