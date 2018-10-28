/* 
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
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.GroupPrincipal;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.util.TextUtil;

/**
 * <p>
 * Returns user profile attributes, or empty strings if the user has not been
 * validated. This tag has a single attribute, "property."
 * The <code>property</code> attribute may contain one of the following
 * case-insensitive values:
 * </p>
 * <ul>
 * <li><code>created</code> - creation date</li>
 * <li><code>email</code> - user's e-mail address</li>
 * <li><code>fullname</code> - user's full name</li>
 * <li><code>groups</code> - a sorted list of the groups a user belongs to</li>
 * <li><code>loginname</code> - user's login name. If the current user does not have
 * a profile, the user's login principal (such as one provided by a container
 * login module, user cookie, or anonyous IP address), will supply the login
 * name property</li>
 * <li><code>roles</code> - a sorted list of the roles a user possesses</li>
 * <li><code>wikiname</code> - user's wiki name</li>
 * <li><code>modified</code> - last modification date</li>
 * <li><code>exists</code> - evaluates the body of the tag if user's profile exists
 * in the user database
 * <li><code>new</code> - evaluates the body of the tag if user's profile does not
 * exist in the user database
 * <li><code>canChangeLoginName</code> - always true if custom auth used; also true for container auth
 * and current UserDatabase.isSharedWithContainer() is true.</li>
 * <li><code>canChangePassword</code> - always true if custom auth used; also true for container auth
 * and current UserDatabase.isSharedWithContainer() is true.</li>
 * </ul>
 * <p>In addition, the values <code>exists</code>, <code>new</code>, <code>canChangeLoginName</code>
 * and <code>canChangeLoginName</code> can also be prefixed with <code>!</code> to indicate the
 * negative condition (for example, <code>!exists</code>).</p>
 * @since 2.3
 */
public class UserProfileTag extends WikiTagBase
{
    private static final long serialVersionUID = 3258410625431582003L;

    public  static final String BLANK = "(not set)";

    private static final String CREATED   = "created";

    private static final String EMAIL     = "email";

    private static final String EXISTS    = "exists";

    private static final String NOT_EXISTS= "!exists";

    private static final String FULLNAME  = "fullname";

    private static final String GROUPS    = "groups";

    private static final String LOGINNAME = "loginname";

    private static final String MODIFIED  = "modified";

    private static final String NEW       = "new";

    private static final String NOT_NEW   = "!new";

    private static final String ROLES     = "roles";

    private static final String WIKINAME  = "wikiname";

    private static final String CHANGE_LOGIN_NAME     = "canchangeloginname";

    private static final String NOT_CHANGE_LOGIN_NAME = "!canchangeloginname";

    private static final String CHANGE_PASSWORD       = "canchangepassword";

    private static final String NOT_CHANGE_PASSWORD   = "!canchangepassword";

    private String             m_prop;

    public void initTag()
    {
        super.initTag();
        m_prop = null;
    }

