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
        Vector<Hashtable<?, ?>> result    = new Vector<Hashtable<?, ?>>();

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
