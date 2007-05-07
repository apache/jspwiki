/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 JSPWiki development group

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
