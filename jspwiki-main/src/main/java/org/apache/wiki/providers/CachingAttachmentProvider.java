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
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.search.QueryItem;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.TextUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;


/**
 *  Provides a caching attachment provider.  This class rests on top of a
 *  real provider class and provides a cache to speed things up.  Only the
 *  Attachment objects are cached; the actual attachment contents are
 *  fetched always from the provider.
 *
 *  @since 2.1.64.
 */

//        EntryRefreshPolicy for that.
public class CachingAttachmentProvider
    implements WikiAttachmentProvider
{
    private static final Logger log = Logger.getLogger(CachingAttachmentProvider.class);

    private WikiAttachmentProvider m_provider;

    private CacheManager m_cacheManager = CacheManager.getInstance();

    /** Default cache capacity for now. */
    public static final int m_capacity = 1000;

    /**
     *  The cache contains Collection objects which contain Attachment objects.
     *  The key is the parent wiki page name (String).
     */
    private Cache m_cache;
    /** Name of the attachment cache. */
    public static final String ATTCOLLCACHE_NAME = "jspwiki.attachmentCollectionsCache";

    /**
     * This cache contains Attachment objects and is keyed by attachment name.
     * This provides for quickly giving recently changed attachments (for the RecentChanges plugin)
     */
    private Cache m_attCache;
    /** Name of the attachment cache. */
    public static final String ATTCACHE_NAME = "jspwiki.attachmentsCache";

    private long m_cacheMisses = 0;
    private long m_cacheHits = 0;

    /** The extension to append to directory names to denote an attachment directory. */
    public static final String DIR_EXTENSION   = "-att";

    /** Property that supplies the directory used to store attachments. */
    public static final String PROP_STORAGEDIR = "jspwiki.basicAttachmentProvider.storageDir";

    private boolean m_gotall = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize( WikiEngine engine, Properties properties ) throws NoRequiredPropertyException, IOException {
        log.info("Initing CachingAttachmentProvider");

        String attCollCacheName = engine.getApplicationName() + "." + ATTCOLLCACHE_NAME;
        if (m_cacheManager.cacheExists(attCollCacheName)) {
            m_cache = m_cacheManager.getCache(attCollCacheName);
        } else {
            m_cache = new Cache(attCollCacheName, m_capacity, false, false, 0, 0);
            m_cacheManager.addCache(m_cache);
        }

        //
        // cache for the individual Attachment objects, attachment name is key, the Attachment object is the cached object
        //
        String attCacheName = engine.getApplicationName() + "." + ATTCACHE_NAME;
        if (m_cacheManager.cacheExists(attCacheName)) {
            m_attCache = m_cacheManager.getCache(attCacheName);
        } else {
            m_attCache = new Cache(attCacheName, m_capacity, false, false, 0, 0);
            m_cacheManager.addCache(m_attCache);
        }
        //
        //  Find and initialize real provider.
        //
        final String classname;
        try {
            classname = TextUtil.getRequiredProperty( properties, AttachmentManager.PROP_PROVIDER );
        } catch( final NoSuchElementException e ) {
            throw new NoRequiredPropertyException( e.getMessage(), AttachmentManager.PROP_PROVIDER );
        }

        try
        {
            Class<?> providerclass = ClassUtil.findClass( "org.apache.wiki.providers", classname);

            m_provider = (WikiAttachmentProvider)providerclass.newInstance();

            log.debug("Initializing real provider class "+m_provider);
            m_provider.initialize( engine, properties );
        }
        catch( ClassNotFoundException e )
        {
            log.error("Unable to locate provider class "+classname,e);
            throw new IllegalArgumentException("no provider class", e);
        }
        catch( InstantiationException e )
        {
            log.error("Unable to create provider class "+classname,e);
            throw new IllegalArgumentException("faulty provider class", e);
        }
        catch( IllegalAccessException e )
        {
            log.error("Illegal access to provider class "+classname,e);
            throw new IllegalArgumentException("illegal provider class", e);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAttachmentData( Attachment att, InputStream data )
        throws ProviderException, IOException {
        m_provider.putAttachmentData( att, data );

        m_cache.remove(att.getParentName());
        att.setLastModified(new Date());
        m_attCache.put(new Element(att.getName(), att));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getAttachmentData( Attachment att )
        throws ProviderException, IOException {
        return m_provider.getAttachmentData( att );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List< Attachment > listAttachments(WikiPage page) throws ProviderException {
        log.debug("Listing attachments for " + page);
        Element element = m_cache.get(page.getName());

        if (element != null) {
            @SuppressWarnings("unchecked") 
            List< Attachment > c = ( List< Attachment > )element.getObjectValue();
            log.debug("LIST from cache, " + page.getName() + ", size=" + c.size());
            return cloneCollection(c);
        }

        log.debug("list NOT in cache, " + page.getName());

        return refresh(page);
    }

    private <T> List<T> cloneCollection( Collection<T> c )
    {
        ArrayList<T> list = new ArrayList<>();

        list.addAll( c );

        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection< Attachment > findAttachments( QueryItem[] query )
    {
        return m_provider.findAttachments( query );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Attachment> listAllChanged(Date timestamp) throws ProviderException {
        List< Attachment > all = null;
        //
        // we do a one-time build up of the cache, after this the cache is updated for every attachment add/delete
        if (m_gotall == false) {
            all = m_provider.listAllChanged(timestamp);

            // Put all pages in the cache :

            synchronized (this) {
                for (Iterator< Attachment > i = all.iterator(); i.hasNext(); ) {
                    Attachment att = i.next();
                    m_attCache.put(new Element(att.getName(), att));
                }
                m_gotall = true;
            }
        } else {
            @SuppressWarnings("unchecked")
            List< String > keys = m_attCache.getKeysWithExpiryCheck();
            all = new ArrayList<>();
            for (String key : keys) {
                Element element = m_attCache.get(key);
                Attachment cachedAttachment = ( Attachment )element.getObjectValue();
                if (cachedAttachment != null) {
                    all.add(cachedAttachment);
                }
            }
        }

        return all;
    }

    /**
     *  Simply goes through the collection and attempts to locate the
     *  given attachment of that name.
     *
     *  @return null, if no such attachment was in this collection.
     */
    private Attachment findAttachmentFromCollection( Collection< Attachment > c, String name ) {
        for( Attachment att : new ArrayList< >( c ) ) {
            if( name.equals( att.getFileName() ) ) {
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
    private List<Attachment> refresh( WikiPage page ) throws ProviderException
    {
        List<Attachment> c = m_provider.listAttachments( page );
        m_cache.put(new Element(page.getName(), c));

        return c;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Attachment getAttachmentInfo(WikiPage page, String name, int version) throws ProviderException {
        if (log.isDebugEnabled()) {
            log.debug("Getting attachments for " + page + ", name=" + name + ", version=" + version);
        }

        //
        //  We don't cache previous versions
        //
        if (version != WikiProvider.LATEST_VERSION) {
            log.debug("...we don't cache old versions");
            return m_provider.getAttachmentInfo(page, name, version);
        }

        Collection<Attachment> c = null;
        Element element =   m_cache.get(page.getName());

        if (element == null) {
            log.debug(page.getName() + " wasn't in the cache");
            c = refresh(page);

            if (c == null) {
                return null; // No such attachment
            }
        } else {
            log.debug(page.getName() + " FOUND in the cache");
            c = (Collection<Attachment>) element.getObjectValue();
        }

        return findAttachmentFromCollection(c, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Attachment> getVersionHistory( Attachment att )
    {
        return m_provider.getVersionHistory( att );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteVersion( Attachment att ) throws ProviderException
    {
        // This isn't strictly speaking correct, but it does not really matter
        m_cache.remove(att.getParentName());
        m_provider.deleteVersion( att );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAttachment( Attachment att ) throws ProviderException
    {
        m_cache.remove(att.getParentName());
        m_attCache.remove(att.getName());
        m_provider.deleteAttachment( att );
    }


    /**
     * Gets the provider class name, and cache statistics (misscount and,hitcount of the attachment cache).
     *
     * @return A plain string with all the above mentioned values.
     */
    @Override
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
    @Override
    public void moveAttachmentsForPage( String oldParent, String newParent ) throws ProviderException
    {
        m_provider.moveAttachmentsForPage(oldParent, newParent);
        m_cache.remove(newParent);
        m_cache.remove(oldParent);

        //
        //  This is a kludge to make sure that the pages are removed
        //  from the other cache as well.
        //
        String checkName = oldParent + "/";

        @SuppressWarnings("unchecked")
        List< String > names = m_cache.getKeysWithExpiryCheck();
        for( String name : names )
        {
            if( name.startsWith( checkName ) )
            {
                m_attCache.remove(name);
            }
        }
    }

}