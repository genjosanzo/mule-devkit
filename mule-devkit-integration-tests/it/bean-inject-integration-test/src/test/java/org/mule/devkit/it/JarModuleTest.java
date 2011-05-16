package org.mule.devkit.it;

import java.lang.String;

public class JarModuleTest extends AbstractModuleTest
{

    @Override
    protected String getConfigResources()
    {
        return "jar.xml";
    }

    public void testSetManifest() throws Exception
    {
        runFlow("setManifest");
    }
}