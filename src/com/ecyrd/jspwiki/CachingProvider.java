/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki;

import java.lang.ref.SoftReference;
import java.util.Properties;
import java.util.Collection;
import java.util.HashMap;
import java.util.Date;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Category;

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
        throws NoRequiredPropertyException
    {
        log.debug("Initing CachingProvider");

        String classname = WikiEngine.getRequiredProperty( properties, 
                                                               "jspwiki.cachingProvider.realProvider" );
        
        try
        {
            Class providerclass = Class.forName( classname );

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
        return m_provider.pageExists( page );
    }

    public String getPageText( String page, int version )
    {
        return m_provider.getPageText( page, version );
    }

    public String getPageText( String page )
    {
        CacheItem item = (CacheItem)m_cache.get( page );

        if( item == null )
        {
            // Page has never been seen.
            log.debug("Page "+page+" never seen.");
            String text = m_provider.getPageText( page );

            item = new CacheItem();
            item.m_page = m_provider.getPageInfo( page );
            item.m_text = new SoftReference( text );

            m_cache.put( page, item );

            m_cacheMisses++;

            return text;
        }
        else
        {
            String text = (String)item.m_text.get();

            if( text == null )
            {
                // Oops, expired already
                log.debug("Page "+page+" expired.");
                text = m_provider.getPageText( page );
                item.m_text = new SoftReference( text );

                m_cacheMisses++;

                return text;
            }

            log.debug("Page "+page+" found in cache.");

            m_cacheHits++;

            return text;
        }
    }

    public void putPageText( WikiPage page, String text )
    {
        m_provider.putPageText( page, text );

        // Invalidate cache after writing to it.
        // FIXME: possible race condition here.  Someone might still get
        // the old version.

        m_cache.remove( page.getName() );
    }

    // FIXME: This MUST be cached somehow.

    private boolean m_gotall = false;

    public Collection getAllPages()
    {
        Collection all;

        if( m_gotall == false )
        {
            all = m_provider.getAllPages();
            m_gotall = true;

            // FIXME: This has the unfortunate side effect of clearing
            // the cache.

            for( Iterator i = m_cache.keySet().iterator(); i.hasNext(); )
            {
                CacheItem item = new CacheItem();
                item.m_page = (WikiPage) i.next();
                item.m_text = new SoftReference( null );

                m_cache.put( item.m_page.getName(), item );
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

    public Collection getAllChangedSince( Date date )
    {
        return m_provider.getAllChangedSince( date );
    }

    public int getPageCount()
    {
        return m_provider.getPageCount();
    }

    public Collection findPages( QueryItem[] query )
    {
        return m_provider.findPages( query );
    }

    public WikiPage getPageInfo( String page )
    {        
        CacheItem item = (CacheItem)m_cache.get( page );

        if( item == null )
        {
            item = new CacheItem();
            item.m_page = m_provider.getPageInfo( page );
            item.m_text = new SoftReference( null );

            m_cache.put( page, item );
        }

        return item.m_page;
    }

    public Collection getVersionHistory( String page )
    {
        return m_provider.getVersionHistory( page );
    }

    public String getProviderInfo()
    {              
        return("Real provider: "+m_provider.getClass().getName()+
               "<BR>Cache misses: "+m_cacheMisses+
               "<BR>Cache hits: "+m_cacheHits);
    }

    private class CacheItem
    {
        WikiPage      m_page;
        SoftReference m_text;
    }
}
