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
import org.apache.maven.project.MavenProject;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.jfrog.maven.annomojo.annotations.MojoRequiresDependencyResolution;

import java.io.File;
import java.util.List;

@MojoGoal("generate-sources")
@MojoRequiresDependencyResolution(value = "compile")
@MojoPhase("generate-sources")
public class ModuleAnnotationProcessorMojo extends AbstractAnnotationProcessorMojo {
    private static String[] processors = new String[]{"org.mule.devkit.apt.ModuleAnnotationProcessor"};

    /**
     * project classpath
     */
    @MojoParameter(expression = "${project.compileClasspathElements}", required = true, readonly = true)
    private List classpathElements;

    @MojoParameter(expression = "${project.build.sourceDirectory}", required = true)
    private File sourceDirectory;

    @MojoParameter(expression = "${project.build.directory}/generated-sources/mule", required = true)
    private File defaultOutputDirectory;

    @MojoParameter(required = false, expression = "${project.build.outputDirectory}", description = "Set the destination directory for class files (same behaviour of -d option)")
    private File outputClassDirectory;

    @MojoParameter(required = false, expression = "${devkit.javadoc.check.skip}", description = "Skip JavaDoc validation", defaultValue = "false")
    private boolean skipJavaDocValidation;

    @Override
    public File getSourceDirectory() {
        return sourceDirectory;
    }

    @Override
    protected File getOutputClassDirectory() {
        return outputClassDirectory;
    }

    @Override
    protected String[] getProcessors() {
        return processors;
    }

    protected void addCompileSourceRoot(MavenProject project, String dir) {
        project.addCompileSourceRoot(dir);
    }

    @Override
    public File getDefaultOutputDirectory() {
        return defaultOutputDirectory;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected java.util.Set<String> getClasspathElements(java.util.Set<String> result) {
        List<Resource> resources = project.getResources();

        if (resources != null) {
            for (Resource r : resources) {
                result.add(r.getDirectory());
            }
        }

        result.addAll(classpathElements);

        return result;
    }

    @Override
    protected void addCompilerArguments(List<String> options) {
        if (skipJavaDocValidation) {
            options.add("-AskipJavaDocValidation=true");
        }

        super.addCompilerArguments(options);
    }

}
