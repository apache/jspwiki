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
 *  Writes page content in HTML.
 *  <P><B>Attributes<B></P>
 *  <UL>
 *    <LI>page - Page name to refer to.  Default is the current page.
 *  </UL>
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class InsertPageTag
    extends WikiTagBase
{
    public static final int HTML  = 0;
    public static final int PLAIN = 1;

    protected String m_pageName;
    private   int    m_mode = HTML;

    public void setPage( String page )
    {
        m_pageName = page;
    }

    public String getPage()
    {
        return m_pageName;
    }

    public void setMode( String arg )
    {
        if( "plain".equals(arg) )
            m_mode = PLAIN;
        else
            m_mode = HTML;
    }

    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine = m_wikiContext.getEngine();
        WikiPage   page;

        if( m_pageName == null )
        {
            page = m_wikiContext.getPage();
        }
        else
        {
            page = engine.getPage( m_pageName );
        }

        if( page != null )
        {
            JspWriter out = pageContext.getOut();

            switch(m_mode)
            {
              case HTML:
                out.print( engine.getHTML(m_wikiContext, page) );
                break;
              case PLAIN:
                out.print( engine.getText(m_wikiContext, page) );
                break;
            }
        }

        return SKIP_BODY;
    }
}
