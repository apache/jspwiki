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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.log4j.Logger;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.api.search.QueryItem;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.api.spi.Wiki;
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
// FIXME: Synchronization is a bit inconsistent in places.
// FIXME: A part of the stuff is now redundant, since we could easily use the text cache for a lot of things.  RefactorMe.
public class CachingProvider implements PageProvider {

    private static final Logger log = Logger.getLogger( CachingProvider.class );

    private final CacheManager m_cacheManager = CacheManager.getInstance();

    private PageProvider m_provider;
    // FIXME: Find another way to the search engine to use instead of from Engine?
    private Engine m_engine;

    private Cache m_cache;
    /** Name of the regular page cache. */
    public static final String CACHE_NAME = "jspwiki.pageCache";

    private Cache            m_textCache;
    /** Name of the page text cache. */
    public static final String TEXTCACHE_NAME = "jspwiki.pageTextCache";

    private Cache            m_historyCache;
    /** Name of the page history cache. */
    public static final String HISTORYCACHE_NAME = "jspwiki.pageHistoryCache";

    private long             m_cacheMisses = 0;
    private long             m_cacheHits   = 0;

    private long             m_historyCacheMisses = 0;
    private long             m_historyCacheHits   = 0;

    // FIXME: This MUST be cached somehow.

    private boolean          m_gotall = false;

    // The default settings of the caches, if you want something else, provide an "ehcache.xml" file
    // Please note that JSPWiki ships with a default "ehcache.xml" in the classpath
    public static final int   DEFAULT_CACHECAPACITY   = 1000; // Good most wikis
    public static final int   DEFAULT_CACHETIMETOLIVESECONDS = 24*3600;
    public static final int   DEFAULT_CACHETIMETOIDLESECONDS = 24*3600;

    /**
     *  {@inheritDoc}
     */
    @Override
    public void initialize( final Engine engine, final Properties properties ) throws NoRequiredPropertyException, IOException {
        log.debug("Initing CachingProvider");

        // engine is used for getting the search engine
        m_engine = engine;

        final String cacheName = engine.getApplicationName() + "." + CACHE_NAME;
        if (m_cacheManager.cacheExists(cacheName)) {
            m_cache = m_cacheManager.getCache(cacheName);
        } else {
            log.info("cache with name " + cacheName +  " not found in ehcache.xml, creating it with defaults.");
            m_cache = new Cache(cacheName, DEFAULT_CACHECAPACITY, false, false, DEFAULT_CACHETIMETOLIVESECONDS, DEFAULT_CACHETIMETOIDLESECONDS);
            m_cacheManager.addCache(m_cache);
        }

        final String textCacheName = engine.getApplicationName() + "." + TEXTCACHE_NAME;
        if (m_cacheManager.cacheExists(textCacheName)) {
            m_textCache= m_cacheManager.getCache(textCacheName);
        } else {
            log.info("cache with name " + textCacheName +  " not found in ehcache.xml, creating it with defaults.");
            m_textCache = new Cache(textCacheName, DEFAULT_CACHECAPACITY, false, false, DEFAULT_CACHETIMETOLIVESECONDS, DEFAULT_CACHETIMETOIDLESECONDS);
            m_cacheManager.addCache(m_textCache);
        }

        final String historyCacheName = engine.getApplicationName() + "." + HISTORYCACHE_NAME;
        if (m_cacheManager.cacheExists(historyCacheName)) {
            m_historyCache= m_cacheManager.getCache(historyCacheName);
        } else {
            log.info("cache with name " + historyCacheName +  " not found in ehcache.xml, creating it with defaults.");
            m_historyCache = new Cache(historyCacheName, DEFAULT_CACHECAPACITY, false, false, DEFAULT_CACHETIMETOLIVESECONDS, DEFAULT_CACHETIMETOIDLESECONDS);
            m_cacheManager.addCache(m_historyCache);
        }

        //
        // m_cache.getCacheEventNotificationService().registerListener(new CacheItemCollector());

        //
        //  Find and initialize real provider.
        //
        final String classname;
        try {
            classname = TextUtil.getRequiredProperty( properties, PageManager.PROP_PAGEPROVIDER );
        } catch( final NoSuchElementException e ) {
            throw new NoRequiredPropertyException( e.getMessage(), PageManager.PROP_PAGEPROVIDER );
        }

        try {
            final Class< ? > providerclass = ClassUtil.findClass( "org.apache.wiki.providers", classname );
            m_provider = ( PageProvider )providerclass.newInstance();

            log.debug( "Initializing real provider class " + m_provider );
            m_provider.initialize( engine, properties );
        } catch( final ClassNotFoundException e ) {
            log.error( "Unable to locate provider class " + classname, e );
            throw new IllegalArgumentException( "no provider class", e );
        } catch( final InstantiationException e ) {
            log.error( "Unable to create provider class " + classname, e );
            throw new IllegalArgumentException( "faulty provider class", e );
        } catch( final IllegalAccessException e ) {
            log.error( "Illegal access to provider class " + classname, e );
            throw new IllegalArgumentException( "illegal provider class", e );
        }
    }

