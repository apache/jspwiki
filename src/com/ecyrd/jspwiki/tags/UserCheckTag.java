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

/**
 *  Includes the content if an user check validates.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class UserCheckTag
    extends WikiTagBase
{
    private String m_exists;

    public String getExists()
    {
        return m_exists;
    }

    public void setExists( String arg )
    {
        m_exists = arg;
    }

    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine = m_wikiContext.getEngine();
        String user = engine.getUserName( (javax.servlet.http.HttpServletRequest)pageContext.getRequest() );

        if( "true".equals(getExists()) && user != null )
        {
            return EVAL_BODY_INCLUDE;
        }
        else if( "false".equals(getExists()) && user == null )
        {
            return EVAL_BODY_INCLUDE;
        }

        return SKIP_BODY;
    }
}
