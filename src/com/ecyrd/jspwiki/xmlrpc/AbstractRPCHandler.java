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

import com.ecyrd.jspwiki.*;
import java.util.*;

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
    public static final int ERR_NOPAGE    = 1;

    public static final String LINK_LOCAL    = "local";
    public static final String LINK_EXTERNAL = "external";
    public static final String LINK_INLINE   = "inline";

    protected WikiEngine m_engine;

    /**
     *  This is the currently implemented JSPWiki XML-RPC code revision.
     */
    public static final int RPC_VERSION = 1;

    public void initialize( WikiEngine engine )
    {
        m_engine = engine;
    }

    protected abstract Hashtable encodeWikiPage( WikiPage p );

    public Vector getRecentChanges( Date since )
    {
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
     *  Returns the current supported JSPWiki XML-RPC API.
     */
    public int getRPCVersionSupported()
    {
        return RPC_VERSION;
    }
}
