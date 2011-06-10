package org.mule.devkit.it;

import java.lang.String;

public class SchemaLocationModuleTest extends AbstractModuleTest
{

    @Override
    protected String getConfigResources()
    {
        return "schemaloc.xml";
    }

    public void testConfigA() throws Exception
    {
        runFlow("passthruStringFlowConfigA", "mulesoft123");
    }

    public void testConfigB() throws Exception
    {
        runFlow("passthruStringFlowConfigB", "ftw456");
    }
}