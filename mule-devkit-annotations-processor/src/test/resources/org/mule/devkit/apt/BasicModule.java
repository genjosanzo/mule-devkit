package org.mule.devkit.apt;

import org.mule.devkit.annotations.Module;
import org.mule.devkit.annotations.Processor;

/**
 * Basic module
 */
@Module(name="basic")
public class BasicModule {
    @Processor
    public String add()
    {
        return "";
    }

    public String doNotAdd()
    {
        return "";
    }
}
