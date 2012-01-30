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
package org.mule.devkit.dynamic.api.transformer;

import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;

import java.net.MalformedURLException;
import java.net.URL;

public class StringToURL extends AbstractTransformer implements DiscoverableTransformer {

    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

    public StringToURL() {
        registerSourceType(new SimpleDataType<Object>(String.class));
        setReturnDataType(DataTypeFactory.create(URL.class));
    }

    @Override
    protected final Object doTransform(final Object source, final String encoding) throws TransformerException {
        try {
            return new URL(source.toString());
        } catch (MalformedURLException e) {
            throw new TransformerException(CoreMessages.createStaticMessage("Unable to transform <"+source+"> to a "+URL.class.getSimpleName()), e);
        }
    }

    @Override
    public final int getPriorityWeighting() {
        return this.priorityWeighting;
    }

    @Override
    public final void setPriorityWeighting(final int priorityWeighting) {
        this.priorityWeighting = priorityWeighting;
    }
    
}