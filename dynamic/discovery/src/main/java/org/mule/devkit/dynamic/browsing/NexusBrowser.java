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
package org.mule.devkit.dynamic.browsing;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.mule.devkit.dynamic.api.helper.Reflections;
import org.sonatype.nexus.client.NexusClient;
import org.sonatype.nexus.client.NexusClientException;
import org.sonatype.nexus.client.NexusConnectionException;
import org.sonatype.nexus.client.rest.NexusRestClient;
import org.sonatype.nexus.rest.model.NexusArtifact;

public class NexusBrowser {

    private final String nexusUrl;
    private static final String DEFAULT_NEXUS_URL = "https://repository.mulesoft.org/nexus/";
    private static final String DEFAULT_GROUPD_ID = "org.mule.modules";
    private static final String DEFAULT_PACKAGING = "mule-module";
    private static final String CLIENT_HELPER_FIELD_NAME = "clientHelper";
    private static final String CHALLENGE_FIELD_NAME = "challenge";

    public NexusBrowser() {
        this(NexusBrowser.DEFAULT_NEXUS_URL);
    }

    public NexusBrowser(final String nexusUrl) {
        this.nexusUrl = nexusUrl;
    }

    protected final NexusClient createConnectedClient(final String url) throws NexusClientException, NexusConnectionException {
        final NexusClient client = new NexusRestClient();
        
        client.connect(url, null, null);
        Reflections.set(Reflections.get(client, NexusBrowser.CLIENT_HELPER_FIELD_NAME), NexusBrowser.CHALLENGE_FIELD_NAME, null);
        return client;
    }

    protected final List<NexusArtifact> getMatchingGroup(final NexusArtifact artifact, final List<List<NexusArtifact>> allGroups) {
        List<NexusArtifact> matchingGroup = null;
        for (final List<NexusArtifact> group : allGroups) {
            final NexusArtifact template = group.get(0);
            if (template.getArtifactId().equals(artifact.getArtifactId()) && template.getGroupId().equals(template.getGroupId())) {
                matchingGroup = group;
                break;
            }
        }
        if ( matchingGroup == null) {
            matchingGroup = new LinkedList<NexusArtifact>();
            allGroups.add(matchingGroup);
        }
        return matchingGroup;
    }

    public final Set<String> listArtifactIds() throws Exception {
        return listArtifactIds(NexusBrowser.DEFAULT_GROUPD_ID, NexusBrowser.DEFAULT_PACKAGING);
    }

    public final Set<String> listArtifactIds(final String groupId, final String packaging)  throws NexusClientException, NexusConnectionException  {
        final NexusArtifact template = new NexusArtifact();
        template.setGroupId(groupId);
        template.setPackaging(packaging);

        final Set<String> allArtifactIds = new HashSet<String>();
        final NexusClient client = createConnectedClient(this.nexusUrl);
        try {
            final List<NexusArtifact> artifacts = client.searchByGAV(template);
            for (final NexusArtifact artifact : artifacts) {
                allArtifactIds.add(artifact.getArtifactId());
            }
            return allArtifactIds;
        } finally {
            client.disconnect();
        }
    }

    public final List<List<NexusArtifact>> listArtifacts() throws NexusClientException, NexusConnectionException {
        return listArtifacts(NexusBrowser.DEFAULT_GROUPD_ID, NexusBrowser.DEFAULT_PACKAGING);
    }

    public final List<List<NexusArtifact>> listArtifacts(final String groupId, final String packaging) throws NexusClientException, NexusConnectionException  {
        final NexusArtifact template = new NexusArtifact();
        template.setGroupId(groupId);
        template.setPackaging(packaging);

        final List<List<NexusArtifact>> allGroups = new LinkedList<List<NexusArtifact>>();
        final NexusClient client = createConnectedClient(this.nexusUrl);
        try {
            final List<NexusArtifact> artifacts = client.searchByGAV(template);
            for (final NexusArtifact artifact : artifacts) {
                final List<NexusArtifact> group = getMatchingGroup(artifact, allGroups);
                group.add(artifact);
            }
            return allGroups;
        } finally {
            client.disconnect();
        }
    }

    public final Set<String> listArtifactVersions(final String artifactId) throws Exception {
        return listArtifactVersions(NexusBrowser.DEFAULT_GROUPD_ID, artifactId, NexusBrowser.DEFAULT_PACKAGING);
    }

    public final Set<String> listArtifactVersions(final String groupId, final String artifactId, final String packaging)  throws NexusClientException, NexusConnectionException  {
        final NexusArtifact template = new NexusArtifact();
        template.setGroupId(groupId);
        template.setArtifactId(artifactId);
        template.setPackaging(packaging);

        final Set<String> versions = new HashSet<String>();
        final NexusClient client = createConnectedClient(this.nexusUrl);
        try {
            final List<NexusArtifact> artifacts = client.searchByGAV(template);
            for (final NexusArtifact artifact : artifacts) {
                versions.add(artifact.getVersion());
            }
            return versions;
        } finally {
            client.disconnect();
        }
    }

}