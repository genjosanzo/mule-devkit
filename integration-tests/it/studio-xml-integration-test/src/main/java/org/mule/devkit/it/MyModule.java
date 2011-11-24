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

import org.mule.api.ConnectionException;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.ConnectionIdentifier;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Disconnect;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.ValidateConnection;
import org.mule.api.annotations.param.ConnectionKey;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.annotations.param.Payload;

import java.util.List;
import java.util.Map;

/**
 * Simple Module for testing
 *
 * @author MuleSoft, inc.
 */
@Connector(name = "mymodule")
public class MyModule {

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
     * operation1 method description
     *
     * @param stringParameter  stringParameter description
     * @param intParameter     intParameter description
     * @param booleanParameter booleanParameter description
     */
    @Processor
    public void operation1(String stringParameter, int intParameter, boolean booleanParameter) {
    }

    /**
     * operation2 method description
     *
     * @param stringParameter  stringParameter description
     * @param intParameter     intParameter description
     * @param booleanParameter booleanParameter description
     */
    @Processor
    public void operation2(@Optional String stringParameter, @Optional Integer intParameter, @Optional Boolean booleanParameter) {
    }

    /**
     * operation3 method description
     *
     * @param stringParameter  stringParameter description
     * @param intParameter     intParameter description
     * @param booleanParameter booleanParameter description
     */
    @Processor
    public void operation3(@Optional @Default("fede") String stringParameter,
                           @Optional @Default("10") int intParameter,
                           @Optional @Default("true") boolean booleanParameter) {
    }

    /**
     * operation4 method description
     *
     * @param stringParameter stringParameter description
     */
    @Processor
    public void operation4(String stringParameter) {
    }

    /**
     * operation5 method description
     *
     * @param integers represents the integers
     * @param strings  represents the strings
     * @param booleans represents the booleans
     */
    @Processor
    public void operation5(List<Integer> integers, List<String> strings, List<Boolean> booleans) {
    }

    /**
     * operation6 method description
     *
     * @param stringStringMap represents the stringStringMap
     * @param stringObjectMap represents the stringObjectMap
     */
    @Processor
    public void operation6(Map<String, String> stringStringMap, Map<String, Object> stringObjectMap) {
    }

    /**
     * operation7 method description
     *
     * @param payload the payload
     */
    @Processor
    public void operation7(@Payload Object payload) {
    }

    /**
     * operation8 method description
     *
     * @param siNoEnum                represents the siNoEnum
     * @param siNoEnumOptional        represents the siNoEnumOptional
     * @param siNoEnumOptionalDefault represents the siNoEnumOptionalDefault
     */
    @Processor
    public void operation8(SiNoEnum siNoEnum, @Optional SiNoEnum siNoEnumOptional, @Optional @Default("NO") SiNoEnum siNoEnumOptionalDefault) {
    }

    /**
     * operation9 method description
     *
     * @param object         represents the object
     * @param customObject represents the customObject
     */
    @Processor
    public void operation9(Object object, @Optional CustomObject customObject) {
    }

//    /**
//     * operation10 method description
//     *
//     * @param httpCallback represents the httpCallback
//     */
//    @Processor
//    public void operation10(HttpCallback httpCallback) {
//    }

    /**
     * Create a connection
     *
     * @param user     the user name to use
     * @param password the password to use
     * @return the new session
     */
    @Connect
    public void connect(@ConnectionKey String user, String password)
            throws ConnectionException {
    }

    /**
     * Disconnect
     */
    @Disconnect
    public void disconnect() {
    }

    /**
     * Is Connected
     */
    @ValidateConnection
    public boolean isConnected() {
        return true;
    }

    /**
     * Is Connected
     */
    @ConnectionIdentifier
    public String connectionId() {
        return "001";
    }

    public void setConfigurableString(String configurableString) {
        this.configurableString = configurableString;
    }

    public void setOptionalConfigurableString(String optionalConfigurableString) {
        this.optionalConfigurableString = optionalConfigurableString;
    }

    public void setOptionalWithDefaultConfigurableString(String optionalWithDefaultConfigurableString) {
        this.optionalWithDefaultConfigurableString = optionalWithDefaultConfigurableString;
    }
}