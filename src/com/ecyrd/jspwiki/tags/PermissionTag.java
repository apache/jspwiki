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
import java.security.Permission;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.WikiProvider;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.AuthorizationManager;
import com.ecyrd.jspwiki.auth.GroupPrincipal;
import com.ecyrd.jspwiki.auth.permissions.AllPermission;
import com.ecyrd.jspwiki.auth.permissions.GroupPermission;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;
import com.ecyrd.jspwiki.ui.Command;
import com.ecyrd.jspwiki.ui.GroupCommand;

/**
 *  Tells whether the user in the current wiki context possesses a particular
 *  permission. The permission is typically a PagePermission (e.g., "edit", "view",
 *  "delete", "comment", "upload"). It may also be a wiki-wide WikiPermission
 *  ("createPages", "createGroups", "editProfile", "editPreferences", "login")
 *  or the administrator permission ("allPermission"). GroupPermissions 
 *  (e.g., "viewGroup", "editGroup", "deleteGroup").
 *
 *  @author Janne Jalkanen
 *  @author Andrew Jaquith
 *  @since 2.0
 */
public class PermissionTag
    extends WikiTagBase
{
    private static final String ALL_PERMISSION = "allPermission";
    private static final String CREATE_GROUPS = "createGroups";
    private static final String CREATE_PAGES = "createPages";
    private static final String DELETE_GROUP = "deleteGroup";
    private static final String EDIT = "edit";
    private static final String EDIT_GROUP = "editGroup";
    private static final String EDIT_PREFERENCES = "editPreferences";
    private static final String EDIT_PROFILE = "editProfile";
    private static final String LOGIN = "login";
    private static final String VIEW_GROUP = "viewGroup";
    
    private static final long serialVersionUID = 3761412993048982325L;
    
    private String m_permission;

    public void initTag()
    {
        super.initTag();
        m_permission = null;
    }

    /**
     * Sets the permission to look for (case sensitive).
     * @param permission
     */
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
        WikiSession session        = m_wikiContext.getWikiSession();
        
        if ( CREATE_GROUPS.equals(m_permission) || CREATE_PAGES.equals(m_permission)
             || EDIT_PREFERENCES.equals( m_permission ) || EDIT_PROFILE.equals( m_permission )
             || LOGIN.equals( m_permission ) )
        {
            got_permission = mgr.checkPermission( session, new WikiPermission( page.getWiki(), m_permission ) );
        }
        else if ( VIEW_GROUP.equals( m_permission ) 
             || EDIT_GROUP.equals( m_permission )
             || DELETE_GROUP.equals( m_permission ) )
        {
            Command command = m_wikiContext.getCommand();
            got_permission = false;
            if ( command instanceof GroupCommand && command.getTarget() != null )
            {
                GroupPrincipal group = (GroupPrincipal)command.getTarget();
                String groupName = group.getWiki() + ":" + group.getName();
                String action = "view";
                if ( EDIT_GROUP.equals( m_permission ) )
                {
                    action = "edit";
                }
                else if ( DELETE_GROUP.equals( m_permission ) )
                {
                    action = "delete";
                }
                got_permission = mgr.checkPermission( session, new GroupPermission( groupName, action ) );
            }
        }
        else if ( ALL_PERMISSION.equals( m_permission ) )
        {
            got_permission = mgr.checkPermission( session, new AllPermission( engine.getApplicationName() ) );
        }
        else if ( page != null )
        {
            //
            //  Edit tag also checks that we're not trying to edit an
            //  old version: they cannot be edited.
            //
            if( EDIT.equals(m_permission) )
            {
                WikiPage latest = engine.getPage( page.getName() );
                if( page.getVersion() != WikiProvider.LATEST_VERSION &&
                    latest.getVersion() != page.getVersion() )
                {
                    return SKIP_BODY;
                }
            }

            Permission permission = new PagePermission( page, m_permission );
            got_permission = mgr.checkPermission( session,
                                                  permission );
        }

        return got_permission ? EVAL_BODY_INCLUDE : SKIP_BODY;
    }
}
