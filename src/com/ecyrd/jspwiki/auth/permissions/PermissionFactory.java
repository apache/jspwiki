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
package com.ecyrd.jspwiki.auth.permissions;

import java.util.WeakHashMap;

import com.ecyrd.jspwiki.WikiPage;

/**
 *  Provides a factory for Permission objects.  Since the Permissions are immutable,
 *  and creating them takes a bit of time, caching them makes sense.
 *  <p>
 *  This class stores the permissions in a static HashMap.
 *  @author Janne Jalkanen
 *  @since 2.5.54
 */
public class PermissionFactory
{
    /**
     *  This is a WeakHashMap<Integer,PagePermission>, which stores the
     *  cached page permissions.
     */
    private static WeakHashMap c_cache = new WeakHashMap();
    
    /**
     *  Get a permission object for a WikiPage and a set of actions.
     *  
     *  @param page The page object.
     *  @param actions A list of actions.
     *  @return A PagePermission object, presenting this page+actions combination.
     */
    public static final PagePermission getPagePermission( WikiPage page, String actions )
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
    public static final PagePermission getPagePermission( String page, String actions )
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
    private static final PagePermission getPagePermission( String wiki, String page, String actions )
    {
        PagePermission perm;
        //
        //  Since this is pretty speed-critical, we try to avoid the StringBuffer creation
        //  overhead by XORring the hashcodes.  However, if page name length > 32 characters,
        //  this might result in two same hashCodes.
        //  FIXME: Make this work for page-name lengths > 32 characters (use the alt implementation
        //         if page.length() > 32?)
        // Alternative implementation below, but it does create an extra StringBuffer.
        //String         key = wiki+":"+page+":"+actions;
        
        Integer key = new Integer( wiki.hashCode() ^ page.hashCode() ^ actions.hashCode() );
   
        //
        //  It's fine if two threads update the cache, since the objects mean the same
        //  thing anyway.  And this avoids nasty blocking effects.
        //
        synchronized( c_cache )
        {
            perm = (PagePermission)c_cache.get( key );
        }
        
        if( perm == null )
        {
            if( wiki.length() > 0 ) page = wiki+":"+page;
            perm = new PagePermission( page, actions );
            
            synchronized( c_cache )
            {
                c_cache.put( key, perm );
            }
        }
        
        return perm;
    }

}
