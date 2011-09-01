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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.jfrog.maven.annomojo.annotations.MojoComponent;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;

import java.util.Arrays;
import java.util.List;

@MojoGoal("filter-resources")
@MojoPhase("process-resources")
public class FilterResourcesMojo extends AbstractMuleMojo {
    @MojoComponent(role = "org.apache.maven.shared.filtering.MavenResourcesFiltering", roleHint = "default")
    private MavenResourcesFiltering resourceFilter;

    /**
     * The character encoding scheme to be applied when filtering resources.
     */
    @MojoParameter(expression = "${encoding}", defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    @MojoParameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * @since 1.7
     */
    @MojoParameter(defaultValue = "false")
    private boolean filterAppDirectory;

    /**
     * Whether to escape backslashes and colons in windows-style paths.
     *
     * @since 1.7
     */
    @MojoParameter(defaultValue = "true")
    private boolean escapeWindowsPaths;

    /**
     * Stop searching endToken at the end of line
     *
     * @since 1.7
     */
    @MojoParameter(defaultValue = "false")
    private boolean supportMultiLineFiltering;

    /**
     * Additional file extensions to not apply filtering
     *
     * @since 1.7
     */
    @MojoParameter
    private List<?> nonFilteredFileExtensions;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (filterAppDirectory == false) {
            return;
        }

        getLog().info("filtering resources from " + appDirectory.getAbsolutePath());
        filterResources();
    }

    private void filterResources() throws MojoExecutionException {
        try {
            MavenResourcesExecution execution = new MavenResourcesExecution(getResources(),
                    getFilteredAppDirectory(), project, encoding, null, null, session);
            execution.setEscapeWindowsPaths(escapeWindowsPaths);
            execution.setSupportMultiLineFiltering(supportMultiLineFiltering);
            if (nonFilteredFileExtensions != null) {
                execution.setNonFilteredFileExtensions(nonFilteredFileExtensions);
            }

            resourceFilter.filterResources(execution);
        } catch (MavenFilteringException e) {
            throw new MojoExecutionException("Error while filtering Mule config files", e);
        }
    }

    private List<Resource> getResources() {
        Resource appFolderResource = new Resource();
        appFolderResource.setDirectory(this.appDirectory.getAbsolutePath());
        appFolderResource.setFiltering(true);

        return Arrays.asList(appFolderResource);
    }
}
