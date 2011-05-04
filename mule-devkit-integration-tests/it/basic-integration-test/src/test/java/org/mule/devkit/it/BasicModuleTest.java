package org.mule.devkit.it;

import java.lang.String;

public class BasicModuleTest extends AbstractModuleTest
{

    @Override
    protected String getConfigResources()
    {
        return "basic.xml";
    }

    public void testChar() throws Exception
    {
        runFlow("passthruCharFlow", 'c');
    }

    public void testString() throws Exception
    {
        runFlow("passthruStringFlow", "mulesoft");
    }

    public void testInteger() throws Exception
    {
        runFlow("passthruIntegerFlow", 3);
    }

    public void testFloat() throws Exception
    {
        runFlow("passthruFloatFlow", 3.14f);
    }

    public void testBoolean() throws Exception
    {
        runFlow("passthruBooleanFlow", true);
    }

    public void testLong() throws Exception
    {
        runFlow("passthruLongFlow", 3456443463342345734L);
    }

    public void testTransformer() throws Exception
    {
        runFlowWithPayload("transformerFlow", 'd', "d");
    }
}