/* 
    JSPWiki - a JSP-based WikiWiki clone.

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
import com.opensymphony.oscache.base.events.*;

/**
 *  Provides a caching attachment provider.  This class rests on top of a
 *  real provider class and provides a cache to speed things up.  Only the
 *  Attachment objects are cached; the actual attachment contents are 
 *  fetched always from the provider.
 *
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
    private long m_cacheHits = 0;

    /**
     * This cache contains Attachment objects and is keyed by attachment name.
     * This provides for quickly giving recently changed attachments (for the RecentChanges plugin)
     */
    private Cache m_attCache;

    /** The extension to append to directory names to denote an attachment directory. */
    public static final String DIR_EXTENSION   = "-att";

    /** Property that supplies the directory used to store attachments. */
    public static final String PROP_STORAGEDIR = "jspwiki.basicAttachmentProvider.storageDir";

    // FIXME: Make settable.
    private int  m_refreshPeriod = 60*10; // 10 minutes at the moment
    
    private boolean m_gotall = false;
    private CachedAttachmentCollector m_allCollector = new CachedAttachmentCollector();

    /**
     * {@inheritDoc}
     */
    public void initialize( WikiEngine engine, Properties properties )
        throws NoRequiredPropertyException,
               IOException
    {
        log.info("Initing CachingAttachmentProvider");

        //
        // Construct an unlimited cache of Collection objects
        //
        m_cache = new Cache( true, false, true );

        //
        // Construct an unlimited cache for the individual Attachment objects. 
        // Attachment name is key, the Attachment object is the cached object
        //
        m_attCache = new Cache(true, false, true);
        m_attCache.addCacheEventListener(m_allCollector,CacheEntryEventListener.class);

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

    /**
     * {@inheritDoc}
     */
    public void putAttachmentData( Attachment att, InputStream data )
        throws ProviderException,
               IOException
    {
        m_provider.putAttachmentData( att, data );

        m_cache.flushEntry( att.getParentName() );
        att.setLastModified(new Date());
        m_attCache.putInCache(att.getName(), att);
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getAttachmentData( Attachment att )
        throws ProviderException,
               IOException
    {
        return m_provider.getAttachmentData( att );
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Collection listAttachments( WikiPage page )
        throws ProviderException
    {
        log.debug("Listing attachments for "+page);
        try
        {
            Collection<Attachment> c = (Collection<Attachment>)m_cache.getFromCache( page.getName(), m_refreshPeriod );

            if( c != null )
            {
                log.debug("LIST from cache, "+page.getName()+", size="+c.size());
                m_cacheHits++;
                return cloneCollection(c);
            }

            log.debug("list NOT in cache, "+page.getName());

            refresh( page );
        }
        catch( NeedsRefreshException nre )
        {
            try
            {
                Collection<Attachment> c = refresh( page );

                return cloneCollection(c);
            }
            catch( Exception ex )
            {
                // Is a catch-all, because cache will get confused if
                // we let this one go.
                log.warn("Provider failed, returning cached content",ex);

                m_cache.cancelUpdate(page.getName());
                
                return (Collection)nre.getCacheContent();
            }
        }

        return new ArrayList();
    }

    private <T> Collection<T> cloneCollection( Collection<T> c )
    {
        ArrayList<T> list = new ArrayList<T>();
        
        list.addAll( c );
        
        return list;
    }
    
    /**
     * {@inheritDoc}
     */
    public Collection findAttachments( QueryItem[] query )
    {
        return m_provider.findAttachments( query );
    }

    /**
     * {@inheritDoc}
     */
    public List listAllChanged( Date timestamp )
        throws ProviderException
    {
        List all = null;
        //
        // we do a one-time build up of the cache, after this the cache is updated for every attachment add/delete
        if (m_gotall == false)
        {
            all = m_provider.listAllChanged(timestamp);

            // Put all pages in the cache :

            synchronized (this)
            {
                for (Iterator i = all.iterator(); i.hasNext();)
                {
                    Attachment att = (Attachment) i.next();
                    m_attCache.putInCache(att.getName(), att);
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
    @SuppressWarnings("unchecked")
    private final Collection<Attachment> refresh( WikiPage page )
        throws ProviderException
    {
        m_cacheMisses++;
        Collection<Attachment> c = m_provider.listAttachments( page );
        m_cache.putInCache( page.getName(), c );

        return c;
    }

    /**
     * {@inheritDoc}
     */
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
            catch( Exception ex )
            {
                log.warn("Provider failed, returning cached content",ex);

                m_cache.cancelUpdate( page.getName() );
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
     * {@inheritDoc}
     */
    public List getVersionHistory( Attachment att )
    {
        return m_provider.getVersionHistory( att );
    }

    /**
     * {@inheritDoc}
     */
    public void deleteVersion( Attachment att )
        throws ProviderException
    {
        // This isn't strictly speaking correct, but it does not really matter
        m_cache.removeEntry( att.getParentName() );
        m_provider.deleteVersion( att );
    }

    /**
     * {@inheritDoc}
     */
    public void deleteAttachment( Attachment att )
        throws ProviderException
    {
        m_cache.removeEntry( att.getParentName() );
        m_attCache.removeEntry( att.getName() );
        m_provider.deleteAttachment( att );
    }

    /**
     * {@inheritDoc}
     */
    public synchronized String getProviderInfo()
    {              
        return "Real provider: "+m_provider.getClass().getName()+
               ".  Cache misses: "+m_cacheMisses+
               ".  Cache hits: "+m_cacheHits;
    }

    /**
     *  Returns the WikiAttachmentProvider that this caching provider delegates to.
     * 
     *  @return The real provider underneath this one.
     */
    public WikiAttachmentProvider getRealProvider()
    {
        return m_provider;
    }
    
    /**
     * {@inheritDoc}
     */
    public void moveAttachmentsForPage( String oldParent, String newParent )
        throws ProviderException
    {
        m_provider.moveAttachmentsForPage(oldParent, newParent);
        m_cache.removeEntry( newParent ); 
        m_cache.removeEntry( oldParent );
    }

    /**
     * Keep a list of all Attachments in the OSCache (OSCache does not provide
     * something like that) Idea copied from CacheItemCollector The cache is used to
     * speed up the getRecentChanges function
     * 
     * @author Harry Metske
     * @since 2.5
     */
    private static class CachedAttachmentCollector implements CacheEntryEventListener
    {
        private static final Logger log = Logger.getLogger( CachedAttachmentCollector.class );

        private Map<String, Attachment> m_allItems = new HashMap<String, Attachment>();

        /**
         * Returns a clone of the set - you cannot manipulate this.
         * 
         * @return A list of all items.
         */
        public List<Attachment> getAllItems()
        {
            List<Attachment> ret = new LinkedList<Attachment>();
            ret.addAll( m_allItems.values() );
            log.info( "returning " + ret.size() + " attachments" );
            return ret;
        }

        public void cacheEntryRemoved( CacheEntryEvent aEvent )
        {
            if( aEvent != null )
            {
                if( log.isDebugEnabled() )
                {
                    log.debug( "attachment cache entry removed: " + aEvent.getKey() );
                }
                Attachment item = (Attachment) aEvent.getEntry().getContent();

                if( item != null )
                {
                    m_allItems.remove( item.getName() );
                }
            }
        }

        public void cacheEntryUpdated( CacheEntryEvent aEvent )
        {
            if( log.isDebugEnabled() )
            {
                log.debug( "attachment cache entry updated: " + aEvent.getKey() );
            }

            Attachment item = (Attachment) aEvent.getEntry().getContent();

            if( item != null )
            {
                // Item added or replaced.
                m_allItems.put( item.getName(), item );
            }
            else
            {
                m_allItems.remove( aEvent.getKey() );
            }
        }

        public void cacheEntryAdded( CacheEntryEvent aEvent )
        {
            cacheEntryUpdated( aEvent );
        }

        public void cachePatternFlushed( CachePatternEvent aEvent )
        {
            // do nothing
        }

        public void cacheGroupFlushed( CacheGroupEvent aEvent )
        {
            // do nothing
        }

        public void cacheFlushed( CachewideEvent aEvent )
        {
            // do nothing
        }

        public void cacheEntryFlushed( CacheEntryEvent aEvent )
        {
            cacheEntryRemoved( aEvent );
        }
    }

}
