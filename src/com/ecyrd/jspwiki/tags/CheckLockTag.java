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
import com.ecyrd.jspwiki.PageManager;
import com.ecyrd.jspwiki.PageLock;
import com.ecyrd.jspwiki.providers.ProviderException;

import javax.servlet.http.HttpSession;

/**
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class CheckLockTag
    extends WikiTagBase
{
    public static final int LOCKED    = 0;
    public static final int NOTLOCKED = 1;

    private int m_mode;

    public void setMode( String arg )
    {
        if( "locked".equals(arg) )
        {
            m_mode = LOCKED;
        }
        else
        {
            m_mode = NOTLOCKED;
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
            PageManager mgr = engine.getPageManager();

            PageLock lock = mgr.getCurrentLock( page );

            HttpSession session = pageContext.getSession();

            PageLock userLock = (PageLock) session.getAttribute("lock-"+page.getName());

            if( (lock != null && m_mode == LOCKED && lock != userLock ) ||
                (lock == null && m_mode == NOTLOCKED) )
            {
                pageContext.setAttribute( getId(),
                                          lock );

                return EVAL_BODY_INCLUDE;
            }
        }

        return SKIP_BODY;
    }

}
