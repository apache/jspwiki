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

import java.io.*;
import org.apache.log4j.Category;
import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;
import java.util.*;
import org.apache.xmlrpc.XmlRpcException;

/**
 *  Provides handlers for all RPC routines.  These routines are used by
 *  the UTF-8 interface.
 *
 *  @author Janne Jalkanen
 *  @since 1.6.13
 */

public class RPCHandlerUTF8
    extends AbstractRPCHandler
{
    Category log = Category.getInstance( RPCHandlerUTF8.class ); 

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
            WikiPage p = (WikiPage) i.next();

            if( !(p instanceof Attachment) )
            {
                result.add( p.getName() );
            }
        }

        return result;
    }

    /**
     *  Encodes a single wiki page info into a Hashtable.
     */
    protected Hashtable encodeWikiPage( WikiPage page )
    {
        Hashtable ht = new Hashtable();

        ht.put( "name", page.getName() );

        Date d = page.getLastModified();

        //
        //  Here we reset the DST and TIMEZONE offsets of the
        //  calendar.  Unfortunately, I haven't thought of a better
        //  way to ensure that we're getting the proper date
        //  from the XML-RPC thingy, except to manually adjust the date.
        //

        Calendar cal = Calendar.getInstance();
        cal.setTime( d );
        cal.add( Calendar.MILLISECOND, 
                 - (cal.get( Calendar.ZONE_OFFSET ) + 
                    (cal.getTimeZone().inDaylightTime( d ) ? cal.get( Calendar.DST_OFFSET ) : 0 )) );

        ht.put( "lastModified", cal.getTime() );
        ht.put( "version", new Integer(page.getVersion()) );

        if( page.getAuthor() != null )
        {
            ht.put( "author", page.getAuthor() );
        }

        return ht;
    }

    public Vector getRecentChanges( Date since )
    {
        Collection pages = m_engine.getRecentChanges();
        Vector result = new Vector();

        Calendar cal = Calendar.getInstance();
        cal.setTime( since );

        //
        //  Convert UTC to our time.
        //
        cal.add( Calendar.MILLISECOND,
                 (cal.get( Calendar.ZONE_OFFSET ) +
                  (cal.getTimeZone().inDaylightTime(since) ? cal.get( Calendar.DST_OFFSET ) : 0 ) ) );
        since = cal.getTime();

        for( Iterator i = pages.iterator(); i.hasNext(); )
        {
            WikiPage page = (WikiPage)i.next();

            if( page.getLastModified().after( since ) && !(page instanceof Attachment) )
            {
                result.add( encodeWikiPage( page ) );
            }
        }

        return result;
    }

    /**
     *  Simple helper method, turns the incoming page name into
     *  normal Java string, then checks page condition.
     *
     *  @param pagename Page Name as an RPC string (URL-encoded UTF-8)
     *  @return Real page name, as Java string.
     *  @throws XmlRpcException, if there is something wrong with the page.
     */
    private String parsePageCheckCondition( String pagename )
        throws XmlRpcException
    {
        if( !m_engine.pageExists(pagename) )
        {
            throw new XmlRpcException( ERR_NOPAGE, "No such page '"+pagename+"' found, o master." );
        }

        return pagename;
    }

    public Hashtable getPageInfo( String pagename )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );
        return encodeWikiPage( m_engine.getPage(pagename) );
    }

    public Hashtable getPageInfoVersion( String pagename, int version )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        return encodeWikiPage( m_engine.getPage( pagename, version ) );
    }

    public String getPage( String pagename )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        String text = m_engine.getPureText( pagename, -1 );

        return text;
    }

    public String getPageVersion( String pagename, int version )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        return m_engine.getPureText( pagename, version );
    }

    public String getPageHTML( String pagename )
        throws XmlRpcException    
    {
        pagename = parsePageCheckCondition( pagename );

        return m_engine.getHTML( pagename );
    }

    public String getPageHTMLVersion( String pagename, int version )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        return m_engine.getHTML( pagename, version );
    }

    public Vector listLinks( String pagename )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        String pagedata = m_engine.getPureText( pagename, -1 );

        LinkCollector localCollector = new LinkCollector();
        LinkCollector extCollector   = new LinkCollector();

        m_engine.textToHTML( new WikiContext(m_engine,pagename),
                             pagedata,
                             localCollector,
                             extCollector );

        Vector result = new Vector();

        // FIXME: Contains far too much common with RPCHandler.  Refactor!

        //
        //  Add local links.
        //
        for( Iterator i = localCollector.getLinks().iterator(); i.hasNext(); )
        {
            String link = (String) i.next();
            Hashtable ht = new Hashtable();
            ht.put( "page", link );
            ht.put( "type", LINK_LOCAL );

            if( m_engine.pageExists(link) )
            {
                ht.put( "href", m_engine.getViewURL(link) );
            }
            else
            {
                ht.put( "href", m_engine.getEditURL(link) );
            }

            result.add( ht );
        }

        //
        // External links don't need to be changed into XML-RPC strings,
        // simply because URLs are by definition ASCII.
        //

        for( Iterator i = extCollector.getLinks().iterator(); i.hasNext(); )
        {
            String link = (String) i.next();

            Hashtable ht = new Hashtable();

            ht.put( "page", link );
            ht.put( "type", LINK_EXTERNAL );
            ht.put( "href", link );

            result.add( ht );
        }

        return result;
    }
}
