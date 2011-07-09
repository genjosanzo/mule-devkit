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

import org.slf4j.Logger;
import org.sonatype.nexus.proxy.RepositoryNotAvailableException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.events.EventInspector;
import org.sonatype.nexus.proxy.events.RepositoryEventLocalStatusChanged;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.events.RepositoryRegistryRepositoryEvent;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.plexus.appevents.Event;

import javax.inject.Inject;
import javax.inject.Named;

public class ModuleCatalogEventInspector
    implements EventInspector
{
    private static final String ARCHETYPE_PATH = "/module-catalog.xml";

    @Inject
    private Logger logger;

    @Inject
    @Named( "maven2" )
    private ContentClass maven2ContentClass;

    public boolean accepts( Event<?> evt )
    {
        if ( evt instanceof RepositoryRegistryEventAdd )
        {
            RepositoryRegistryRepositoryEvent registryEvent = (RepositoryRegistryRepositoryEvent) evt;

            Repository repository = registryEvent.getRepository();

            return maven2ContentClass.isCompatible( repository.getRepositoryContentClass() )
                && ( repository.getRepositoryKind().isFacetAvailable( HostedRepository.class )
                    || repository.getRepositoryKind().isFacetAvailable( ProxyRepository.class ) || repository.getRepositoryKind().isFacetAvailable(
                    GroupRepository.class ) );
        }
        else if ( evt instanceof RepositoryEventLocalStatusChanged )
        {
            RepositoryEventLocalStatusChanged localStatusEvent = (RepositoryEventLocalStatusChanged) evt;

            // only if put into service
            return LocalStatus.IN_SERVICE.equals( localStatusEvent.getNewLocalStatus() );
        }
        else
        {
            return false;
        }
    }

    public void inspect( Event<?> evt )
    {
        Repository repository = null;

        if ( evt instanceof RepositoryRegistryEventAdd )
        {
            RepositoryRegistryRepositoryEvent registryEvent = (RepositoryRegistryRepositoryEvent) evt;

            repository = registryEvent.getRepository();
        }
        else if ( evt instanceof RepositoryEventLocalStatusChanged )
        {
            RepositoryEventLocalStatusChanged localStatusEvent = (RepositoryEventLocalStatusChanged) evt;

            repository = localStatusEvent.getRepository();
        }
        else
        {
            // huh?
            return;
        }

        // check is it a maven2 content, and either a "hosted", "proxy" or "group" repository
        if ( maven2ContentClass.isCompatible( repository.getRepositoryContentClass() )
            && ( repository.getRepositoryKind().isFacetAvailable( HostedRepository.class )
                || repository.getRepositoryKind().isFacetAvailable( ProxyRepository.class ) || repository.getRepositoryKind().isFacetAvailable(
                GroupRepository.class ) ) )
        {
            // new repo added or enabled, "install" the archetype catalog
            try
            {
                DefaultStorageFileItem file =
                    new DefaultStorageFileItem( repository, new ResourceStoreRequest( ARCHETYPE_PATH ), true, false,
                        new StringContentLocator( ModuleContentGenerator.ID ) );

                file.setContentGeneratorId( ModuleContentGenerator.ID );

                repository.storeItem( false, file );
            }
            catch ( RepositoryNotAvailableException e )
            {
                logger.info( "Unable to install the generated module catalog, repository \""
                    + e.getRepository().getId() + "\" is out of service." );
            }
            catch ( Exception e )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.info( "Unable to install the generated module catalog!", e );
                }
                else
                {
                    logger.info( "Unable to install the generated module catalog:" + e.getMessage() );
                }
            }
        }
    }
}