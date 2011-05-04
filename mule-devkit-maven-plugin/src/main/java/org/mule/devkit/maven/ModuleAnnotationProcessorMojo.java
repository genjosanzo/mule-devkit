package org.mule.devkit.maven;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.jfrog.maven.annomojo.annotations.MojoRequiresDependencyResolution;

@MojoGoal("generate-sources")
@MojoRequiresDependencyResolution(value = "compile")
@MojoPhase("generate-sources")
public class ModuleAnnotationProcessorMojo extends AbstractAnnotationProcessorMojo
{
    private static String[] processors = new String[]{ "org.mule.devkit.apt.ModuleAnnotationProcessor" };

    /** project classpath */
    @MojoParameter(expression = "${project.compileClasspathElements}", required = true, readonly = true)
    private List classpathElements;

    @MojoParameter(expression = "${project.build.sourceDirectory}", required = true)
    private File sourceDirectory;

    @MojoParameter(expression = "${project.build.directory}/generated-sources/mule", required = true)
    private File defaultOutputDirectory;

    @MojoParameter(required = false, expression="${project.build.outputDirectory}", description = "Set the destination directory for class files (same behaviour of -d option)")
    private File outputClassDirectory;

    @Override
    public File getSourceDirectory()
    {
        return sourceDirectory;
    }

    @Override
    protected File getOutputClassDirectory()
    {
        return outputClassDirectory;
    }

    @Override
    protected String[] getProcessors() {
        return processors;
    }

    protected void addCompileSourceRoot(MavenProject project, String dir)
    {
        project.addCompileSourceRoot(dir);
    }

    @Override
    public File getDefaultOutputDirectory()
    {
        return defaultOutputDirectory;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected java.util.Set<String> getClasspathElements( java.util.Set<String> result)
    {
        List<Resource> resources = project.getResources();

        if( resources!=null ) {
            for( Resource r : resources ) {
                result.add(r.getDirectory());
            }
        }

        result.addAll( classpathElements );
        
        return result;
     }


}
