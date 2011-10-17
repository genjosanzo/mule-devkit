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
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.annotations.param.Session;
import org.mule.api.annotations.param.SessionKey;
import org.mule.api.annotations.session.SessionCreate;
import org.mule.api.annotations.session.SessionDestroy;

import java.util.List;
import java.util.Map;

/**
 * Simple Module for testing
 *
 * @author Mulesoft, inc.
 */
@Module(name = "mymodule")
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
     * @param sesion          represents the session
     * @param stringParameter stringParameter description
     */
    @Processor
    public void operation4(@Session MySession sesion, String stringParameter) {
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
     * Creates the session
     *
     * @param user     the user name to use
     * @param password the password to use
     * @return the new session
     */
    @SessionCreate
    public MySession createSession(@SessionKey String user, String password) {
        return new MySession(user, password);
    }

    /**
     * Destroys the session
     */
    @SessionDestroy
    public void destroySession(MySession session) {
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