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
import java.io.StringReader;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.TranslatorReader;

/**
 *  Writes the version of the current page.  If this is
 *  marked as the current version, then includes body as text instead of
 *  version number.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class PageVersionTag
    extends WikiTagBase
{
    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine = m_wikiContext.getEngine();
        WikiPage   page   = m_wikiContext.getPage();

        if( page != null )
        {
            int version = page.getVersion();
            String verstr;

            if( version > 0 )
            {
                pageContext.getOut().print( Integer.toString(version) );
                return SKIP_BODY;
            }
        }

        return EVAL_BODY_INCLUDE;
    }
}
