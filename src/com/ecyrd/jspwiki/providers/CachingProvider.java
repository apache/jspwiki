/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import java.lang.ref.SoftReference;
import java.util.Properties;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.io.IOException;
import org.apache.log4j.Category;

import com.ecyrd.jspwiki.*;

/**
 *  Heavily based on ideas by Chris Brooking.
 *
 *  @author Janne Jalkanen
 *  @since 1.6.4
 */
// FIXME: Keeps a list of all WikiPages in memory - should cache them too.
public class CachingProvider
    implements WikiPageProvider
{
    private static final Category   log = Category.getInstance(CachingProvider.class);

    private WikiPageProvider m_provider;

    private HashMap          m_cache = new HashMap();

    private long m_cacheMisses = 0;
    private long m_cacheHits   = 0;

    public void initialize( Properties properties )
        throws NoRequiredPropertyException,
               IOException
    {
        log.debug("Initing CachingProvider");

        String classname = WikiEngine.getRequiredProperty( properties, 
                                                           PageManager.PROP_PAGEPROVIDER );
        
        try
        {            
            Class providerclass = WikiEngine.findWikiClass( classname, "com.ecyrd.jspwiki.providers" );

            m_provider = (WikiPageProvider)providerclass.newInstance();

            log.debug("Initializing real provider class "+m_provider);
            m_provider.initialize( properties );
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

    public boolean pageExists( String page )
    {
        CacheItem item = (CacheItem)m_cache.get( page );

        //
        //  A null item means that the page either does not
        //  exist, or has not yet been cached; a non-null
        //  means that the page does exist.
        //
        if( item != null )
        {
            return true;
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
        return m_provider.pageExists( page );
    }

    public String getPageText( String page, int version )
        throws ProviderException
    {
        String result;

        if( version == WikiPageProvider.LATEST_VERSION )
        {
            result = getTextFromCache( page );
        }
        else
        {
            CacheItem item = (CacheItem)m_cache.get( page );

            //
            //  Or is this the latest version fetched by version number?
            //
            if( item != null && item.m_page.getVersion() == version )
            {
                result = getTextFromCache( page );
            }
            else
            {
                result = m_provider.getPageText( page, version );
            }
        }

        return result;
    }

    private String getTextFromCache( String page )
        throws ProviderException
    {
        CacheItem item = (CacheItem)m_cache.get( page );

        if( item == null )
        {
            // Page has never been seen.
            // log.debug("Page "+page+" never seen.");
            String text = m_provider.getPageText( page, WikiPageProvider.LATEST_VERSION );

            addPage( page, text );

            m_cacheMisses++;

            return text;
        }
        else
        {
            String text = (String)item.m_text.get();

            if( text == null )
            {
                // Oops, expired already
                // log.debug("Page "+page+" expired.");
                text = m_provider.getPageText( page, WikiPageProvider.LATEST_VERSION );
                item.m_text = new SoftReference( text );

                m_cacheMisses++;

                return text;
            }

            // log.debug("Page "+page+" found in cache.");

            m_cacheHits++;

            return text;
        }
    }

    public void putPageText( WikiPage page, String text )
        throws ProviderException
    {
        m_provider.putPageText( page, text );

        // Invalidate cache after writing to it.
        // FIXME: possible race condition here.  Someone might still get
        // the old version.

        synchronized(this)
        {
            m_cache.remove( page.getName() );
            addPage( page.getName(), null ); // If fetch fails, we want info to go directly to user
        }
    }

    // FIXME: This MUST be cached somehow.

    private boolean m_gotall = false;

    public Collection getAllPages()
        throws ProviderException
    {
        Collection all;

        if( m_gotall == false )
        {
            all = m_provider.getAllPages();

            // Make sure that all pages are in the cache.

            // FIXME: This has the unfortunate side effect of clearing
            // the cache.

            synchronized(this)
            {
                for( Iterator i = all.iterator(); i.hasNext(); )
                {
                    CacheItem item = new CacheItem();
                    item.m_page = (WikiPage) i.next();
                    item.m_text = new SoftReference( null );

                    m_cache.put( item.m_page.getName(), item );
                }

                m_gotall = true;
            }
        }
        else
        {
            all = new ArrayList();
            for( Iterator i = m_cache.values().iterator(); i.hasNext(); )
            {
                all.add( ((CacheItem)i.next()).m_page );
            }
        }

        return all;
    }

    // Null text for no page
    // Returns null if no page could be found.
    private synchronized CacheItem addPage( String pageName, String text )
        throws ProviderException
    {
        CacheItem item = null;
        
        WikiPage newpage = m_provider.getPageInfo( pageName, WikiPageProvider.LATEST_VERSION );

        if( newpage != null )
        {
            item = new CacheItem();

            item.m_page = newpage;
            item.m_text = new SoftReference( text );

            m_cache.put( pageName, item );
        }

        return item;
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
        TreeSet res = new TreeSet( new SearchResultComparator() );
        SearchMatcher matcher = new SearchMatcher( query );

        Collection allPages = null;
        try
        {
            allPages = getAllPages();
        }
        catch( ProviderException pe )
        {
            log.error( "Unable to retrieve page list", pe );
            return( null );
        }

        Iterator it = allPages.iterator();
        while( it.hasNext() )
        {
            try
            {
                WikiPage page = (WikiPage) it.next();
                String pageName = page.getName();
                String pageContent = getTextFromCache( pageName );
                SearchResult comparison = matcher.matchPageContent( pageName, pageContent );
                if( comparison != null )
                {
                    res.add( comparison );
                }
            }
            catch( ProviderException pe )
            {
                log.error( "Unable to retrieve page from cache", pe );
            }
            catch( IOException ioe )
            {
                log.error( "Failed to search page", ioe );
            }
        }
    
        return( res );
    }

    public WikiPage getPageInfo( String page, int version )
        throws ProviderException
    { 
        CacheItem item = (CacheItem)m_cache.get( page );

        int latestcached = (item != null) ? item.m_page.getVersion() : Integer.MIN_VALUE;
       
        if( version == WikiPageProvider.LATEST_VERSION ||
            version == latestcached )
        {
            if( item == null )
            {
                item = addPage( page, null );

                if( item == null )
                {
                    return null;
                }
            }

            return item.m_page;
        }        
        else
        {
            // We do not cache old versions.
            return m_provider.getPageInfo( page, version );
        }
    }

    public List getVersionHistory( String page )
        throws ProviderException
    {
        return m_provider.getVersionHistory( page );
    }

    public synchronized String getProviderInfo()
    {              
        int cachedPages = 0;
        
        for( Iterator i = m_cache.values().iterator(); i.hasNext(); )
        {
            CacheItem item = (CacheItem) i.next();

            if( item.m_text.get() != null )
            {
                cachedPages++;
            }
        }

        return("Real provider: "+m_provider.getClass().getName()+
               "<br />Cache misses: "+m_cacheMisses+
               "<br />Cache hits: "+m_cacheHits+
               "<br />Cached pages: "+cachedPages);
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
            CacheItem item = (CacheItem)m_cache.get( pageName );

            int latestcached = (item != null) ? item.m_page.getVersion() : Integer.MIN_VALUE;
        
            //
            //  If we have this version cached, remove from cache.
            //
            if( version == WikiPageProvider.LATEST_VERSION ||
                version == latestcached )
            {
                m_cache.remove( pageName );
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
            m_cache.remove( pageName );

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

    private class CacheItem
    {
        WikiPage      m_page;
        SoftReference m_text;
    }
}
