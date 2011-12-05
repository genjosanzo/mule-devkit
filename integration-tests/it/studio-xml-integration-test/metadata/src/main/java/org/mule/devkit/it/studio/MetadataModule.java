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
import org.mule.api.annotations.studio.Display;
import org.mule.api.callback.SourceCallback;

/**
 * Module to test annotations applied to parameters
 *
 * @author MuleSoft inc
 */
@Module(name = "metadata")
public class MetadataModule {

    /**
     * Configurable field in group 1
     */
    @Configurable
    @Display(inputGroup = "Group1")
    private String configurable1;
    /**
     * Configurable field in group 2
     */
    @Configurable
    @Display(inputGroup = "Group2")
    private String configurable2;
    /**
     * Configurable field in group 1
     */
    @Configurable
    @Display(inputGroup = "Group1")
    private String configurable3;

    /**
     * A processor method with Studio metadata
     *
     * @param noInputGroupExplicit parameter with no input group
     * @param advanced1            parameter in Advanced input group
     * @param advanced2            parameter in Advanced input group
     * @param general              parameter in General input group
     */
    @Processor
    public void processor(String noInputGroupExplicit,
                          @Display(inputGroup = "Advanced") String advanced1,
                          @Display(inputGroup = "Advanced") String advanced2,
                          String general) {
    }

    /**
     * A source method with Studio metadata
     *
     * @param sourceCallback       the source callback
     * @param noInputGroupExplicit parameter with no input group
     * @param advanced1            parameter in Advanced input group
     * @param advanced2            parameter in Advanced input group
     * @param general              parameter in General input group
     */
    @Source
    public void source(SourceCallback sourceCallback,
                       String noInputGroupExplicit,
                       @Display(inputGroup = "Advanced") String advanced1,
                       @Display(inputGroup = "Advanced") String advanced2,
                       String general) {
    }
}