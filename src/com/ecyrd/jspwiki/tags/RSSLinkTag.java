/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;

import com.ecyrd.jspwiki.WikiEngine;

/**
 *  Writes a link to the RSS file (using the "link" attribute).
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class RSSLinkTag
    extends WikiTagBase
{
    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine = m_wikiContext.getEngine();

        String rssURL = engine.getGlobalRSSURL();

        if( rssURL != null )
        {
            pageContext.getOut().print("<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS feed\" href=\""+rssURL+"\" />");

        }

        return SKIP_BODY;
    }
}
