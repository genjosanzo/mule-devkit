
package org.mule.devkit.it;

import org.mule.config.spring.handlers.AbstractPojoNamespaceHandler;

public class RssModuleNamespaceHandler
    extends AbstractPojoNamespaceHandler
{


    public void init() {
        registerPojo("config", RssModule.class);
        registerMuleBeanDefinitionParser("item-count", new ItemCountBeanDefinitionParser());
        registerMuleBeanDefinitionParser("channel", new AnyXmlChildDefinitionParser("channel", ItemCountMessageProcessor.class));
    }

}
