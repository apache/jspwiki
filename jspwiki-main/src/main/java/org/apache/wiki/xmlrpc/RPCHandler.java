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
import org.apache.wiki.WikiEngine;
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
import java.util.Iterator;
import java.util.Vector;

/**
 *  Provides handlers for all RPC routines.
 *
 *  @since 1.6.6
 */
// We could use WikiEngine directly, but because of introspection it would
// show just too many methods to be safe.
public class RPCHandler
    extends AbstractRPCHandler
{
    private static Logger log = Logger.getLogger( RPCHandler.class );

    /**
     *  {@inheritDoc}
     */
    @Override
    public void initialize( WikiContext ctx )
    {
        super.initialize( ctx );
    }

    /**
     *  Converts Java string into RPC string.
     */
    private String toRPCString( String src )
    {
        return TextUtil.urlEncodeUTF8( src );
    }

    /**
     *  Converts RPC string (UTF-8, url encoded) into Java string.
     */
    private String fromRPCString( String src )
    {
        return TextUtil.urlDecodeUTF8( src );
    }

    /**
     *  Transforms a Java string into UTF-8.
     */
    private byte[] toRPCBase64( String src )
    {
        return src.getBytes( StandardCharsets.UTF_8 );
    }

    public String getApplicationName()
    {
        checkPermission( PagePermission.VIEW );
        return toRPCString(m_engine.getApplicationName());
    }

    public Vector getAllPages()
    {
        checkPermission( PagePermission.VIEW );
        Collection< WikiPage > pages = m_engine.getRecentChanges();
        Vector<String> result = new Vector<>();

        for( WikiPage p : pages )
        {
            if( !(p instanceof Attachment) )
            {
                result.add( toRPCString(p.getName()) );
            }
        }

        return result;
    }

    /**
     *  Encodes a single wiki page info into a Hashtable.
     */
    @Override
    protected Hashtable<String,Object> encodeWikiPage( WikiPage page )
    {
        Hashtable<String, Object> ht = new Hashtable<>();

        ht.put( "name", toRPCString(page.getName()) );

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
            ht.put( "author", toRPCString(page.getAuthor()) );
        }

        return ht;
    }

    @Override
    public Vector getRecentChanges( Date since )
    {
        checkPermission( PagePermission.VIEW );
        Collection< WikiPage > pages = m_engine.getRecentChanges();
        Vector<Hashtable<String, Object>> result = new Vector<>();

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
        pagename = fromRPCString( pagename );

        if( !m_engine.pageExists(pagename) )
        {
            throw new XmlRpcException( ERR_NOPAGE, "No such page '"+pagename+"' found, o master." );
        }

        WikiPage p = m_engine.getPageManager().getPage( pagename );

        checkPermission( PermissionFactory.getPagePermission( p, PagePermission.VIEW_ACTION ) );

        return pagename;
    }

    public Hashtable getPageInfo( String pagename )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );
        return encodeWikiPage( m_engine.getPageManager().getPage(pagename) );
    }

    public Hashtable getPageInfoVersion( String pagename, int version )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        return encodeWikiPage( m_engine.getPageManager().getPage( pagename, version ) );
    }

    public byte[] getPage( final String pagename ) throws XmlRpcException {
        final String text = m_engine.getPageManager().getPureText( parsePageCheckCondition( pagename ), -1 );
        return toRPCBase64( text );
    }

    public byte[] getPageVersion( String pagename, int version )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        return toRPCBase64( m_engine.getPageManager().getPureText( pagename, version ) );
    }

    public byte[] getPageHTML( String pagename )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        return toRPCBase64( m_engine.getHTML( pagename ) );
    }

    public byte[] getPageHTMLVersion( String pagename, int version )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        return toRPCBase64( m_engine.getHTML( pagename, version ) );
    }

    public Vector listLinks( String pagename )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        WikiPage page = m_engine.getPageManager().getPage( pagename );
        String pagedata = m_engine.getPageManager().getPureText( page );

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

        Vector<Hashtable<String, String>> result = new Vector<>();

        //
        //  Add local links.
        //
        for( Iterator< String > i = localCollector.getLinks().iterator(); i.hasNext(); )
        {
            String link = i.next();
            Hashtable< String, String > ht = new Hashtable<>();
            ht.put( "page", toRPCString( link ) );
            ht.put( "type", LINK_LOCAL );

            //
            //  FIXME: This is a kludge.  The link format should really be queried
            //  from the TranslatorReader itself.  Also, the link format should probably
            //  have information on whether the page exists or not.
            //

            //
            //  FIXME: The current link collector interface is not very good, since
            //  it causes this.
            //

            if( m_engine.pageExists(link) )
            {
                ht.put( "href", context.getURL(WikiContext.VIEW,link) );
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
        for( Iterator< String > i = attCollector.getLinks().iterator(); i.hasNext(); )
        {
            String link = i.next();

            Hashtable< String, String > ht = new Hashtable< >();

            ht.put( "page", toRPCString( link ) );
            ht.put( "type", LINK_LOCAL );
            ht.put( "href", context.getURL( WikiContext.ATTACH, link ) );

            result.add( ht );
        }

        //
        // External links don't need to be changed into XML-RPC strings,
        // simply because URLs are by definition ASCII.
        //

        for( Iterator< String > i = extCollector.getLinks().iterator(); i.hasNext(); )
        {
            String link = i.next();

            Hashtable< String, String > ht = new Hashtable< >();

            ht.put( "page", link );
            ht.put( "type", LINK_EXTERNAL );
            ht.put( "href", link );

            result.add( ht );
        }

        return result;
    }
}
