package org.mule.devkit.it;

import java.lang.String;

public class OptionalModuleTest extends AbstractModuleTest
{

    @Override
    protected String getConfigResources()
    {
        return "optional.xml";
    }

    public void testOptionalParameter() throws Exception
    {
        runFlow("optionalParameter", 10);
    }

    public void testOptionalConfiguration() throws Exception
    {
        runFlow("optionalConfig", 50);
    }

}