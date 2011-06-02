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
            projectHelper.attachArtifact(project, "schema", file);
        }
    }

    private class SchemaFilenameFilter implements FilenameFilter {
        public boolean accept(File file, String s) {
            return s.startsWith("mule-") && s.endsWith("xsd");
        }
    }

}
