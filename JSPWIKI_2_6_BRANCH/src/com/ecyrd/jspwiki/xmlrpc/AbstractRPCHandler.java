/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.xmlrpc;

import java.security.Permission;
import java.util.*;

import org.apache.xmlrpc.AuthenticationFailed;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.auth.AuthorizationManager;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;

/**
 *  Provides definitions for RPC handler routines.
 *
 *  @author Janne Jalkanen
 *  @since 1.6.13
 */

public abstract class AbstractRPCHandler
    implements WikiRPCHandler
{
    /** Error code: no such page. */
    public static final int ERR_NOPAGE       = 1;
    public static final int ERR_NOPERMISSION = 2;

    /**
     *  Link to a local wiki page.
     */
    public static final String LINK_LOCAL    = "local";

    /**
     *  Link to an external resource.
     */
    public static final String LINK_EXTERNAL = "external";

    /**
     *  This is an inlined image.
     */
    public static final String LINK_INLINE   = "inline";

    protected WikiEngine m_engine;
    protected WikiContext m_context;
    
    
    /**
     *  This is the currently implemented JSPWiki XML-RPC code revision.
     */
    public static final int RPC_VERSION = 1;

    public void initialize( WikiContext context )
    {
        m_context = context;
        m_engine  = context.getEngine();
    }

    protected abstract Hashtable encodeWikiPage( WikiPage p );

    public Vector getRecentChanges( Date since )
    {
        checkPermission( PagePermission.VIEW );
        Collection pages = m_engine.getRecentChanges();
        Vector result    = new Vector();

        // Transform UTC into local time.
        Calendar cal = Calendar.getInstance();
        cal.setTime( since );
        cal.add( Calendar.MILLISECOND, 
                 (cal.get( Calendar.ZONE_OFFSET ) + 
                  (cal.getTimeZone().inDaylightTime( since ) ? cal.get( Calendar.DST_OFFSET ) : 0 )) );

        for( Iterator i = pages.iterator(); i.hasNext(); )
        {
            WikiPage page = (WikiPage)i.next();

            if( page.getLastModified().after( cal.getTime() ) )
            {
                result.add( encodeWikiPage( page ) );
            }
        }

        return result;
    }

    /**
     *  Checks whether the current user has permission to perform the RPC action; 
     *  throws an exception if not allowed by {@link com.ecyrd.jspwiki.auth.AuthorizationManager}.
     *  
     *  @throws AuthenticationFailed A RuntimeException, if the authentication fails and the user has no permission.
     *  @param perm the Permission to check
     */
    protected void checkPermission( Permission perm )
    {
        AuthorizationManager mgr = m_engine.getAuthorizationManager();
        
        if( mgr.checkPermission( m_context.getWikiSession(), perm ) )
            return;
        
        throw new AuthenticationFailed( "You have no access to this resource, o master" );
    }
    
    /**
     *  Returns the current supported JSPWiki XML-RPC API.
     */
    public int getRPCVersionSupported()
    {
        checkPermission( WikiPermission.LOGIN );
        
        return RPC_VERSION;
    }
}
