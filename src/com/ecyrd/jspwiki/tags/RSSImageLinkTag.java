package com.ecyrd.jspwiki.tags;

import java.io.IOException;
import javax.servlet.jsp.JspWriter;

import com.ecyrd.jspwiki.WikiEngine;

/**
 *  Writes an image link to the RSS file.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class RSSImageLinkTag
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
            out.print("<A HREF=\""+rssURL+"\">");
            out.print("<IMG SRC=\""+engine.getBaseURL()+"images/xml.png\"");
            out.print("BORDER=\"0\" title=\""+getTitle()+"\"/>");
            out.print("</A>");
        }

        return SKIP_BODY;
    }
}
