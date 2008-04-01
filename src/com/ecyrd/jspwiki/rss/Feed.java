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
package com.ecyrd.jspwiki.rss;

import java.util.*;

import javax.servlet.ServletContext;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;

/**
 *  Represents an abstract feed.
 *
 *  @since
 */
public abstract class Feed
{
    protected List m_entries = new ArrayList();

    protected String m_feedURL;
    protected String m_channelTitle;
    protected String m_channelDescription;
    protected String m_channelLanguage;

    protected WikiContext m_wikiContext;

    protected String m_mode = RSSGenerator.MODE_WIKI;

    public Feed( WikiContext context )
    {
        m_wikiContext = context;
    }

    public void setMode( String mode )
    {
        m_mode = mode;
    }

    public void addEntry( Entry e )
    {
        m_entries.add( e );
    }

    public abstract String getString();
    /**
     * @return Returns the m_channelDescription.
     */
    public String getChannelDescription()
    {
        return m_channelDescription;
    }
    /**
     * @param description The m_channelDescription to set.
     */
    public void setChannelDescription( String description )
    {
        m_channelDescription = description;
    }
    /**
     * @return Returns the m_channelLanguage.
     */
    public String getChannelLanguage()
    {
        return m_channelLanguage;
    }
    /**
     * @param language The m_channelLanguage to set.
     */
    public void setChannelLanguage( String language )
    {
        m_channelLanguage = language;
    }
    /**
     * @return Returns the m_channelTitle.
     */
    public String getChannelTitle()
    {
        return m_channelTitle;
    }
    /**
     * @param title The m_channelTitle to set.
     */
    public void setChannelTitle( String title )
    {
        m_channelTitle = title;
    }

    /**
     * @return Returns the m_feedURL.
     */
    public String getFeedURL()
    {
        return m_feedURL;
    }
    /**
     * @param feedurl The m_feedURL to set.
     */
    public void setFeedURL( String feedurl )
    {
        m_feedURL = feedurl;
    }

    protected String getMimeType(ServletContext c, String name)
    {
        String type = c.getMimeType(name);

        if( type == null ) type = "application/octet-stream";

        return type;
    }

    /**
     *  Does the required formatting and entity replacement for XML.
     */
    public static String format( String s )
    {
        if( s != null )
        {
            s = TextUtil.replaceString( s, "&", "&amp;" );
            s = TextUtil.replaceString( s, "<", "&lt;" );
            s = TextUtil.replaceString( s, ">", "&gt;" );

            return s.trim();
        }
        return null;
    }
}
