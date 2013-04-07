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

import org.apache.wiki.WikiPage;

/**
 *  Represents an entry, that is, an unit of change, in a Feed.
 */
public class Entry
{
    private String   m_content;
    private String   m_url;
    private String   m_title;
    private WikiPage m_page;
    private String   m_author;

    /**
     *  Set the author of this entry.
     *  
     *  @param author Name of the author.
     */
    public void setAuthor( String author )
    {
        m_author = author;
    }

    /**
     *  Return the author set by setAuthor().
     *  
     *  @return A String representing the author.
     */
    public String getAuthor()
    {
        return m_author;
    }

    /**
     *  Returns the page set by {@link #setPage(WikiPage)}.
     *  
     *  @return The WikiPage to which this Entry refers to.
     */
    public WikiPage getPage()
    {
        return m_page;
    }

    /**
     *  Sets the WikiPage to which this Entry refers to.
     *  
     *  @param p A valid WikiPage.
     */
    public void setPage( WikiPage p )
    {
        m_page = p;
    }

    /**
     *  Sets a title for the change.  For example, a WebLog entry might use the
     *  post title, or a Wiki change could use something like "XXX changed page YYY".
     *  
     *  @param title A String description of the change.
     */
    public void setTitle( String title )
    {
        m_title = title;
    }

    /**
     *  Returns the title.
     *  
     *  @return The title set in setTitle.
     */
    public String getTitle()
    {
        return m_title;
    }

    /**
     *  Set the URL - the permalink - of the Entry.
     *  
     *  @param url An absolute URL to the entry.
     */
    public void setURL( String url )
    {
        m_url = url;
    }

    /**
     *  Return the URL set by setURL().
     *  
     *  @return The URL.
     */
    public String getURL()
    {
        return m_url;
    }

    /**
     *  Set the content of this entry.
     *  
     *  @param content A String of the content.
     */
    public void setContent( String content )
    {
        m_content = content;
    }

    /**
     *  Return the content set by {@link #setContent(String)}.
     *  
     *  @return Whatever was set by setContent().
     */
    public String getContent()
    {
        return m_content;
    }
}
