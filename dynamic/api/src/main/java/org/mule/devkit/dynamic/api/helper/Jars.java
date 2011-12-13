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
package org.mule.devkit.dynamic.api.helper;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Helper methods for jar files.
 */
public final class Jars {

    private Jars() {
    }

    /**
     * @param url
     * @return all {@link File} names contained in specified jar
     * @throws IOException 
     */
    public static List<String> allFileNames(final URL url) throws IOException {
        final ZipInputStream jarStream = new ZipInputStream(url.openStream());
        ZipEntry entry = null;
        final List<String> allNames = new LinkedList<String>();
        while((entry = jarStream.getNextEntry()) != null) {
            allNames.add(entry.getName());
        }
        return allNames;
    }

}