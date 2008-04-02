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
