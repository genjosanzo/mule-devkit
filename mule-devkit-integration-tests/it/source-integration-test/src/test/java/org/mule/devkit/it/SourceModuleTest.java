package org.mule.devkit.it;

import java.lang.String;

public class SourceModuleTest extends AbstractModuleTest
{

    @Override
    protected String getConfigResources()
    {
        return "source.xml";
    }

    public void testSource() throws Exception
    {
        runFlow("source");
    }
}