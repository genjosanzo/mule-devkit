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

package org.mule.devkit.model.studio;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

public class NamespaceFilter extends XMLFilterImpl {

    private String usedNamespaceUri;
    private String prefix;
    private boolean addNamespace;
    private boolean addedNamespace;

    public NamespaceFilter(String prefix, String namespaceUri, boolean addNamespace) {

        if (addNamespace) {
            this.prefix = prefix;
            this.usedNamespaceUri = namespaceUri;
        } else {
            this.prefix = "";
            this.usedNamespaceUri = "";
        }
        this.addNamespace = addNamespace;
    }


    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        if (addNamespace) {
            startControlledPrefixMapping();
        }
    }


    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        super.startElement(usedNamespaceUri, localName, qName, atts);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(usedNamespaceUri, localName, qName);
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        super.startPrefixMapping(prefix, uri);
        if (addNamespace) {
            startControlledPrefixMapping();
        } else {
            //Remove the namespace, i.e. don´t call startPrefixMapping for parent!
        }
    }

    private void startControlledPrefixMapping() throws SAXException {
        if (addNamespace && !addedNamespace) {
            //We should add namespace since it is set and has not yet been done.
            super.startPrefixMapping(prefix, usedNamespaceUri);
            //Make sure we dont do it twice
            addedNamespace = true;
        }
    }
}