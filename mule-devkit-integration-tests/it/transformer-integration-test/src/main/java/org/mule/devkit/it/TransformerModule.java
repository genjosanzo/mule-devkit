package org.mule.devkit.it;

import org.mule.devkit.annotations.Module;
import org.mule.devkit.annotations.Transformer;

import java.lang.Character;
import java.lang.String;

@Module(name = "transformer")
public class TransformerModule
{

    @Transformer(sourceTypes={String.class})
    public Character transformStringToChar(Object payload)
    {
        if( payload != null )
        {
            return ((String)payload).charAt(0);
        }

        return null;
    }

}
