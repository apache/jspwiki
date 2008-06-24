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
package com.ecyrd.jspwiki.rpc.atom;

import java.io.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Date;
import java.util.Collection;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.intabulas.sandler.*;
import org.intabulas.sandler.exceptions.*;
import org.intabulas.sandler.elements.*;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.util.*;

import com.ecyrd.jspwiki.plugin.WeblogEntryPlugin;
import com.ecyrd.jspwiki.plugin.WeblogPlugin;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  Handles incoming requests for the Atom API.  This class uses the
 *  "sandler" Atom API implementation.
 *
 *  @since 2.1.97
 */
// FIXME: Rewrite using some other library
public class AtomAPIServlet extends HttpServlet
{
    static final Logger log = Logger.getLogger( AtomAPIServlet.class );

    private static final long serialVersionUID = 0L;

    private WikiEngine       m_engine;

    /**
     *  {@inheritDoc}
     */
    public void init( ServletConfig config )
        throws ServletException
    {
        m_engine = WikiEngine.getInstance( config );
    }

    /**
     *  Takes the name of the page from the request URI.
     *  The initial slash is also removed.  If there is no page,
     *  returns null.
     */
    private String getPageName( HttpServletRequest request )
    {
        String name = request.getPathInfo();

        if( name == null || name.length() <= 1 )
        {
            return null;
        }
        else if( name.charAt(0) == '/' )
        {
            name = name.substring(1);
        }

        name = TextUtil.urlDecodeUTF8( name );

        return name;
    }

    /**
     *  Implements the PostURI of the Atom spec.
     *  <p>
     *  Implementation notes:
     *  <ul>
     *   <li>Only fetches the first content.  All other contents are ignored.
     *   <li>Assumes that incoming code is plain text or WikiMarkup, not html.
     *  </ul>
     *  
     *  @param request {@inheritDoc}
     *  @param response {@inheritDoc}
     *  @throws ServletException {@inheritDoc}
     */
    public void doPost( HttpServletRequest request, HttpServletResponse response )
        throws ServletException
    {
        log.debug("Received POST to AtomAPIServlet");

        try
        {
            String blogid = getPageName( request );

            WikiPage page    = m_engine.getPage( blogid );

            if( page == null )
            {
                throw new ServletException("Page "+blogid+" does not exist, cannot add blog post.");
            }

            //FIXME: Do authentication here
            Entry entry = Sandler.unmarshallEntry( request.getInputStream() );

            //
            //  Fetch the obligatory parts of the content.
            //
            Content title   = entry.getTitle();
            Content content = entry.getContent(0);

            Person  author  = entry.getAuthor();

            //FIXME: Sandler 0.5 does not support generator

            //
            //  Generate new blog entry.
            //
            WeblogEntryPlugin plugin = new WeblogEntryPlugin();

            String pageName = plugin.getNewEntryPage( m_engine, blogid );
            String username = author.getName();

            WikiPage entryPage = new WikiPage( m_engine, pageName );
            entryPage.setAuthor( username );

            WikiContext context = new WikiContext( m_engine, request, entryPage );

            StringBuffer text = new StringBuffer();
            text.append( "!"+title.getBody() );
            text.append( "\n\n" );
            text.append( content.getBody() );

            log.debug("Writing entry: "+text);

            m_engine.saveText( context, text.toString() );

        }
        catch( FeedMarshallException e )
        {
            log.error("Received faulty Atom entry",e);
            throw new ServletException("Faulty Atom entry",e);
        }
        catch( IOException e )
        {
            log.error("I/O exception",e);
            throw new ServletException("Could not get body of request",e);
        }
        catch( WikiException e )
        {
            log.error("Provider exception while posting",e);
            throw new ServletException("JSPWiki cannot save the entry",e);
        }
    }

