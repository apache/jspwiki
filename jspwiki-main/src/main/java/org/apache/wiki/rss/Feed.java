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

import org.apache.commons.text.StringEscapeUtils;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.NoSuchVariableException;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an abstract feed.
 */
public abstract class Feed {
    protected List<Entry> m_entries = new ArrayList<Entry>();

    protected String m_feedURL;
    protected String m_channelTitle;
    protected String m_channelDescription;
    protected String m_channelLanguage;

    protected WikiContext m_wikiContext;

    protected String m_mode = RSSGenerator.MODE_WIKI;

    /**
     * Wiki variable storing the blog's name.
     */
    public static final String VAR_BLOGNAME = "blogname";

    /**
     * Figure out a site name for a feed.
     *
     * @param context the wiki context
     * @return the site name
     */
    public static String getSiteName(WikiContext context) {
        WikiEngine engine = context.getEngine();

        String blogname = null;

        try {
            blogname = engine.getVariableManager().getValue(context, VAR_BLOGNAME);
        } catch (NoSuchVariableException e) {
        }

        if (blogname == null) {
            blogname = engine.getApplicationName() + ": " + context.getPage().getName();
        }

        return blogname;
    }

    /**
     * Create a new Feed for a particular WikiContext.
     *
     * @param context The WikiContext.
     */
    public Feed(WikiContext context) {
        m_wikiContext = context;
    }

    /**
     * Set the mode of the Feed.  It can be any of the following:
     * <ul>
     * <li>{@link RSSGenerator#MODE_WIKI} - to create a wiki diff list per page.</li>
     * <li>{@link RSSGenerator#MODE_BLOG} - to assume that the Entries are blog entries.</li>
     * <li>{@link RSSGenerator#MODE_FULL} - to create a wiki diff list for the entire blog.</li>
     * </ul>
     * As the Entry list itself is generated elsewhere, this mostly just affects the way
     * that the layout and metadata for each entry is generated.
     *
     * @param mode As defined in RSSGenerator.
     */
    public void setMode(String mode) {
        m_mode = mode;
    }

    /**
     * Adds a new Entry to the Feed, at the end of the list.
     *
     * @param e The Entry to add.
     */
    public void addEntry(Entry e) {
        m_entries.add(e);
    }

    /**
     * Returns the XML for the feed contents in a String format.  All subclasses must implement.
     *
     * @return valid XML, ready to be shoved out.
     */
    public abstract String getString();

    /**
     * @return Returns the m_channelDescription.
     */
    public String getChannelDescription() {
        return m_channelDescription;
    }

    /**
     * @param description The m_channelDescription to set.
     */
    public void setChannelDescription(String description) {
        m_channelDescription = description;
    }

    /**
     * @return Returns the m_channelLanguage.
     */
    public String getChannelLanguage() {
        return m_channelLanguage;
    }

    /**
     * @param language The m_channelLanguage to set.
     */
    public void setChannelLanguage(String language) {
        m_channelLanguage = language;
    }

    /**
     * @return Returns the m_channelTitle.
     */
    public String getChannelTitle() {
        return m_channelTitle;
    }

    /**
     * @param title The m_channelTitle to set.
     */
    public void setChannelTitle(String title) {
        m_channelTitle = title;
    }

    /**
     * @return Returns the m_feedURL.
     */
    public String getFeedURL() {
        return m_feedURL;
    }

    /**
     * @param feedurl The m_feedURL to set.
     */
    public void setFeedURL(String feedurl) {
        m_feedURL = feedurl;
    }

    /**
     * A helper method for figuring out the MIME type for an enclosure.
     *
     * @param c    A ServletContext
     * @param name The filename
     * @return Something sane for a MIME type.
     */
    protected String getMimeType(ServletContext c, String name) {
        String type = c.getMimeType(name);

        if (type == null) {
            type = "application/octet-stream";
        }

        return type;
    }

    /**
     * Does the required formatting and entity replacement for XML.
     *
     * @param s The String to format. Null is safe.
     * @return A formatted string.
     */
    public static String format( String s ) {
        return StringEscapeUtils.escapeXml11( s );
    }
}
