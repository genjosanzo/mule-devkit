package org.mule.devkit.it;

import java.lang.String;

public class CollectionModuleTest extends AbstractModuleTest
{

    @Override
    protected String getConfigResources()
    {
        return "collection.xml";
    }

    public void testList() throws Exception
    {
        runFlow("flowList", 2);
    }

    public void testConfigStrings() throws Exception
    {
        runFlow("flowConfigStrings", 2);
    }

    public void testConfigItems() throws Exception
    {
        runFlow("flowConfigItems", 2);
    }

}