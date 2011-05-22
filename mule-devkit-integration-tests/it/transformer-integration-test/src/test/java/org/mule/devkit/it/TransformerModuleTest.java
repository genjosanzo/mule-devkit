package org.mule.devkit.it;

import java.lang.String;

public class TransformerModuleTest extends AbstractModuleTest
{

    @Override
    protected String getConfigResources()
    {
        return "transformer.xml";
    }

    public void testString() throws Exception
    {
        runFlow("transform", 'm');
    }
}