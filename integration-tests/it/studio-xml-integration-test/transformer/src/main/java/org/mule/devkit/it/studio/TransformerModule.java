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

import org.mule.api.annotations.Module;
import org.mule.api.annotations.Transformer;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Transformer class
 *
 * @author MuleSoft inc
 */
@Module(name = "transformer")
public class TransformerModule {

    /**
     * Transforms a String to a character
     *
     * @param payload the input for this transformer
     * @return a character
     */
    @Transformer(sourceTypes = {String.class})
    public static Character stringToChar(Object payload) {
        if (payload != null) {
            return ((String) payload).charAt(0);
        }

        return null;
    }

    /**
     * Transforms a String to a URL
     *
     * @param payload the input for this transformer
     * @return a URL
     * @throws MalformedURLException
     */
    @Transformer(sourceTypes = {String.class})
    public static URL stringToUrl(Object payload) throws MalformedURLException {
        if (payload != null) {
            return new URL(String.valueOf(payload));
        }

        return null;
    }
}