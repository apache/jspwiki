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
package org.apache.wiki.xmlrpc;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.apache.wiki.LinkCollector;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.xmlrpc.XmlRpcException;

/**
 *  Provides handlers for all RPC routines.  These routines are used by
 *  the UTF-8 interface.
 *
 *  @since 1.6.13
 */

public class RPCHandlerUTF8
    extends AbstractRPCHandler
{
    public String getApplicationName()
    {
        checkPermission( PagePermission.VIEW );

        return m_engine.getApplicationName();
    }

    public Vector getAllPages()
    {
        checkPermission( PagePermission.VIEW );

        Collection< WikiPage > pages = m_engine.getRecentChanges();
        Vector<String> result = new Vector<String>();

        for( WikiPage p : pages )
        {
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
    protected Hashtable<String, Object> encodeWikiPage( WikiPage page )
    {
        Hashtable<String, Object> ht = new Hashtable<String, Object>();

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
        ht.put( "version", page.getVersion() );

        if( page.getAuthor() != null )
        {
            ht.put( "author", page.getAuthor() );
        }

        return ht;
    }

    public Vector getRecentChanges( Date since )
    {
        checkPermission( PagePermission.VIEW );

        Collection< WikiPage > pages = m_engine.getRecentChanges();
        Vector<Hashtable<String, Object>> result = new Vector<Hashtable<String, Object>>();

        Calendar cal = Calendar.getInstance();
        cal.setTime( since );

        //
        //  Convert UTC to our time.
        //
        cal.add( Calendar.MILLISECOND,
                 (cal.get( Calendar.ZONE_OFFSET ) +
                  (cal.getTimeZone().inDaylightTime(since) ? cal.get( Calendar.DST_OFFSET ) : 0 ) ) );
        since = cal.getTime();

        for( WikiPage page : pages )
        {
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

        WikiPage p = m_engine.getPage( pagename );

        checkPermission( PermissionFactory.getPagePermission( p, PagePermission.VIEW_ACTION ) );
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

        WikiPage page = m_engine.getPage( pagename );
        String pagedata = m_engine.getPureText( page );

        LinkCollector localCollector = new LinkCollector();
        LinkCollector extCollector   = new LinkCollector();
        LinkCollector attCollector   = new LinkCollector();

        WikiContext context = new WikiContext( m_engine, page );
        context.setVariable( WikiEngine.PROP_REFSTYLE, "absolute" );

        m_engine.textToHTML( context,
                             pagedata,
                             localCollector,
                             extCollector,
                             attCollector );

        Vector<Hashtable<String, String>> result = new Vector<Hashtable<String, String>>();

        // FIXME: Contains far too much common with RPCHandler.  Refactor!

        //
        //  Add local links.
        //
        for( Iterator< String > i = localCollector.getLinks().iterator(); i.hasNext(); )
        {
            String link = i.next();
            Hashtable< String, String > ht = new Hashtable< String, String >();
            ht.put( "page", link );
            ht.put( "type", LINK_LOCAL );

            if( m_engine.pageExists(link) )
            {
                ht.put( "href", context.getViewURL(link) );
            }
            else
            {
                ht.put( "href", context.getURL(WikiContext.EDIT,link) );
            }

            result.add( ht );
        }

        //
        // Add links to inline attachments
        //
        for( String link : attCollector.getLinks() )
        {
            Hashtable<String, String> ht = new Hashtable<String, String>();

            ht.put( "page", link );
            ht.put( "type", LINK_LOCAL );
            ht.put( "href", context.getURL(WikiContext.ATTACH,link) );

            result.add( ht );
        }

        //
        // External links don't need to be changed into XML-RPC strings,
        // simply because URLs are by definition ASCII.
        //

        for( String link : extCollector.getLinks() )
        {
            Hashtable<String, String> ht = new Hashtable<String, String>();

            ht.put( "page", link );
            ht.put( "type", LINK_EXTERNAL );
            ht.put( "href", link );

            result.add( ht );
        }

        return result;
    }
}
