package com.ecyrd.jspwiki.tags;

import java.io.IOException;
import javax.servlet.jsp.JspWriter;

import com.ecyrd.jspwiki.WikiEngine;

/**
 *  Writes an image link to the RSS file with the Coffee Cup
 *  for Userland aggregation.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class RSSCoffeeCupLinkTag
    extends WikiTagBase
{
    protected String m_title;

    public void setTitle( String title )
    {
        m_title = title;
    }

    public String getTitle()
    {
        return m_title;
    }

    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine = m_wikiContext.getEngine();

        String rssURL = engine.getGlobalRSSURL();

        if( rssURL != null )
        {
            JspWriter out = pageContext.getOut();
            out.print("<A HREF=\"http://127.0.0.1:5335/system/pages/subscriptions/?url="+rssURL+"\">");
            out.print("<IMG SRC=\""+engine.getBaseURL()+"images/xmlCoffeeCup.png\"");
            out.print("BORDER=\"0\" title=\""+getTitle()+"\"/>");
            out.print("</A>");
        }

        return SKIP_BODY;
    }
}
