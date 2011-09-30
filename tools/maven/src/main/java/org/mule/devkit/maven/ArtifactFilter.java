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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArtifactFilter {
    private static final Set<String> MULE_GROUP_IDS;

    private Set<Artifact> projectArtifacts;
    private List<Exclusion> excludes;
    private List<Inclusion> includes;
    private boolean excludeMuleArtifacts;

    static {
        MULE_GROUP_IDS = new HashSet<String>();
        MULE_GROUP_IDS.add("org.mule");
        MULE_GROUP_IDS.add("com.mulesource.muleesb");
        MULE_GROUP_IDS.add("com.mulesoft.muleesb");
    }

    @SuppressWarnings("unchecked")
    public ArtifactFilter(MavenProject project, List<Inclusion> inclusions, List<Exclusion> exclusions, boolean excludeMuleDependencies) {
        super();
        projectArtifacts = Collections.unmodifiableSet(project.getArtifacts());
        includes = inclusions;
        excludes = exclusions;
        excludeMuleArtifacts = excludeMuleDependencies;
    }

    public Set<Artifact> getArtifactsToArchive() {
        Set<Artifact> filteredArtifacts = keepOnlyArtifactsWithCompileOrRuntimeScope();
        if (excludeMuleArtifacts) {
            filteredArtifacts = keepOnlyArtifactsWithoutMuleGroupId(filteredArtifacts);
        }
        filteredArtifacts = applyAllExcludes(filteredArtifacts);
        filteredArtifacts = applyAllIncludes(filteredArtifacts);
        return filteredArtifacts;
    }

    private Set<Artifact> keepOnlyArtifactsWithCompileOrRuntimeScope() {
        Set<Artifact> filteredArtifacts = new HashSet<Artifact>();

        for (Artifact artifact : projectArtifacts) {
            String scope = artifact.getScope();
            if (Artifact.SCOPE_COMPILE.equals(scope) || Artifact.SCOPE_RUNTIME.equals(scope)) {
                filteredArtifacts.add(artifact);
            }
        }

        return filteredArtifacts;
    }

    private Set<Artifact> keepOnlyArtifactsWithoutMuleGroupId(Set<Artifact> artifacts) {
        Set<Artifact> filteredArtifacts = new HashSet<Artifact>();

        for (Artifact artifact : artifacts) {
            if (artifact.getArtifactId().equals("mule-devkit-annotations")) {
                filteredArtifacts.add(artifact);
            } else {
                if (isDependencyWithMuleGroupId(artifact) == false) {
                    filteredArtifacts.add(artifact);
                }
            }
        }

        return filteredArtifacts;
    }

    private boolean isDependencyWithMuleGroupId(Artifact artifact) {
        List<String> dependencyTrail = getDependencyTrailWithoutProjectArtifact(artifact);
        for (String trailElement : dependencyTrail) {
            for (String groupId : MULE_GROUP_IDS) {
                if (trailElement.startsWith(groupId)) {
                    return true;
                }
            }

        }
        return false;
    }

    /**
     * The first element on dependency tail is the project that is compiled. The project's groupId
     * can be anything, we don't filter it.
     */
    @SuppressWarnings("all")
    private List<String> getDependencyTrailWithoutProjectArtifact(Artifact artifact) {
        @SuppressWarnings("unchecked")
        List<String> dependencyTrail = new ArrayList<String>(artifact.getDependencyTrail());
        dependencyTrail.remove(0);
        return dependencyTrail;
    }

    private Set<Artifact> applyAllExcludes(Set<Artifact> artifacts) {
        if (excludes != null) {
            for (Exclusion exclude : excludes) {
                artifacts = applyExclude(exclude, artifacts);
            }
        }

        return artifacts;
    }

    private Set<Artifact> applyExclude(Exclusion exclude, Set<Artifact> artifacts) {
        String filter = exclude.asFilter();
        Set<Artifact> filteredArtifacts = new HashSet<Artifact>();

        for (Artifact artifact : artifacts) {
            if (dependencyTrailContains(artifact, filter) == false) {
                filteredArtifacts.add(artifact);
            }
        }

        return filteredArtifacts;
    }

    private boolean dependencyTrailContains(Artifact artifact, String filter) {
        List<?> dependencyTrail = artifact.getDependencyTrail();
        for (Object trailElement : dependencyTrail) {
            if (trailElement.toString().startsWith(filter)) {
                return true;
            }
        }

        return false;
    }

    private Set<Artifact> applyAllIncludes(Set<Artifact> filteredArtifacts) {
        if (includes != null) {
            for (Inclusion inc : includes) {
                applyInclude(inc, filteredArtifacts);
            }
        }

        return filteredArtifacts;
    }

    private void applyInclude(Inclusion inclusion, Set<Artifact> filteredArtifacts) {
        // append a ':' to the filter. This will result in "gid:aid:" which can be safely
        // matched against the toString representation of an artifact without accidentally
        // matching an artifact that has a "longer" groupId
        String filter = inclusion.asFilter() + ":";

        for (Artifact artifact : projectArtifacts) {
            if (dependencyTrailContains(artifact, filter) && (artifact.isOptional() == false)) {
                filteredArtifacts.add(artifact);
            }
        }
    }
}
