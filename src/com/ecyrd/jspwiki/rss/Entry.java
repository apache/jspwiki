/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.rss;

import com.ecyrd.jspwiki.WikiPage;

/**
 *  @author jalkanen
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
