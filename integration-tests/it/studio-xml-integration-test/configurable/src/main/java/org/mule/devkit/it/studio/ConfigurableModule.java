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
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;

import java.net.URL;

/**
 * Connector class
 *
 * @author MuleSoft inc
 */
@Module(name = "configurable")
public class ConfigurableModule {

    /**
     * Configurable String
     */
    @Configurable
    private String configurableString;
    /**
     * Configurable optional String
     */
    @Configurable
    @Optional
    private String optionalConfigurableString;
    /**
     * Configurable optional String with default value
     */
    @Configurable
    @Optional
    @Default("a default")
    private String optionalWithDefaultConfigurableString;

    /**
     * Configurable URL
     */
    @Configurable
    @Optional
    @Default("http://myUrl:9999")
    private URL url;

    /**
     * Configurable enumerated
     */
    @Configurable
    @Optional
    @Default("NO")
    private SiNoEnum siNoEnum;

    public enum SiNoEnum {
        SI, NO
    }
}