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

import java.util.List;
import java.util.Map;

@Module(name = "enums")
public class EnumModule {

    private Map<Property, String> properties;
    @Configurable
    private Property myProperty;

    @Processor
    public void setProperties(Map<Property, String> properties) {
        this.properties = properties;
    }

    @Processor
    public String getPropertyValue(Property property) {
        return properties.get(property);
    }

    @Processor
    public boolean checkPropertiesSet(Map<Property, String> allProperties, List<Property> propertiesToCheck) {
        for (Property property : propertiesToCheck) {
            if (!allProperties.containsKey(property)) {
                return false;
            }
        }
        return true;
    }

    public Property getMyProperty() {
        return myProperty;
    }

    public void setMyProperty(Property myProperty) {
        this.myProperty = myProperty;
    }
}