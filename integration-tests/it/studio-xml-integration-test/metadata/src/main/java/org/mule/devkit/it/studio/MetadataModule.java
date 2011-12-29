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
package org.mule.devkit.it.studio;

import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Password;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.display.Summary;
import org.mule.api.callback.SourceCallback;

/**
 * Module to test annotations applied to parameters
 *
 * @author MuleSoft inc
 */
@Module(name = "metadata", description = "This description overrides class-level javadoc.")
public class MetadataModule {

    /**
     * Configurable field in group 1
     */
    @Configurable
    @Placement(group = "Group1", order = 1)
    @FriendlyName("non-default caption")
    private String configurable1;
    /**
     * Configurable field in group 2
     */
    @Configurable
    @Placement(group = "Group2", order = 2)
    @Summary("non-default description")
    private String configurable2;
    /**
     * Configurable field in group 1
     */
    @Configurable
    @Placement(group = "Group1", order = 3)
    @FriendlyName("non-default caption")
    @Summary("non-default description")
    private String configurable3;

    /**
     * A processor method with Studio metadata
     *
     * @param showFirst parameter with no input group
     * @param advanced1 parameter in Advanced input group
     * @param advanced2 parameter in Advanced input group
     * @param general   parameter in General input group
     * @param password  password parameter
     */
    @Processor
    public void processor(@Placement(order = 1) @FriendlyName("non-default caption") @Summary("non-default description") String showFirst,
                          @Placement(group = "Advanced", order = 2) @FriendlyName("non-default caption") String advanced1,
                          @Placement(group = "Advanced", order = 1) @Summary("non-default description") String advanced2,
                          String general,
                          @Password String password) {
    }

    /**
     * A source method with Studio metadata
     *
     * @param sourceCallback       the source callback
     * @param advanced1            parameter in Advanced input group
     * @param advanced2            parameter in Advanced input group
     * @param general              parameter in General input group
     */
    @Source
    public void source(SourceCallback sourceCallback,
                       @Placement(group = "Advanced", order = 1) @FriendlyName("non-default caption") String advanced1,
                       @Placement(group = "Advanced", order = 2) @Summary("non-default description") String advanced2,
                       @Placement(order = 1) String general) {
    }
}