package org.mule.devkit.it;

import org.mule.devkit.annotations.Module;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.annotations.Configurable;
import org.mule.devkit.annotations.Parameter;

@Module(name = "optional")
public class OptionalModule
{
    @Configurable(optional = true, defaultValue="10")
    private int base;

    @Processor
    public int sumMultiplyAndDivide(int sum1, int sum2, @Parameter(optional=true, defaultValue="1") int multiply)
    {
        return ((sum1 + sum2) * multiply ) / base;
    }

    public void setBase(int value)
    {
        this.base = value;
    }

}
