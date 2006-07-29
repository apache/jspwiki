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
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.UserManager;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.authorize.Group;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.user.UserProfile;

/**
 * <p>
 * Returns user profile attributes, or empty strings if the user has not been
 * validated. This tag has a single attribute, "property." 
 * The <code>property</code> attribute may contain one of the following 
 * case-insensitive values:
 * </p>
 * <ul>
 * <code>created</code> - creation date</li>
 * <code>email</code> - user's e-mail address</li>
 * <code>fullname</code> - user's full name</li>
 * <code>groups</code> - a sorted list of the groups a user belongs to</li>
 * <code>loginname</code> - user's login name. If the current user does not have
 * a profile, the user's login principal (such as one provided by a container
 * login module, user cookie, or anonyous IP address), will supply the login 
 * name property</li>
 * <code>roles</code> - a sorted list of the roles a user possesses</li>
 * <code>wikiname</code> - user's wiki name</li>
 * <code>modified</code> - last modification date</li>
 * <code>exists</code> - evaluates the body of the tag if user's profile exists
 * in the user database
 * <code>new</code> - evaluates the body of the tag if user's profile does not
 * exist in the user database
 * </ul>
 * @author Andrew Jaquith
 * @version $Revision: 1.8 $ $Date: 2006-07-29 19:53:29 $
 * @since 2.3
 */
public class UserProfileTag extends WikiTagBase
{
    private static final long serialVersionUID = 3258410625431582003L;

    public  static final String BLANK = "(not set)";
    
    private static final String CREATED   = "created";

    private static final String EMAIL     = "email";

    private static final String EXISTS    = "exists";
    
    private static final String FULLNAME  = "fullname";
    
    private static final String GROUPS    = "groups";
    
    private static final String LOGINNAME = "loginname";
        
    private static final String MODIFIED  = "modified";
    
    private static final String NEW       = "new";
    
    private static final String ROLES     = "roles";
    
    private static final String WIKINAME  = "wikiname";
    
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
        
        if ( EXISTS.equals( m_prop ) )
        {
            return profile.isNew() ? SKIP_BODY : EVAL_BODY_INCLUDE;
        }
        else if ( NEW.equals( m_prop ) )
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
        if ( result != null )
        {
            pageContext.getOut().print( result );
        }
        return SKIP_BODY;
    }

    public void setProperty( String property )
    {
        m_prop = property.toLowerCase().trim();
    }
    
    /**
     * Returns a sorted list of the {@link com.ecyrd.jspwiki.auth.authorize.Group} objects a user possesses
     * in his or her WikiSession. The result is computed by consulting
     * {@link com.ecyrd.jspwiki.WikiSession#getRoles()}
     * and extracting those that are of type Group.
     * @return the list of groups, sorted by name
     */
    public static String printGroups( WikiContext context )
    {
        Principal[] roles = context.getWikiSession().getRoles();
        List tempRoles = new ArrayList();
        for ( int i = 0; i < roles.length; i++ )
        {
            if ( roles[i] instanceof Group )
            {
                tempRoles.add( roles[i].getName() );
            }
        }
        if ( tempRoles.size() == 0 )
        {
            return "(none)";
        }
        
        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < tempRoles.size(); i++ )
        {
            String name = (String)tempRoles.get( i );
            {
                sb.append( name );
                if ( i < ( tempRoles.size() - 1 ) ) 
                {
                    sb.append(',');
                    sb.append(' ');
                }
            }
        }
        return sb.toString();
    }
    
    /**
     * Returns a sorted list of the {@link com.ecyrd.jspwiki.auth.authorize.Role} objects a user possesses
     * in his or her WikiSession. The result is computed by consulting
     * {@link com.ecyrd.jspwiki.WikiSession#getRoles()}
     * and extracting those that are of type Role.
     * @return the list of roles, sorted by name
     */
    public static String printRoles( WikiContext context )
    {
        Principal[] roles = context.getWikiSession().getRoles();
        List tempRoles = new ArrayList();
        for ( int i = 0; i < roles.length; i++ )
        {
            if ( roles[i] instanceof Role )
            {
                tempRoles.add( roles[i].getName() );
            }
        }
        if ( tempRoles.size() == 0 )
        {
            return "(none)";
        }
        
        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < tempRoles.size(); i++ )
        {
            String name = (String)tempRoles.get( i );
            {
                sb.append( name );
                if ( i < ( tempRoles.size() - 1 ) ) 
                {
                    sb.append(',');
                    sb.append(' ');
                }
            }
        }
        return sb.toString();
    }
}
