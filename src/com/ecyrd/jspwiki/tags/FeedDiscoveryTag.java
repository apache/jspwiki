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

import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.util.UrlBuilder;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.action.RSSActionBean;
import com.ecyrd.jspwiki.plugin.WeblogPlugin;
import com.ecyrd.jspwiki.util.BlogUtil;

/**
 *  Outputs links to all the site feeds and APIs this Wiki/blog supports.
 *
 *  @author Janne Jalkanen
 *  @since 2.2
 */
public class FeedDiscoveryTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine = m_actionBean.getEngine();

        String rssSiteURL      = engine.getGlobalRSSURL();
        
        if( rssSiteURL != null )
        {
            // Write the feed URL for the site
            String siteName = BlogUtil.getSiteName(m_actionBean);
            siteName = TextUtil.replaceEntities( siteName );
            pageContext.getOut().print("<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS wiki feed for the entire site.\" href=\""+rssSiteURL+"\" />\n");

            // Write the feed URL for this page
            if ( m_page != null )
            {
                HttpServletResponse httpResponse = (HttpServletResponse)pageContext.getResponse();
                String encodedPageName = engine.encodeName( m_page.getName() );
                String url = RSSActionBean.class.getAnnotation(UrlBinding.class).value();
                
                // Create the feed
                UrlBuilder urlBuilder = new UrlBuilder( url, true );
                urlBuilder.addParameter("page", encodedPageName);
                urlBuilder.addParameter("mode", "wiki");
                String rssPageURL  =  httpResponse.encodeURL( urlBuilder.toString() );
                if( rssPageURL != null )
                {
                    pageContext.getOut().print("<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS wiki feed for page "+siteName+".\" href=\""+rssPageURL+"\" />\n");

                    // TODO: Enable this
                    /*
                    pageContext.getOut().print("<link rel=\"service.post\" type=\"application/atom+xml\" title=\""+
                                               siteName+"\" href=\""+atomPostURL+"\" />\n");
                    */
                    // FIXME: This does not work always, as plugins are not initialized until the first fetch
                    if( "true".equals(m_page.getAttribute(WeblogPlugin.ATTR_ISWEBLOG)) )
                    {
                        // Create blog feed URL
                        urlBuilder = new UrlBuilder( url, true );
                        urlBuilder.addParameter("page", encodedPageName);
                        String blogPageFeedURL = httpResponse.encodeURL( urlBuilder.toString() );
                        pageContext.getOut().print("<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS feed for weblog "+
                                                   siteName+".\" href=\""+blogPageFeedURL+"\" />\n");
                        
                        // Create atom feed URL
                        urlBuilder.addParameter("type", "atom");
                        String atomPageFeedURL = httpResponse.encodeURL( urlBuilder.toString() );

                        pageContext.getOut().print("<link rel=\"service.feed\" type=\"application/atom+xml\" title=\"Atom 1.0 weblog feed for "+
                                                   siteName+"\" href=\""+atomPageFeedURL+"\" />\n");
                    }
                }
            }
        }

        return SKIP_BODY;
    }
}
