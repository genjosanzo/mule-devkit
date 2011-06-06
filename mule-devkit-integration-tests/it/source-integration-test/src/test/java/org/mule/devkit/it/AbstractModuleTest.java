package org.mule.devkit.it;

import org.mule.api.MuleEvent;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.AbstractMuleTestCase;

import junit.framework.Assert;

public abstract class AbstractModuleTest extends FunctionalTestCase
{
    private static final String EMPTY_PAYLOAD = "";

    protected SimpleFlowConstruct lookupFlowConstruct(String name)
    {
        return (SimpleFlowConstruct) AbstractMuleTestCase.muleContext.getRegistry().lookupFlowConstruct(name);
    }

    protected <T> void runFlow(String flowName) throws Exception
    {
        String payload = EMPTY_PAYLOAD;
        SimpleFlowConstruct flow = lookupFlowConstruct(flowName);
        MuleEvent event = AbstractMuleTestCase.getTestEvent(payload);
        MuleEvent responseEvent = flow.process(event);
    }

    protected <T> void runFlow(String flowName, T expect) throws Exception
    {
        String payload = EMPTY_PAYLOAD;
        SimpleFlowConstruct flow = lookupFlowConstruct(flowName);
        MuleEvent event = AbstractMuleTestCase.getTestEvent(payload);
        MuleEvent responseEvent = flow.process(event);

        assertEquals(expect, responseEvent.getMessage().getPayload());
    }

    protected <T, U> void runFlowWithPayload(String flowName, T expect, U payload) throws Exception
    {
        SimpleFlowConstruct flow = lookupFlowConstruct(flowName);
        MuleEvent event = AbstractMuleTestCase.getTestEvent(payload);
        MuleEvent responseEvent = flow.process(event);

        assertEquals(expect, responseEvent.getMessage().getPayload());
    }
}
