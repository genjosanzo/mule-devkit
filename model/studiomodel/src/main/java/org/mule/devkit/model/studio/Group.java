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

public class Group {

    private String caption;
    private String id;
    private List<Parameter> parameters = new ArrayList<Parameter>();
    private ModeSwitch modeSwitch;
    private Name name;
    private String description;

    public Group(XStream xStream) {
        xStream.alias("group", Group.class);
        xStream.addImplicitCollection(Group.class, "parameters");
        xStream.useAttributeFor(Group.class, "caption");
        xStream.useAttributeFor(Group.class, "id");
        xStream.useAttributeFor(Group.class, "description");
        caption = "Generic";
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public ModeSwitch getModeSwitch() {
        return modeSwitch;
    }

    public void setModeSwitch(ModeSwitch modeSwitch) {
        this.modeSwitch = modeSwitch;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}