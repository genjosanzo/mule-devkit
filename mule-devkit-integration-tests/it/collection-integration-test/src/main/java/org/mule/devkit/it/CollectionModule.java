package org.mule.devkit.it;

import org.mule.devkit.annotations.Configurable;
import org.mule.devkit.annotations.Module;
import org.mule.devkit.annotations.Processor;

import java.util.List;

@Module(name = "collection")
public class CollectionModule
{
	@Configurable(optional=true)
	private List<String> strings;

	@Configurable(optional=true)
	private List items;
	
    @Processor
    public int countListOfStrings(List<String> strings)
    {
        return strings.size();
    }

    @Processor
    public int countConfigStrings()
    {
        return this.strings.size();
    }

    @Processor
    public int countConfigItems()
    {
        return this.items.size();
    }

	public void setStrings(List strings)
	{
		this.strings = strings;
	}

	public void setItems(List<String> items)
	{
		this.items = items;
	}
	
}
