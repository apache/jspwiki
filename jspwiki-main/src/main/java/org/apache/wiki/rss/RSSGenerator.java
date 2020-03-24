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

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.engine.Initializable;
import org.apache.wiki.util.TextUtil;

import java.util.List;

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
public interface RSSGenerator extends Initializable {

    /** Parameter value to represent RSS 1.0 feeds.  Value is <tt>{@value}</tt>. */
    String RSS10 = "rss10";

    /** Parameter value to represent RSS 2.0 feeds.  Value is <tt>{@value}</tt>. */
    String RSS20 = "rss20";
    
    /** Parameter value to represent Atom feeds.  Value is <tt>{@value}</tt>.  */
    String ATOM  = "atom";

    /** Parameter value to represent a 'blog' style feed. Value is <tt>{@value}</tt>. */
    String MODE_BLOG = "blog";
    
    /** Parameter value to represent a 'wiki' style feed. Value is <tt>{@value}</tt>. */
    String MODE_WIKI = "wiki";

    /** Parameter value to represent a 'full' style feed. Value is <tt>{@value}</tt>. */
    String MODE_FULL = "full";

    /**
     *  Defines the property name for the RSS channel description.  Default value for the channel description is an empty string.
     *
     *  @since 1.7.6.
     */
    String PROP_CHANNEL_DESCRIPTION = "jspwiki.rss.channelDescription";

    /**
     *  Defines the property name for the RSS channel language.  Default value for the language is "en-us".
     *
     *  @since 1.7.6.
     */
    String PROP_CHANNEL_LANGUAGE = "jspwiki.rss.channelLanguage";

    /** Defines the property name for the RSS channel title.  Value is <tt>{@value}</tt>. */
    String PROP_CHANNEL_TITLE = "jspwiki.rss.channelTitle";

    /**
     *  Defines the property name for the RSS generator main switch.
     *
     *  @since 1.7.6.
     */
    String PROP_GENERATE_RSS = "jspwiki.rss.generate";

    /**
     *  Defines the property name for the RSS file that the wiki should generate.
     *
     *  @since 1.7.6.
     */
    String PROP_RSSFILE = "jspwiki.rss.fileName";

    /**
     *  Defines the property name for the RSS generation interval in seconds.
     *
     *  @since 1.7.6.
     */
    String PROP_INTERVAL = "jspwiki.rss.interval";

    /** Defines the property name for the RSS author.  Value is <tt>{@value}</tt>. */
    String PROP_RSS_AUTHOR = "jspwiki.rss.author";

    /** Defines the property name for the RSS author email.  Value is <tt>{@value}</tt>. */
    String PROP_RSS_AUTHOREMAIL = "jspwiki.rss.author.email";

    /**
     *  Generates the RSS resource.  You probably want to output this result into a file or something, or serve as output from a servlet.
     *
     *  @return A RSS 1.0 feed in the "full" mode.
     */
    String generate();

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
    String generateFeed( final Context wikiContext, final List< Page > changed, final String mode, final String type ) throws IllegalArgumentException;

    /**
     * Returns <code>true</code> if RSS generation is enabled.
     *
     * @return whether RSS generation is currently enabled
     */
    boolean isEnabled();

    /**
     * Turns RSS generation on or off. This setting is used to set the "enabled" flag only for use by callers, and does not
     * actually affect whether the {@link #generate()} or {@link #generateFeed(Context, List, String, String)} methods output anything.
     *
     * @param enabled whether RSS generation is considered enabled.
     */
    void setEnabled( final boolean enabled );

    /**
     * returns the rss file.
     *
     * @return the rss file.
     */
    String getRssFile();

    /**
     *  Generates an RSS feed for the entire wiki.  Each item should be an instance of the RSSItem class.
     *
     *  @param wikiContext A WikiContext
     *  @param feed A Feed to generate the feed to.
     *  @return feed.getString().
     */
    String generateFullWikiRSS( Context wikiContext, Feed feed );

    /**
     * Create RSS/Atom as if this page was a wikipage (in contrast to Blog mode).
     *
     * @param wikiContext The WikiContext
     * @param changed A List of changed WikiPages.
     * @param feed A Feed object to fill.
     * @return the RSS representation of the wiki context
     */
    String generateWikiPageRSS( Context wikiContext, List< Page > changed, Feed feed );

    /**
     *  Creates RSS from modifications as if this page was a blog (using the WeblogPlugin).
     *
     *  @param wikiContext The WikiContext, as usual.
     *  @param changed A list of the changed pages.
     *  @param feed A valid Feed object.  The feed will be used to create the RSS/Atom, depending on which kind of an object you want to put in it.
     *  @return A String of valid RSS or Atom.
     */
    String generateBlogRSS( Context wikiContext, List< Page > changed, Feed feed );

    /**
     *  Does the required formatting and entity replacement for XML.
     *  
     *  @param s String to format.
     *  @return A formatted string.
     */
    // FIXME: Replicates Feed.format().
    static String format( String s ) {
        s = TextUtil.replaceString( s, "&", "&amp;" );
        s = TextUtil.replaceString( s, "<", "&lt;" );
        s = TextUtil.replaceString( s, "]]>", "]]&gt;" );

        return s.trim();
    }

    /**
     * Returns the content type of this RSS feed.
     *
     * @since 2.3.15
     * @param mode the RSS mode: {@link #RSS10}, {@link #RSS20} or {@link #ATOM}.
     * @return the content type
     */
    static String getContentType( final String mode ) {
        if( mode.equals( RSS10 ) || mode.equals( RSS20 ) ) {
            return "application/rss+xml";
        } else if( mode.equals( ATOM ) ) {
            return "application/atom+xml";
        }

        return "application/octet-stream"; // Unknown type
    }

}
