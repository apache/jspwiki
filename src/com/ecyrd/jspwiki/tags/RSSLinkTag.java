package com.ecyrd.jspwiki.tags;

import java.io.IOException;

import com.ecyrd.jspwiki.WikiEngine;

/**
 *  Writes a link to the RSS file (using the "link" attribute).
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
