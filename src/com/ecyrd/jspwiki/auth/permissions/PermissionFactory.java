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

import java.security.Permission;
import java.util.HashMap;
import java.util.Map;
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
    private static Map<Class<? extends Permission>,Map<Integer,Permission>> c_cache 
         = new HashMap<Class<? extends Permission>,Map<Integer,Permission>>();
    
    /**
     *  Get a permission object for a Group and a set of actions.
     *  
     *  @param group the fully-qualified name of the group
     *  @param actions A list of actions.
     *  @return A GroupPermission object, presenting this group+actions combination.
     */
    public static final GroupPermission getGroupPermission( String group, String actions )
    {
        Map<Integer,Permission> cachedPerms = getPermissionCache( GroupPermission.class );
        Integer key = new Integer( group.hashCode() ^ actions.hashCode() );
        GroupPermission perm;
        synchronized( cachedPerms )
        {
            perm = (GroupPermission)cachedPerms.get( key );
            if( perm == null )
            {
                perm = new GroupPermission( group, actions );
                cachedPerms.put( key, perm );
            }
        }
        return perm;
    }
    
    /**
     *  Get a permission object for a WikiPage and a set of actions.
     *  
     *  @param page The page object.
     *  @param actions A list of actions.
     *  @return A PagePermission object, presenting this page+actions combination.
     */
    public static final PagePermission getPagePermission( WikiPage page, String actions )
    {
        Map<Integer,Permission> cachedPerms = getPermissionCache( GroupPermission.class );
        Integer key = new Integer( page.getWiki().hashCode() ^ page.getName().hashCode() ^ actions.hashCode() );
        PagePermission perm;
        synchronized( cachedPerms )
        {
            perm = (PagePermission)cachedPerms.get( key );
            if( perm == null )
            {
                perm = new PagePermission( page, actions );
                cachedPerms.put( key, perm );
            }
        }
        return perm;
    }
    
    /**
     *  Get a permission object for a WikiPage and a set of actions.
     *  
     *  @param page The name of the page, including the wiki prefix (e.g., MyWiki:Main)
     *  @param actions A list of actions.
     *  @return A PagePermission object, presenting this page+actions combination.
     */
    public static final PagePermission getPagePermission( String page, String actions )
    {
        Map<Integer,Permission> cachedPerms = getPermissionCache( GroupPermission.class );
        Integer key = new Integer( page.hashCode() ^ actions.hashCode() );
        PagePermission perm;
        synchronized( cachedPerms )
        {
            perm = (PagePermission)cachedPerms.get( key );
            if( perm == null )
            {
                perm = new PagePermission( page, actions );
                cachedPerms.put( key, perm );
            }
        }
        return perm;
    }

    /**
     *  Get a page permission based on a wiki, page, and actions.
     *  
     *  @param wiki The name of the wiki. Can be an empty string, but must not be null.
     *  @param page The page name
     *  @param actions A list of actions.
     *  @return A PagePermission object.
     */
    public static final WikiPermission getWikiPermission( String wiki, String actions )
    {
        Map<Integer,Permission> cachedPerms = getPermissionCache( GroupPermission.class );
        Integer key = new Integer( wiki.hashCode() ^ actions.hashCode() );
        WikiPermission perm;
        synchronized( cachedPerms )
        {
            perm = (WikiPermission)cachedPerms.get( key );
            if( perm == null )
            {
                perm = new WikiPermission( wiki, actions );
                cachedPerms.put( key, perm );
            }
        }
        return perm;
    }
    
    private static Map<Integer,Permission>getPermissionCache( Class<? extends Permission> permClass )
    {
        // Get the HashMap for our permission type (creating it if needed)
        Map<Integer,Permission> cachedPerms = c_cache.get(permClass);
        if ( cachedPerms == null )
        {
            synchronized ( c_cache )
            {
                cachedPerms = new WeakHashMap<Integer,Permission>();
                c_cache.put(permClass,cachedPerms);
            }
        }
        return cachedPerms;
    }

}
