
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
import org.mule.transformer.TransformerTemplate;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;
import org.mule.util.TemplateParser;

import java.util.ArrayList;
import java.util.List;

public class SumMultiplyAndDivideMessageProcessor
    implements MuleContextAware, Initialisable, MessageProcessor
{

    private Object sum1;
    private Object sum2;
    private Object multiply;
    private OptionalModule object;
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
                object = muleContext.getRegistry().lookupObject(OptionalModule.class);
            } catch (RegistrationException e) {
                throw new InitialisationException(CoreMessages.initialisationFailure("org.mule.devkit.it.OptionalModule"), e, this);
            }
        }
    }

    public void setMuleContext(MuleContext context) {
        this.muleContext = context;
    }

    public void setObject(OptionalModule object) {
        this.object = object;
    }

    public void setSum2(Object value) {
        this.sum2 = value;
    }

    public void setSum1(Object value) {
        this.sum1 = value;
    }

    public void setMultiply(Object value) {
        this.multiply = value;
    }

    public MuleEvent process(MuleEvent event)
        throws MuleException
    {
        MuleMessage muleMessage;
        muleMessage = event.getMessage();
        try {
            Object evaluatedSum2;
            Integer transformedSum2;
            if (sum2 instanceof String) {
                if (((String) sum2).startsWith(patternInfo.getPrefix())&&((String) sum2).endsWith(patternInfo.getSuffix())) {
                    evaluatedSum2 = expressionManager.evaluate(((String) sum2), muleMessage);
                } else {
                    evaluatedSum2 = expressionManager.parse(((String) sum2), muleMessage);
                }
            } else {
                evaluatedSum2 = sum2;
            }
            if (!Integer.class.isAssignableFrom(evaluatedSum2 .getClass())) {
                DataType source;
                DataType target;
                source = DataTypeFactory.create(evaluatedSum2 .getClass());
                target = DataTypeFactory.create(Integer.class);
                Transformer t;
                t = muleContext.getRegistry().lookupTransformer(source, target);
                transformedSum2 = ((Integer) t.transform(evaluatedSum2));
            } else {
                transformedSum2 = ((Integer) evaluatedSum2);
            }
            Object evaluatedSum1;
            Integer transformedSum1;
            if (sum1 instanceof String) {
                if (((String) sum1).startsWith(patternInfo.getPrefix())&&((String) sum1).endsWith(patternInfo.getSuffix())) {
                    evaluatedSum1 = expressionManager.evaluate(((String) sum1), muleMessage);
                } else {
                    evaluatedSum1 = expressionManager.parse(((String) sum1), muleMessage);
                }
            } else {
                evaluatedSum1 = sum1;
            }
            if (!Integer.class.isAssignableFrom(evaluatedSum1 .getClass())) {
                DataType source;
                DataType target;
                source = DataTypeFactory.create(evaluatedSum1 .getClass());
                target = DataTypeFactory.create(Integer.class);
                Transformer t;
                t = muleContext.getRegistry().lookupTransformer(source, target);
                transformedSum1 = ((Integer) t.transform(evaluatedSum1));
            } else {
                transformedSum1 = ((Integer) evaluatedSum1);
            }
            Object evaluatedMultiply;
            Integer transformedMultiply;
            if (multiply instanceof String) {
                if (((String) multiply).startsWith(patternInfo.getPrefix())&&((String) multiply).endsWith(patternInfo.getSuffix())) {
                    evaluatedMultiply = expressionManager.evaluate(((String) multiply), muleMessage);
                } else {
                    evaluatedMultiply = expressionManager.parse(((String) multiply), muleMessage);
                }
            } else {
                evaluatedMultiply = multiply;
            }
            if (!Integer.class.isAssignableFrom(evaluatedMultiply.getClass())) {
                DataType source;
                DataType target;
                source = DataTypeFactory.create(evaluatedMultiply.getClass());
                target = DataTypeFactory.create(Integer.class);
                Transformer t;
                t = muleContext.getRegistry().lookupTransformer(source, target);
                transformedMultiply = ((Integer) t.transform(evaluatedMultiply));
            } else {
                transformedMultiply = ((Integer) evaluatedMultiply);
            }
            Object resultPayload;
            resultPayload = object.sumMultiplyAndDivide(transformedMultiply, transformedSum1, transformedSum2);
            if (resultPayload == null) {
                return new DefaultMuleEvent(new DefaultMuleMessage(NullPayload.getInstance(), muleContext), event);
            }
            List<Transformer> transformerList;
            transformerList = new ArrayList<Transformer>();
            transformerList.add(new TransformerTemplate(new TransformerTemplate.OverwitePayloadCallback(resultPayload)));
            event.getMessage().applyTransformers(event, transformerList);
            return event;
        } catch (Exception e) {
            throw new MessagingException(CoreMessages.failedToInvoke("sumMultiplyAndDivide"), event, e);
        }
    }

}
