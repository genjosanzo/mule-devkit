/**
 * Mule Development Kit
 * Copyright 2010-2011 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule.devkit.it;

import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.param.Optional;

import java.util.List;
import java.util.Map;

/**
 * Collection module
 *
 * @author MuleSoft, Inc.
 */
@Module(name = "collection")
public class CollectionModule
{
    /**
     * Configurable strings
     */
	@Configurable
    @Optional
	private List<String> strings;

    /**
     * Configurable items
     */
	@Configurable
    @Optional
	private List items;

    /**
     * Configurable map of strings
     */
	@Configurable
    @Optional
	private Map<String, String> mapStrings;

    /**
     * Configurable list of strings
     */
	@Configurable
    @Optional
	private Map mapItems;

    /**
     * Count list of strings
     *
     * @param strings Strigns to count
     * @return Count
     */
    @Processor
    public int countListOfStrings(List<String> strings)
    {
        return strings.size();
    }

    /**
     * Count strings in config
     *
     * @return Count
     */
    @Processor
    public int countConfigStrings()
    {
        return this.strings.size();
    }

    /**
     * Count items in config
     *
     * @return Count
     */
    @Processor
    public int countConfigItems()
    {
        return this.items.size();
    }

    @Processor
    public int countMapOfStrings(Map<String, String> mapStrings)
    {
        return mapStrings.size();
    }

    @Processor
    public String retrieveKey(String key, Map<String, String> mapStrings)
    {
        return mapStrings.get(key);
    }
    @Processor
    public int countConfigMapStrings()
    {
        return this.mapStrings.size();
    }

    @Processor
    public String appendConfigMapItems()
    {
        String result = "";
        for( Object part : this.mapItems.keySet() )
        {
            result += this.mapItems.get(part);
        }

        return result;
    }

    @Processor
    public void hasFirstName(Map properties)
    {
        if( !properties.containsKey("FirstName") )
            throw new RuntimeException("Does not have a first name");
    }

    @Processor
    public void acceptNested(List<Map<String, String>> objects)
    {
        for( Map<String, String> object : objects )
        {
            if( object.keySet().size() != 3 )
                throw new RuntimeException("Invalid object");
        }
    }

    @Processor
    public int countTwoLists(List<String> firstLists, List<String> secondLists) {
        return firstLists.size() + secondLists.size();
    }

	public void setStrings(List strings)
	{
		this.strings = strings;
	}

	public void setItems(List<String> items)
	{
		this.items = items;
	}

	public void setMapStrings(Map mapStrings)
	{
		this.mapStrings = mapStrings;
	}

	public void setMapItems(Map<String, String> mapItems)
	{
		this.mapItems = mapItems;
	}
}
