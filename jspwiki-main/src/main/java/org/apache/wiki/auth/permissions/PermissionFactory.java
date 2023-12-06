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
package org.apache.wiki.auth.permissions;

import org.apache.wiki.api.core.Page;
import org.apache.wiki.util.Synchronizer;

import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;


/**
 *  Provides a factory for Permission objects.  Since the Permissions are immutable,
 *  and creating them takes a bit of time, caching them makes sense.
 *  <p>
 *  This class stores the permissions in a static HashMap.
 *  @since 2.5.54
 */
public final class PermissionFactory
{
    /**
     *  Prevent instantiation.
     */
    private PermissionFactory() {}
    
    /**
     *  This is a WeakHashMap<Integer,PagePermission>, which stores the
     *  cached page permissions.
     */
    private static final WeakHashMap<Integer, PagePermission> c_cache = new WeakHashMap<>();

    /**
     * A lock used to ensure thread safety when accessing shared resources.
     * This lock provides more flexibility and capabilities than the intrinsic locking mechanism,
     * such as the ability to attempt to acquire a lock with a timeout, or to interrupt a thread
     * waiting to acquire a lock.
     *
     * @see java.util.concurrent.locks.ReentrantLock
     */
    private static final ReentrantLock lock = new ReentrantLock();
    
    /**
     *  Get a permission object for a WikiPage and a set of actions.
     *  
     *  @param page The page object.
     *  @param actions A list of actions.
     *  @return A PagePermission object, presenting this page+actions combination.
     */
    public static PagePermission getPagePermission( final Page page, final String actions )
    {
        return getPagePermission( page.getWiki(), page.getName(), actions );
    }
    
    /**
     *  Get a permission object for a WikiPage and a set of actions.
     *  
     *  @param page The name of the page.
     *  @param actions A list of actions.
     *  @return A PagePermission object, presenting this page+actions combination.
     */
    public static PagePermission getPagePermission( final String page, final String actions )
    {
        return getPagePermission( "", page, actions );
    }

    /**
     *  Get a page permission based on a wiki, page, and actions.
     *  
     *  @param wiki The name of the wiki. Can be an empty string, but must not be null.
     *  @param page The page name
     *  @param actions A list of actions.
     *  @return A PagePermission object.
     */
    private static PagePermission getPagePermission(final String wiki, String page, final String actions) {
        final Integer key = wiki.hashCode() ^ page.hashCode() ^ actions.hashCode();
        AtomicReference<PagePermission> permRef = new AtomicReference<>();

        Synchronizer.synchronize(lock, () -> {
            PagePermission perm = c_cache.get(key);
            permRef.set(perm);
        });

        if (permRef.get() == null) {
            if (!wiki.isEmpty()) page = wiki + ":" + page;
            PagePermission newPerm = new PagePermission(page, actions);

            Synchronizer.synchronize(lock, () -> {
                c_cache.put(key, newPerm);
            });

            return newPerm;
        }

        return permRef.get();
    }


}
