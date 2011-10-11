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

public class Nested extends BaseAttributes {

    private String localId;
    private String icon;
    private String image;
    private ChildElement childElement;
    private List<Parameter> parameters = new ArrayList<Parameter>();

    public Nested(XStream xStream, String moduleName) {
        super(xStream);
        xStream.alias("nested", Nested.class);
        xStream.useAttributeFor(Nested.class, "localId");
        xStream.useAttributeFor(Nested.class, "icon");
        xStream.useAttributeFor(Nested.class, "image");
        xStream.addImplicitCollection(Nested.class, "parameters");
        icon = "icons/small/" + moduleName + "-connector-24x16.png";
        image = "icons/large/" + moduleName + "-connector-48x32.png";
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setChildElement(ChildElement childElement) {
        this.childElement = childElement;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }
}