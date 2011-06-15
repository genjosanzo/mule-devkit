
package org.mule.devkit.it;

import org.mule.config.spring.handlers.AbstractPojoNamespaceHandler;

public class OptionalModuleNamespaceHandler
    extends AbstractPojoNamespaceHandler
{


    public void init() {
        registerPojo("config", OptionalModule.class);
        registerMuleBeanDefinitionParser("sum-multiply-and-divide", new SumMultiplyAndDivideBeanDefinitionParser());
    }

}
