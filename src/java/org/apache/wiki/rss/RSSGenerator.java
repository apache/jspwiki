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
package org.apache.wiki.rss;

import java.util.*;

import org.apache.wiki.*;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.WikiName;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.util.TextUtil;



/**
 *  The master class for generating different kinds of Feeds (including RSS1.0, 2.0 and Atom).
 *  <p>
 *  This class can produce quite a few different styles of feeds.  The following modes are
 *  available:
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
public class RSSGenerator
{
    static Logger              log = LoggerFactory.getLogger( RSSGenerator.class );
    private WikiEngine         m_engine;

    private String             m_channelDescription = "";
    private String             m_channelLanguage    = "en-us";
    private boolean            m_enabled = true;

    /**
     *  Parameter value to represent RSS 1.0 feeds.  Value is <tt>{@value}</tt>. 
     */
    public static final String RSS10 = "rss10";

    /**
     *  Parameter value to represent RSS 2.0 feeds.  Value is <tt>{@value}</tt>. 
     */
    public static final String RSS20 = "rss20";
    
    /**
     *  Parameter value to represent Atom feeds.  Value is <tt>{@value}</tt>. 
     */
    public static final String ATOM  = "atom";

    /**
     *  Parameter value to represent a 'blog' style feed. Value is <tt>{@value}</tt>.
     */
    public static final String MODE_BLOG = "blog";
    
    /**
     *  Parameter value to represent a 'wiki' style feed. Value is <tt>{@value}</tt>.
     */
    public static final String MODE_WIKI = "wiki";

    /**
     *  Parameter value to represent a 'full' style feed. Value is <tt>{@value}</tt>.
     */
    public static final String MODE_FULL = "full";

    /**
     *  Defines the property name for the RSS channel description.  Default value for the
     *  channel description is an empty string.
     *  @since 1.7.6.
     */
    public static final String PROP_CHANNEL_DESCRIPTION = "jspwiki.rss.channelDescription";

    /**
     *  Defines the property name for the RSS channel language.  Default value for the
     *  language is "en-us".
     *  @since 1.7.6.
     */
    public static final String PROP_CHANNEL_LANGUAGE    = "jspwiki.rss.channelLanguage";

    /**
     *  Defins the property name for the RSS channel title.  Value is <tt>{@value}</tt>.
     */
    public static final String PROP_CHANNEL_TITLE       = "jspwiki.rss.channelTitle";

    /**
     *  Defines the property name for the RSS generator main switch.
     *  @since 1.7.6.
     */
    public static final String PROP_GENERATE_RSS        = "jspwiki.rss.generate";

    /**
     *  Defines the property name for the RSS file that the wiki should generate.
     *  @since 1.7.6.
     */
    public static final String PROP_RSSFILE             = "jspwiki.rss.fileName";

    /**
     *  Defines the property name for the RSS generation interval in seconds.
     *  @since 1.7.6.
     */
    public static final String PROP_INTERVAL            = "jspwiki.rss.interval";

    /**
     *  Defines the property name for the RSS author.  Value is <tt>{@value}</tt>.
     */
    public static final String PROP_RSS_AUTHOR          = "jspwiki.rss.author";

    /**
     *  Defines the property name for the RSS author email.  Value is <tt>{@value}</tt>.
     */
    public static final String PROP_RSS_AUTHOREMAIL     = "jspwiki.rss.author.email";

    /**
     *  Property name for the RSS copyright info.  Value is <tt>{@value}</tt>.
     */
    public static final String PROP_RSS_COPYRIGHT       = "jspwiki.rss.copyright";

    /** Just for compatibilty.  @deprecated */
    public static final String PROP_RSSAUTHOR           = PROP_RSS_AUTHOR;

    /** Just for compatibilty.  @deprecated */
    public static final String PROP_RSSAUTHOREMAIL      = PROP_RSS_AUTHOREMAIL;


    private static final int MAX_CHARACTERS             = Integer.MAX_VALUE-1;

    /**
     *  Initialize the RSS generator for a given WikiEngine.  Currently the only 
     *  required property is <tt>{@value org.apache.wiki.WikiEngine#PROP_BASEURL}</tt>.
     *  
     *  @param engine The WikiEngine.
     *  @param properties The properties.
     *  @throws NoRequiredPropertyException If something is missing from the given property set.
     */
    public RSSGenerator( WikiEngine engine, Properties properties )
        throws NoRequiredPropertyException
    {
        m_engine = engine;

        // FIXME: This assumes a bit too much.
        if( engine.getBaseURL() == null || engine.getBaseURL().length() == 0 )
        {
            throw new NoRequiredPropertyException( "RSS requires jspwiki.baseURL to be set!",
                                                   WikiEngine.PROP_BASEURL );
        }

        m_channelDescription = properties.getProperty( PROP_CHANNEL_DESCRIPTION,
                                                       m_channelDescription );
        m_channelLanguage    = properties.getProperty( PROP_CHANNEL_LANGUAGE,
                                                       m_channelLanguage );
    }

    /**
     *  Does the required formatting and entity replacement for XML.
     *  
     *  @param s String to format.
     *  @return A formatted string.
     */
    // FIXME: Replicates Feed.format().
    public static String format( String s )
    {
        s = TextUtil.replaceString( s, "&", "&amp;" );
        s = TextUtil.replaceString( s, "<", "&lt;" );
        s = TextUtil.replaceString( s, "]]>", "]]&gt;" );

        return s.trim();
    }

    private String getAuthor( WikiPage page )
    {
        String author = page.getAuthor();

        if( author == null ) author = "An unknown author";

        return author;
    }

    private String getAttachmentDescription( WikiPage att )
    {
        String author = getAuthor(att);
        StringBuilder sb = new StringBuilder();

        if( att.getVersion() != 1 )
        {
            sb.append(author+" uploaded a new version of this attachment on "+att.getLastModified() );
        }
        else
        {
            sb.append(author+" created this attachment on "+att.getLastModified() );
        }

        sb.append("<br /><hr /><br />");
        
        try
        {
            sb.append( "Parent page: <a href=\""+
                       m_engine.getURL( WikiContext.VIEW, att.getParent().getName(), null, true ) +
                       "\">"+att.getParent().getName()+"</a><br />" );
            sb.append( "Info page: <a href=\""+
                       m_engine.getURL( WikiContext.INFO, att.getParent().getName(), null, true ) +
                       "\">"+att.getName()+"</a>" );

        }
        catch( ProviderException e )
        {
            log.debug( "Unable to load parent", e );
        }

        return sb.toString();
    }

    private String getPageDescription( WikiPage page ) throws PageNotFoundException, ProviderException
    {
        StringBuilder buf = new StringBuilder();
        String author = getAuthor(page);

        WikiContext ctx = m_engine.getWikiContextFactory().newViewContext( page );
        if( page.getVersion() > 1 )
        {
            String diff = m_engine.getDiff( ctx,
                                            page.getVersion()-1, // FIXME: Will fail when non-contiguous versions
                                            page.getVersion() );

            buf.append(author+" changed this page on "+page.getLastModified()+":<br /><hr /><br />" );
            buf.append(diff);
        }
        else
        {
            buf.append(author+" created this page on "+page.getLastModified()+":<br /><hr /><br />" );
            buf.append(m_engine.getHTML( page.getName() ));
        }

        return buf.toString();
    }

    private String getEntryDescription( WikiPage page )
    {
        String res = "";

        try
        {
            if( page instanceof Attachment )
            {
                res = getAttachmentDescription( (WikiPage)page );
            }
            else
            {
                res = getPageDescription( page );
            }
        }
        catch( ProviderException e )
        {
            // FIXME: We should check if returning a plain empty string is ok,
            //        as this just gobbles up the actual error.
            log.error( "Unable to get description", e );
        }

        return res;
    }

    // FIXME: This should probably return something more intelligent
    private String getEntryTitle( WikiPage page )
    {
        return page.getName()+", version "+page.getVersion();
    }

    /**
     *  Generates the RSS resource.  You probably want to output this
     *  result into a file or something, or serve as output from a servlet.
     *  
     *  @return A RSS 1.0 feed in the "full" mode.
     */
    public String generate() throws WikiException
    {
        WikiContext context = m_engine.getWikiContextFactory().newContext(null,null,WikiContext.RSS);
        context.setPage( m_engine.createPage( WikiName.valueOf( "__DUMMY" ) ) );
        Feed feed = new RSS10Feed( context );

        String result = generateFullWikiRSS( context, feed );

        result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + result;

        return result;
    }

    /**
     * Returns the content type of this RSS feed.
     *  @since 2.3.15
     * @param mode the RSS mode: {@link #RSS10}, {@link #RSS20} or {@link #ATOM}.
     * @return the content type
     */
    public static String getContentType( String mode )
    {
        if( mode.equals( RSS10 )||mode.equals(RSS20) )
        {
            return "application/rss+xml";
        }
        else if( mode.equals(ATOM) )
        {
            return "application/atom+xml";
        }

        return "application/octet-stream"; // Unknown type
    }

    /**
     *  Generates a feed based on a context and list of changes.
     * @param wikiContext The WikiContext
     * @param changed A list of Entry objects
     * @param mode The mode (wiki/blog)
     * @param type The type (RSS10, RSS20, ATOM).  Default is RSS 1.0
     * @return Fully formed XML.
     *
     * @throws ProviderException If the underlying provider failed.
     * @throws IllegalArgumentException If an illegal mode is given.
     */
    public String generateFeed( WikiContext wikiContext, List changed, String mode, String type )
        throws ProviderException, IllegalArgumentException
    {
        Feed feed = null;
        String res = null;

        if( ATOM.equals(type) )
        {
            feed = new AtomFeed( wikiContext );
        }
        else if( RSS20.equals( type ) )
        {
            feed = new RSS20Feed( wikiContext );
        }
        else
        {
            feed = new RSS10Feed( wikiContext );
        }

        feed.setMode( mode );

        if( MODE_BLOG.equals( mode ) )
        {
            res = generateBlogRSS( wikiContext, changed, feed );
        }
        else if( MODE_FULL.equals(mode) )
        {
            res = generateFullWikiRSS( wikiContext, feed );
        }
        else if( MODE_WIKI.equals(mode) )
        {
            res = generateWikiPageRSS( wikiContext, changed, feed );
        }
        else
        {
            throw new IllegalArgumentException( "Invalid value for feed mode: "+mode );
        }

        return res;
    }

    /**
     * Returns <code>true</code> if RSS generation is enabled.
     * @return whether RSS generation is currently enabled
     */
    public boolean isEnabled()
    {
        return m_enabled;
    }

    /**
     * Turns RSS generation on or off. This setting is used to set
     * the "enabled" flag only for use by callers, and does not
     * actually affect whether the {@link #generate()} or
     * {@link #generateFeed(WikiContext, List, String, String)}
     * methods output anything.
     * @param enabled whether RSS generation is considered enabled.
     */
    public synchronized void setEnabled( boolean enabled )
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
    protected String generateFullWikiRSS( WikiContext wikiContext, Feed feed )
    {
        feed.setChannelTitle( m_engine.getApplicationName() );
        feed.setFeedURL( m_engine.getBaseURL() );
        feed.setChannelLanguage( m_channelLanguage );
        feed.setChannelDescription( m_channelDescription );

        Collection changed = m_engine.getRecentChanges(wikiContext.getPage().getWiki());

        WikiSession session = WikiSession.guestSession( m_engine );
        int items = 0;
        for( Iterator i = changed.iterator(); i.hasNext() && items < 15; items++ )
        {
            WikiPage page = (WikiPage) i.next();

            //
            //  Check if the anonymous user has view access to this page.
            //

            if( !m_engine.getAuthorizationManager().checkPermission(session,
                                                                    new PagePermission(page,PagePermission.VIEW_ACTION) ) )
            {
                // No permission, skip to the next one.
                continue;
            }

            Entry e = new Entry();

            e.setPage( page );

            String url;

            if( page instanceof Attachment )
            {
                url = m_engine.getURL( WikiContext.ATTACH,
                                       page.getName(),
                                       null,
                                       true );
            }
            else
            {
                url = m_engine.getURL( WikiContext.VIEW,
                                       page.getName(),
                                       null,
                                       true );
            }

            e.setURL( url );
            e.setTitle( page.getName() );
            e.setContent( getEntryDescription(page) );
            e.setAuthor( getAuthor(page) );

            feed.addEntry( e );
        }

        return feed.getString();
    }

    /**
     *  Create RSS/Atom as if this page was a wikipage (in contrast to Blog mode).
     *
     * @param wikiContext The WikiContext
     * @param changed A List of changed WikiPages.
     * @param feed A Feed object to fill.
     * @return the RSS representation of the wiki context
     */
    @SuppressWarnings("unchecked")
    protected String generateWikiPageRSS( WikiContext wikiContext, List changed, Feed feed )
    {
        feed.setChannelTitle( m_engine.getApplicationName()+": "+wikiContext.getPage().getName() );
        feed.setFeedURL( wikiContext.getViewURL( wikiContext.getPage().getName() ) );
        String language = m_engine.getVariable( wikiContext, PROP_CHANNEL_LANGUAGE );

        if( language != null )
            feed.setChannelLanguage( language );
        else
            feed.setChannelLanguage( m_channelLanguage );

        String channelDescription = m_engine.getVariable( wikiContext, PROP_CHANNEL_DESCRIPTION );

        if( channelDescription != null )
        {
            feed.setChannelDescription( channelDescription );
        }

        Collections.sort( changed, new PageTimeComparator() );

        int items = 0;
        for( Iterator i = changed.iterator(); i.hasNext() && items < 15; items++ )
        {
            WikiPage page = (WikiPage) i.next();

            Entry e = new Entry();

            e.setPage( page );

            String url;

            if( page instanceof Attachment )
            {
                url = m_engine.getURL( WikiContext.ATTACH,
                                       page.getName(),
                                       "version="+page.getVersion(),
                                       true );
            }
            else
            {
                url = m_engine.getURL( WikiContext.VIEW,
                                       page.getName(),
                                       "version="+page.getVersion(),
                                       true );
            }

            // Unfortunately, this is needed because the code will again go through
            // replacement conversion

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
     *  @param feed A valid Feed object.  The feed will be used to create the RSS/Atom, depending
     *              on which kind of an object you want to put in it.
     *  @return A String of valid RSS or Atom.
     *  @throws ProviderException If reading of pages was not possible.
     */
    @SuppressWarnings("unchecked")
    protected String generateBlogRSS( WikiContext wikiContext, List changed, Feed feed )
        throws ProviderException
    {
        if( log.isDebugEnabled() ) log.debug("Generating RSS for blog, size="+changed.size());

        String ctitle = m_engine.getVariable( wikiContext, PROP_CHANNEL_TITLE );

        if( ctitle != null )
            feed.setChannelTitle( ctitle );
        else
            feed.setChannelTitle( m_engine.getApplicationName()+":"+wikiContext.getPage().getName() );

        feed.setFeedURL( wikiContext.getViewURL( wikiContext.getPage().getName() ) );

        String language = m_engine.getVariable( wikiContext, PROP_CHANNEL_LANGUAGE );

        if( language != null )
            feed.setChannelLanguage( language );
        else
            feed.setChannelLanguage( m_channelLanguage );

        String channelDescription = m_engine.getVariable( wikiContext, PROP_CHANNEL_DESCRIPTION );

        if( channelDescription != null )
        {
            feed.setChannelDescription( channelDescription );
        }

        Collections.sort( changed, new PageTimeComparator() );

        int items = 0;
        for( Iterator i = changed.iterator(); i.hasNext() && items < 15; items++ )
        {
            WikiPage page = (WikiPage) i.next();

            Entry e = new Entry();

            e.setPage( page );

            String url;

            if( page instanceof Attachment )
            {
                url = m_engine.getURL( WikiContext.ATTACH,
                                       page.getName(),
                                       null,
                                       true );
            }
            else
            {
                url = m_engine.getURL( WikiContext.VIEW,
                                       page.getName(),
                                       null,
                                       true );
            }

            e.setURL( url );

            //
            //  Title
            //

            String pageText = m_engine.getPureText(page.getName(), WikiProvider.LATEST_VERSION );

            String title = "";
            int firstLine = pageText.indexOf('\n');

            if( firstLine > 0 )
            {
                title = pageText.substring( 0, firstLine ).trim();
            }

            if( title.length() == 0 ) title = page.getName();

            // Remove wiki formatting
            while( title.startsWith("!") ) title = title.substring(1);

            e.setTitle( title );

            //
            //  Description
            //

            if( firstLine > 0 )
            {
                int maxlen = pageText.length();
                if( maxlen > MAX_CHARACTERS ) maxlen = MAX_CHARACTERS;

                if( maxlen > 0 )
                {
                    pageText = m_engine.textToHTML( wikiContext,
                                                    pageText.substring( firstLine+1,
                                                                        maxlen ).trim() );

                    if( maxlen == MAX_CHARACTERS ) pageText += "...";

                    e.setContent( pageText );
                }
                else
                {
                    e.setContent( title );
                }
            }
            else
            {
                e.setContent( title );
            }

            e.setAuthor( getAuthor(page) );

            feed.addEntry( e );
        }

        return feed.getString();
    }

}
