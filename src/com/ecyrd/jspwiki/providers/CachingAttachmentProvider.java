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
import java.io.InputStream;
import java.util.*;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.attachment.AttachmentManager;
import com.ecyrd.jspwiki.util.ClassUtil;
import com.opensymphony.oscache.base.Cache;
import com.opensymphony.oscache.base.NeedsRefreshException;

/**
 *  Provides a caching attachment provider.  This class rests on top of a
 *  real provider class and provides a cache to speed things up.  Only the
 *  Attachment objects are cached; the actual attachment contents are 
 *  fetched always from the provider.
 *
 *  @author Janne Jalkanen
 *  @since 2.1.64.
 */

// FIXME: Do we need to clear the cache entry if we get an NRE and the attachment is not there?
// FIXME: We probably clear the cache a bit too aggressively in places.
// FIXME: Does not yet react well to external cache changes.  Should really use custom
//        EntryRefreshPolicy for that.

public class CachingAttachmentProvider
    implements WikiAttachmentProvider
{
    private static final Logger log = Logger.getLogger(CachingAttachmentProvider.class);

    private WikiAttachmentProvider m_provider;

    /**
     *  The cache contains Collection objects which contain Attachment objects.
     *  The key is the parent wiki page name (String).
     */
    private Cache m_cache;

    private long m_cacheMisses = 0;
    private long m_cacheHits   = 0;

    public static final String DIR_EXTENSION   = "-att";
    public static final String PROP_STORAGEDIR = "jspwiki.basicAttachmentProvider.storageDir";
    

    // FIXME: Make settable.
    private int  m_refreshPeriod = 60*10; // 10 minutes at the moment

    public void initialize( WikiEngine engine, Properties properties )
        throws NoRequiredPropertyException,
               IOException
    {
        log.debug("Initing CachingAttachmentProvider");

        //
        //  Construct an unlimited cache.
        //
        m_cache = new Cache( true, false, true );

        //
        //  Find and initialize real provider.
        //
        String classname = WikiEngine.getRequiredProperty( properties, 
                                                           AttachmentManager.PROP_PROVIDER );
        
        try
        {            
            Class providerclass = ClassUtil.findClass( "com.ecyrd.jspwiki.providers",
                                                       classname );

            m_provider = (WikiAttachmentProvider)providerclass.newInstance();

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

    public void putAttachmentData( Attachment att, InputStream data )
        throws ProviderException,
               IOException
    {
        m_provider.putAttachmentData( att, data );

        m_cache.flushEntry( att.getParentName() );
    }

    public InputStream getAttachmentData( Attachment att )
        throws ProviderException,
               IOException
    {
        return m_provider.getAttachmentData( att );
    }

    public Collection listAttachments( WikiPage page )
        throws ProviderException
    {
        log.debug("Listing attachments for "+page);
        try
        {
            Collection c = (Collection)m_cache.getFromCache( page.getName(), m_refreshPeriod );

            if( c != null )
            {
                log.debug("LIST from cache, "+page.getName()+", size="+c.size());
                m_cacheHits++;
                return cloneCollection(c);
            }

            log.debug("list NOT in cache, "+page.getName());

            c = refresh( page );
        }
        catch( NeedsRefreshException nre )
        {
            try
            {
                Collection c = refresh( page );

                return cloneCollection(c);
            }
            catch( ProviderException ex )
            {
                log.warn("Provider failed, returning cached content");

                return (Collection)nre.getCacheContent();
            }
        }

        return new ArrayList();
    }

    private Collection cloneCollection( Collection c )
    {
        ArrayList list = new ArrayList();
        
        list.addAll( c );
        
        return list;
    }
    
    public Collection findAttachments( QueryItem[] query )
    {
        return m_provider.findAttachments( query );
    }

    public List listAllChanged( Date timestamp )
        throws ProviderException
    {
        // FIXME: Should cache
        return m_provider.listAllChanged( timestamp );
    }

    /**
     *  Simply goes through the collection and attempts to locate the
     *  given attachment of that name.
     *
     *  @return null, if no such attachment was in this collection.
     */
    private Attachment findAttachmentFromCollection( Collection c, String name )
    {
        for( Iterator i = c.iterator(); i.hasNext(); )
        {
            Attachment att = (Attachment) i.next();

            if( name.equals( att.getFileName() ) )
            {
                return att;
            }
        }

        return null;
    }

    /**
     *  Refreshes the cache content and updates counters.
     *
     *  @return The newly fetched object from the provider.
     */
    private final Collection refresh( WikiPage page )
        throws ProviderException
    {
        m_cacheMisses++;
        Collection c = m_provider.listAttachments( page );
        m_cache.putInCache( page.getName(), c );

        return c;
    }

    public Attachment getAttachmentInfo( WikiPage page, String name, int version )
        throws ProviderException
    {
        if( log.isDebugEnabled() )
        {
            log.debug("Getting attachments for "+page+", name="+name+", version="+version);
        }

        //
        //  We don't cache previous versions
        //
        if( version != WikiProvider.LATEST_VERSION )
        {       
            log.debug("...we don't cache old versions");
            return m_provider.getAttachmentInfo( page, name, version );
        }

        try
        {
            Collection c = (Collection)m_cache.getFromCache( page.getName(), m_refreshPeriod );
            
            if( c == null )
            {
                log.debug("...wasn't in the cache");
                c = refresh( page );

                if( c == null ) return null; // No such attachment
            }
            else
            {
                log.debug("...FOUND in the cache");
                m_cacheHits++;
            }

            return findAttachmentFromCollection( c, name );

        }
        catch( NeedsRefreshException nre )
        {
            log.debug("...needs refresh");
            Collection c = null;

            try
            {
                c = refresh( page );
            }
            catch( ProviderException ex )
            {
                log.warn("Provider failed, returning cached content");

                c = (Collection)nre.getCacheContent();
            }

            if( c != null )
            {
                return findAttachmentFromCollection( c, name );
            }
        }

        return null;
    }

            

    /**
     *  Returns version history.  Each element should be
     *  an Attachment.
     */
    public List getVersionHistory( Attachment att )
    {
        return m_provider.getVersionHistory( att );
    }

    public void deleteVersion( Attachment att )
        throws ProviderException
    {
        // This isn't strictly speaking correct, but it does not really matter
        m_cache.putInCache( att.getParentName(), null );
        m_provider.deleteVersion( att );
    }

    public void deleteAttachment( Attachment att )
        throws ProviderException
    {
        m_cache.putInCache( att.getParentName(), null );
        m_provider.deleteAttachment( att );
    }


    public synchronized String getProviderInfo()
    {              
        return("Real provider: "+m_provider.getClass().getName()+
               "<br />Cache misses: "+m_cacheMisses+
               "<br />Cache hits: "+m_cacheHits);
    }

    public WikiAttachmentProvider getRealProvider()
    {
        return m_provider;
    }
    

    public void moveAttachmentsForPage( String oldParent, String newParent )
        throws ProviderException
    {
        m_provider.moveAttachmentsForPage(oldParent, newParent);
        m_cache.putInCache( newParent, null );
    }
}
