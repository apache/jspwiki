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
import com.ecyrd.jspwiki.WikiProvider;
import com.ecyrd.jspwiki.auth.AuthorizationManager;
import com.ecyrd.jspwiki.auth.UserProfile;

/**
 *  Tells if a page may be edited.  This tag takes care of all possibilities,
 *  user permissions, page version, etc.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class PermissionTag
    extends WikiTagBase
{
    private String m_permission;

    public void setPermission( String permission )
    {
        m_permission = permission;
    }

    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine  engine         = m_wikiContext.getEngine();
        WikiPage    page           = m_wikiContext.getPage();
        AuthorizationManager mgr   = engine.getAuthorizationManager();
        boolean     got_permission = false;
        UserProfile userprofile    = m_wikiContext.getCurrentUser();
        
        if( page != null )
        {
            //
            //  Edit tag also checks that we're not trying to edit an
            //  old version: they cannot be edited.
            //
            if( "edit".equals(m_permission) )
            {
                WikiPage latest = engine.getPage( page.getName() );
                if( page.getVersion() != WikiProvider.LATEST_VERSION &&
                    latest.getVersion() != page.getVersion() )
                {
                    return SKIP_BODY;
                }
            }

            got_permission = mgr.checkPermission( page,
                                                  userprofile,
                                                  m_permission );
        }

        return got_permission ? EVAL_BODY_INCLUDE : SKIP_BODY;
    }
}
