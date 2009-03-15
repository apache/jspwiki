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
package org.apache.wiki.xmlrpc;

import java.io.UnsupportedEncodingException;
import java.util.*;


import org.apache.wiki.*;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.util.TextUtil;
import org.apache.xmlrpc.XmlRpcException;


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
    private static Logger log = LoggerFactory.getLogger( RPCHandler.class ); 

    /**
     *  {@inheritDoc}
     */
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
        try
        {
            return src.getBytes("UTF-8");
        }
        catch( UnsupportedEncodingException e )
        {
            //
            //  You shouldn't be running JSPWiki on a platform that does not
            //  use UTF-8.  We revert to platform default, so that the other
            //  end might have a chance of getting something.
            //
            log.error("Platform does not support UTF-8, reverting to platform default");
            return src.getBytes();
        }
    }

    public String getApplicationName()
    {
        checkPermission( PagePermission.VIEW );
        return toRPCString(m_engine.getApplicationName());
    }

    public Vector getAllPages()
    {
        checkPermission( PagePermission.VIEW );
        Collection pages = m_engine.getRecentChanges(m_context.getPage().getWiki());
        Vector<String> result = new Vector<String>();

        for( Iterator i = pages.iterator(); i.hasNext(); )
        {
            WikiPage p = (WikiPage) i.next();
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
    protected Hashtable<String,Object> encodeWikiPage( WikiPage page )
    {
        Hashtable<String, Object> ht = new Hashtable<String, Object>();

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

    public Vector getRecentChanges( Date since )
    {
        checkPermission( PagePermission.VIEW );
        Collection pages = m_engine.getRecentChanges(m_context.getPage().getWiki());
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
     * @throws  
     * @throws PageNotFoundException 
     */
    private String parsePageCheckCondition( String pagename )
        throws XmlRpcException
    {
        try
        {
            pagename = fromRPCString( pagename );

            if( !m_engine.pageExists(pagename) )
            {
                throw new XmlRpcException( ERR_NOPAGE, "No such page '"+pagename+"' found, o master." );
            }

            WikiPage p = m_engine.getPage( pagename );
        
            checkPermission( PermissionFactory.getPagePermission( p, PagePermission.VIEW_ACTION ) );
        
            return pagename;
        }
        catch( PageNotFoundException e )
        {
            throw new XmlRpcException(1,"Not found");
        }
        catch( ProviderException e )
        {
            throw new XmlRpcException(1,e.getMessage());            
        }
    }

    private WikiPage getWikiPage( String name, int version ) throws XmlRpcException
    {
        try
        {
            return m_engine.getPage(name,version);
        }
        catch( PageNotFoundException e )
        {
            throw new XmlRpcException(1,"Not found");
        }
        catch( ProviderException e )
        {
            throw new XmlRpcException(2,e.getMessage());
        }
    }
    
    public Hashtable getPageInfo( String pagename )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );
        return encodeWikiPage( getWikiPage(pagename,-1) );
    }

    public Hashtable getPageInfoVersion( String pagename, int version )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        return encodeWikiPage( getWikiPage( pagename, version ) );
    }

    public byte[] getPage( String pagename )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        String text = m_engine.getPureText( pagename, -1 );

        return toRPCBase64( text );
    }

    public byte[] getPageVersion( String pagename, int version )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        return toRPCBase64( m_engine.getPureText( pagename, version ) );
    }

    public byte[] getPageHTML( String pagename )
        throws XmlRpcException    
    {
        pagename = parsePageCheckCondition( pagename );

        try
        {
            return toRPCBase64( m_engine.getHTML( pagename ) );
        }
        catch( ProviderException e )
        {
            throw new XmlRpcException( 1, pagename );
        }
    }

    public byte[] getPageHTMLVersion( String pagename, int version )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        try
        {
            return toRPCBase64( m_engine.getHTML( pagename, version ) );
        }
        catch( ProviderException e )
        {
            throw new XmlRpcException( 1, e.getMessage() );
        }
    }

    public Vector listLinks( String pagename )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        try
        {
            WikiPage page = m_engine.getPage( pagename );
            String pagedata = m_engine.getPureText( page );

            LinkCollector localCollector = new LinkCollector();
            LinkCollector extCollector   = new LinkCollector();
            LinkCollector attCollector   = new LinkCollector();

            WikiContext context = m_engine.getWikiContextFactory().newViewContext( page );
            context.setVariable( WikiEngine.PROP_REFSTYLE, "absolute" );

            m_engine.textToHTML( context,
                                 pagedata,
                                 localCollector,
                                 extCollector,
                                 attCollector );

            Vector<Hashtable<String, String>> result = new Vector<Hashtable<String, String>>();

            //
            //  Add local links.
            //
            for( Iterator i = localCollector.getLinks().iterator(); i.hasNext(); )
            {
                String link = (String) i.next();
                Hashtable<String, String> ht = new Hashtable<String, String>();
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
            for( Iterator i = attCollector.getLinks().iterator(); i.hasNext(); )
            {
                String link = (String) i.next();

                Hashtable<String, String> ht = new Hashtable<String, String>();

                ht.put( "page", toRPCString( link ) );
                ht.put( "type", LINK_LOCAL );
                ht.put( "href", context.getURL(WikiContext.ATTACH,link) );

                result.add( ht );
            }

            //
            // External links don't need to be changed into XML-RPC strings,
            // simply because URLs are by definition ASCII.
            //

            for( Iterator i = extCollector.getLinks().iterator(); i.hasNext(); )
            {
                String link = (String) i.next();
                
                Hashtable<String, String> ht = new Hashtable<String, String>();

                ht.put( "page", link );
                ht.put( "type", LINK_EXTERNAL );
                ht.put( "href", link );

                result.add( ht );
            }

            return result;
        }
        catch( ProviderException e )
        {
            throw new XmlRpcException( 1, e.getMessage() );
        }

    }
}
