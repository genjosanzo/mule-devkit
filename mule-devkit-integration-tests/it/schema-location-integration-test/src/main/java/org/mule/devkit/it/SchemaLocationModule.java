package org.mule.devkit.it;

import org.mule.devkit.annotations.Configurable;
import org.mule.devkit.annotations.Module;
import org.mule.devkit.annotations.Processor;

@Module(name = "schemaloc",
        schemaLocation = "http://repository.mulesoft.org/releases/org/mule/modules/mule-module-schemaloc/1.0-SNAPSHOT/mule-schemaloc.xsd",
		namespace = "http://repository.mulesoft.org/releases/org/mule/modules/mule-module-schemaloc")
public class SchemaLocationModule
{
	@Configurable
	private String append;
	
    @Processor
    public String passthruString(String value)
    {
        return value + this.append;
    }

	public void setAppend(String app)
	{
		this.append = app;
	}
}
