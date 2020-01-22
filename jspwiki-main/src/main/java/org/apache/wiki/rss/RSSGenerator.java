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
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.WikiSession;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.pages.PageTimeComparator;
import org.apache.wiki.util.TextUtil;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 *  The master class for generating different kinds of Feeds (including RSS1.0, 2.0 and Atom).
 *  <p>
 *  This class can produce quite a few different styles of feeds.  The following modes are available:
 *  
 *  <ul>
 *  <li><b>wiki</b> - All the changes to the given page are enumerated and announced as diffs.</li>
 *  <li><b>full</b> - Each page is only considered once.  This produces a very RecentChanges-style feed,
 *                   where each page is only listed once, even if it has changed multiple times.</li>
 *  <li><b>blog</b> - Each page change is assumed to be a blog entry, so no diffs are produced, but
 *                    the page content is always completely in the entry in rendered HTML.</li>
 *
 *  @since  1.7.5.
 */
// FIXME: Limit diff and page content size.
// FIXME3.0: This class would need a bit of refactoring.  Method names, e.g. are confusing.
public class RSSGenerator {

    private static final Logger log = Logger.getLogger( RSSGenerator.class );
    private WikiEngine m_engine;

    private String m_channelDescription = "";
    private String m_channelLanguage = "en-us";
    private boolean m_enabled = true;

    /** Parameter value to represent RSS 1.0 feeds.  Value is <tt>{@value}</tt>. */
    public static final String RSS10 = "rss10";

    /** Parameter value to represent RSS 2.0 feeds.  Value is <tt>{@value}</tt>. */
    public static final String RSS20 = "rss20";
    
    /** Parameter value to represent Atom feeds.  Value is <tt>{@value}</tt>.  */
    public static final String ATOM  = "atom";

    /** Parameter value to represent a 'blog' style feed. Value is <tt>{@value}</tt>. */
    public static final String MODE_BLOG = "blog";
    
    /** Parameter value to represent a 'wiki' style feed. Value is <tt>{@value}</tt>. */
    public static final String MODE_WIKI = "wiki";

    /** Parameter value to represent a 'full' style feed. Value is <tt>{@value}</tt>. */
    public static final String MODE_FULL = "full";

    /**
     *  Defines the property name for the RSS channel description.  Default value for the channel description is an empty string.
     *  @since 1.7.6.
     */
    public static final String PROP_CHANNEL_DESCRIPTION = "jspwiki.rss.channelDescription";

    /**
     *  Defines the property name for the RSS channel language.  Default value for the language is "en-us".
     *  @since 1.7.6.
     */
    public static final String PROP_CHANNEL_LANGUAGE = "jspwiki.rss.channelLanguage";

    /** Defines the property name for the RSS channel title.  Value is <tt>{@value}</tt>. */
    public static final String PROP_CHANNEL_TITLE = "jspwiki.rss.channelTitle";

    /**
     *  Defines the property name for the RSS generator main switch.
     *  @since 1.7.6.
     */
    public static final String PROP_GENERATE_RSS = "jspwiki.rss.generate";

    /**
     *  Defines the property name for the RSS file that the wiki should generate.
     *  @since 1.7.6.
     */
    public static final String PROP_RSSFILE = "jspwiki.rss.fileName";

    /**
     *  Defines the property name for the RSS generation interval in seconds.
     *  @since 1.7.6.
     */
    public static final String PROP_INTERVAL = "jspwiki.rss.interval";

    /** Defines the property name for the RSS author.  Value is <tt>{@value}</tt>. */
    public static final String PROP_RSS_AUTHOR = "jspwiki.rss.author";

    /** Defines the property name for the RSS author email.  Value is <tt>{@value}</tt>. */
    public static final String PROP_RSS_AUTHOREMAIL = "jspwiki.rss.author.email";

    private static final int MAX_CHARACTERS = Integer.MAX_VALUE-1;

    /**
     *  Initialize the RSS generator for a given WikiEngine.
     *
     *  @param engine The WikiEngine.
     *  @param properties The properties.
     */
    public RSSGenerator( final WikiEngine engine, final Properties properties ) {
        m_engine = engine;
        m_channelDescription = properties.getProperty( PROP_CHANNEL_DESCRIPTION, m_channelDescription );
        m_channelLanguage = properties.getProperty( PROP_CHANNEL_LANGUAGE, m_channelLanguage );
    }

