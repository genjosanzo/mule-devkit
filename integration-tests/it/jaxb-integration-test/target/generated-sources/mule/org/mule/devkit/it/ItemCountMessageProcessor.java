
package org.mule.devkit.it;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.*;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.it.rss.RssChannel;
import org.mule.transformer.TransformerTemplate;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;
import org.mule.util.TemplateParser;

import java.util.ArrayList;
import java.util.List;

public class ItemCountMessageProcessor
    implements MuleContextAware, Initialisable, MessageProcessor
{

    private Object channel;
    private RssModule object;
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
                object = muleContext.getRegistry().lookupObject(RssModule.class);
            } catch (RegistrationException e) {
                throw new InitialisationException(CoreMessages.initialisationFailure("org.mule.devkit.it.RssModule"), e, this);
            }
        }
    }

    public void setMuleContext(MuleContext context) {
        this.muleContext = context;
    }

    public void setObject(RssModule object) {
        this.object = object;
    }

    public void setChannel(Object value) {
        this.channel = value;
    }

    public MuleEvent process(MuleEvent event)
        throws MuleException
    {
        MuleMessage muleMessage;
        muleMessage = event.getMessage();
        try {
            Object evaluatedChannel;
            RssChannel transformedChannel;
            if (channel instanceof String) {
                if (((String) channel).startsWith(patternInfo.getPrefix())&&((String) channel).endsWith(patternInfo.getSuffix())) {
                    evaluatedChannel = expressionManager.evaluate(((String) channel), muleMessage);
                } else {
                    evaluatedChannel = expressionManager.parse(((String) channel), muleMessage);
                }
            } else {
                evaluatedChannel = channel;
            }
            if (!RssChannel.class.isAssignableFrom(evaluatedChannel.getClass())) {
                DataType source;
                DataType target;
                source = DataTypeFactory.create(evaluatedChannel.getClass());
                target = DataTypeFactory.create(RssChannel.class);
                Transformer t;
                t = muleContext.getRegistry().lookupTransformer(source, target);
                transformedChannel = ((RssChannel) t.transform(evaluatedChannel));
            } else {
                transformedChannel = ((RssChannel) evaluatedChannel);
            }
            Object resultPayload;
            resultPayload = object.itemCount(transformedChannel);
            if (resultPayload == null) {
                return new DefaultMuleEvent(new DefaultMuleMessage(NullPayload.getInstance(), muleContext), event);
            }
            List<Transformer> transformerList;
            transformerList = new ArrayList<Transformer>();
            transformerList.add(new TransformerTemplate(new TransformerTemplate.OverwitePayloadCallback(resultPayload)));
            event.getMessage().applyTransformers(event, transformerList);
            return event;
        } catch (Exception e) {
            throw new MessagingException(CoreMessages.failedToInvoke("itemCount"), event, e);
        }
    }

}
