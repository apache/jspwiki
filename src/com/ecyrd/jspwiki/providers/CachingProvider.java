/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.providers;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.PageManager;
import com.ecyrd.jspwiki.QueryItem;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.util.ClassUtil;
import com.opensymphony.module.oscache.base.Cache;
import com.opensymphony.module.oscache.base.NeedsRefreshException;
import com.opensymphony.module.oscache.base.events.CacheEntryEvent;
import com.opensymphony.module.oscache.base.events.CacheEntryEventListener;

/**
 *  Provides a caching page provider.  This class rests on top of a
 *  real provider class and provides a cache to speed things up.  Only
 *  if the cache copy of the page text has expired, we fetch it from
 *  the provider.
 *  <p>
 *  This class also detects if someone has modified the page
 *  externally, not through JSPWiki routines, and throws the proper
 *  RepositoryModifiedException.
 *  <p>
 *  Heavily based on ideas by Chris Brooking.
 *  <p>
 *  Since 2.1.52 uses the OSCache library from OpenSymphony.
 *  <p>
 *  Since 2.1.100 uses the Apache Lucene library to help in searching.
 *
 *  @author Janne Jalkanen
 *  @since 1.6.4
 *  @see RepositoryModifiedException
 */
// FIXME: Synchronization is a bit inconsistent in places.
// FIXME: A part of the stuff is now redundant, since we could easily use the text cache
//        for a lot of things.  RefactorMe.

