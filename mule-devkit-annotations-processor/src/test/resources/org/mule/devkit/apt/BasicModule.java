package org.mule.devkit.apt;

import org.mule.devkit.annotations.Configurable;
import org.mule.devkit.annotations.Module;
import org.mule.devkit.annotations.Processor;

/**
 * Basic module
 */
@Module(name="basic")
public class BasicModule {

    @Configurable
    private int mandatoryField;

    @Configurable(optional = true)
    private String optionalField;

    @Processor
    public int sum(int a, int b)
    {
        return a + b;
    }

    public String doNotAdd()
    {
        return "";
    }
}
