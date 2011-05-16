package org.mule.devkit.it;

import java.lang.String;

public class NamedConfigModuleTest extends AbstractModuleTest
{

    @Override
    protected String getConfigResources()
    {
        return "nc.xml";
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