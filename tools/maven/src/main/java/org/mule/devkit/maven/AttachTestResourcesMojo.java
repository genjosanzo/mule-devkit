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

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoPhase;

@MojoPhase("validate")
@MojoGoal("attach-test-resources")
public class AttachTestResourcesMojo extends AbstractMuleMojo {
    public void execute() throws MojoExecutionException, MojoFailureException {
        String appFolder = this.appDirectory.getAbsolutePath();

        getLog().info("Attaching test resource " + appFolder);

        Resource appFolderResource = new Resource();
        appFolderResource.setDirectory(appFolder);

        this.project.addTestResource(appFolderResource);
    }
}
