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
package org.apache.wiki.rss;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.providers.WikiProvider;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.diff.DifferenceManager;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.pages.PageTimeComparator;
import org.apache.wiki.render.RenderingManager;
import org.apache.wiki.util.TextUtil;
import org.apache.wiki.variables.VariableManager;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;


/**
 * Default implementation for {@link RSSGenerator}.
 *
 * {@inheritDoc}
 */
// FIXME: Limit diff and page content size.
public class DefaultRSSGenerator implements RSSGenerator {

    private static final Logger log = Logger.getLogger( DefaultRSSGenerator.class );
    private Engine m_engine;

    /** The RSS file to generate. */
    private String m_rssFile;
    private String m_channelDescription = "";
    private String m_channelLanguage = "en-us";
    private boolean m_enabled = true;

    private static final int MAX_CHARACTERS = Integer.MAX_VALUE-1;

    /**
     *  Builds the RSS generator for a given Engine.
     *
     *  @param engine The Engine.
     *  @param properties The properties.
     */
    public DefaultRSSGenerator( final Engine engine, final Properties properties ) {
        m_engine = engine;
        m_channelDescription = properties.getProperty( PROP_CHANNEL_DESCRIPTION, m_channelDescription );
        m_channelLanguage = properties.getProperty( PROP_CHANNEL_LANGUAGE, m_channelLanguage );
        m_rssFile = TextUtil.getStringProperty( properties, DefaultRSSGenerator.PROP_RSSFILE, "rss.rdf" );
    }

    /**
     * {@inheritDoc}
     *
     * Start the RSS generator & generator thread
     */
    @Override
    public void initialize( final Engine engine, final Properties properties ) {
        final File rssFile;
        if( m_rssFile.startsWith( File.separator ) ) { // honor absolute pathnames
            rssFile = new File( m_rssFile );
        } else { // relative path names are anchored from the webapp root path
            rssFile = new File( engine.getRootPath(), m_rssFile );
        }
        final int rssInterval = TextUtil.getIntegerProperty( properties, DefaultRSSGenerator.PROP_INTERVAL, 3600 );
        final RSSThread rssThread = new RSSThread( engine, rssFile, rssInterval );
        rssThread.start();
    }

    private String getAuthor( final Page page ) {
        String author = page.getAuthor();
        if( author == null ) {
            author = "An unknown author";
        }

        return author;
    }

    private String getAttachmentDescription( final Attachment att ) {
        final String author = getAuthor( att );
        final StringBuilder sb = new StringBuilder();

        if( att.getVersion() != 1 ) {
            sb.append( author ).append( " uploaded a new version of this attachment on " ).append( att.getLastModified() );
        } else {
            sb.append( author ).append( " created this attachment on " ).append( att.getLastModified() );
        }

        sb.append( "<br /><hr /><br />" )
          .append( "Parent page: <a href=\"" )
          .append( m_engine.getURL( WikiContext.VIEW, att.getParentName(), null ) )
          .append( "\">" ).append( att.getParentName() ).append( "</a><br />" )
          .append( "Info page: <a href=\"" )
          .append( m_engine.getURL( WikiContext.INFO, att.getName(), null ) )
          .append( "\">" ).append( att.getName() ).append( "</a>" );

        return sb.toString();
    }

    private String getPageDescription( final Page page ) {
        final StringBuilder buf = new StringBuilder();
        final String author = getAuthor( page );
        final WikiContext ctx = new WikiContext( m_engine, page );
        if( page.getVersion() > 1 ) {
            final String diff = m_engine.getManager( DifferenceManager.class ).getDiff( ctx,
                                                                page.getVersion() - 1, // FIXME: Will fail when non-contiguous versions
                                                                         page.getVersion() );

            buf.append( author ).append( " changed this page on " ).append( page.getLastModified() ).append( ":<br /><hr /><br />" );
            buf.append( diff );
        } else {
            buf.append( author ).append( " created this page on " ).append( page.getLastModified() ).append( ":<br /><hr /><br />" );
            buf.append( m_engine.getManager( RenderingManager.class ).getHTML( page.getName() ) );
        }

        return buf.toString();
    }

