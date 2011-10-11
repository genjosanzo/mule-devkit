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

public class CloudConnector extends BaseAttributes {

    private String localId;
    private String icon;
    private String image;
    private AttributeCategory attributeCategory;
    private String aliasId;
    private String _abstract;
    private String _extends;

    public CloudConnector(XStream xStream, String moduleName) {
        super(xStream);
        xStream.alias("cloud-connector", CloudConnector.class);
        xStream.useAttributeFor(CloudConnector.class, "_abstract");
        xStream.aliasField("abstract", CloudConnector.class, "_abstract");
        xStream.useAttributeFor(CloudConnector.class, "_extends");
        xStream.aliasField("extends", CloudConnector.class, "_extends");
        xStream.useAttributeFor(CloudConnector.class, "localId");
        xStream.useAttributeFor(CloudConnector.class, "icon");
        xStream.useAttributeFor(CloudConnector.class, "image");
        xStream.useAttributeFor(CloudConnector.class, "aliasId");
        xStream.aliasField("attribute-category", CloudConnector.class, "attributeCategory");
        icon = "icons/small/" + moduleName + "-connector-24x16.png";
        image = "icons/large/" + moduleName + "-connector-48x32.png";
    }

    public AttributeCategory getAttributeCategory() {
        return attributeCategory;
    }

    public void setAttributeCategory(AttributeCategory attributeCategory) {
        this.attributeCategory = attributeCategory;
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

    public void setAliasId(String aliasId) {
        this.aliasId = aliasId;
    }

    public String getAbstract() {
        return _abstract;
    }

    public void setAbstract(String _abstract) {
        this._abstract = _abstract;
    }

    public String getExtends() {
        return _extends;
    }

    public void setExtends(String _extends) {
        this._extends = _extends;
    }
}