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
package org.mule.devkit.maven;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;

/**
 * Creates the structure and archive for a Mule Application
 */
public class ModuleArchiver extends ZipArchiver {
    public final static String LIB_LOCATION = "lib" + File.separator;
    public final static String CLASSES_LOCATION = "classes" + File.separator;
    public final static String PLUGINS_LOCATION = "plugins" + File.separator;
    public final static String ROOT_LOCATION = "";

    public void addResources(final File directoryName) throws ArchiverException {
        addDirectory(directoryName, ROOT_LOCATION, null, addDefaultExcludes(null));
    }

    public void addLib(final File file) throws ArchiverException {
        addFile(file, LIB_LOCATION + file.getName());
    }

    public void addLibs(final File directoryName, final String[] includes, final String[] excludes) throws ArchiverException {
        addDirectory(directoryName, LIB_LOCATION, includes, addDefaultExcludes(excludes));
    }

    public void addPlugin(final File plugin) throws ArchiverException {
        addFile(plugin, PLUGINS_LOCATION + plugin.getName());
    }

    /**
     * add files under /classes
     */
    public void addClasses(File directoryName, String[] includes, String[] excludes)
            throws ArchiverException {
        addDirectory(directoryName, CLASSES_LOCATION, includes, addDefaultExcludes(excludes));
    }

    private String[] addDefaultExcludes(String[] excludes) {
        if ((excludes == null) || (excludes.length == 0)) {
            return DirectoryScanner.DEFAULTEXCLUDES;
        } else {
            String[] newExcludes = new String[excludes.length + DirectoryScanner.DEFAULTEXCLUDES.length];

            System.arraycopy(DirectoryScanner.DEFAULTEXCLUDES, 0, newExcludes, 0, DirectoryScanner.DEFAULTEXCLUDES.length);
            System.arraycopy(excludes, 0, newExcludes, DirectoryScanner.DEFAULTEXCLUDES.length, excludes.length);

            return newExcludes;
        }
    }

}
