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

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.IOException;

import com.ecyrd.jspwiki.WikiPage;

/**
 *  Writes the modification date of the page, formatted
 *  as specified in the attribute "format".
 *
 *  <UL>
 *   <LI>format = A string describing which format you want to use.
 *       This is exactly like in "java.text.SimpleDateFormat".
 *  </UL>
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */

// FIXME: Should also take the current user TimeZone into account.

public class PageDateTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    public static final String DEFAULT_FORMAT = "dd-MMM-yyyy HH:mm:ss zzz";

    private String m_format = null;

    public String getFormat()
    {
        if( m_format == null )
            return DEFAULT_FORMAT;

        return m_format;
    }

    public void setFormat( String arg )
    {
        m_format = arg;
    }

    public final int doWikiStartTag()
        throws IOException
    {
        WikiPage   page   = m_wikiContext.getPage();

        if( page != null )
        {
            Date d = page.getLastModified();

            //
            //  Date may be null if the page does not exist.
            //
            if( d != null )
            {
                SimpleDateFormat fmt = new SimpleDateFormat( getFormat() );

                pageContext.getOut().write( fmt.format( d ) );
            }
            else
            {
                pageContext.getOut().write( "&lt;never&gt;" );
            }
        }

        return SKIP_BODY;
    }
}
