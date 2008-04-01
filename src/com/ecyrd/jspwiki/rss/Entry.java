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

import com.ecyrd.jspwiki.WikiPage;

/**
 *  Represents an entry.
 *
 *  @since
 */
public class Entry
{
    private String m_content;
    private String m_URL;
    private String m_title;
    private WikiPage m_page;
    private String m_author;

    public void setAuthor( String author )
    {
        m_author = author;
    }

    public String getAuthor()
    {
        return m_author;
    }

    public WikiPage getPage()
    {
        return m_page;
    }

    public void setPage( WikiPage p )
    {
        m_page = p;
    }

    public void setTitle( String title )
    {
        m_title = title;
    }

    public String getTitle()
    {
        return m_title;
    }

    public void setURL( String url )
    {
        m_URL = url;
    }

    public String getURL()
    {
        return m_URL;
    }

    public void setContent( String content )
    {
        m_content = content;
    }

    public String getContent()
    {
        return m_content;
    }
}
