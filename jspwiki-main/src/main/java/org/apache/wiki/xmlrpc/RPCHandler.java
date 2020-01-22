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

import org.apache.log4j.Logger;
import org.apache.wiki.LinkCollector;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.util.TextUtil;
import org.apache.xmlrpc.XmlRpcException;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

/**
 *  Provides handlers for all RPC routines.
 *
 *  @since 1.6.6
 */
// We could use WikiEngine directly, but because of introspection it would show just too many methods to be safe.
public class RPCHandler extends AbstractRPCHandler {

    private static final Logger log = Logger.getLogger( RPCHandler.class );

    /**
     *  Converts Java string into RPC string.
     */
    private String toRPCString( final String src )
    {
        return TextUtil.urlEncodeUTF8( src );
    }

    /**
     *  Converts RPC string (UTF-8, url encoded) into Java string.
     */
    private String fromRPCString( final String src )
    {
        return TextUtil.urlDecodeUTF8( src );
    }

    /**
     *  Transforms a Java string into UTF-8.
     */
    private byte[] toRPCBase64( final String src )
    {
        return src.getBytes( StandardCharsets.UTF_8 );
    }

    public String getApplicationName() {
        checkPermission( PagePermission.VIEW );
        return toRPCString(m_engine.getApplicationName());
    }

    public Vector< String > getAllPages() {
        checkPermission( PagePermission.VIEW );
        final Collection< WikiPage > pages = m_engine.getPageManager().getRecentChanges();
        final Vector< String > result = new Vector<>();

        for( final WikiPage p : pages ) {
            if( !( p instanceof Attachment ) ) {
                result.add( toRPCString( p.getName() ) );
            }
        }

        return result;
    }

    /**
     *  Encodes a single wiki page info into a Hashtable.
     */
    @Override
    protected Hashtable<String,Object> encodeWikiPage( final WikiPage page ) {
        final Hashtable<String, Object> ht = new Hashtable<>();
        ht.put( "name", toRPCString(page.getName()) );

        final Date d = page.getLastModified();

        //
        //  Here we reset the DST and TIMEZONE offsets of the
        //  calendar.  Unfortunately, I haven't thought of a better
        //  way to ensure that we're getting the proper date
        //  from the XML-RPC thingy, except to manually adjust the date.
        //

        final Calendar cal = Calendar.getInstance();
        cal.setTime( d );
        cal.add( Calendar.MILLISECOND,
                 - (cal.get( Calendar.ZONE_OFFSET ) +
                    (cal.getTimeZone().inDaylightTime( d ) ? cal.get( Calendar.DST_OFFSET ) : 0 ) ) );

        ht.put( "lastModified", cal.getTime() );
        ht.put( "version", page.getVersion() );

        if( page.getAuthor() != null ) {
            ht.put( "author", toRPCString( page.getAuthor() ) );
        }

        return ht;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
            if( page.getLastModified().after( since ) && !(page instanceof Attachment) ) {
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
    private String parsePageCheckCondition( String pagename ) throws XmlRpcException {
        pagename = fromRPCString( pagename );

        if( !m_engine.getPageManager().wikiPageExists(pagename) ) {
            throw new XmlRpcException( ERR_NOPAGE, "No such page '"+pagename+"' found, o master." );
        }

        final WikiPage p = m_engine.getPageManager().getPage( pagename );

        checkPermission( PermissionFactory.getPagePermission( p, PagePermission.VIEW_ACTION ) );

        return pagename;
    }

    public Hashtable getPageInfo( String pagename ) throws XmlRpcException {
        pagename = parsePageCheckCondition( pagename );
        return encodeWikiPage( m_engine.getPageManager().getPage(pagename) );
    }

    public Hashtable getPageInfoVersion( String pagename, int version ) throws XmlRpcException {
        pagename = parsePageCheckCondition( pagename );

        return encodeWikiPage( m_engine.getPageManager().getPage( pagename, version ) );
    }

    public byte[] getPage( final String pagename ) throws XmlRpcException {
        final String text = m_engine.getPageManager().getPureText( parsePageCheckCondition( pagename ), -1 );
        return toRPCBase64( text );
    }

    public byte[] getPageVersion( String pagename, final int version ) throws XmlRpcException {
        pagename = parsePageCheckCondition( pagename );

        return toRPCBase64( m_engine.getPageManager().getPureText( pagename, version ) );
    }

    public byte[] getPageHTML( String pagename ) throws XmlRpcException {
        pagename = parsePageCheckCondition( pagename );

        return toRPCBase64( m_engine.getRenderingManager().getHTML( pagename ) );
    }

    public byte[] getPageHTMLVersion( String pagename, int version ) throws XmlRpcException {
        pagename = parsePageCheckCondition( pagename );

        return toRPCBase64( m_engine.getRenderingManager().getHTML( pagename, version ) );
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

        //
        //  Add local links.
        //
        for( final String link : localCollector.getLinks() ) {
            final Hashtable< String, String > ht = new Hashtable<>();
            ht.put( "page", toRPCString( link ) );
            ht.put( "type", LINK_LOCAL );

            //
            //  FIXME: This is a kludge.  The link format should really be queried
            //  from the TranslatorReader itself.  Also, the link format should probably
            //  have information on whether the page exists or not.
            //

            //
            //  FIXME: The current link collector interface is not very good, since it causes this.
            //

            if( m_engine.getPageManager().wikiPageExists( link ) ) {
                ht.put( "href", context.getURL( WikiContext.VIEW, link ) );
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
            ht.put( "page", toRPCString( link ) );
            ht.put( "type", LINK_LOCAL );
            ht.put( "href", context.getURL( WikiContext.ATTACH, link ) );
            result.add( ht );
        }

        //
        // External links don't need to be changed into XML-RPC strings, simply because URLs are by definition ASCII.
        //
        for( final String link : extCollector.getLinks() ) {
            final Hashtable<String, String> ht = new Hashtable<>();
            ht.put( "page", link );
            ht.put( "type", LINK_EXTERNAL );
            ht.put( "href", link );
            result.add( ht );
        }

        return result;
    }

}