    private Page getPageInfoFromCache( final String name) throws ProviderException {
        // Sanity check; seems to occur sometimes
        if( name == null ) {
            return null;
        }

        final Element cacheElement = m_cache.get( name );
        if( cacheElement == null ) {
            final Page refreshed = m_provider.getPageInfo( name, PageProvider.LATEST_VERSION );
            if( refreshed != null ) {
                m_cache.put( new Element( name, refreshed ) );
                return refreshed;
            } else {
                // page does not exist anywhere
                return null;
            }
        }
        return ( Page )cacheElement.getObjectValue();
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
            log.info( "Provider failed while trying to check if page exists: " + pageName );
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
            log.info( "Provider failed while trying to check if page exists: " + pageName );
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
        if (pageName == null) {
            return null;
        }

        final String text;
        final Element cacheElement = m_textCache.get(pageName);
        if( cacheElement != null ) {
            m_cacheHits++;
            return ( String )cacheElement.getObjectValue();
        }
        if( pageExists( pageName ) ) {
            text = m_provider.getPageText( pageName, PageProvider.LATEST_VERSION );
            m_textCache.put( new Element( pageName, text ) );
            m_cacheMisses++;
            return text;
        }
        //page not found (not in cache, not by real provider)
        return  null;
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
            m_cache.remove( page.getName() );
            m_textCache.remove( page.getName() );
            m_historyCache.remove( page.getName() );

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
                    m_cache.put( new Element( p.getName(), p ) );
                }

                m_gotall = true;
            }
        } else {
            @SuppressWarnings("unchecked") final List< String > keys = m_cache.getKeysWithExpiryCheck();
            all = new TreeSet<>();
            for( final String key : keys ) {
                final Element element = m_cache.get( key );
                final Page cachedPage = ( Page )element.getObjectValue();
                if( cachedPage != null ) {
                    all.add( cachedPage );
                }
            }
        }

        if( all.size() >= m_cache.getCacheConfiguration().getMaxEntriesLocalHeap() ) {
        	log.warn( "seems " + m_cache.getName() + " can't hold all pages from your page repository, " +
        			  "so we're delegating on the underlying provider instead. Please consider increasing " +
        			  "your cache sizes on ehcache.xml to avoid this behaviour" );
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
                log.debug( "Failed to retrieve variables for wikipage " + page );
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
            if( cached == null ) {
                final Page data = m_provider.getPageInfo( pageName, version );
                if( data != null ) {
                    m_cache.put( new Element( pageName, data ) );
                }
                page = data;
            } else {
                page = cached;
            }
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
    @SuppressWarnings("unchecked")
    @Override
    public List< Page > getVersionHistory( final String pageName) throws ProviderException {
        final List< Page > history;
        if( pageName == null ) {
            return null;
        }
        final Element element = m_historyCache.get( pageName );
        if( element != null ) {
            m_historyCacheHits++;
            history = ( List< Page > )element.getObjectValue();
        } else {
            history = m_provider.getVersionHistory( pageName );
            m_historyCache.put( new Element( pageName, history ) );
            m_historyCacheMisses++;
        }

        return history;
    }

    /**
     * Gets the provider class name, and cache statistics (misscount and hitcount of page cache and history cache).
     *
     * @return A plain string with all the above mentioned values.
     */
    @Override
    public synchronized String getProviderInfo() {
        return "Real provider: " + m_provider.getClass().getName()+
                ". Cache misses: " + m_cacheMisses+
                ". Cache hits: " + m_cacheHits+
                ". History cache hits: " + m_historyCacheHits+
                ". History cache misses: " + m_historyCacheMisses;
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
                m_cache.remove( pageName );
                m_textCache.remove( pageName );
            }

            m_provider.deleteVersion( pageName, version );
            m_historyCache.remove( pageName );
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void deletePage( final String pageName ) throws ProviderException {
        //  See note in deleteVersion().
        synchronized( this ) {
            m_cache.put( new Element( pageName, null ) );
            m_textCache.put( new Element( pageName, null ) );
            m_historyCache.put( new Element( pageName, null ) );
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
            m_cache.remove( from );
            m_textCache.remove( from );
            m_historyCache.remove( from );
            log.debug( "Removing to page " + to + " from cache" );
            m_cache.remove( to );
            m_textCache.remove( to );
            m_historyCache.remove( to );
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
