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
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.attachment.Attachment;

/**
 *  Writes a link to a Wiki page.  Body of the link becomes the actual text.
 *  The link is written regardless to whether the page exists or not.
 *
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>page - Page name to refer to.  Default is the current page.
 *    <LI>format - either "anchor" or "url" to output either an <A>... or just the HREF part of one.
 *  </UL>
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class LinkToTag
    extends WikiLinkTag
{
    private String m_version = null;

    public String getVersion()
    {
        return m_version;
    }

    public void setVersion( String arg )
    {
        m_version = arg;
    }

    public int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine   = m_wikiContext.getEngine();
        String     pageName = m_pageName;
        boolean    isattachment = false;

        if( m_pageName == null )
        {
            WikiPage p = m_wikiContext.getPage();

            if( p != null )
            {
                pageName = p.getName();

                isattachment = (p instanceof Attachment);
            }
            else
            {
                return SKIP_BODY;
            }
        }

        JspWriter out = pageContext.getOut();
        String url;
        String linkclass;

        if( isattachment )
        {
            url = engine.getBaseURL()+"attach?page="+engine.encodeName(pageName);
            linkclass = "attachment";
        }
        else
        {
            url = engine.getViewURL( pageName );
            linkclass = "wikipage";
        }

        if( getVersion() != null )
        {
            url += "&version="+getVersion();
        }

        switch( m_format )
        {
          case ANCHOR:
            out.print("<A CLASS=\""+linkclass+"\" HREF=\""+url+"\">");
            break;
          case URL:
            out.print( url );
            break;
        }

        return EVAL_BODY_INCLUDE;
    }

}
