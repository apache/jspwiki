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
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.plugin.WeblogPlugin;
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
        String atomPostURL = engine.getBaseURL()+"atom/"+encodedName;
        String rssFeedURL  = engine.getBaseURL()+"rss.jsp?page="+encodedName+"&amp;mode=wiki";
        
        if( rssURL != null )
        {
            String siteName = BlogUtil.getSiteName(m_wikiContext);
            siteName = TextUtil.replaceEntities( siteName );
            
            pageContext.getOut().print("<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS feed for the entire site.\" href=\""+rssURL+"\" />\n");
            pageContext.getOut().print("<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS feed for page "+siteName+".\" href=\""+rssFeedURL+"\" />\n");

            // TODO: Enable this
            /*
            pageContext.getOut().print("<link rel=\"service.post\" type=\"application/atom+xml\" title=\""+
                                       siteName+"\" href=\""+atomPostURL+"\" />\n");
            */
            // FIXME: This does not work always, as plugins are not initialized until the first fetch
            if( "true".equals(page.getAttribute(WeblogPlugin.ATTR_ISWEBLOG)) )
            {
                String blogFeedURL  = engine.getBaseURL()+"rss.jsp?page="+encodedName;
                String atomFeedURL = engine.getBaseURL()+"atom.jsp?page="+encodedName;
        
                pageContext.getOut().print("<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS feed for weblog "+
                                           siteName+".\" href=\""+blogFeedURL+"\" />\n");

                pageContext.getOut().print("<link rel=\"service.feed\" type=\"application/atom+xml\" title=\""+
                                           siteName+"\" href=\""+atomFeedURL+"\" />\n");
            }
        }

        return SKIP_BODY;
    }
}
