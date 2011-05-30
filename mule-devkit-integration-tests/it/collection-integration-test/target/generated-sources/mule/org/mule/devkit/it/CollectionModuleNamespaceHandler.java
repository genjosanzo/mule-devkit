
package org.mule.devkit.it;

import org.mule.config.spring.handlers.AbstractPojoNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.springframework.beans.factory.config.ListFactoryBean;

public class CollectionModuleNamespaceHandler
    extends AbstractPojoNamespaceHandler
{


    public void init() {
        registerPojo("config", CollectionModule.class);
        registerMuleBeanDefinitionParser("strings", new ChildDefinitionParser("strings", ListFactoryBean.class));
        registerMuleBeanDefinitionParser("string", new ChildListEntryDefinitionParser("sourceList"));
        registerMuleBeanDefinitionParser("items", new ChildDefinitionParser("items", ListFactoryBean.class));
        registerMuleBeanDefinitionParser("item", new ChildListEntryDefinitionParser("sourceList"));
        registerMuleBeanDefinitionParser("count-list-of-strings", new CountListOfStringsBeanDefinitionParser());
        registerMuleBeanDefinitionParser("strings", new ChildDefinitionParser("strings", ListFactoryBean.class));
        registerMuleBeanDefinitionParser("string", new ChildListEntryDefinitionParser("sourceList"));
        registerMuleBeanDefinitionParser("count-config-strings", new CountConfigStringsBeanDefinitionParser());
        registerMuleBeanDefinitionParser("count-config-items", new CountConfigItemsBeanDefinitionParser());
    }

}