public class CachingProvider
    implements WikiPageProvider
{
    private static final Logger log = Logger.getLogger(CachingProvider.class);

    private WikiPageProvider m_provider;
    // FIXME: Find another way to the search engine to use instead of from WikiEngine?
    private WikiEngine       m_engine;

    private Cache            m_cache;
    private Cache            m_negCache; // Cache for holding non-existing pages
    
    private Cache            m_textCache;
    private Cache            m_historyCache;

    private long             m_cacheMisses = 0;
    private long             m_cacheHits   = 0;

    private long             m_historyCacheMisses = 0;
    private long             m_historyCacheHits   = 0;

    private int              m_expiryPeriod = 30;
    
    /**
     *  This can be very long, as normally all modifications are noticed in an earlier
     *  stage.
     */
    private int              m_pageContentExpiryPeriod = 24*60*60;
    
    // FIXME: This MUST be cached somehow.

    private boolean          m_gotall = false;

    private CacheItemCollector m_allCollector = new CacheItemCollector();
    
    /**
     *  Defines, in seconds, the amount of time a text will live in the cache
     *  at most before requiring a refresh.
     */
    
    public static final String PROP_CACHECHECKINTERVAL = "jspwiki.cachingProvider.cacheCheckInterval";
    public static final String PROP_CACHECAPACITY      = "jspwiki.cachingProvider.capacity";

    private static final int   DEFAULT_CACHECAPACITY   = 1000; // Good most wikis

    private static final String OSCACHE_ALGORITHM      = "com.opensymphony.module.oscache.base.algorithm.LRUCache";

    
    public void initialize( WikiEngine engine, Properties properties )
        throws NoRequiredPropertyException,
               IOException
    {
        log.debug("Initing CachingProvider");

        //
        //  Cache consistency checks
        //
        m_expiryPeriod = TextUtil.getIntegerProperty( properties,
                                                      PROP_CACHECHECKINTERVAL,
                                                      m_expiryPeriod );

        log.debug("Cache expiry period is "+m_expiryPeriod+" s");

        //
        //  Text cache capacity
        //
        int capacity = TextUtil.getIntegerProperty( properties,
                                                    PROP_CACHECAPACITY,
                                                    DEFAULT_CACHECAPACITY );

        log.debug("Cache capacity "+capacity+" pages.");

        m_cache = new Cache( true, false );
        m_cache.addCacheEntryEventListener( m_allCollector );
        
        m_negCache = new Cache( true, false );
        
        m_textCache = new Cache( true, false,
                                 OSCACHE_ALGORITHM,
                                 capacity );

        m_historyCache = new Cache( true, false,
                                    OSCACHE_ALGORITHM,
                                    capacity );
                                    
        //
        //  Find and initialize real provider.
        //
        String classname = WikiEngine.getRequiredProperty( properties, 
                                                           PageManager.PROP_PAGEPROVIDER );
        // engine is used for getting the search engine
        m_engine = engine;

        try
        {            
            Class providerclass = ClassUtil.findClass( "com.ecyrd.jspwiki.providers",
                                                       classname );

            m_provider = (WikiPageProvider)providerclass.newInstance();

            log.debug("Initializing real provider class "+m_provider);
            m_provider.initialize( engine, properties );
        }
        catch( ClassNotFoundException e )
        {
            log.error("Unable to locate provider class "+classname,e);
            throw new IllegalArgumentException("no provider class");
        }
        catch( InstantiationException e )
        {
            log.error("Unable to create provider class "+classname,e);
            throw new IllegalArgumentException("faulty provider class");
        }
        catch( IllegalAccessException e )
        {
            log.error("Illegal access to provider class "+classname,e);
            throw new IllegalArgumentException("illegal provider class");
        }
    }




    private WikiPage getPageInfoFromCache( String name )
        throws ProviderException,
               RepositoryModifiedException
    {
        try
        {
            WikiPage item = (WikiPage)m_cache.getFromCache( name, m_expiryPeriod );
            
            if( item != null )
                return item;
            
            return null;
        }
        catch( NeedsRefreshException e )
        {
            WikiPage cached = (WikiPage)e.getCacheContent();
            
            // int version = (cached != null) ? cached.getVersion() : WikiPageProvider.LATEST_VERSION;
            
            WikiPage refreshed = m_provider.getPageInfo( name, WikiPageProvider.LATEST_VERSION );
  
            if( refreshed == null && cached != null )
            {
                //  Page has been removed evilly by a goon from outer space

                log.debug("Page "+name+" has been removed externally.");
                
                m_cache.putInCache( name, null );
                m_textCache.putInCache( name, null );
                m_historyCache.putInCache( name, null );
                // We cache a page miss
                m_negCache.putInCache( name, name );

                throw new RepositoryModifiedException( "Removed: "+name, name );
            }
            else if( cached == null )
            {
                // The page did not exist in the first place
                
                if( refreshed != null )
                {
                    // We must now add it
                    m_cache.putInCache( name, refreshed );
                    // Requests for this page are now no longer denied
                    m_negCache.putInCache( name, null );
                    
                    throw new RepositoryModifiedException( "Added: "+name, name );
                    // return refreshed;
                }
                else
                {
                    // Cache page miss
                    m_negCache.putInCache( name, name );
                }
            }
            else if( cached.getVersion() != refreshed.getVersion() )
            {
                //  The newest version has been deleted, but older versions still remain
                log.debug("Page "+cached.getName()+" newest version deleted, reloading...");
                
                m_cache.putInCache( name, refreshed );
                // Requests for this page are now no longer denied
                m_negCache.putInCache( name, null );

                m_textCache.flushEntry( name );
                m_historyCache.flushEntry( name );
                
                return refreshed;
            }
            else if( Math.abs(refreshed.getLastModified().getTime()-cached.getLastModified().getTime()) > 1000L )
            {
                //  Yes, the page has been modified externally and nobody told us
         
                log.info("Page "+cached.getName()+" changed, reloading...");

                m_cache.putInCache( name, refreshed );
                // Requests for this page are now no longer denied
                m_negCache.putInCache( name, null );
                m_textCache.flushEntry( name );
                m_historyCache.flushEntry( name );

                throw new RepositoryModifiedException( "Modified: "+name, name );
            }
            else
            {
                // Refresh the cache by putting the same object back
                m_cache.putInCache( name, cached );
                // Requests for this page are now no longer denied
                m_negCache.putInCache( name, null );
            }
            return cached;
        }
    }

    public boolean pageExists( String pageName )
    {
        //
        //  First, check the negative cache if we've seen it before
        //
        try
        {        
            String isNonExistant = (String) m_negCache.getFromCache( pageName, m_expiryPeriod );
            
            if( isNonExistant != null ) return false; // No such page
        }
        catch( NeedsRefreshException e )
        {
            // OSCache 2.1 locks the Entry which leads to a deadlock. We must unlock the entry
            // if there is no entry yet in there. If you want to use OSCache 2.1, uncomment the
            // following line.
            // m_negCache.cancelUpdate(pageName);

            // Let's just check if the page exists in the normal way
        }

        WikiPage p = null;
        
        try
        {
            p = getPageInfoFromCache( pageName );
        }
        catch( RepositoryModifiedException e ) 
        {
            // The repository was modified, we need to check now if the page was removed or
            // added.
            // TODO: This information would be available in the exception, but we would
            //       need to subclass.
            
            try
            {
                p = getPageInfoFromCache( pageName );
            }
            catch( Exception ex ) { return false; } // This should not happen
        }
        catch( ProviderException e ) 
        {
            log.info("Provider failed while trying to check if page exists: "+pageName);
            return false;
        }
        
        //
        //  A null item means that the page either does not
        //  exist, or has not yet been cached; a non-null
        //  means that the page does exist.
        //
        if( p != null )
        {
            return true;
        }

        //
        //  If we have a list of all pages in memory, then any page
        //  not in the cache must be non-existent.
        //
        //  FIXME: There's a problem here; if someone modifies the
        //         repository by adding a page outside JSPWiki, 
        //         we won't notice it.

        if( m_gotall )
        {
            return false;
        }

        //
        //  We could add the page to the cache here as well,
        //  but in order to understand whether that is a
        //  good thing or not we would need to analyze
        //  the JSPWiki calling patterns extensively.  Presumably
        //  it would be a good thing if pageExists() is called
        //  many times before the first getPageText() is called,
        //  and the whole page is cached.
        //
        return m_provider.pageExists( pageName );
    }

    /**
     *  @throws RepositoryModifiedException If the page has been externally modified.
     */
    public String getPageText( String pageName, int version )
        throws ProviderException,
               RepositoryModifiedException
    {
        String result = null;

        if( version == WikiPageProvider.LATEST_VERSION )
        {
            result = getTextFromCache( pageName );
        }
        else
        {
            WikiPage p = getPageInfoFromCache( pageName );

            //
            //  Or is this the latest version fetched by version number?
            //
            if( p != null && p.getVersion() == version )
            {
                result = getTextFromCache( pageName );
            }
            else
            {
                result = m_provider.getPageText( pageName, version );
            }
        }

        return result;
    }


    /**
     *  @throws RepositoryModifiedException If the page has been externally modified.
     */
    private String getTextFromCache( String pageName )
        throws ProviderException,
               RepositoryModifiedException
    {
        String text;

        WikiPage page = getPageInfoFromCache( pageName );

        try
        {
            text = (String)m_textCache.getFromCache( pageName,
                                                     m_pageContentExpiryPeriod );
            
            if( text == null )
            {
                if( page != null )
                {
                    text = m_provider.getPageText( pageName, WikiPageProvider.LATEST_VERSION );
                
                    m_textCache.putInCache( pageName, text );

                    m_engine.getSearchManager().addToQueue( page );

                    m_cacheMisses++;
                }
                else
                {
                    return null;
                }
            }
            else
            {
                m_cacheHits++;
            }
        }
        catch( NeedsRefreshException e )
        {            
            if( pageExists(pageName) )
            {
                text = m_provider.getPageText( pageName, WikiPageProvider.LATEST_VERSION );
                    
                m_textCache.putInCache( pageName, text );

                m_engine.getSearchManager().addToQueue( page );

                m_cacheMisses++;
            }
            else
            {
                m_textCache.putInCache( pageName, null );
                return null; // No page exists
            }
        }
        
        return text;
    }

    public void putPageText( WikiPage page, String text )
        throws ProviderException
    {
        synchronized(this)
        {
            m_provider.putPageText( page, text );

            page.setLastModified( new Date() );
            
            // Refresh caches properly
            
            m_cache.flushEntry( page.getName() );
            m_textCache.flushEntry( page.getName() );
            m_historyCache.flushEntry( page.getName() );
            m_negCache.flushEntry( page.getName() );
            
            // Refresh caches
            try
            {
                getPageInfoFromCache( page.getName() );
            }
            catch(RepositoryModifiedException e) {} // Expected
            
            m_engine.getSearchManager().addToQueue( page );
        }
    }


    public Collection getAllPages()
        throws ProviderException
    {
        Collection all;

        if( m_gotall == false )
        {
            all = m_provider.getAllPages();

            // Make sure that all pages are in the cache.

            synchronized(this)
            {
                for( Iterator i = all.iterator(); i.hasNext(); )
                {
                    WikiPage p = (WikiPage) i.next();
                    
                    m_cache.putInCache( p.getName(), p );
                    // Requests for this page are now no longer denied
                    m_negCache.putInCache( p.getName(), null );
                }

                m_gotall = true;
            }
        }
        else
        {
            all = m_allCollector.getAllItems();
        }

        return all;
    }

    public Collection getAllChangedSince( Date date )
    {
        return m_provider.getAllChangedSince( date );
    }

    public int getPageCount()
        throws ProviderException
    {
        return m_provider.getPageCount();
    }

    public Collection findPages( QueryItem[] query )
    {
        //
        //  If the provider is a fast searcher, then
        //  just pass this request through.
        //
        return m_provider.findPages( query );
        
        // FIXME: Does not implement fast searching
    }


    public WikiPage getPageInfo( String pageName, int version )
        throws ProviderException,
               RepositoryModifiedException
    {
        WikiPage cached = getPageInfoFromCache( pageName );
        
        int latestcached = (cached != null) ? cached.getVersion() : Integer.MIN_VALUE;
       
        if( version == WikiPageProvider.LATEST_VERSION ||
            version == latestcached )
        {
            if( cached == null )
            {
                WikiPage data = m_provider.getPageInfo( pageName, version );

                if( data != null )
                {
                    m_cache.putInCache( pageName, data );
                    // Requests for this page are now no longer denied
                    m_negCache.putInCache( pageName, null );
                }
                return data;
            }

            return cached;
        }        
        else
        {
            // We do not cache old versions.
            return m_provider.getPageInfo( pageName, version );
        }
    }

    public List getVersionHistory( String page )
        throws ProviderException
    {
        List history = null;

        try
        {
            history = (List)m_historyCache.getFromCache( page,
                                                         m_expiryPeriod );

            log.debug("History cache hit for page "+page);
            m_historyCacheHits++;
        }
        catch( NeedsRefreshException e )
        {
            history = m_provider.getVersionHistory( page );

            m_historyCache.putInCache( page, history );

            log.debug("History cache miss for page "+page);
            m_historyCacheMisses++;
        }

        return history;
    }

    public synchronized String getProviderInfo()
    {              
        return("Real provider: "+m_provider.getClass().getName()+
               "<br />Cache misses: "+m_cacheMisses+
               "<br />Cache hits: "+m_cacheHits+
               "<br />History cache hits: "+m_historyCacheHits+
               "<br />History cache misses: "+m_historyCacheMisses+
               "<br />Cache consistency checks: "+m_expiryPeriod+"s");
    }

    public void deleteVersion( String pageName, int version )
        throws ProviderException
    {
        //
        //  Luckily, this is such a rare operation it is okay
        //  to synchronize against the whole thing.
        //
        synchronized( this )
        {
            WikiPage cached = getPageInfoFromCache( pageName );

            int latestcached = (cached != null) ? cached.getVersion() : Integer.MIN_VALUE;
        
            //
            //  If we have this version cached, remove from cache.
            //
            if( version == WikiPageProvider.LATEST_VERSION ||
                version == latestcached )
            {
                m_cache.flushEntry( pageName );
                m_textCache.putInCache( pageName, null );
                m_historyCache.putInCache( pageName, null );
            }

            m_provider.deleteVersion( pageName, version );
        }
    }

    public void deletePage( String pageName )
        throws ProviderException
    {
        //
        //  See note in deleteVersion().
        //
        synchronized(this)
        {
            m_cache.putInCache( pageName, null );
            m_textCache.putInCache( pageName, null );
            m_historyCache.putInCache( pageName, null );
            m_negCache.putInCache( pageName, pageName );
            m_provider.deletePage( pageName );
        }
    }

    /**
     *  Returns the actual used provider.
     *  @since 2.0
     */
    public WikiPageProvider getRealProvider()
    {
        return m_provider;
    }

    /**
     *  This is a simple class that keeps a list of all WikiPages that
     *  we have in memory.  Because the OSCache cannot give us a list
     *  of all pages currently in cache, we'll have to check this.
     * 
     *  @author jalkanen
     *
     *  @since
     */
    private class CacheItemCollector
        implements CacheEntryEventListener
    {
        private TreeSet m_allItems = new TreeSet();
        
        public Set getAllItems()
        {
            return m_allItems;
        }
        
        public void cacheEntryAdded( CacheEntryEvent arg0 )
        {
        }

        public void cacheEntryFlushed( CacheEntryEvent arg0 )
        {
            WikiPage item = (WikiPage) arg0.getEntry().getContent();

            if( item != null )
            {
                m_allItems.remove( item );
            }
        }

        public void cacheEntryRemoved( CacheEntryEvent arg0 )
        {
            WikiPage item = (WikiPage) arg0.getEntry().getContent();

            if( item != null )
            {
                m_allItems.remove( item );
            }
        }

        public void cacheEntryUpdated( CacheEntryEvent arg0 )
        {
            WikiPage item = (WikiPage) arg0.getEntry().getContent();

            if( item != null )
            {
                // Item added or replaced.
                m_allItems.add( item );
            }
            else
            {
                // Removed item
                // FIXME: If the page system is changed during this time, we'll just fail gracefully
                
                try
                {
                    for( Iterator i = m_allItems.iterator(); i.hasNext(); )
                    {
                        WikiPage p = (WikiPage)i.next();
                    
                        if( p.getName().equals( arg0.getKey() ) )
                        {
                            i.remove();
                            break;
                        }
                    }
                }
                catch( Exception e )
                {}
            }
        }
    }
}
