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

import org.apache.commons.lang.StringUtils;

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
 *  <p>
 *  Since 2.6, it is possible to list several permissions or use negative permissions,
 *  e.g.
 *  <pre>
 *     &lt;wiki:Permission permission="edit|rename|view"&gt;
 *        You have edit, rename, or  view permissions!
 *     &lt;/wiki:Permission&gt;
 *  </pre>
 *  
 *  or
 *
 *  <pre>
 *     &lt;wiki:Permission permission="!upload"&gt;
 *        You do not have permission to upload!
 *     &lt;/wiki:Permission&gt;
 *  </pre>
 *  
 *  @author Janne Jalkanen
 *  @author Andrew Jaquith
 *  @since 2.0
 */
public class PermissionTag
    extends WikiTagBase
{
    private static final String ALL_PERMISSION   = "allPermission";
    private static final String CREATE_GROUPS    = "createGroups";
    private static final String CREATE_PAGES     = "createPages";
    private static final String DELETE_GROUP     = "deleteGroup";
    private static final String EDIT             = "edit";
    private static final String EDIT_GROUP       = "editGroup";
    private static final String EDIT_PREFERENCES = "editPreferences";
    private static final String EDIT_PROFILE     = "editProfile";
    private static final String LOGIN            = "login";
    private static final String VIEW_GROUP       = "viewGroup";
    
    private static final long serialVersionUID = 3761412993048982325L;
    
    private String[] m_permissionList;

    public void initTag()
    {
        super.initTag();
        m_permissionList = null;
    }

    /**
     * Sets the permissions to look for (case sensitive).  See above for the format.
     * 
     * @param permission A list of permissions
     */
    public void setPermission( String permission )
    {
        m_permissionList = StringUtils.split(permission,'|');
    }

    /**
     *  Checks a single permission.
     *  
     *  @param permission
     *  @return
     */
    private boolean checkPermission( String permission )
    {
        WikiSession session        = m_wikiContext.getWikiSession();
        WikiPage    page           = m_wikiContext.getPage();
        AuthorizationManager mgr   = m_wikiContext.getEngine().getAuthorizationManager();
        boolean got_permission     = false;
        
        if ( CREATE_GROUPS.equals( permission ) || CREATE_PAGES.equals( permission )
            || EDIT_PREFERENCES.equals( permission ) || EDIT_PROFILE.equals( permission )
            || LOGIN.equals( permission ) )
        {
            got_permission = mgr.checkPermission( session, new WikiPermission( page.getWiki(), permission ) );
        }
        else if ( VIEW_GROUP.equals( permission ) 
            || EDIT_GROUP.equals( permission )
            || DELETE_GROUP.equals( permission ) )
        {
            Command command = m_wikiContext.getCommand();
            got_permission = false;
            if ( command instanceof GroupCommand && command.getTarget() != null )
            {
                GroupPrincipal group = (GroupPrincipal)command.getTarget();
                String groupName = group.getWiki() + ":" + group.getName();
                String action = "view";
                if( EDIT_GROUP.equals( permission ) )
                {
                    action = "edit";
                }
                else if ( DELETE_GROUP.equals( permission ) )
                {
                    action = "delete";
                }
                got_permission = mgr.checkPermission( session, new GroupPermission( groupName, action ) );
            }
        }
        else if ( ALL_PERMISSION.equals( permission ) )
        {
            got_permission = mgr.checkPermission( session, new AllPermission( m_wikiContext.getEngine().getApplicationName() ) );
        }
        else if ( page != null )
        {
            //
            //  Edit tag also checks that we're not trying to edit an
            //  old version: they cannot be edited.
            //
            if( EDIT.equals(permission) )
            {
                WikiPage latest = m_wikiContext.getEngine().getPage( page.getName() );
                if( page.getVersion() != WikiProvider.LATEST_VERSION &&
                    latest.getVersion() != page.getVersion() )
                {
                    return false;
                }
            }

            Permission p = new PagePermission( page, permission );
            got_permission = mgr.checkPermission( session,
                                                  p );
        }
        
        return got_permission;
    }
    
    public final int doWikiStartTag()
        throws IOException
    {
        boolean got_permission = false;
        
        for( int i = 0; i < m_permissionList.length; i++ )
        {
            String perm = m_permissionList[i];
         
            boolean has_permission = false;

            if( perm.charAt(0) == '!' )
            {
                has_permission = !checkPermission( perm.substring(1) );
            }
            else
            {
                has_permission = checkPermission( perm );
            }
            
            if( has_permission )
                return EVAL_BODY_INCLUDE;
        }

        return SKIP_BODY;
    }
}
