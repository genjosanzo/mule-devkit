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

import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.callback.SourceCallback;

import java.util.HashMap;
import java.util.Map;

@Module(name = "source")
public class SourceModule
{
    @Source
    public void count(int startAt, int endAt, int step, SourceCallback callback) throws Exception
    {
		int count = startAt;
        while(true)
        {
            if(Thread.interrupted() || count == endAt)
                throw new InterruptedException();

            callback.process(count);

			count += step;
        }	
    }

    @Source
    public void countWithProperty(int startAt, int endAt, int step, String key, String value, SourceCallback callback) throws Exception
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(key, value);
		int count = startAt;
        while(true)
        {
            if(Thread.interrupted() || count == endAt)
                throw new InterruptedException();

            callback.process(count, properties);

			count += step;
        }
    }

    @Processor
    public void throwExceptionIfNoProperty(String key, @InboundHeaders("*") Map<String, Object> properties) throws Exception {
        if( !properties.containsKey(key) ) {
            throw new IllegalArgumentException();
        }
    }

}