    public final int doWikiStartTag() throws IOException, WikiSecurityException
    {
        UserManager manager = m_wikiContext.getEngine().getUserManager();
        UserProfile profile = manager.getUserProfile( m_wikiContext.getWikiSession() );
        String result = null;

        if ( EXISTS.equals( m_prop ) || NOT_NEW.equals( m_prop ) )
        {
            return profile.isNew() ? SKIP_BODY : EVAL_BODY_INCLUDE;
        }
        else if ( NEW.equals( m_prop ) || NOT_EXISTS.equals( m_prop ) )
        {
            return profile.isNew() ? EVAL_BODY_INCLUDE : SKIP_BODY;
        }

        else if ( CREATED.equals( m_prop ) && profile.getCreated() != null )
        {
            result = profile.getCreated().toString();
        }
        else if ( EMAIL.equals( m_prop ) )
        {
            result = profile.getEmail();
        }
        else if ( FULLNAME.equals( m_prop ) )
        {
            result = profile.getFullname();
        }
        else if ( GROUPS.equals( m_prop ) )
        {
            result = printGroups( m_wikiContext );
        }
        else if ( LOGINNAME.equals( m_prop ) )
        {
            result = profile.getLoginName();
        }
        else if ( MODIFIED.equals( m_prop ) && profile.getLastModified() != null )
        {
            result = profile.getLastModified().toString();
        }
        else if ( ROLES.equals( m_prop ) )
        {
            result = printRoles( m_wikiContext );
        }
        else if ( WIKINAME.equals( m_prop ) )
        {
            result = profile.getWikiName();

            if( result == null )
            {
                //
                //  Default back to the declared user name
                //
                WikiEngine engine = this.m_wikiContext.getEngine();
                WikiSession wikiSession = WikiSession.getWikiSession( engine, (HttpServletRequest)pageContext.getRequest() );
                Principal user = wikiSession.getUserPrincipal();

                if( user != null )
                {
                    result = user.getName();
                }
            }
        }
        else if ( CHANGE_PASSWORD.equals( m_prop ) || CHANGE_LOGIN_NAME.equals( m_prop ) )
        {
            AuthenticationManager authMgr = m_wikiContext.getEngine().getAuthenticationManager();
            if ( !authMgr.isContainerAuthenticated() )
            {
                return EVAL_BODY_INCLUDE;
            }
        }
        else if ( NOT_CHANGE_PASSWORD.equals( m_prop ) || NOT_CHANGE_LOGIN_NAME.equals( m_prop ) )
        {
            AuthenticationManager authMgr = m_wikiContext.getEngine().getAuthenticationManager();
            if ( authMgr.isContainerAuthenticated() )
            {
                return EVAL_BODY_INCLUDE;
            }
        }

        if ( result != null )
        {
            pageContext.getOut().print( TextUtil.replaceEntities(result) );
        }
        return SKIP_BODY;
    }

    public void setProperty( String property )
    {
        m_prop = property.toLowerCase().trim();
    }

    /**
     * Returns a sorted list of the {@link org.apache.wiki.auth.authorize.Group} objects a user possesses
     * in his or her WikiSession. The result is computed by consulting
     * {@link org.apache.wiki.WikiSession#getRoles()}
     * and extracting those that are of type Group.
     * @return the list of groups, sorted by name
     */
    public static String printGroups( WikiContext context )
    {
        Principal[] roles = context.getWikiSession().getRoles();
        List<String> tempRoles = new ArrayList<String>();
        ResourceBundle rb = Preferences.getBundle( context, InternationalizationManager.CORE_BUNDLE );
        
        for ( Principal role : roles )
        {
            if( role instanceof GroupPrincipal )
            {
                tempRoles.add( role.getName() );
            }
        }
        if ( tempRoles.size() == 0 )
        {
            return rb.getString("userprofile.nogroups");
        }

        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < tempRoles.size(); i++ )
        {
            String name = tempRoles.get( i );

            sb.append( name );
            if ( i < ( tempRoles.size() - 1 ) )
            {
                sb.append(',');
                sb.append(' ');
            }

        }
        return sb.toString();
    }

    /**
     * Returns a sorted list of the {@link org.apache.wiki.auth.authorize.Role} objects a user possesses
     * in his or her WikiSession. The result is computed by consulting
     * {@link org.apache.wiki.WikiSession#getRoles()}
     * and extracting those that are of type Role.
     * @return the list of roles, sorted by name
     */
    public static String printRoles( WikiContext context )
    {
        Principal[] roles = context.getWikiSession().getRoles();
        List<String> tempRoles = new ArrayList<String>();
        ResourceBundle rb = Preferences.getBundle( context, InternationalizationManager.CORE_BUNDLE );
        
        for ( Principal role : roles )
        {
            if ( role instanceof Role )
            {
                tempRoles.add( role.getName() );
            }
        }
        if ( tempRoles.size() == 0 )
        {
            return rb.getString( "userprofile.noroles" );
        }

        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < tempRoles.size(); i++ )
        {
            String name = tempRoles.get( i );

            sb.append( name );
            if ( i < ( tempRoles.size() - 1 ) )
            {
                sb.append(',');
                sb.append(' ');
            }

        }
        return sb.toString();
    }
}
