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

import org.apache.jspwiki.api.WikiPage;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.plugin.WeblogPlugin;
import com.ecyrd.jspwiki.util.BlogUtil;
import com.ecyrd.jspwiki.util.TextUtil;

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
        WikiEngine engine = m_wikiContext.getEngine();
        WikiPage   page   = m_wikiContext.getPage();

        String encodedName = engine.encodeName( page.getName() );

        String rssURL      = engine.getGlobalRSSURL();
        String rssFeedURL  = engine.getURL(WikiContext.NONE, "rss.jsp", 
                                           "page="+encodedName+"&amp;mode=wiki",
                                           true );
        
        if( rssURL != null )
        {
            String siteName = BlogUtil.getSiteName(m_wikiContext);
            siteName = TextUtil.replaceEntities( siteName );
            
            pageContext.getOut().print("<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS wiki feed for the entire site.\" href=\""+rssURL+"\" />\n");
            pageContext.getOut().print("<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS wiki feed for page "+siteName+".\" href=\""+rssFeedURL+"\" />\n");

            // TODO: Enable this
            /*
            pageContext.getOut().print("<link rel=\"service.post\" type=\"application/atom+xml\" title=\""+
                                       siteName+"\" href=\""+atomPostURL+"\" />\n");
            */
            // FIXME: This does not work always, as plugins are not initialized until the first fetch
            if( "true".equals(page.getAttribute(WeblogPlugin.ATTR_ISWEBLOG)) )
            {
                String blogFeedURL = engine.getURL(WikiContext.NONE,"rss.jsp","page="+encodedName,true);
                String atomFeedURL = engine.getURL(WikiContext.NONE,"rss.jsp","page="+encodedName+"&amp;type=atom",true);
        
                pageContext.getOut().print("<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS feed for weblog "+
                                           siteName+".\" href=\""+blogFeedURL+"\" />\n");

                pageContext.getOut().print("<link rel=\"service.feed\" type=\"application/atom+xml\" title=\"Atom 1.0 weblog feed for "+
                                           siteName+"\" href=\""+atomFeedURL+"\" />\n");
            }
        }

        return SKIP_BODY;
    }
}
