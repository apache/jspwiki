/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.tags;

import java.io.IOException;
import javax.servlet.jsp.JspWriter;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;

/**
 *  Writes a diff link.  Body of the link becomes the link text.
 *  <P><B>Attributes<B></P>
 *  <UL>
 *    <LI>page - Page name to refer to.  Default is the current page.
 *  </UL>
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class DiffLinkTag
    extends WikiLinkTag
{
    private String m_version;
    private String m_newVersion;

    public String getVersion()
    {
        return m_version;
    }

    public void setVersion( String arg )
    {
        m_version = arg;
    }

    public String getNewVersion()
    {
        return m_newVersion;
    }

    public void setNewVersion( String arg )
    {
        m_newVersion = arg;
    }

    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine   = m_wikiContext.getEngine();
        String     pageName = m_pageName;

        if( m_pageName == null )
        {
            if( m_wikiContext.getPage() != null )
            {
                pageName = m_wikiContext.getPage().getName();
            }
            else
            {
                return SKIP_BODY;
            }
        }

        JspWriter out = pageContext.getOut();
        String encodedlink = engine.encodeName( pageName );

        int r1 = 0;
        int r2 = 0;

        if( getVersion().equals("latest") )
        {
            r1 = m_wikiContext.getPage().getVersion();
            r2 = (r1 > 1) ? (r1-1) : -1;
        }

        switch( m_format )
        {
          case ANCHOR:
            out.print("<A HREF=\""+engine.getBaseURL()+"Diff.jsp?page="+encodedlink+"&r1="+r1+"&r2="+r2+"\">");
            break;

          case URL:
            out.print( engine.getBaseURL()+"Diff.jsp?page="+encodedlink+"&r1="+r1+"&r2="+r2 );
            break;
        }

        return EVAL_BODY_INCLUDE;
    }
}
