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
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.jfrog.maven.annomojo.annotations.MojoParameter;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public abstract class AbstractAnnotationProcessorMojo extends AbstractMuleMojo {
    @MojoParameter(expression = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @MojoParameter(expression = "${plugin.artifacts}", readonly = true)
    private List<Artifact> pluginArtifacts;

    @MojoParameter(required = false, description = "Specify the directory where to place generated source files (same behaviour of -s option)")
    private File outputDirectory;

    @MojoParameter(required = false, description = "Additional compiler arguments")
    private String compilerArguments;

    @MojoParameter(required = false, description = "Controls whether or not the output directory is added to compilation")
    private Boolean addOutputDirectoryToCompilationSources;

    @MojoParameter(required = true, defaultValue = "true", expression = "${annotation.failOnError}", description = "Indicates whether the build will continue even if there are compilation errors; defaults to true.")
    private Boolean failOnError = true;

    @MojoParameter(required = true, defaultValue = "true", expression = "${annotation.outputDiagnostics}", description = "Indicates whether the compiler output should be visible, defaults to true.")
    private boolean outputDiagnostics = true;

    @SuppressWarnings("rawtypes")
    @MojoParameter(required = false, description = "System properties set before processor invocation.")
    private Map systemProperties;

    @MojoParameter(description = "includes pattern")
    private String[] includes;
    @MojoParameter(description = "excludes pattern")
    private String[] excludes;

    public abstract File getDefaultOutputDirectory();

    protected abstract File getOutputClassDirectory();

    protected abstract String[] getProcessors();

    protected abstract void addCompileSourceRoot(MavenProject project, String dir);

    protected abstract Set<String> getClasspathElements(Set<String> result);

    private String buildProcessor() {
        if (getProcessors() == null || getProcessors().length == 0) {
            return null;
        }

        StringBuilder result = new StringBuilder();

        int i;
        for (i = 0; i < getProcessors().length - 1; ++i) {
            result.append(getProcessors()[i]).append(',');
        }

        result.append(getProcessors()[i]);
        return result.toString();
    }


    private String buildCompileClasspath() {

        Set<String> pathElements = new HashSet<String>();
        if (pluginArtifacts != null) {
            for (Artifact a : pluginArtifacts) {
                if ("compile".equalsIgnoreCase(a.getScope()) || "runtime".equalsIgnoreCase(a.getScope())) {
                    File f = a.getFile();
                    if (f != null) {
                        pathElements.add(a.getFile().getAbsolutePath());
                    }
                }
            }
        }

        getClasspathElements(pathElements);
        StringBuilder result = new StringBuilder();

        for (String elem : pathElements) {
            result.append(elem).append(File.pathSeparator);
        }
        return result.toString();
    }

    /**
     *
     */
    public void execute() throws MojoExecutionException {
        if ("pom".equalsIgnoreCase(project.getPackaging())) {
            return;
        }

        try {
            executeWithExceptionsHandled();
        } catch (Exception e1) {
            getLog().error("error on execute: " + e1.getMessage());
            if (failOnError) {
                throw new MojoExecutionException(e1.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void executeWithExceptionsHandled() throws Exception {
        if (outputDirectory == null) {
            outputDirectory = getDefaultOutputDirectory();
        }

        ensureOutputDirectoryExists();
        addOutputToSourcesIfNeeded();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        final String includesString = (includes == null || includes.length == 0) ? "**/*.java" : StringUtils.join(includes, ",");
        final String excludesString = (excludes == null || excludes.length == 0) ? null : StringUtils.join(excludes, ",");

        List<File> filesToCompile = new ArrayList<File>(100);
        for (Object sourceDirectory : project.getCompileSourceRoots()) {
            File directory = new File((String) sourceDirectory);
            if (directory.exists()) {
                if (!directory.equals(outputDirectory)) {
                    filesToCompile.addAll(FileUtils.getFiles(directory, includesString, excludesString));
                }
            }
        }

        if (filesToCompile.isEmpty()) {
            getLog().warn("no source file(s) detected! processor compilationTask will be skipped!");
            return;
        }

        List<String> options = new ArrayList<String>(10);

        options.add("-cp");
        options.add(buildCompileClasspath());
        options.add("-proc:only");

        String processor = buildProcessor();
        if (processor != null) {
            options.add("-processor");
            options.add(processor);
        } else {
            getLog().info("No processors specified. Using default discovery mechanism.");
        }
        options.add("-d");
        options.add(getOutputClassDirectory().getPath());

        options.add("-s");
        options.add(outputDirectory.getPath());

        addCompilerArguments(options);

        setSystemProperties();

        CompilationTask compilationTask = compiler.getTask(
                new PrintWriter(System.out),
                fileManager,
                createDiagnosticListener(),
                options,
                null,
                fileManager.getJavaFileObjectsFromFiles(filesToCompile));

        // Perform the compilation compilationTask.
        if (!compilationTask.call()) {
            throw new Exception("An error ocurred while the DevKit was generating Java code. Check the logs for further details.");
        }
    }

    private void setSystemProperties() {
        if (systemProperties != null) {
            Set<Map.Entry<String, String>> pSet = systemProperties.entrySet();

            for (Map.Entry<String, String> e : pSet) {
                getLog().info(String.format("set system property : [%s] = [%s]", e.getKey(), e.getValue()));
                System.setProperty(e.getKey(), e.getValue());
            }

        }
    }

    private DiagnosticListener<JavaFileObject> createDiagnosticListener() {
        if (outputDiagnostics) {
            return new DiagnosticListener<JavaFileObject>() {
                @Override
                public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                    switch (diagnostic.getKind()) {
                        case ERROR:
                            getLog().error(diagnostic.toString().replace("error: ", "").replace("error on execute: ", ""));
                            break;
                        case WARNING:
                        case MANDATORY_WARNING:
                            getLog().warn(diagnostic.toString().replace("warning: ", ""));
                            break;
                        case NOTE:
                            getLog().info(diagnostic.toString().replace("Note: ", ""));
                            break;
                        case OTHER:
                            getLog().info(diagnostic.toString());
                            break;
                    }
                }
            };
        } else {
            return new DiagnosticListener<JavaFileObject>() {
                @Override
                public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                    // don't do anything since output diagnostics are disabled
                }
            };
        }
    }

    protected void addCompilerArguments(List<String> options) {
        if (!StringUtils.isEmpty(compilerArguments)) {
            for (String arg : compilerArguments.split(" ")) {
                if (!StringUtils.isEmpty(arg)) {
                    arg = arg.trim();
                    getLog().info("Adding compiler arg: " + arg);
                    options.add(arg);
                }
            }
        }
    }

    private void addOutputToSourcesIfNeeded() {
        final Boolean add = addOutputDirectoryToCompilationSources;
        if (add == null || add.booleanValue()) {
            getLog().info("Source directory: " + outputDirectory + " added");
            addCompileSourceRoot(project, outputDirectory.getAbsolutePath());
            getLog().info("Resource directory: " + outputDirectory + " added");

            Resource resourceDirectory = new Resource();
            resourceDirectory.setDirectory(outputDirectory.getAbsolutePath());
            project.addResource(resourceDirectory);
        }
    }

    private void ensureOutputDirectoryExists() {
        final File f = outputDirectory;
        if (!f.exists()) {
            f.mkdirs();
        }
        if (!getOutputClassDirectory().exists()) {
            getOutputClassDirectory().mkdirs();
        }
    }
}