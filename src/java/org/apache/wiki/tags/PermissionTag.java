/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package org.apache.wiki.tags;

import java.io.IOException;
import java.security.Permission;

import org.apache.commons.lang.StringUtils;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.WikiSession;
import org.apache.wiki.action.GroupActionBean;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.authorize.Group;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.permissions.GroupPermission;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.auth.permissions.WikiPermission;


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

    /**
     * Initializes the tag.
     */
    @Override
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
        boolean gotPermission     = false;
        
        if ( CREATE_GROUPS.equals( permission ) || CREATE_PAGES.equals( permission )
            || EDIT_PREFERENCES.equals( permission ) || EDIT_PROFILE.equals( permission )
            || LOGIN.equals( permission ) )
        {
            gotPermission = mgr.checkPermission( session, new WikiPermission( "*", permission ) );
        }
        else if ( VIEW_GROUP.equals( permission ) )
        {
            Group group = ((GroupActionBean)m_wikiActionBean).getGroup();
            Permission perm = new GroupPermission( group.getName(), GroupPermission.VIEW_ACTION );
            gotPermission = mgr.checkPermission( session, perm );
        }
        else if ( EDIT_GROUP.equals( permission ) )
        {
            Group group = ((GroupActionBean)m_wikiActionBean).getGroup();
            Permission perm = new GroupPermission( group.getName(), GroupPermission.EDIT_ACTION);
            gotPermission = mgr.checkPermission( session, perm );
        }
        else if ( DELETE_GROUP.equals( permission ) )
        {
            Group group = ((GroupActionBean)m_wikiActionBean).getGroup();
            Permission perm = new GroupPermission( group.getName(), GroupPermission.DELETE_ACTION );
            gotPermission = mgr.checkPermission( session, perm );
        }
        else if ( ALL_PERMISSION.equals( permission ) )
        {
            gotPermission = mgr.checkPermission( session, new AllPermission( m_wikiContext.getEngine().getApplicationName() ) );
        }
        else if ( page != null )
        {
            //
            //  Edit tag also checks that we're not trying to edit an
            //  old version: they cannot be edited.
            //
            if( EDIT.equals(permission) )
            {
                try
                {
                    WikiEngine engine = m_wikiContext.getEngine();
                    if ( engine.pageExists( page ) )
                    {
                        WikiPage latest = engine.getPage( page.getName() );
                        if( page.getVersion() != WikiProvider.LATEST_VERSION &&
                            latest.getVersion() != page.getVersion() )
                        {
                            return false;
                        }
                    }
                }
                catch( Exception e )
                {
                    // FIXME: What is the correct action here?
                    log.error( "Failed to find latest version", e );
                    return false;
                }
            }

            Permission p = PermissionFactory.getPagePermission( page, permission );
            gotPermission = mgr.checkPermission( session,
                                                  p );
        }
        
        return gotPermission;
    }
    
    /**
     * Initializes the tag.
     * @return the result of the tag: SKIP_BODY or EVAL_BODY_CONTINUE
     * @throws IOException this exception will never be thrown
     */
    @Override
    public final int doWikiStartTag()
        throws IOException
    {
        for( int i = 0; i < m_permissionList.length; i++ )
        {
            String perm = m_permissionList[i];
         
            boolean hasPermission = false;

            if( perm.charAt(0) == '!' )
            {
                hasPermission = !checkPermission( perm.substring(1) );
            }
            else
            {
                hasPermission = checkPermission( perm );
            }
            
            if( hasPermission )
                return EVAL_BODY_INCLUDE;
        }

        return SKIP_BODY;
    }
}
