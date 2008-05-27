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

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.attachment.Attachment;

/**
 *  Returns the parent of the currently requested page.  Weblog entries are recognized
 *  as subpages of the weblog page.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class ParentPageNameTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine = m_wikiContext.getEngine();
        WikiPage   page   = m_wikiContext.getPage();

        if( page != null )
        {
            if( page instanceof Attachment )
            {
                pageContext.getOut().print( engine.beautifyTitle( ((Attachment)page).getParentName()) );
            }
            else
            {
                String name = page.getName();

                int entrystart = name.indexOf("_blogentry_");

                if( entrystart != -1 )
                {
                    name = name.substring( 0, entrystart );
                }

                int commentstart = name.indexOf("_comments_");

                if( commentstart != -1 )
                {
                    name = name.substring( 0, commentstart );
                }

                pageContext.getOut().print( engine.beautifyTitle(name) );
            }
        }

        return SKIP_BODY;
    }
}
