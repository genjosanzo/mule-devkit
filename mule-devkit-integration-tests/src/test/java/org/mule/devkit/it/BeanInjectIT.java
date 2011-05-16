package org.mule.devkit.it;

import java.io.File;

public class BeanInjectIT extends AbstractMavenIT
{

    protected String getArtifactVersion()
    {
        return "1.0";
    }

    protected String getArtifactId()
    {
        return "bean-inject-integration-test";
    }

    protected String getGroupId()
    {
        return "org.mule.devkit.it";
    }

    protected File getRoot()
    {
        return new File("target/integration-tests/" + getArtifactId());
    }
}
