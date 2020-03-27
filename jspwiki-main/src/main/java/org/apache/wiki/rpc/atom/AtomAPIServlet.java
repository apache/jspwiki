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
package org.apache.wiki.rpc.atom;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.plugin.WeblogEntryPlugin;
import org.apache.wiki.plugin.WeblogPlugin;
import org.apache.wiki.util.TextUtil;
import org.intabulas.sandler.Sandler;
import org.intabulas.sandler.SyndicationFactory;
import org.intabulas.sandler.elements.Content;
import org.intabulas.sandler.elements.Entry;
import org.intabulas.sandler.elements.Feed;
import org.intabulas.sandler.elements.Link;
import org.intabulas.sandler.elements.Person;
import org.intabulas.sandler.elements.impl.LinkImpl;
import org.intabulas.sandler.exceptions.FeedMarshallException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;


/**
 *  Handles incoming requests for the Atom API.  This class uses the "sandler" Atom API implementation.
 *
 *  @since 2.1.97
 */
// FIXME: Rewrite using some other library
public class AtomAPIServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger( AtomAPIServlet.class );

    private static final long serialVersionUID = 0L;

    private Engine m_engine;

    /**
     *  {@inheritDoc}
     */
    @Override
    public void init( final ServletConfig config ) throws ServletException {
        m_engine = Wiki.engine().find( config );
    }

    /**
     *  Takes the name of the page from the request URI.
     *  The initial slash is also removed.  If there is no page,
     *  returns null.
     */
    private String getPageName( final HttpServletRequest request ) {
        String name = request.getPathInfo();
        if( name == null || name.length() <= 1 ) {
            return null;
        } else if( name.charAt( 0 ) == '/' ) {
            name = name.substring( 1 );
        }

        return TextUtil.urlDecodeUTF8( name );
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
     *  {@inheritDoc}
     */
    @Override
    public void doPost( final HttpServletRequest request, final HttpServletResponse response ) throws ServletException {
        log.debug( "Received POST to AtomAPIServlet" );

        try {
            final String blogid = getPageName( request );
            final Page page = m_engine.getManager( PageManager.class ).getPage( blogid );
            if( page == null ) {
                throw new ServletException( "Page " + blogid + " does not exist, cannot add blog post." );
            }

            // FIXME: Do authentication here
            final Entry entry = Sandler.unmarshallEntry( request.getInputStream() );

            //  Fetch the obligatory parts of the content.
            final Content title = entry.getTitle();
            final Content content = entry.getContent( 0 );
            final Person author = entry.getAuthor();

            // FIXME: Sandler 0.5 does not support generator
            // Generate new blog entry.
            final WeblogEntryPlugin plugin = new WeblogEntryPlugin();
            final String pageName = plugin.getNewEntryPage( m_engine, blogid );
            final String username = author.getName();
            final Page entryPage = new WikiPage( m_engine, pageName );
            entryPage.setAuthor( username );

            final Context context = Wiki.context().create( m_engine, request, entryPage );
            final StringBuilder text = new StringBuilder();
            text.append( "!" )
                .append( title.getBody() )
                .append( "\n\n" )
                .append( content.getBody() );
            log.debug( "Writing entry: " + text );
            m_engine.getManager( PageManager.class ).saveText( context, text.toString() );
        } catch( final FeedMarshallException e ) {
            log.error("Received faulty Atom entry",e);
            throw new ServletException("Faulty Atom entry",e);
        } catch( final IOException e ) {
            log.error("I/O exception",e);
            throw new ServletException("Could not get body of request",e);
        } catch( final WikiException e ) {
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
    @Override
    public void doGet( final HttpServletRequest request, final HttpServletResponse response ) throws ServletException {
        log.debug( "Received HTTP GET to AtomAPIServlet" );
        final String blogid = getPageName( request );
        log.debug( "Requested page " + blogid );
        try {
            if( blogid == null ) {
                final Feed feed = listBlogs();
                response.setContentType( "application/x.atom+xml; charset=UTF-8" );
                response.getWriter().println( Sandler.marshallFeed( feed ) );
            } else {
                final Entry entry = getBlogEntry( blogid );
                response.setContentType( "application/x.atom+xml; charset=UTF-8" );
                response.getWriter().println( Sandler.marshallEntry( entry ) );
            }
            response.getWriter().flush();
        } catch( final Exception e ) {
            log.error( "Unable to generate response", e );
            throw new ServletException( "Internal problem - whack Janne on the head to get a better error report", e );
        }
    }

    private Entry getBlogEntry( final String entryid ) {
        final Page page = m_engine.getManager( PageManager.class ).getPage( entryid );
        final Page firstVersion = m_engine.getManager( PageManager.class ).getPage( entryid, 1 );
        final Entry entry = SyndicationFactory.newSyndicationEntry();
        final String pageText = m_engine.getManager( PageManager.class ).getText(page.getName());
        final int firstLine = pageText.indexOf('\n');

        String title = "";
        if( firstLine > 0 ) {
            title = pageText.substring( 0, firstLine );
        }

        if( title.trim().length() == 0 ) {
            title = page.getName();
        }

        // Remove wiki formatting
        while( title.startsWith("!") ) {
            title = title.substring(1);
        }

        entry.setTitle( title );
        entry.setCreated( firstVersion.getLastModified() );
        entry.setModified( page.getLastModified() );
        entry.setAuthor( SyndicationFactory.createPerson( page.getAuthor(), null, null ) );
        entry.addContent( SyndicationFactory.createEscapedContent(pageText) );

        return entry;
    }

    /**
     *  Creates and outputs a full list of all available blogs
     */
    private Feed listBlogs() throws ProviderException {
        final Collection< Page > pages = m_engine.getManager( PageManager.class ).getAllPages();
        final Feed feed = SyndicationFactory.newSyndicationFeed();
        feed.setTitle("List of blogs at this site");
        feed.setModified( new Date() );

        for( final Page p : pages ) {
            //  List only weblogs
            //  FIXME: Unfortunately, a weblog is not known until it has een executed once, because plugins are off during the initial startup phase.
            log.debug( p.getName() + " = " + p.getAttribute( WeblogPlugin.ATTR_ISWEBLOG ) );

            if( !( "true".equals( p.getAttribute( WeblogPlugin.ATTR_ISWEBLOG ) ) ) ) {
                continue;
            }

            final String encodedName = TextUtil.urlEncodeUTF8( p.getName() );
            final Context context = Wiki.context().create( m_engine, p );
            final String title = TextUtil.replaceEntities( org.apache.wiki.rss.Feed.getSiteName( context ) );
            final Link postlink = createLink( "service.post", m_engine.getBaseURL() + "atom/" + encodedName, title );
            final Link editlink = createLink( "service.edit", m_engine.getBaseURL() + "atom/" + encodedName, title );
            final Link feedlink = createLink( "service.feed", m_engine.getBaseURL() + "atom.jsp?page=" + encodedName, title );

            feed.addLink( postlink );
            feed.addLink( feedlink );
            feed.addLink( editlink );
        }

        return feed;
    }

    private Link createLink( final String rel, final String href, final String title ) {
        final LinkImpl link = new LinkImpl();
        link.setRelationship( rel );
        link.setTitle( title );
        link.setType( "application/x.atom+xml" );
        link.setHref( href );

        return link;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void doDelete( final HttpServletRequest request, final HttpServletResponse response ) {
        log.debug( "Received HTTP DELETE" );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void doPut( final HttpServletRequest request, final HttpServletResponse response ) {
        log.debug( "Received HTTP PUT" );
    }

}
