/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki;

import java.util.Date;

import com.ecyrd.jspwiki.providers.WikiPageProvider;

/**
 *  Simple wrapper class for the Wiki page attributes.  The Wiki page
 *  content is moved around in Strings, though.
 */
public class WikiPage
{
    private String m_name;
    private Date   m_lastModified;

    private int    m_version = WikiPageProvider.LATEST_VERSION;

    private String m_author = null;

    public WikiPage( String name )
    {
        m_name = name;
    }

    public String getName()
    {
        return m_name;
    }

    public Date getLastModified()
    {
        return m_lastModified;
    }

    public void setLastModified( Date date )
    {
        m_lastModified = date;
    }

    public void setVersion( int version )
    {
        m_version = version;
    }

    public int getVersion()
    {
        return m_version;
    }

    public void setAuthor( String author )
    {
        m_author = author;
    }

    /**
     *  Returns author name, or null, if no author has been defined.
     */
    public String getAuthor()
    {
        return m_author;
    }

    public String toString()
    {
        return "WikiPage ["+m_name+",ver="+m_version+",mod="+m_lastModified+"]";
    }
}
