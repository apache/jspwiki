/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
            out.print("<a href=\"http://127.0.0.1:5335/system/pages/subscriptions/?url="+rssURL+"\">");
            out.print("<img src=\""+engine.getBaseURL()+"images/xmlCoffeeCup.png\"");
            out.print("border=\"0\" title=\""+getTitle()+"\"/>");
            out.print("</a>");
        }

        return SKIP_BODY;
    }
}
