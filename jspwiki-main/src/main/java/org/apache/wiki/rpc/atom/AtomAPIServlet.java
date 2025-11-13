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

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.feed.synd.SyndLink;
import com.rometools.rome.feed.synd.SyndLinkImpl;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.SyndFeedOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.apache.wiki.util.HttpUtil;


/**
 *  Handles incoming requests for the Atom API.  This class uses the "sandler" Atom API implementation.
 *
 *  @since 2.1.97
 */
// FIXME: Rewrite using some other library
public class AtomAPIServlet extends HttpServlet {

    private static final Logger LOG = LogManager.getLogger( AtomAPIServlet.class );

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
        LOG.debug( "Received POST to AtomAPIServlet" );

        try {
            final String blogid = getPageName( request );
            final Page page = m_engine.getManager( PageManager.class ).getPage( blogid );
            if( page == null ) {
                throw new ServletException( "Page " + blogid + " does not exist, cannot add blog post." );
            }

            // FIXME: Do authentication here
            SyndFeed entry = new SyndFeedInput().build(new InputStreamReader(request.getInputStream() ));
            

            //  Fetch the obligatory parts of the content.
            final String title = entry.getTitle();
            final SyndEntry content = entry.getEntries().get( 0 );
            final String author = entry.getAuthor();

            // FIXME: Sandler 0.5 does not support generator
            // Generate new blog entry.
            final WeblogEntryPlugin plugin = new WeblogEntryPlugin();
            final String pageName = plugin.getNewEntryPage( m_engine, blogid );
            final String username = author;
            final Page entryPage = Wiki.contents().page( m_engine, pageName );
            entryPage.setAuthor( username );

            final Context context = Wiki.context().create( m_engine, request, entryPage );
            final StringBuilder text = new StringBuilder();
            text.append( "!" )
                .append( title )
                .append( "\n\n" )
                .append( content );
            LOG.debug( "Writing entry: " + text );
            m_engine.getManager( PageManager.class ).saveText( context, text.toString() );
        } catch( final IOException e ) {
            LOG.error("I/O exception",e);
            throw new ServletException("Could not get body of request");
        } catch( final WikiException e ) {
            LOG.error("Provider exception while posting",e);
            throw new ServletException("JSPWiki cannot save the entry");
        } catch( final Exception e ) {
            LOG.error("Received faulty Atom entry",e);
            throw new ServletException("Faulty Atom entry");
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
        LOG.debug( "Received HTTP GET to AtomAPIServlet" );
        final String blogid = getPageName( request );
        LOG.debug( "Requested page " + blogid );
        try {
            if( blogid == null ) {
                final  SyndFeed feed = listBlogs(request);
                response.setContentType( "application/x.atom+xml; charset=UTF-8" );
                response.setHeader("Content-Disposition", " attachment; filename=\"atom.xml\"");
                feed.setFeedType("atom_0.3");
                SyndFeedOutput output = new SyndFeedOutput();
                output.output(feed,response.getWriter());
            } else {
                final SyndEntry entry = getBlogEntry( blogid );
                response.setContentType( "application/x.atom+xml; charset=UTF-8" );
                response.setHeader("Content-Disposition", " attachment; filename=\"atom.xml\"");
                SyndFeed atom = new SyndFeedImpl();
                atom.setFeedType("atom_0.3");
                atom.getEntries().add(entry);
                SyndFeedOutput output = new SyndFeedOutput();
                output.output(atom,response.getWriter());
            }
            response.getWriter().flush();
        } catch( final Exception e ) {
            LOG.error( "Unable to generate response", e );
            throw new ServletException( "Internal problem - whack Janne on the head to get a better error report");
        }
    }

    private SyndEntry getBlogEntry( final String entryid ) {
        final Page page = m_engine.getManager( PageManager.class ).getPage( entryid );
        final Page firstVersion = m_engine.getManager( PageManager.class ).getPage( entryid, 1 );
        final SyndEntry entry = new SyndEntryImpl();
        final String pageText = m_engine.getManager( PageManager.class ).getText(page.getName());
        final int firstLine = pageText.indexOf('\n');

        String title = "";
        if( firstLine > 0 ) {
            title = pageText.substring( 0, firstLine );
        }

        if( title.trim().isEmpty() ) {
            title = page.getName();
        }

        // Remove wiki formatting
        while( title.startsWith("!") ) {
            title = title.substring(1);
        }

        entry.setTitle( title );
        entry.setPublishedDate(firstVersion.getLastModified() );
        entry.setUpdatedDate(page.getLastModified() );
        entry.setAuthor( page.getAuthor());
        List<SyndContent> list  = new ArrayList<>();
        list.add(new SyndContentImpl());
        list.get(0).setValue(pageText);

        return entry;
    }

    /**
     *  Creates and outputs a full list of all available blogs
     */
    private SyndFeed listBlogs( final HttpServletRequest request ) throws ProviderException{
        final Collection< Page > pages = m_engine.getManager( PageManager.class ).getAllPages();
        final SyndFeed feed = new SyndFeedImpl();
        feed.setTitle("List of blogs at this site");
        feed.setPublishedDate( new Date() );

        for( final Page p : pages ) {
            //  List only weblogs
            //  FIXME: Unfortunately, a weblog is not known until it has een executed once, because plugins are off during the initial startup phase.
            LOG.debug( p.getName() + " = " + p.getAttribute( WeblogPlugin.ATTR_ISWEBLOG ) );
            
            if( !( "true".equals( p.getAttribute( WeblogPlugin.ATTR_ISWEBLOG ) ) ) &&
                    !( "true".equals( p.getAttribute( "@" + WeblogPlugin.ATTR_ISWEBLOG ) ) )) {
                continue;
            }

            final String encodedName = TextUtil.urlEncodeUTF8( p.getName() );
            final Context context = Wiki.context().create( m_engine, p );
            final String title = TextUtil.replaceEntities( org.apache.wiki.rss.Feed.getSiteName( context ) );
            //FIXME this needs to be an absolute URL not a relative one
            final SyndLink postlink = createLink( "service.post", HttpUtil.getAbsoluteUrl(request, m_engine.getBaseURL()) + "/atom/" + encodedName, title );
            final SyndLink editlink = createLink( "service.edit", HttpUtil.getAbsoluteUrl(request, m_engine.getBaseURL()) + "/atom/" + encodedName, title );
            final SyndLink feedlink = createLink( "service.feed", HttpUtil.getAbsoluteUrl(request, m_engine.getBaseURL()) + "/rss.jsp?page=" + encodedName, title );

            feed.getLinks().add( postlink );
            feed.getLinks().add( feedlink );
            feed.getLinks().add( editlink );
        }

        return feed;
    }

    private SyndLink createLink( final String rel, final String href, final String title ) {
        final SyndLink link = new SyndLinkImpl();
        link.setRel( rel );
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
        LOG.debug( "Received HTTP DELETE" );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void doPut( final HttpServletRequest request, final HttpServletResponse response ) {
        LOG.debug( "Received HTTP PUT" );
    }

}