    private String getEntryDescription( final Page page ) {
        final String res;
        if( page instanceof Attachment ) {
            res = getAttachmentDescription( (Attachment)page );
        } else {
            res = getPageDescription( page );
        }

        return res;
    }

    // FIXME: This should probably return something more intelligent
    private String getEntryTitle( final Page page ) {
        return page.getName() + ", version " + page.getVersion();
    }

    /** {@inheritDoc} */
    @Override
    public String generate() {
        final WikiContext context = new WikiContext( m_engine, new WikiPage( m_engine, "__DUMMY" ) );
        context.setRequestContext( WikiContext.RSS );
        final Feed feed = new RSS10Feed( context );
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + generateFullWikiRSS( context, feed );
    }

    /** {@inheritDoc} */
    @Override
    public String generateFeed( final Context wikiContext, final List< Page > changed, final String mode, final String type ) throws IllegalArgumentException {
        final Feed feed;
        final String res;

        if( ATOM.equals(type) ) {
            feed = new AtomFeed( wikiContext );
        } else if( RSS20.equals( type ) ) {
            feed = new RSS20Feed( wikiContext );
        } else {
            feed = new RSS10Feed( wikiContext );
        }

        feed.setMode( mode );

        if( MODE_BLOG.equals( mode ) ) {
            res = generateBlogRSS( wikiContext, changed, feed );
        } else if( MODE_FULL.equals(mode) ) {
            res = generateFullWikiRSS( wikiContext, feed );
        } else if( MODE_WIKI.equals(mode) ) {
            res = generateWikiPageRSS( wikiContext, changed, feed );
        } else {
            throw new IllegalArgumentException( "Invalid value for feed mode: "+mode );
        }

        return res;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEnabled() {
        return m_enabled;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void setEnabled( final boolean enabled ) {
        m_enabled = enabled;
    }

    /** {@inheritDoc} */
    @Override
    public String getRssFile() {
        return m_rssFile;
    }

    /** {@inheritDoc} */
    @Override
    public String generateFullWikiRSS( final Context wikiContext, final Feed feed ) {
        feed.setChannelTitle( m_engine.getApplicationName() );
        feed.setFeedURL( m_engine.getBaseURL() );
        feed.setChannelLanguage( m_channelLanguage );
        feed.setChannelDescription( m_channelDescription );

        final Set< Page > changed = m_engine.getManager( PageManager.class ).getRecentChanges();

        final Session session = WikiSession.guestSession( m_engine );
        int items = 0;
        for( final Iterator< Page > i = changed.iterator(); i.hasNext() && items < 15; items++ ) {
            final Page page = i.next();

            //  Check if the anonymous user has view access to this page.
            if( !m_engine.getManager( AuthorizationManager.class ).checkPermission(session, new PagePermission(page,PagePermission.VIEW_ACTION) ) ) {
                // No permission, skip to the next one.
                continue;
            }

            final String url;
            if( page instanceof Attachment ) {
                url = m_engine.getURL( WikiContext.ATTACH, page.getName(),null );
            } else {
                url = m_engine.getURL( WikiContext.VIEW, page.getName(), null );
            }

            final Entry e = new Entry();
            e.setPage( page );
            e.setURL( url );
            e.setTitle( page.getName() );
            e.setContent( getEntryDescription(page) );
            e.setAuthor( getAuthor(page) );

            feed.addEntry( e );
        }

        return feed.getString();
    }

    /** {@inheritDoc} */
    @Override
    public String generateWikiPageRSS( final Context wikiContext, final List< Page > changed, final Feed feed ) {
        feed.setChannelTitle( m_engine.getApplicationName()+": "+wikiContext.getPage().getName() );
        feed.setFeedURL( wikiContext.getViewURL( wikiContext.getPage().getName() ) );
        final String language = m_engine.getManager( VariableManager.class ).getVariable( wikiContext, PROP_CHANNEL_LANGUAGE );

        if( language != null ) {
            feed.setChannelLanguage( language );
        } else {
            feed.setChannelLanguage( m_channelLanguage );
        }
        final String channelDescription = m_engine.getManager( VariableManager.class ).getVariable( wikiContext, PROP_CHANNEL_DESCRIPTION );

        if( channelDescription != null ) {
            feed.setChannelDescription( channelDescription );
        }

        changed.sort( new PageTimeComparator() );

        int items = 0;
        for( final Iterator< Page > i = changed.iterator(); i.hasNext() && items < 15; items++ ) {
            final Page page = i.next();
            final Entry e = new Entry();
            e.setPage( page );
            String url;

            if( page instanceof Attachment ) {
                url = m_engine.getURL( WikiContext.ATTACH, page.getName(), "version=" + page.getVersion() );
            } else {
                url = m_engine.getURL( WikiContext.VIEW, page.getName(), "version=" + page.getVersion() );
            }

            // Unfortunately, this is needed because the code will again go through replacement conversion
            url = TextUtil.replaceString( url, "&amp;", "&" );
            e.setURL( url );
            e.setTitle( getEntryTitle(page) );
            e.setContent( getEntryDescription(page) );
            e.setAuthor( getAuthor(page) );

            feed.addEntry( e );
        }

        return feed.getString();
    }


    /** {@inheritDoc} */
    @Override
    public String generateBlogRSS( final Context wikiContext, final List< Page > changed, final Feed feed ) {
        if( log.isDebugEnabled() ) {
            log.debug( "Generating RSS for blog, size=" + changed.size() );
        }

        final String ctitle = m_engine.getManager( VariableManager.class ).getVariable( wikiContext, PROP_CHANNEL_TITLE );
        if( ctitle != null ) {
            feed.setChannelTitle( ctitle );
        } else {
            feed.setChannelTitle( m_engine.getApplicationName() + ":" + wikiContext.getPage().getName() );
        }

        feed.setFeedURL( wikiContext.getViewURL( wikiContext.getPage().getName() ) );

        final String language = m_engine.getManager( VariableManager.class ).getVariable( wikiContext, PROP_CHANNEL_LANGUAGE );
        if( language != null ) {
            feed.setChannelLanguage( language );
        } else {
            feed.setChannelLanguage( m_channelLanguage );
        }

        final String channelDescription = m_engine.getManager( VariableManager.class ).getVariable( wikiContext, PROP_CHANNEL_DESCRIPTION );
        if( channelDescription != null ) {
            feed.setChannelDescription( channelDescription );
        }

        changed.sort( new PageTimeComparator() );

        int items = 0;
        for( final Iterator< Page > i = changed.iterator(); i.hasNext() && items < 15; items++ ) {
            final Page page = i.next();
            final Entry e = new Entry();
            e.setPage( page );
            final String url;

            if( page instanceof Attachment ) {
                url = m_engine.getURL( WikiContext.ATTACH, page.getName(),null );
            } else {
                url = m_engine.getURL( WikiContext.VIEW, page.getName(),null );
            }

            e.setURL( url );

            //  Title
            String pageText = m_engine.getManager( PageManager.class ).getPureText( page.getName(), WikiProvider.LATEST_VERSION );

            String title = "";
            final int firstLine = pageText.indexOf('\n');

            if( firstLine > 0 ) {
                title = pageText.substring( 0, firstLine ).trim();
            }

            if( title.length() == 0 ) {
                title = page.getName();
            }

            // Remove wiki formatting
            while( title.startsWith("!") ) {
                title = title.substring(1);
            }

            e.setTitle( title );

            //  Description
            if( firstLine > 0 ) {
                int maxlen = pageText.length();
                if( maxlen > MAX_CHARACTERS ) {
                    maxlen = MAX_CHARACTERS;
                }
                pageText = m_engine.getManager( RenderingManager.class ).textToHTML( wikiContext, pageText.substring( firstLine + 1, maxlen ).trim() );
                if( maxlen == MAX_CHARACTERS ) {
                    pageText += "...";
                }
                e.setContent( pageText );
            } else {
                e.setContent( title );
            }
            e.setAuthor( getAuthor(page) );
            feed.addEntry( e );
        }

        return feed.getString();
    }

}
