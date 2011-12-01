
package org.mule.devkit.it.studio.config.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;


/**
 * Registers bean definitions parsers for handling elements in <code>http://www.mulesoft.org/schema/mule/connector</code>.
 * 
 */
public class ConnectorModuleNamespaceHandler
    extends NamespaceHandlerSupport
{


    /**
     * Invoked by the {@link DefaultBeanDefinitionDocumentReader} after construction but before any custom elements are parsed. 
     * @see NamespaceHandlerSupport#registerBeanDefinitionParser(String, BeanDefinitionParser)
     * 
     */
    public void init() {
        registerBeanDefinitionParser("config", new ConnectorModuleConfigDefinitionParser());
        registerBeanDefinitionParser("invalidate", new InvalidateDefinitionParser());
        registerBeanDefinitionParser("get-username", new GetUsernameDefinitionParser());
    }

}
