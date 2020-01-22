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

import org.apache.wiki.LinkCollector;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.xmlrpc.XmlRpcException;

import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

/**
 *  Provides handlers for all RPC routines.  These routines are used by
 *  the UTF-8 interface.
 *
 *  @since 1.6.13
 */

public class RPCHandlerUTF8 extends AbstractRPCHandler {

    public String getApplicationName() {
        checkPermission( PagePermission.VIEW );
        return m_engine.getApplicationName();
    }

    public Vector< String > getAllPages() {
        checkPermission( PagePermission.VIEW );

        final Set< WikiPage > pages = m_engine.getPageManager().getRecentChanges();
        final Vector< String > result = new Vector<>();

        for( final WikiPage p : pages ) {
            if( !( p instanceof Attachment ) ) {
                result.add( p.getName() );
            }
        }

        return result;
    }

    /**
     *  Encodes a single wiki page info into a Hashtable.
     */
    protected Hashtable<String, Object> encodeWikiPage( final WikiPage page ) {
        final Hashtable<String, Object> ht = new Hashtable<>();
        ht.put( "name", page.getName() );

        final Date d = page.getLastModified();

        //
        //  Here we reset the DST and TIMEZONE offsets of the calendar.  Unfortunately, I haven't thought of a better
        //  way to ensure that we're getting the proper date from the XML-RPC thingy, except to manually adjust the date.
        //
        final Calendar cal = Calendar.getInstance();
        cal.setTime( d );
        cal.add( Calendar.MILLISECOND,
                 - (cal.get( Calendar.ZONE_OFFSET ) +
                    (cal.getTimeZone().inDaylightTime( d ) ? cal.get( Calendar.DST_OFFSET ) : 0 )) );

        ht.put( "lastModified", cal.getTime() );
        ht.put( "version", page.getVersion() );

        if( page.getAuthor() != null ) {
            ht.put( "author", page.getAuthor() );
        }

        return ht;
    }

    public Vector< Hashtable< String, Object > > getRecentChanges( Date since ) {
        checkPermission( PagePermission.VIEW );

        final Set< WikiPage > pages = m_engine.getPageManager().getRecentChanges();
        final Vector< Hashtable< String, Object > > result = new Vector<>();

        final Calendar cal = Calendar.getInstance();
        cal.setTime( since );

        //
        //  Convert UTC to our time.
        //
        cal.add( Calendar.MILLISECOND,
                 (cal.get( Calendar.ZONE_OFFSET ) +
                  (cal.getTimeZone().inDaylightTime(since) ? cal.get( Calendar.DST_OFFSET ) : 0 ) ) );
        since = cal.getTime();

        for( final WikiPage page : pages ) {
            if( page.getLastModified().after( since ) && !( page instanceof Attachment ) ) {
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
    private String parsePageCheckCondition( final String pagename ) throws XmlRpcException {
        if( !m_engine.getPageManager().wikiPageExists(pagename) ) {
            throw new XmlRpcException( ERR_NOPAGE, "No such page '"+pagename+"' found, o master." );
        }

        final WikiPage p = m_engine.getPageManager().getPage( pagename );

        checkPermission( PermissionFactory.getPagePermission( p, PagePermission.VIEW_ACTION ) );
        return pagename;
    }

    public Hashtable<String, Object> getPageInfo( final String pagename ) throws XmlRpcException {
        return encodeWikiPage( m_engine.getPageManager().getPage( parsePageCheckCondition( pagename ) ) );
    }

    public Hashtable<String, Object> getPageInfoVersion( String pagename, final int version ) throws XmlRpcException {
        pagename = parsePageCheckCondition( pagename );

        return encodeWikiPage( m_engine.getPageManager().getPage( pagename, version ) );
    }

    public String getPage( final String pagename ) throws XmlRpcException {
        return m_engine.getPageManager().getPureText( parsePageCheckCondition( pagename ), -1 );
    }

    public String getPageVersion( final String pagename, final int version ) throws XmlRpcException {
        return m_engine.getPageManager().getPureText( parsePageCheckCondition( pagename ), version );
    }

    public String getPageHTML( final String pagename ) throws XmlRpcException  {
        return m_engine.getRenderingManager().getHTML( parsePageCheckCondition( pagename ) );
    }

    public String getPageHTMLVersion( final String pagename, final int version ) throws XmlRpcException {
        return m_engine.getRenderingManager().getHTML( parsePageCheckCondition( pagename ), version );
    }

    public Vector< Hashtable< String, String > > listLinks( String pagename ) throws XmlRpcException {
        pagename = parsePageCheckCondition( pagename );

        final WikiPage page = m_engine.getPageManager().getPage( pagename );
        final String pagedata = m_engine.getPageManager().getPureText( page );

        final LinkCollector localCollector = new LinkCollector();
        final LinkCollector extCollector   = new LinkCollector();
        final LinkCollector attCollector   = new LinkCollector();

        final WikiContext context = new WikiContext( m_engine, page );
        m_engine.getRenderingManager().textToHTML( context, pagedata, localCollector, extCollector, attCollector );

        final Vector< Hashtable< String, String > > result = new Vector<>();

        // FIXME: Contains far too much common with RPCHandler.  Refactor!

        //
        //  Add local links.
        //
        for( final String link : localCollector.getLinks() ) {
            final Hashtable<String, String> ht = new Hashtable<>();
            ht.put( "page", link );
            ht.put( "type", LINK_LOCAL );

            if( m_engine.getPageManager().wikiPageExists( link ) ) {
                ht.put( "href", context.getViewURL( link ) );
            } else {
                ht.put( "href", context.getURL( WikiContext.EDIT, link ) );
            }

            result.add( ht );
        }

        //
        // Add links to inline attachments
        //
        for( final String link : attCollector.getLinks() ) {
            final Hashtable<String, String> ht = new Hashtable<>();
            ht.put( "page", link );
            ht.put( "type", LINK_LOCAL );
            ht.put( "href", context.getURL(WikiContext.ATTACH,link) );
            result.add( ht );
        }

        //
        // External links don't need to be changed into XML-RPC strings, simply because URLs are by definition ASCII.
        //
        for( final String link : extCollector.getLinks() )  {
            final Hashtable<String, String> ht = new Hashtable<>();
            ht.put( "page", link );
            ht.put( "type", LINK_EXTERNAL );
            ht.put( "href", link );
            result.add( ht );
        }

        return result;
    }

}