    /**
     *  Does the required formatting and entity replacement for XML.
     *  
     *  @param s String to format.
     *  @return A formatted string.
     */
    // FIXME: Replicates Feed.format().
    public static String format( String s ) {
        s = TextUtil.replaceString( s, "&", "&amp;" );
        s = TextUtil.replaceString( s, "<", "&lt;" );
        s = TextUtil.replaceString( s, "]]>", "]]&gt;" );

        return s.trim();
    }

    private String getAuthor( final WikiPage page ) {
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

    private String getPageDescription( final WikiPage page ) {
        final StringBuilder buf = new StringBuilder();
        final String author = getAuthor( page );
        final WikiContext ctx = new WikiContext( m_engine, page );
        if( page.getVersion() > 1 ) {
            final String diff = m_engine.getDifferenceManager().getDiff( ctx,
                                                                page.getVersion() - 1, // FIXME: Will fail when non-contiguous versions
                                                                         page.getVersion() );

            buf.append( author ).append( " changed this page on " ).append( page.getLastModified() ).append( ":<br /><hr /><br />" );
            buf.append( diff );
        } else {
            buf.append( author ).append( " created this page on " ).append( page.getLastModified() ).append( ":<br /><hr /><br />" );
            buf.append( m_engine.getRenderingManager().getHTML( page.getName() ) );
        }

        return buf.toString();
    }

    private String getEntryDescription( final WikiPage page ) {
        final String res;
        if( page instanceof Attachment ) {
            res = getAttachmentDescription( (Attachment)page );
        } else {
            res = getPageDescription( page );
        }

        return res;
    }

    // FIXME: This should probably return something more intelligent
    private String getEntryTitle( final WikiPage page )
    {
        return page.getName() + ", version " + page.getVersion();
    }

    /**
     *  Generates the RSS resource.  You probably want to output this result into a file or something, or serve as output from a servlet.
     *  
     *  @return A RSS 1.0 feed in the "full" mode.
     */
    public String generate() {
        final WikiContext context = new WikiContext( m_engine, new WikiPage( m_engine, "__DUMMY" ) );
        context.setRequestContext( WikiContext.RSS );
        final Feed feed = new RSS10Feed( context );
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + generateFullWikiRSS( context, feed );
    }

    /**
     * Returns the content type of this RSS feed.
     *
     * @since 2.3.15
     * @param mode the RSS mode: {@link #RSS10}, {@link #RSS20} or {@link #ATOM}.
     * @return the content type
     */
    public static String getContentType( final String mode ) {
        if( mode.equals( RSS10 ) || mode.equals( RSS20 ) ) {
            return "application/rss+xml";
        } else if( mode.equals( ATOM ) ) {
            return "application/atom+xml";
        }

        return "application/octet-stream"; // Unknown type
    }

    /**
     * Generates a feed based on a context and list of changes.
     *
     * @param wikiContext The WikiContext
     * @param changed A list of Entry objects
     * @param mode The mode (wiki/blog)
     * @param type The type (RSS10, RSS20, ATOM).  Default is RSS 1.0
     * @return Fully formed XML.
     * @throws IllegalArgumentException If an illegal mode is given.
     */
    public String generateFeed( final WikiContext wikiContext, final List< WikiPage > changed, final String mode, final String type ) throws IllegalArgumentException {
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

    /**
     * Returns <code>true</code> if RSS generation is enabled.
     *
     * @return whether RSS generation is currently enabled
     */
    public boolean isEnabled()
    {
        return m_enabled;
    }

    /**
     * Turns RSS generation on or off. This setting is used to set the "enabled" flag only for use by callers, and does not
     * actually affect whether the {@link #generate()} or {@link #generateFeed(WikiContext, List, String, String)} methods output anything.
     *
     * @param enabled whether RSS generation is considered enabled.
     */
    public synchronized void setEnabled( final boolean enabled )
    {
        m_enabled = enabled;
    }

    /**
     *  Generates an RSS feed for the entire wiki.  Each item should be an instance of the RSSItem class.
     *  
     *  @param wikiContext A WikiContext
     *  @param feed A Feed to generate the feed to.
     *  @return feed.getString().
     */
    protected String generateFullWikiRSS( final WikiContext wikiContext, final Feed feed ) {
        feed.setChannelTitle( m_engine.getApplicationName() );
        feed.setFeedURL( m_engine.getBaseURL() );
        feed.setChannelLanguage( m_channelLanguage );
        feed.setChannelDescription( m_channelDescription );

        final Set< WikiPage > changed = m_engine.getPageManager().getRecentChanges();

        final WikiSession session = WikiSession.guestSession( m_engine );
        int items = 0;
        for( final Iterator< WikiPage > i = changed.iterator(); i.hasNext() && items < 15; items++ ) {
            final WikiPage page = i.next();

            //  Check if the anonymous user has view access to this page.
            if( !m_engine.getAuthorizationManager().checkPermission(session, new PagePermission(page,PagePermission.VIEW_ACTION) ) ) {
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

    /**
     * Create RSS/Atom as if this page was a wikipage (in contrast to Blog mode).
     *
     * @param wikiContext The WikiContext
     * @param changed A List of changed WikiPages.
     * @param feed A Feed object to fill.
     * @return the RSS representation of the wiki context
     */
    protected String generateWikiPageRSS( final WikiContext wikiContext, final List< WikiPage > changed, final Feed feed ) {
        feed.setChannelTitle( m_engine.getApplicationName()+": "+wikiContext.getPage().getName() );
        feed.setFeedURL( wikiContext.getViewURL( wikiContext.getPage().getName() ) );
        final String language = m_engine.getVariableManager().getVariable( wikiContext, PROP_CHANNEL_LANGUAGE );

        if( language != null ) {
            feed.setChannelLanguage( language );
        } else {
            feed.setChannelLanguage( m_channelLanguage );
        }
        final String channelDescription = m_engine.getVariableManager().getVariable( wikiContext, PROP_CHANNEL_DESCRIPTION );

        if( channelDescription != null ) {
            feed.setChannelDescription( channelDescription );
        }

        changed.sort( new PageTimeComparator() );

        int items = 0;
        for( final Iterator< WikiPage > i = changed.iterator(); i.hasNext() && items < 15; items++ ) {
            final WikiPage page = i.next();
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


    /**
     *  Creates RSS from modifications as if this page was a blog (using the WeblogPlugin).
     *
     *  @param wikiContext The WikiContext, as usual.
     *  @param changed A list of the changed pages.
     *  @param feed A valid Feed object.  The feed will be used to create the RSS/Atom, depending on which kind of an object you want to put in it.
     *  @return A String of valid RSS or Atom.
     */
    protected String generateBlogRSS( final WikiContext wikiContext, final List< WikiPage > changed, final Feed feed ) {
        if( log.isDebugEnabled() ) {
            log.debug( "Generating RSS for blog, size=" + changed.size() );
        }

        final String ctitle = m_engine.getVariableManager().getVariable( wikiContext, PROP_CHANNEL_TITLE );
        if( ctitle != null ) {
            feed.setChannelTitle( ctitle );
        } else {
            feed.setChannelTitle( m_engine.getApplicationName() + ":" + wikiContext.getPage().getName() );
        }

        feed.setFeedURL( wikiContext.getViewURL( wikiContext.getPage().getName() ) );

        final String language = m_engine.getVariableManager().getVariable( wikiContext, PROP_CHANNEL_LANGUAGE );
        if( language != null ) {
            feed.setChannelLanguage( language );
        } else {
            feed.setChannelLanguage( m_channelLanguage );
        }

        final String channelDescription = m_engine.getVariableManager().getVariable( wikiContext, PROP_CHANNEL_DESCRIPTION );
        if( channelDescription != null ) {
            feed.setChannelDescription( channelDescription );
        }

        changed.sort( new PageTimeComparator() );

        int items = 0;
        for( final Iterator< WikiPage > i = changed.iterator(); i.hasNext() && items < 15; items++ ) {
            final WikiPage page = i.next();
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
            String pageText = m_engine.getPageManager().getPureText( page.getName(), WikiProvider.LATEST_VERSION );

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
                pageText = m_engine.getRenderingManager().textToHTML( wikiContext, pageText.substring( firstLine + 1, maxlen ).trim() );
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
