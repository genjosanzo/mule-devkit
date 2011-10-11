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

import com.thoughtworks.xstream.XStream;

import java.util.ArrayList;
import java.util.List;

public class Namespace {

    private String xmlns;
    private String prefix;
    private String url;
    private GlobalCloudConnector globalCloudConnector;
    private List<CloudConnector> cloudConnectors = new ArrayList<CloudConnector>();
    private List<Nested> nesteds = new ArrayList<Nested>();

    public Namespace(XStream xStream) {
        xStream.alias("namespace", Namespace.class);
        xStream.addImplicitCollection(Namespace.class, "cloudConnectors");
        xStream.addImplicitCollection(Namespace.class, "nesteds");
        xStream.useAttributeFor(Namespace.class, "xmlns");
        xStream.useAttributeFor(Namespace.class, "prefix");
        xStream.useAttributeFor(Namespace.class, "url");
        xStream.aliasField("global-cloud-connector", Namespace.class, "globalCloudConnector");
    }

    public List<CloudConnector> getCloudConnectors() {
        return cloudConnectors;
    }

    public void setCloudConnectors(List<CloudConnector> cloudConnectors) {
        this.cloudConnectors = cloudConnectors;
    }

    public GlobalCloudConnector getGlobalCloudConnector() {
        return globalCloudConnector;
    }

    public void setGlobalCloudConnector(GlobalCloudConnector globalCloudConnector) {
        this.globalCloudConnector = globalCloudConnector;
    }

    public String getXmlns() {
        return xmlns;
    }

    public void setXmlns(String xmlns) {
        this.xmlns = xmlns;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Nested> getNesteds() {
        return nesteds;
    }

    public void setNesteds(List<Nested> nesteds) {
        this.nesteds = nesteds;
    }
}