/**
 * This file was automatically generated by the Mule Development Kit
 */
#set($D='$')
#set($moduleNameLower = "${muleModuleName.toLowerCase()}")
#set($moduleGroupIdPath = $groupId.replace(".", "/"))
package ${package};

import org.mule.api.annotations.Module;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Transformer;

/**
 * Module
 *
 * @author MuleSoft, Inc.
 */
@Module(name="${moduleNameLower}", schemaVersion="${version}")
public class ${muleModuleName}Module
{
    /**
     * Transformer
     *
     * {@sample.xml ../../../doc/${muleModuleName}-connector.xml.sample ${moduleNameLower}:my-transform}
     * @param source Source object
     * @return Transformed object
     */
    @Transformer(sourceTypes={String.class})
    public static String myTransform(Object source)
    {
        if( source != null )
        {
            return ((String)source);
        }

        return null;
    }
}
