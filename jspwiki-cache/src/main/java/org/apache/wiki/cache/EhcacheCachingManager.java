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
package org.apache.wiki.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.engine.Initializable;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.util.CheckedSupplier;
import org.apache.wiki.util.TextUtil;

import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Ehcache-based {@link CachingManager}.
 */
public class EhcacheCachingManager implements CachingManager, Initializable {

    private static final Logger LOG = LogManager.getLogger( EhcacheCachingManager.class );
    private static final int DEFAULT_CACHE_SIZE = 1_000;
    private static final int DEFAULT_CACHE_EXPIRY_PERIOD = 24*60*60;

    final Map< String, Cache > cacheMap = new ConcurrentHashMap<>();
    final Map< String, CacheInfo > cacheStats = new ConcurrentHashMap<>();
    CacheManager cacheManager;

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        if( cacheMap.size() > 0 ) {
            CacheManager.getInstance().shutdown();
            cacheMap.clear();
            cacheStats.clear();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void initialize( final Engine engine, final Properties props ) throws WikiException {
        final String cacheEnabled = TextUtil.getStringProperty( props, PROP_CACHE_ENABLE, PROP_USECACHE_DEPRECATED, "true" );
        final boolean useCache = "true".equalsIgnoreCase( cacheEnabled );
        final String confLocation = "/" + TextUtil.getStringProperty( props, PROP_CACHE_CONF_FILE, "ehcache-jspwiki.xml" );
        if( useCache ) {
            final URL location = this.getClass().getResource( confLocation );
            LOG.info( "Reading ehcache configuration file from classpath on /{}", location );
            cacheManager = CacheManager.create( location );
            registerCache( CACHE_ATTACHMENTS );
            registerCache( CACHE_ATTACHMENTS_COLLECTION );
            registerCache( CACHE_ATTACHMENTS_DYNAMIC );
            registerCache( CACHE_DOCUMENTS );
            registerCache( CACHE_PAGES );
            registerCache( CACHE_PAGES_HISTORY );
            registerCache( CACHE_PAGES_TEXT );
        }
    }

    void registerCache( final String cacheName ) {
        final Cache cache;
        if( cacheManager.cacheExists( cacheName ) ) {
            cache = cacheManager.getCache( cacheName );
        } else {
            LOG.info( "cache with name {} not found in ehcache configuration file, creating it with defaults.", cacheName );
            cache = new Cache( cacheName, DEFAULT_CACHE_SIZE, false, false, DEFAULT_CACHE_EXPIRY_PERIOD, DEFAULT_CACHE_EXPIRY_PERIOD );
            cacheManager.addCache( cache );
        }
        cacheMap.put( cacheName, cache );
        cacheStats.put( cacheName, new CacheInfo( cacheName, cache.getCacheConfiguration().getMaxEntriesLocalHeap() ) );
    }

    /** {@inheritDoc} */
    @Override
    public boolean enabled( final String cacheName ) {
        return cacheMap.get( cacheName ) != null;
    }

    /** {@inheritDoc} */
    @Override
    public CacheInfo info( final String cacheName ) {
        if( enabled( cacheName ) ) {
            return cacheStats.get( cacheName );
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings( "unchecked" )
    public List< String > keys( final String cacheName ) {
        if( enabled( cacheName ) ) {
            return cacheMap.get( cacheName ).getKeysWithExpiryCheck();
        }
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings( "unchecked" )
    public < T, E extends Exception > T get( final String cacheName, final Serializable key, final CheckedSupplier< T, E > supplier ) throws E {
        if( keyAndCacheAreNotNull( cacheName, key ) ) {
            final Element element = cacheMap.get( cacheName ).get( key );
            if( element != null ) {
                cacheStats.get( cacheName ).hit();
                return ( T )element.getObjectValue();
            } else {
                // element doesn't exist in cache, try to retrieve from the cached service instead.
                final T value = supplier.get();
                if( value != null ) {
                    cacheStats.get( cacheName ).miss();
                    cacheMap.get( cacheName ).put( new Element( key, value ) );
                }
                return value;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void put( final String cacheName, final Serializable key, final Object val ) {
        if( keyAndCacheAreNotNull( cacheName, key ) ) {
            cacheMap.get( cacheName ).put( new Element( key, val ) );
        }
    }

    /** {@inheritDoc} */
    @Override
    public void remove( final String cacheName, final Serializable key ) {
        if( keyAndCacheAreNotNull( cacheName, key ) ) {
            cacheMap.get( cacheName ).remove( key );
        }
    }

    boolean keyAndCacheAreNotNull( final String cacheName, final Serializable key ) {
        return enabled( cacheName ) && key != null;
    }

}
