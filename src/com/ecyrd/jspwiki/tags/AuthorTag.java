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
 *  Writes the author name of the current page.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class AuthorTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine = m_wikiContext.getEngine();
        WikiPage   page   = m_wikiContext.getPage();

        String author = page.getAuthor();

        if( author != null )
        {
            if( engine.pageExists(author) )
            {
                // FIXME: It's very boring to have to do this.
                //        Slow, too.

                TranslatorReader tr = new TranslatorReader( m_wikiContext, 
                                                            new StringReader("") );
                author = tr.makeLink( TranslatorReader.READ,
                                      author,
                                      author );
            }
            pageContext.getOut().print( author );

        }
        else
        {
            pageContext.getOut().print( "unknown" );
        }

        return SKIP_BODY;
    }
}
