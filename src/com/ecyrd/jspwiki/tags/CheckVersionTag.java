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
import java.io.StringReader;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.TranslatorReader;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  Does a version check on the page.  Mode is as follows:
 *  <UL>
 *   <LI>latest = Include page content, if the page is the latest version.
 *   <LI>notlatest = Include page content, if the page is NOT the latest version.
 *  </UL>
 *  If the page does not exist, body content is never included.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class CheckVersionTag
    extends WikiTagBase
{
    public static final int LATEST    = 0;
    public static final int NOTLATEST = 1;

    private int m_mode;

    public void setMode( String arg )
    {
        if( "latest".equals(arg) )
        {
            m_mode = LATEST;
        }
        else
        {
            m_mode = NOTLATEST;
        }
    }

    public final int doWikiStartTag()
        throws IOException,
               ProviderException
    {
        WikiEngine engine = m_wikiContext.getEngine();
        WikiPage   page   = m_wikiContext.getPage();

        if( page != null && engine.pageExists(page) )
        {
            int version = page.getVersion();
            boolean include = false;

            WikiPage latest = engine.getPage( page.getName() );

            switch( m_mode )
            {
              case LATEST:
                include = (version < 0) || (latest.getVersion() == page.getVersion());
                break;

              case NOTLATEST:
                include = (version > 0) && (latest.getVersion() != page.getVersion());
                break;
            }

            if( include )
            {
                return EVAL_BODY_INCLUDE;
            }
        }

        return SKIP_BODY;
    }
}
