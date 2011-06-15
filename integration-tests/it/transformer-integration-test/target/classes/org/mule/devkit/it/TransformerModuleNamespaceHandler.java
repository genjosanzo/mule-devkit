
package org.mule.devkit.it;

import org.mule.config.spring.handlers.AbstractPojoNamespaceHandler;

public class TransformerModuleNamespaceHandler
    extends AbstractPojoNamespaceHandler
{


    public void init() {
        registerPojo("config", TransformerModule.class);
    }

}