    /**
     *  Handles HTTP GET.  However, we do not respond to GET requests,
     *  other than to show an explanatory text.
     *  
     *  {@inheritDoc}
     */
    public void doGet( HttpServletRequest request, HttpServletResponse response )
        throws ServletException
    {
        log.debug("Received HTTP GET to AtomAPIServlet");

        String blogid = getPageName( request );

        log.debug("Requested page "+blogid);

        try
        {
            if( blogid == null )
            {
                Feed feed = listBlogs();

                response.setContentType("application/x.atom+xml; charset=UTF-8");
                response.getWriter().println( Sandler.marshallFeed(feed) );

                response.getWriter().flush();
            }
            else
            {
                Entry entry = getBlogEntry( blogid );

                response.setContentType("application/x.atom+xml; charset=UTF-8");
                response.getWriter().println( Sandler.marshallEntry(entry) );

                response.getWriter().flush();
            }
        }
        catch( Exception e )
        {
            log.error("Unable to generate response",e);
            throw new ServletException("Internal problem - whack Janne on the head to get a better error report",e);
        }

    }

    private Entry getBlogEntry( String entryid )
        throws ProviderException
    {
        WikiPage page = m_engine.getPage( entryid );
        WikiPage firstVersion = m_engine.getPage( entryid, 1 );

        Entry entry = SyndicationFactory.newSyndicationEntry();

        String pageText = m_engine.getText(page.getName());
        String title = "";
        int firstLine = pageText.indexOf('\n');

        if( firstLine > 0 )
        {
            title = pageText.substring( 0, firstLine );
        }

        if( title.trim().length() == 0 ) title = page.getName();

        // Remove wiki formatting
        while( title.startsWith("!") ) title = title.substring(1);

        entry.setTitle( title );
        entry.setCreated( firstVersion.getLastModified() );
        entry.setModified( page.getLastModified() );
        entry.setAuthor( SyndicationFactory.createPerson( page.getAuthor(),
                                                          null,
                                                          null ) );

        entry.addContent( SyndicationFactory.createEscapedContent(pageText) );

        return entry;
    }

    /**
     *  Creates and outputs a full list of all available blogs
     */
    private Feed listBlogs()
        throws ProviderException,
               IOException
    {
        Collection pages = m_engine.getPageManager().getAllPages();

        Feed feed = SyndicationFactory.newSyndicationFeed();
        feed.setTitle("List of blogs at this site");
        feed.setModified( new Date() );

        for( Iterator i = pages.iterator(); i.hasNext(); )
        {
            WikiPage p = (WikiPage) i.next();

            //
            //  List only weblogs
            //  FIXME: Unfortunately, a weblog is not known until it has
            //         been executed once, because plugins are off during
            //         the initial startup phase.
            //

            log.debug( p.getName()+" = "+p.getAttribute(WeblogPlugin.ATTR_ISWEBLOG)) ;

            if( !("true".equals(p.getAttribute(WeblogPlugin.ATTR_ISWEBLOG)) ) )
                continue;

            String encodedName = TextUtil.urlEncodeUTF8( p.getName() );

            WikiContext context = new WikiContext( m_engine, p );

            String title = TextUtil.replaceEntities(BlogUtil.getSiteName(context));

            Link postlink = createLink( "service.post",
                                        m_engine.getBaseURL()+"atom/"+encodedName,
                                        title );

            Link editlink = createLink( "service.edit",
                                        m_engine.getBaseURL()+"atom/"+encodedName,
                                        title );

            Link feedlink = createLink( "service.feed",
                                        m_engine.getBaseURL()+"atom.jsp?page="+encodedName,
                                        title );


            feed.addLink( postlink );
            feed.addLink( feedlink );
            feed.addLink( editlink );
        }

        return feed;
    }

    private Link createLink( String rel,
                             String href,
                             String title )
    {
        org.intabulas.sandler.elements.impl.LinkImpl link = new org.intabulas.sandler.elements.impl.LinkImpl();

        link.setRelationship( rel );
        link.setTitle( title );
        link.setType( "application/x.atom+xml" );
        link.setHref( href );

        return link;
    }

    /**
     *  {@inheritDoc}
     */
    public void doDelete( HttpServletRequest request, HttpServletResponse response )
        throws ServletException
    {
        log.debug("Received HTTP DELETE");
    }

    /**
     *  {@inheritDoc}
     */
    public void doPut( HttpServletRequest request, HttpServletResponse response )
        throws ServletException
    {
        log.debug("Received HTTP PUT");
    }
}
