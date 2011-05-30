
package org.mule.devkit.it;

import java.util.ArrayList;
import java.util.List;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.TransformerTemplate;
import org.mule.transport.NullPayload;
import org.mule.util.TemplateParser;

public class CountConfigItemsMessageProcessor
    implements MuleContextAware, Initialisable, MessageProcessor
{

    private CollectionModule object;
    private MuleContext muleContext;
    private ExpressionManager expressionManager;
    private TemplateParser.PatternInfo patternInfo;

    public void initialise()
        throws InitialisationException
    {
        expressionManager = muleContext.getExpressionManager();
        patternInfo = TemplateParser.createMuleStyleParser().getStyle();
        if (object == null) {
            try {
                object = muleContext.getRegistry().lookupObject(CollectionModule.class);
            } catch (RegistrationException e) {
                throw new InitialisationException(CoreMessages.initialisationFailure("org.mule.devkit.it.CollectionModule"), e, this);
            }
        }
    }

    public void setMuleContext(MuleContext context) {
        this.muleContext = context;
    }

    public void setObject(CollectionModule object) {
        this.object = object;
    }

    public MuleEvent process(MuleEvent event)
        throws MuleException
    {
        MuleMessage muleMessage;
        muleMessage = event.getMessage();
        try {
            Object resultPayload;
            resultPayload = object.countConfigItems();
            if (resultPayload == null) {
                return new DefaultMuleEvent(new DefaultMuleMessage(NullPayload.getInstance(), muleContext), event);
            }
            List<Transformer> transformerList;
            transformerList = new ArrayList<Transformer>();
            transformerList.add(new TransformerTemplate(new TransformerTemplate.OverwitePayloadCallback(resultPayload)));
            event.getMessage().applyTransformers(event, transformerList);
            return event;
        } catch (Exception e) {
            throw new MessagingException(CoreMessages.failedToInvoke("countConfigItems"), event, e);
        }
    }

}
