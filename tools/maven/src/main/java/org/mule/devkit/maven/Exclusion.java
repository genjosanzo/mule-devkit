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

public class Exclusion extends org.apache.maven.model.Exclusion {
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(128);
        buf.append("<");
        buf.append(getClass().getSimpleName());
        buf.append(" ");
        buf.append(getGroupId());
        buf.append(":");
        buf.append(getArtifactId());
        buf.append(">");
        return buf.toString();
    }

    public String asFilter() {
        StringBuilder buf = new StringBuilder(128);
        buf.append(getGroupId());
        buf.append(":");
        buf.append(getArtifactId());
        return buf.toString();
    }
}
