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
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.attachment.Attachment;

/**
 *  Returns the currently requested page or attachment size.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class PageSizeTag
    extends WikiTagBase
{
    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine = m_wikiContext.getEngine();
        WikiPage   page   = m_wikiContext.getPage();

        try
        {
            if( page != null && engine.pageExists(page) )
            {
                long size = page.getSize();

                if( size == -1 && !(page instanceof Attachment) )
                {
                    size = engine.getPureText( page.getName(), page.getVersion() ).length();
                }

                pageContext.getOut().write( Long.toString(size) );
            }
        }
        catch( ProviderException e )
        {
            log.warn("Providers did not work: ",e);
            pageContext.getOut().write("Error determining page size: "+e.getMessage());
        }

        return SKIP_BODY;
    }
}
