/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2004 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.util.BlogUtil;
import com.ecyrd.jspwiki.TextUtil;

/**
 *  Outputs links to all the site feeds and APIs this Wiki/blog supports.
 *
 *  @author Janne Jalkanen
 *  @since 2.2
 */
public class FeedDiscoveryTag
    extends WikiTagBase
{

    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine = m_wikiContext.getEngine();
        WikiPage   page   = m_wikiContext.getPage();

        String encodedName = engine.encodeName( page.getName() );

        String rssURL      = engine.getGlobalRSSURL();
        String atomFeedURL = engine.getBaseURL()+"atom.jsp?page="+encodedName;

        if( rssURL != null )
        {
            pageContext.getOut().print("<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS feed for the entire site.\" href=\""+rssURL+"\" />\n");
            pageContext.getOut().print("<link rel=\"service.feed\" type=\"application/atom+xml\" title=\""+
                                       TextUtil.replaceEntities(BlogUtil.getSiteName(m_wikiContext))+"\" href=\""+atomFeedURL+"\" />\n");
        }

        return SKIP_BODY;
    }
}
