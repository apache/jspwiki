/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.xmlrpc;

import java.io.*;
import org.apache.log4j.Category;
import com.ecyrd.jspwiki.*;
import java.util.*;

/**
 *  Provides handlers for all RPC routines.
 *
 *  @author Janne Jalkanen
 *  @since 1.6.6
 */
// We could use WikiEngine directly, but because of introspection it would
// show just too many methods to be safe.

public class RPCHandler
{
    private WikiEngine m_engine;

    public RPCHandler( WikiEngine engine )
    {
        m_engine = engine;
    }

    public String getApplicationName()
    {
        return m_engine.getApplicationName();
    }

    public Vector getAllPages()
    {
        Collection pages = m_engine.getRecentChanges();
        Vector result = new Vector();
        int count = 0;

        for( Iterator i = pages.iterator(); i.hasNext(); )
        {
            result.add( ((WikiPage)i.next()).getName() );
        }

        return result;
    }

    private Hashtable encodeWikiPage( WikiPage page )
    {
        Hashtable ht = new Hashtable();

        ht.put( "name", page.getName() );
        ht.put( "lastModified", page.getLastModified() );
        ht.put( "version", new Integer(page.getVersion()) );
        ht.put( "author", page.getAuthor() );

        return ht;
    }

    public Vector getRecentChanges( Date since )
    {
        Collection pages = m_engine.getRecentChanges();
        Vector result = new Vector();

        for( Iterator i = pages.iterator(); i.hasNext(); )
        {
            WikiPage page = (WikiPage)i.next();

            if( page.getLastModified().after( since ) )
            {
                result.add( encodeWikiPage( page ) );
            }
        }

        return result;
    }

    public Hashtable getPageInfo( String pagename )
    {
        return encodeWikiPage( m_engine.getPage(pagename) );
    }

    public Hashtable getPageInfo( String pagename, int version )
    {
        return encodeWikiPage( m_engine.getPage( pagename, version ) );
    }

    public String getPage( String pagename )
    {
        return m_engine.getPureText( pagename, -1 );
    }

    public String getPage( String pagename, int version )
    {
        return m_engine.getPureText( pagename, version );
    }

    public String getPageHTML( String pagename )
    {
        return m_engine.getHTML( pagename );
    }

    public String getPageHTML( String pagename, int version )
    {
        return m_engine.getHTML( pagename, version );
    }
}
