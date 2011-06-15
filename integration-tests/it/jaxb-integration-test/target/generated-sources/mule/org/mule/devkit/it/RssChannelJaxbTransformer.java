
package org.mule.devkit.it;

import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.it.rss.RssChannel;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class RssChannelJaxbTransformer
    extends AbstractTransformer
    implements DiscoverableTransformer
{

    private int weighting = (DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1);
    private static JAXBContext JAXB_CONTEXT = loadJaxbContext(RssChannel.class);

    public RssChannelJaxbTransformer() {
        registerSourceType(DataTypeFactory.STRING);
        setReturnClass(RssChannel.class);
        setName("org.mule.devkit.it.RssChannelJaxbTransformer");
    }

    private static JAXBContext loadJaxbContext(Class clazz) {
        JAXBContext context;
        try {
            context = JAXBContext.newInstance(clazz);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        return context;
    }

    protected Object doTransform(Object src, String encoding)
        throws TransformerException
    {
        RssChannel result = null;
        try {
            Unmarshaller unmarshaller;
            unmarshaller = JAXB_CONTEXT.createUnmarshaller();
            InputStream is = new ByteArrayInputStream(((String) src).getBytes(encoding));
            StreamSource ss = new StreamSource(is);
            result = unmarshaller.unmarshal(ss, RssChannel.class).getValue();
        } catch (UnsupportedEncodingException unsupportedEncoding) {
            throw new TransformerException(CoreMessages.transformFailed("String", "org.mule.devkit.it.rss.RssChannel"), this, unsupportedEncoding);
        } catch (JAXBException jaxbException) {
            throw new TransformerException(CoreMessages.transformFailed("String", "org.mule.devkit.it.rss.RssChannel"), this, jaxbException);
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
