/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */
package org.apache.wiki.providers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.providers.AttachmentProvider;
import org.apache.wiki.api.providers.WikiProvider;
import org.apache.wiki.api.search.QueryItem;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.cache.CacheInfo;
import org.apache.wiki.cache.CachingManager;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.TextUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;


/**
 * Provides a caching attachment provider.  This class rests on top of a real provider class and provides a cache to speed things up.
 * Only the Attachment objects are cached; the actual attachment contents are fetched always from the provider.
 *
 *  @since 2.1.64.
 */
public class CachingAttachmentProvider implements AttachmentProvider {

    private static final Logger LOG = LogManager.getLogger( CachingAttachmentProvider.class );

    private AttachmentProvider m_provider;
    private CachingManager cachingManager;
    private boolean m_gotall;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize( final Engine engine, final Properties properties ) throws NoRequiredPropertyException, IOException {
        LOG.info( "Initing CachingAttachmentProvider" );
        cachingManager = engine.getManager( CachingManager.class );

        // Find and initialize real provider.
        final String classname;
        try {
            classname = TextUtil.getRequiredProperty( properties, AttachmentManager.PROP_PROVIDER, AttachmentManager.PROP_PROVIDER_DEPRECATED );
        } catch( final NoSuchElementException e ) {
            throw new NoRequiredPropertyException( e.getMessage(), AttachmentManager.PROP_PROVIDER );
        }

        try {
            m_provider = ClassUtil.buildInstance( "org.apache.wiki.providers", classname );
            LOG.debug( "Initializing real provider class {}", m_provider );
            m_provider.initialize( engine, properties );
        } catch( final ReflectiveOperationException e ) {
            LOG.error( "Unable to instantiate provider class {}", classname, e );
            throw new IllegalArgumentException( "illegal provider class", e );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAttachmentData( final Attachment att, final InputStream data ) throws ProviderException, IOException {
        m_provider.putAttachmentData( att, data );
        cachingManager.remove( CachingManager.CACHE_ATTACHMENTS_COLLECTION, att.getParentName() );
        att.setLastModified( new Date() );
        cachingManager.put( CachingManager.CACHE_ATTACHMENTS, att.getName(), att );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getAttachmentData( final Attachment att ) throws ProviderException, IOException {
        return m_provider.getAttachmentData( att );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List< Attachment > listAttachments( final Page page ) throws ProviderException {
        LOG.debug( "Listing attachments for {}", page );
        final List< Attachment > atts = cachingManager.get( CachingManager.CACHE_ATTACHMENTS_COLLECTION, page.getName(),
                                                            () -> m_provider.listAttachments( page ) );
        return cloneCollection( atts );
    }

    private < T > List< T > cloneCollection( final Collection< T > c ) {
        return c != null ? new ArrayList<>( c ) : Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection< Attachment > findAttachments( final QueryItem[] query ) {
        return m_provider.findAttachments( query );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List< Attachment > listAllChanged( final Date timestamp ) throws ProviderException {
        final List< Attachment > all;
        if ( !m_gotall ) {
            all = m_provider.listAllChanged( timestamp );

            // Make sure that all attachments are in the cache.
            synchronized( this ) {
                for( final Attachment att : all ) {
                    cachingManager.put( CachingManager.CACHE_ATTACHMENTS, att.getName(), att );
                }
                m_gotall = true;
            }
        } else {
            final List< String > keys = cachingManager.keys( CachingManager.CACHE_ATTACHMENTS );
            all = new ArrayList<>();
            for( final String key : keys) {
                final Attachment cachedAttachment = cachingManager.get( CachingManager.CACHE_ATTACHMENTS, key, () -> null );
                if( cachedAttachment != null ) {
                    all.add( cachedAttachment );
                }
            }
        }

        if( cachingManager.enabled( CachingManager.CACHE_ATTACHMENTS )
                && all.size() >= cachingManager.info( CachingManager.CACHE_ATTACHMENTS ).getMaxElementsAllowed() ) {
            LOG.warn( "seems {} can't hold all attachments from your page repository, " +
                    "so we're delegating on the underlying provider instead. Please consider increasing " +
                    "your cache sizes on the ehcache configuration file to avoid this behaviour", CachingManager.CACHE_ATTACHMENTS );
            return m_provider.listAllChanged( timestamp );
        }

        return all;
    }

    /**
     *  Simply goes through the collection and attempts to locate the
     *  given attachment of that name.
     *
     *  @return null, if no such attachment was in this collection.
     */
    private Attachment findAttachmentFromCollection( final Collection< Attachment > c, final String name ) {
        if( c != null ) {
            for( final Attachment att : c ) {
                if( name.equals( att.getFileName() ) ) {
                    return att;
                }
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attachment getAttachmentInfo( final Page page, final String name, final int version ) throws ProviderException {
        LOG.debug( "Getting attachments for {}, name={}, version={}", page, name, version );
        //  We don't cache previous versions
        if( version != WikiProvider.LATEST_VERSION ) {
            LOG.debug( "...we don't cache old versions" );
            return m_provider.getAttachmentInfo( page, name, version );
        }
        final Collection< Attachment > c = cachingManager.get( CachingManager.CACHE_ATTACHMENTS_COLLECTION, page.getName(),
                                                               ()-> m_provider.listAttachments( page ) );
        return findAttachmentFromCollection( c, name );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List< Attachment > getVersionHistory( final Attachment att ) {
        return m_provider.getVersionHistory( att );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteVersion( final Attachment att ) throws ProviderException {
        // This isn't strictly speaking correct, but it does not really matter
        cachingManager.remove( CachingManager.CACHE_ATTACHMENTS_COLLECTION, att.getParentName() );
        m_provider.deleteVersion( att );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAttachment( final Attachment att ) throws ProviderException {
        cachingManager.remove( CachingManager.CACHE_ATTACHMENTS_COLLECTION, att.getParentName() );
        cachingManager.remove( CachingManager.CACHE_ATTACHMENTS, att.getName() );
        m_provider.deleteAttachment( att );
    }

    /**
     * Gets the provider class name, and cache statistics (misscount and hitcount of the attachment cache).
     *
     * @return A plain string with all the above-mentioned values.
     */
    @Override
    public synchronized String getProviderInfo() {
        final CacheInfo attCacheInfo = cachingManager.info( CachingManager.CACHE_ATTACHMENTS );
        final CacheInfo attColCacheInfo = cachingManager.info( CachingManager.CACHE_ATTACHMENTS_COLLECTION );
        return "Real provider: " + m_provider.getClass().getName() +
                ". Attachment cache hits: " + attCacheInfo.getHits() +
                ". Attachment cache misses: " + attCacheInfo.getMisses() +
                ". Attachment collection cache hits: " + attColCacheInfo.getHits() +
                ". Attachment collection cache misses: " + attColCacheInfo.getMisses();
    }

    /**
     *  Returns the WikiAttachmentProvider that this caching provider delegates to.
     *
     *  @return The real provider underneath this one.
     */
    public AttachmentProvider getRealProvider() {
        return m_provider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAttachmentsForPage( final String oldParent, final String newParent ) throws ProviderException {
        m_provider.moveAttachmentsForPage( oldParent, newParent );
        cachingManager.remove( CachingManager.CACHE_ATTACHMENTS_COLLECTION, newParent );
        cachingManager.remove( CachingManager.CACHE_ATTACHMENTS_COLLECTION, oldParent );

        // This is a kludge to make sure that the pages are removed from the other cache as well.
        final String checkName = oldParent + "/";
        final List< String > names = cachingManager.keys( CachingManager.CACHE_ATTACHMENTS_COLLECTION );
        for( final String name : names ) {
            if( name.startsWith( checkName ) ) {
                cachingManager.remove( CachingManager.CACHE_ATTACHMENTS, name );
            }
        }
    }

}