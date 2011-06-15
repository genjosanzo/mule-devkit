
package org.mule.devkit.it;

import org.apache.commons.lang.UnhandledException;
import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class AnyXmlChildDefinitionParser
    extends ChildDefinitionParser
{


    public AnyXmlChildDefinitionParser(String setterMethod, Class clazz) {
        super(setterMethod, clazz);
        addIgnored("xmlns");
    }

    protected Class getBeanClass(Element element) {
        return String.class;
    }

    protected void processProperty(Attr attribute, BeanAssembler assembler) {
    }

    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        AbstractBeanDefinition bd;
        bd = super.parseInternal(element, parserContext);
        bd.setAttribute(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE, Boolean.TRUE);
        if (Node.ELEMENT_NODE == element.getNodeType()) {
            NodeList childs = element.getChildNodes();
            int i;
            for (i = 0; (i<childs.getLength()); i ++) {
                Node child = childs.item(i);
                if (Node.ELEMENT_NODE == child.getNodeType()) {
                    try {
                        DOMSource domSource = new DOMSource(child);
                        StringWriter stringWriter = new StringWriter();
                        StreamResult result = new StreamResult(stringWriter);
                        TransformerFactory tf = TransformerFactory.newInstance();
                        Transformer transformer = tf.newTransformer();
                        transformer.transform(domSource, result);
                        stringWriter.flush();
                        bd.getConstructorArgumentValues().addIndexedArgumentValue(0, stringWriter.toString());
                    } catch (TransformerConfigurationException e) {
                        throw new UnhandledException(e);
                    } catch (TransformerException e) {
                        throw new UnhandledException(e);
                    } catch (TransformerFactoryConfigurationError e) {
                        throw new UnhandledException(e);
                    }
                }
            }
        }
        return bd;
    }

}
