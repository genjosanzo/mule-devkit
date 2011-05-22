package org.mule.devkit.it;

import org.mule.devkit.annotations.Module;
import org.mule.devkit.annotations.Transformer;

@Module(name = "transformer")
public class TransformerModule
{

    @Transformer(sourceType={  })
    public char transformStringToChar(String value)
    {
        return value.at(0);
    }

}
