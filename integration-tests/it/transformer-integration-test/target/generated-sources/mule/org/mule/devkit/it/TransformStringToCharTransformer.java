
package org.mule.devkit.it;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

public class TransformStringToCharTransformer
    extends AbstractTransformer
    implements MuleContextAware, Initialisable, DiscoverableTransformer
{

    private org.mule.devkit.it.TransformerModule object;
    private MuleContext muleContext;
    private int weighting = (DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 5);

    public TransformStringToCharTransformer() {
        registerSourceType(DataTypeFactory.create(String.class));
        registerSourceType(DataTypeFactory.create(Boolean.class));
        setReturnClass(Character.class);
        setName("org.mule.devkit.it.TransformStringToCharTransformer");
    }

    public void initialise()
        throws InitialisationException
    {
        if (object == null) {
            try {
                object = muleContext.getRegistry().lookupObject(org.mule.devkit.it.TransformerModule.class);
            } catch (RegistrationException e) {
                throw new InitialisationException(CoreMessages.initialisationFailure("org.mule.devkit.it.TransformerModule"), e, this);
            }
        }
    }

    public void setMuleContext(MuleContext context) {
        this.muleContext = context;
    }

    public void setObject(org.mule.devkit.it.TransformerModule object) {
        this.object = object;
    }

    protected Object doTransform(Object src, String encoding)
        throws TransformerException
    {
        Character result = null;
        try {
            result = object.transformStringToChar(src);
        } catch (Exception exception) {
            throw new TransformerException(CoreMessages.transformFailed(src.getClass().getName(), "java.lang.Character"), this, exception);
        }
        return result;
    }

    public int getPriorityWeighting() {
        return weighting;
    }

    public void setPriorityWeighting(int weighting) {
        this.weighting = weighting;
    }

}
