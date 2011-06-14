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
