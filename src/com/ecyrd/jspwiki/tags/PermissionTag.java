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

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.WikiProvider;
import com.ecyrd.jspwiki.auth.AccessRuleSet;
import com.ecyrd.jspwiki.UserProfile;



/**
 * Tells if a page may be accessed.  
 * This tag takes care of all possibilities, user permissions, page version, etc.
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
        WikiEngine engine = m_wikiContext.getEngine();
        WikiPage   page   = m_wikiContext.getPage();

        if( page != null )
        {
            HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
            UserProfile wup = engine.getUserProfile( request );
            AccessRuleSet rules = page.getAccessRules();
            
            if( rules == null )
            {
                log.error( "No rules for page found. Should be impossible." );
                return SKIP_BODY;
            }
            if( wup == null )
            {
                // May happen when strict login policy is used.
                return SKIP_BODY;
            }

            if( "edit".equals(m_permission) )
            {
                if( rules.hasWriteAccess( wup ) )
                {
                    //
                    //  Check if we're at the current version (old versions cannot
                    //  be edited).
                    //
                    WikiPage latest = engine.getPage( page.getName() );
                    if( page.getVersion() == WikiProvider.LATEST_VERSION ||
                        latest.getVersion() == page.getVersion() )
                    {
                        return EVAL_BODY_INCLUDE;
                    }
                }
            }
            else if( "read".equals(m_permission) )
            {
                if( rules.hasReadAccess( wup ) )
                {
                    return EVAL_BODY_INCLUDE;
                }
            }
            else
            {
                throw new IOException("Unknown permission requested: "+m_permission);
            }
        }

        return SKIP_BODY;
    }
}
