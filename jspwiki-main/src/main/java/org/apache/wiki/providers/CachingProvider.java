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
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.api.search.QueryItem;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.cache.CacheInfo;
import org.apache.wiki.cache.CachingManager;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.render.RenderingManager;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.TextUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.TreeSet;


/**
 *  Provides a caching page provider.  This class rests on top of a real provider class and provides a cache to speed things up.  Only
 *  if the cache copy of the page text has expired, we fetch it from the provider.
 *  <p>
 *  This class does not detect if someone has modified the page externally, not through JSPWiki routines.
 *  <p>
 *  Heavily based on ideas by Chris Brooking.
 *  <p>
 *  Since 2.10 uses the Ehcache library.
 *
 *  @since 1.6.4
 */
public class CachingProvider implements PageProvider {

    private static final Logger LOG = LogManager.getLogger( CachingProvider.class );

    private CachingManager cachingManager;
    private PageProvider m_provider;
    private Engine m_engine;

    // FIXME: This MUST be cached somehow.
    private boolean m_gotall;

    /**
     *  {@inheritDoc}
     */
    @Override
    public void initialize( final Engine engine, final Properties properties ) throws NoRequiredPropertyException, IOException {
        LOG.debug( "Initing CachingProvider" );

        // engine is used for getting the search engine
        m_engine = engine;
        cachingManager = m_engine.getManager( CachingManager.class );

        //  Find and initialize real provider.
        final String classname;
        try {
            classname = TextUtil.getRequiredProperty( properties, PageManager.PROP_PAGEPROVIDER );
        } catch( final NoSuchElementException e ) {
            throw new NoRequiredPropertyException( e.getMessage(), PageManager.PROP_PAGEPROVIDER );
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

    private Page getPageInfoFromCache( final String name ) throws ProviderException {
        // Sanity check; seems to occur sometimes
        if( name == null ) {
            return null;
        }
        return cachingManager.get( CachingManager.CACHE_PAGES, name, () -> m_provider.getPageInfo( name, PageProvider.LATEST_VERSION ) );
    }


    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean pageExists( final String pageName, final int version ) {
        if( pageName == null ) {
            return false;
        }

        final Page p;
        try {
            p = getPageInfoFromCache( pageName );
        } catch( final ProviderException e ) {
            LOG.info( "Provider failed while trying to check if page exists: {}", pageName );
            return false;
        }

        if( p != null ) {
            final int latestVersion = p.getVersion();
            if( version == latestVersion || version == LATEST_VERSION ) {
                return true;
            }

            return m_provider.pageExists( pageName, version );
        }

        try {
            return getPageInfo( pageName, version ) != null;
        } catch( final ProviderException e ) {
        }

        return false;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean pageExists( final String pageName ) {
        if( pageName == null ) {
            return false;
        }

        final Page p;
        try {
            p = getPageInfoFromCache( pageName );
        } catch( final ProviderException e ) {
            LOG.info( "Provider failed while trying to check if page exists: {}", pageName );
            return false;
        }

        //  A null item means that the page either does not exist, or has not yet been cached; a non-null means that the page does exist.
        if( p != null ) {
            return true;
        }

        //  If we have a list of all pages in memory, then any page not in the cache must be non-existent.
        if( m_gotall ) {
            return false;
        }

        //  We could add the page to the cache here as well, but in order to understand whether that is a good thing or not we would
        //  need to analyze the JSPWiki calling patterns extensively.  Presumably it would be a good thing if pageExists() is called
        //  many times before the first getPageText() is called, and the whole page is cached.
        return m_provider.pageExists( pageName );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String getPageText( final String pageName, final int version ) throws ProviderException {
        if( pageName == null ) {
            return null;
        }

        final String result;
        if( version == PageProvider.LATEST_VERSION ) {
            result = getTextFromCache( pageName );
        } else {
            final Page p = getPageInfoFromCache( pageName );

            //  Or is this the latest version fetched by version number?
            if( p != null && p.getVersion() == version ) {
                result = getTextFromCache( pageName );
            } else {
                result = m_provider.getPageText( pageName, version );
            }
        }

        return result;
    }

    private String getTextFromCache( final String pageName ) throws ProviderException {
        if( pageName == null ) {
            return null;
        }

        return cachingManager.get( CachingManager.CACHE_PAGES_TEXT, pageName, () -> {
            if( pageExists( pageName ) ) {
                return m_provider.getPageText( pageName, PageProvider.LATEST_VERSION );
            }
            return null;
        } );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void putPageText( final Page page, final String text ) throws ProviderException {
        synchronized( this ) {
            m_provider.putPageText( page, text );
            page.setLastModified( new Date() );

            // Refresh caches properly
            cachingManager.remove( CachingManager.CACHE_PAGES, page.getName() );
            cachingManager.remove( CachingManager.CACHE_PAGES_TEXT, page.getName() );
            cachingManager.remove( CachingManager.CACHE_PAGES_HISTORY, page.getName() );

            getPageInfoFromCache( page.getName() );
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Collection< Page > getAllPages() throws ProviderException {
        final Collection< Page > all;
        if ( !m_gotall ) {
            all = m_provider.getAllPages();
            // Make sure that all pages are in the cache.
            synchronized( this ) {
                for( final Page p : all ) {
                    cachingManager.put( CachingManager.CACHE_PAGES,  p.getName(), p );
                }
                m_gotall = true;
            }
        } else {
            final List< String > keys = cachingManager.keys( CachingManager.CACHE_PAGES );
            all = new TreeSet<>();
            for( final String key : keys ) {
                final Page cachedPage = cachingManager.get( CachingManager.CACHE_PAGES, key, () -> null );
                if( cachedPage != null ) {
                    all.add( cachedPage );
                }
            }
        }

        if( cachingManager.enabled( CachingManager.CACHE_PAGES )
                && all.size() >= cachingManager.info( CachingManager.CACHE_PAGES ).getMaxElementsAllowed() ) {
            LOG.warn( "seems {} can't hold all pages from your page repository, " +
                    "so we're delegating on the underlying provider instead. Please consider increasing " +
                    "your cache sizes on the ehcache configuration file to avoid this behaviour", CachingManager.CACHE_PAGES );
            return m_provider.getAllPages();
        }

        return all;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Collection< Page > getAllChangedSince( final Date date ) {
        return m_provider.getAllChangedSince( date );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public int getPageCount() throws ProviderException {
        return m_provider.getPageCount();
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Collection< SearchResult > findPages( final QueryItem[] query ) {
        //  If the provider is a fast searcher, then just pass this request through.
        return m_provider.findPages( query );
        // FIXME: Does not implement fast searching
    }

    //  FIXME: Kludge: make sure that the page is also parsed and it gets all the necessary variables.
    private void refreshMetadata( final Page page ) {
        if( page != null && !page.hasMetadata() ) {
            final RenderingManager mgr = m_engine.getManager( RenderingManager.class );
            try {
                final String data = m_provider.getPageText( page.getName(), page.getVersion() );
                final Context ctx = Wiki.context().create( m_engine, page );
                final MarkupParser parser = mgr.getParser( ctx, data );

                parser.parse();
            } catch( final Exception ex ) {
                LOG.debug( "Failed to retrieve variables for wikipage {}", page );
            }
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Page getPageInfo( final String pageName, final int version ) throws ProviderException {
        final Page page;
        final Page cached = getPageInfoFromCache( pageName );
        final int latestcached = ( cached != null ) ? cached.getVersion() : Integer.MIN_VALUE;
        if( version == PageProvider.LATEST_VERSION || version == latestcached ) {
            page = cached;
        } else {
            // We do not cache old versions.
            page = m_provider.getPageInfo( pageName, version );
        }
        refreshMetadata( page );
        return page;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public List< Page > getVersionHistory( final String pageName) throws ProviderException {
        if( pageName == null ) {
            return null;
        }
        return cachingManager.get( CachingManager.CACHE_PAGES_HISTORY, pageName, () -> m_provider.getVersionHistory( pageName ) );
    }

    /**
     * Gets the provider class name, and cache statistics (misscount and hitcount of page cache and history cache).
     *
     * @return A plain string with all the above-mentioned values.
     */
    @Override
    public synchronized String getProviderInfo() {
        final CacheInfo pageCacheInfo = cachingManager.info( CachingManager.CACHE_PAGES );
        final CacheInfo pageHistoryCacheInfo = cachingManager.info( CachingManager.CACHE_PAGES_HISTORY );
        return "Real provider: " + m_provider.getClass().getName()+
                ". Page cache hits: " + pageCacheInfo.getHits() +
                ". Page cache misses: " + pageCacheInfo.getMisses() +
                ". History cache hits: " + pageHistoryCacheInfo.getHits() +
                ". History cache misses: " + pageHistoryCacheInfo.getMisses();
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void deleteVersion( final String pageName, final int version ) throws ProviderException {
        //  Luckily, this is such a rare operation it is okay to synchronize against the whole thing.
        synchronized( this ) {
            final Page cached = getPageInfoFromCache( pageName );
            final int latestcached = ( cached != null ) ? cached.getVersion() : Integer.MIN_VALUE;

            //  If we have this version cached, remove from cache.
            if( version == PageProvider.LATEST_VERSION || version == latestcached ) {
                cachingManager.remove( CachingManager.CACHE_PAGES, pageName );
                cachingManager.remove( CachingManager.CACHE_PAGES_TEXT, pageName );
            }

            m_provider.deleteVersion( pageName, version );
            cachingManager.remove( CachingManager.CACHE_PAGES_HISTORY, pageName );
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void deletePage( final String pageName ) throws ProviderException {
        //  See note in deleteVersion().
        synchronized( this ) {
            cachingManager.put( CachingManager.CACHE_PAGES, pageName, null );
            cachingManager.put( CachingManager.CACHE_PAGES_TEXT, pageName, null );
            cachingManager.put( CachingManager.CACHE_PAGES_HISTORY, pageName, null );
            m_provider.deletePage( pageName );
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void movePage( final String from, final String to ) throws ProviderException {
        m_provider.movePage( from, to );
        synchronized( this ) {
            // Clear any cached version of the old page and new page
            cachingManager.remove( CachingManager.CACHE_PAGES, from );
            cachingManager.remove( CachingManager.CACHE_PAGES_TEXT, from );
            cachingManager.remove( CachingManager.CACHE_PAGES_HISTORY, from );
            LOG.debug( "Removing to page {} from cache", to );
            cachingManager.remove( CachingManager.CACHE_PAGES, to );
            cachingManager.remove( CachingManager.CACHE_PAGES_TEXT, to );
            cachingManager.remove( CachingManager.CACHE_PAGES_HISTORY, to );
        }
    }

    /**
     *  Returns the actual used provider.
     *
     *  @since 2.0
     *  @return The real provider.
     */
    public PageProvider getRealProvider() {
        return m_provider;
    }

}
