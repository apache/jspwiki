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


import org.apache.wiki.util.CheckedSupplier;

import java.io.Serializable;
import java.util.List;

/**
 * Cache manager abstraction.
 */
public interface CachingManager {

    /**
     * The property value for setting the cache on/off.  Value is {@value}.
     * This key is deprecated, use instead {@link #PROP_CACHE_ENABLE}.
     */
    @Deprecated
    String PROP_USECACHE_DEPRECATED = "jspwiki.usePageCache";

    /** The property value for setting the cache on/off.  Value is {@value}. */
    String PROP_CACHE_ENABLE = "jspwiki.cache.enable";

    /** The property value with the location of the cache configuration file.  Value is {@value}. */
    String PROP_CACHE_CONF_FILE = "jspwiki.cache.config-file";

    /** Name of the attachment cache. */
    String CACHE_ATTACHMENTS = "jspwiki.attachmentsCache";

    /** Name of the collection attachment cache. */
    String CACHE_ATTACHMENTS_COLLECTION = "jspwiki.attachmentCollectionsCache";

    /** Name of the dynamic attachment cache. */
    String CACHE_ATTACHMENTS_DYNAMIC = "jspwiki.dynamicAttachmentCache";

    /** Name of the page cache. */
    String CACHE_PAGES = "jspwiki.pageCache";

    /** Name of the page text cache. */
    String CACHE_PAGES_TEXT = "jspwiki.pageTextCache";

    /** Name of the page history cache. */
    String CACHE_PAGES_HISTORY = "jspwiki.pageHistoryCache";

    /** Name of the rendering cache. */
    String CACHE_DOCUMENTS = "jspwiki.renderingCache";

    /**
     * Shuts down the underlying cache manager
     */
    void shutdown();

    /**
     * Checks if the requested cache is enabled or not.
     *
     * @param cacheName The cache to be queried.
     * @return {@code true} if the cache is enabled, {@code false} otherwise.
     */
    boolean enabled( String cacheName );

    /**
     * Retrieves cache usage information.
     *
     * @param cacheName The cache to be queried.
     * @return cache usage information, or {@code null} if the requested cache is not enabled.
     */
    CacheInfo info( String cacheName );

    /**
     * Returns the list of keys from elements present in a cache.
     *
     * @param cacheName The cache to be queried.
     * @return list of keys from elements present in a cache.
     */
    < T extends Serializable > List< T > keys( String cacheName );

    /**
     * Returns an item from a cache. If it is not found on the cache, try to retrieve from the provided supplier. If
     * found there, put the value in the cache, and return it. Otherwise, return {@code null}.
     *
     * @param cacheName The cache in which the item lives.
     * @param key item's identifier.
     * @param supplier if the element is not cached, try to retrieve from the cached system.
     * @throws E the supplier may throw a checked exception, which is propagated upwards.
     * @return The requested item or {@code null} if either the cache is not enabled or the item is not present on the cache / cached service.
     */
    < T, E extends Exception > T get( String cacheName, Serializable key, CheckedSupplier< T, E > supplier ) throws E;

    /**
     * Puts an item on a cache.
     *
     * @param cacheName The cache in which the item will live.
     * @param key item's identifier.
     * @param val item to insert in the cache.
     */
    void put( String cacheName, Serializable key, Object val );

    /**
     * Removes an item from a cache.
     *
     * @param cacheName The cache in which the item to be removed lives.
     * @param key item's identifier.
     */
    void remove( String cacheName, Serializable key );

}
