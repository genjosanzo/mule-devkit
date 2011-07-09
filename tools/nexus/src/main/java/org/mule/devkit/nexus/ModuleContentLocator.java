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
package org.mule.devkit.nexus;

import org.apache.lucene.search.Query;
import org.apache.maven.index.AndMultiArtifactInfoFilter;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactInfoFilter;
import org.apache.maven.index.IteratorSearchRequest;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.SearchType;
import org.apache.maven.index.context.IndexingContext;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.StringContentLocator;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;

public class ModuleContentLocator
    implements ContentLocator
{
    private final String repositoryId;

    private final IndexingContext indexingContext;

    private final ArtifactInfoFilter artifactInfoFilter;

    private final NexusIndexer nexusIndexer;

    public ModuleContentLocator( String repositoryId, IndexingContext indexingContext, NexusIndexer nexusIndexer,
                                    ArtifactInfoFilter artifactInfoFilter )
    {
        this.repositoryId = repositoryId;
        this.indexingContext = indexingContext;
        this.nexusIndexer = nexusIndexer;
        this.artifactInfoFilter = artifactInfoFilter;
    }

    @Override
    public InputStream getContent()
        throws IOException
    {
        Query pq = nexusIndexer.constructQuery(MAVEN.PACKAGING, "mule-module", SearchType.EXACT);

        // to have sorted results by version in descending order
        IteratorSearchRequest sreq = new IteratorSearchRequest( pq, indexingContext );

        // filter that filters out classified artifacts
        ClassifierArtifactInfoFilter classifierFilter = new ClassifierArtifactInfoFilter();

        // combine it with others if needed (unused in cli, but perm filtering in server!)
        if ( artifactInfoFilter != null )
        {
            AndMultiArtifactInfoFilter andArtifactFilter =
                new AndMultiArtifactInfoFilter( Arrays.asList(new ArtifactInfoFilter[]{classifierFilter,
                        artifactInfoFilter}) );

            sreq.setArtifactInfoFilter( andArtifactFilter );
        }
        else
        {
            sreq.setArtifactInfoFilter( classifierFilter );
        }

        IteratorSearchResponse hits = nexusIndexer.searchIterator( sreq );

        ModuleCatalog catalog = new ModuleCatalog();
        Module module = null;

        // fill it in
        for ( ArtifactInfo info : hits )
        {
            module = new Module();
            module.setGroupId( info.groupId );
            module.setArtifactId( info.artifactId );
            module.setVersion( info.version );
            module.setDescription( info.description );

            if ( StringUtils.isNotEmpty( indexingContext.getRepositoryUrl() ) )
            {
                module.setRepository( indexingContext.getRepositoryUrl() );
            }

            catalog.addModule( module );
        }

        // serialize it to XML
        StringWriter sw = new StringWriter();

        return new StringContentLocator( sw.toString() ).getContent();
    }

    @Override
    public String getMimeType()
    {
        return "text/xml";
    }

    @Override
    public boolean isReusable()
    {
        return true;
    }

    public static class ClassifierArtifactInfoFilter
        implements ArtifactInfoFilter
    {
        public boolean accepts( IndexingContext ctx, ArtifactInfo ai )
        {
            return StringUtils.isBlank(ai.classifier);
        }
    }
}
