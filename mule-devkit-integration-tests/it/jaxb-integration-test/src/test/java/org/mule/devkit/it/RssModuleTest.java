package org.mule.devkit.it;

import java.lang.String;

public class RssModuleTest extends AbstractModuleTest
{

    @Override
    protected String getConfigResources()
    {
        return "rss.xml";
    }

    public void testJAXB() throws Exception
    {
        runFlow("feedCountFlow", 1);
    }
}