/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.auth;


import java.util.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.UserProfile;


/**
 * Authorizer retrieves access authorities for UserProfiles.
 *
 * <P>Authorizer acts as a unified interface and simplified loader of 
 * a dynamically defined WikiAuthorizer object. Also provides 
 * utility methods for building authorization rules and comparing
 * them agains a WikiUserPrincipal's permissions. 
 *
 * <P>Authorizer also takes care of the special case where no
 * custom WikiAuthorizer has been defined in <i>jspwiki.properties</i>.
 * In this case, access is always allowed. 
 *  
 *
 * <p>JSPWiki authorization features users, roles, and permissions.
 * Users are validated against a password, and, if successful, their
 * roles and permissions are loaded into a UserProfile. These can
 * then be compared to per-page access restrictions.
 *
 * <p><i>Roles</i> are, essentially, groups that a user can belong to.
 * Pages may be restricted to/from certain roles with the ALLOW and
 * DENY directives.
 *
 * <p><i>Permissions</i> offer a more fine-grained access system:
 * roles can be further mapped to any number of permissions. This
 * may be useful with a large user base. Pages may be restricted
 * to/from permissions with the REQUIRE directive. 
 * 
 * <p>As an example, you might have user <i>quibble</i> with password
 * <i>frobozz</i> (hopefully encrypted in storage), who belongs to 
 * groups <i>users</i> and <i>editors</i>; <i>users</i> may specify
 * permissions <i>read-user-data</i>, editors <i>read-all</i> and
 * <i>write-user-data</i>. Quibble would thus have access to pages
 * restricted to groups users and editors, and to those pages that
 * require permissions read-user-data, read-all, or write-user-data.
 * 
 * <P>Authorizer specifies the following special roles:
 * <ul>
 * <li> <i>guest</i> (AUTH_ROLE_GUEST) - the role given to casual 
 *      visitors who haven't been authenticated or recognized in
 *      any way.
 * <li> <i>participant</i> (AUTH_PARTICIPANT_ROLE) - the role given 
 *      to visitors who have set their name (user properties) and
 *      allowed JSPWiki to store a cookie on their computer.
 * <li> <i>admin</i> (AUTH_AUTH_ROLE_ADMIN) - a role that always has
 *      access everywhere, whatever the explicit rules state.
 *      (This needs to be assigned to administrative users in the
 *      authorization source.)
 * <li> <i>ALL</i> (AUTH_ROLE_ALL) - a special label used in access
 *      rules that implies everyone. Mainly useful in a deny all/allow all
 *      purpose. 
 *
 */
public class Authorizer
{
    /** In jspwiki.properties, the class name to use as authorizer. */
    public static final String PROP_AUTHORIZER    = "jspwiki.authorizer";

    static Category log = Category.getInstance( Authorizer.class );

    private WikiAuthorizer m_authorizer;

    /** The default role assigned to people who have bothered to name themselves. */
    public static final String AUTH_ROLE_PARTICIPANT = "participant";

    /** The default role assigned to people who haven't bothered to name themselves. */
    public static final String AUTH_ROLE_GUEST = "guest";

    /** The default name assigned to people who haven't bothered to name themselves. */
    public static final String AUTH_UNKNOWN_UID = "unknown";

    /** Special permission key for including all users. */
    public static final String AUTH_ROLE_ALL = "ALL";

    /** Special role identifier for uninhibited access. */
    public static final String AUTH_ROLE_ADMIN = "admin";

    private AccessRuleSet m_openAccess;


    /**
     * Creates a new Authorizer, using the class defined in the
     * properties (jspwiki.properties file). If none is defined, 
     * full access is always allowed.
     */
    public Authorizer( Properties props )
        throws WikiException, NoRequiredPropertyException
    {
        // Special case: open access if no authorizer is used. 
        // The following could be optimized with a TrivialAccessRuleSet. FIX!
        m_openAccess = new AccessRuleSet();
        m_openAccess.addRule( "ALLOW READ ALL" );
        m_openAccess.addRule( "ALLOW WRITE ALL" );

        String classname = null;
        try
        {
            classname = props.getProperty( PROP_AUTHORIZER );
            if( classname == null )
            {
                log.warn( PROP_AUTHORIZER + " not defined; open access mode is used." );
                m_authorizer = null;
            }
            else
            {
                Class authClass = WikiEngine.findWikiClass( classname, "com.ecyrd.jspwiki.auth" );
                m_authorizer = (WikiAuthorizer)authClass.newInstance();
                m_authorizer.initialize( props );
            }
        }
        catch( ClassNotFoundException e )
        {
            log.error( "Unable to locate authorizer class " + classname, e );
            throw new WikiException( "no authorizer class " + classname );
        }
        catch( InstantiationException e )
        {
            log.error( "Unable to create authorizer class " + classname, e );
            throw new WikiException( "faulty provider class " + classname );
        }
        catch( IllegalAccessException e )
        {
            log.error( "Illegal access to authorizer class " + classname, e );
            throw new WikiException( "illegal provider class " + classname );
        }
        catch( NoRequiredPropertyException e )
        {
            log.error("Authorizer did not found a property it was looking for: " + 
                      e.getMessage(), e );
            throw e;  // Same exception works.
        }
    }


    /**
     * Returns true if a WikiAuthorizer has been initialized and is used for 
     * authorization, false if not. 
     */
    public boolean usePageRules()
    {
        return( m_authorizer != null );
    }


    /**
     * Calls the current authorizer's loadPermissions() method.
     * (This should be renamed to loadAccessInfo, or something,
     * since it loads roles as well as permissions. FIX!)
     */
    public void loadPermissions( UserProfile wup )
    {
        if( m_authorizer != null )
        {
            m_authorizer.loadPermissions( wup );
        }
        else
        {
            // Open access mode, nothing needs to be done. 
            log.debug( "No Authorizer defined in jspwiki.properties. " +
                       wup.getName() + " has full access." );
        }
    }

    /**
     * Calls the current authorizer's addRole() method.
     */
    public void addRole( UserProfile wup, String roleName )
    {
        if( m_authorizer != null )
        {
            m_authorizer.addRole( wup, roleName );
        }
        else
        {
            // Open access mode, nothing is done. 
            log.debug( "No Authorizer defined in jspwiki.properties - role " + roleName + 
                       " not added for " + wup.getName() + "." );
        }
    }

    /**
     * Calls the current authorizer's addPermission() method.
     */
    public void addPermission( UserProfile wup, String permName )
    {
        if( m_authorizer != null )
        {
            m_authorizer.addPermission( wup, permName );
        }
        else
        {
            // Open access mode, nothing is done.
            log.debug( "No Authorizer defined in jspwiki.properties - perm " + permName + 
                       " not added for " + wup.getName() + "." );
        }
    }


    /**
     * Returns a copy of the default permissions. This object is
     * safe to modify.
     *
     * <p>If no default permissions exist, returns an empty AccessRuleSet.
     */
    public AccessRuleSet getDefaultPermissions()
    {
       if( m_authorizer != null )
        {
            return( m_authorizer.getDefaultPermissions() );
        }
        else
        {
            // No authorizer defined, open access.
            log.debug( "XXX Providing default rules: " + m_openAccess );
            return( m_openAccess );
        }
     }

    /**
     * Returns the Authorizer in use.
     * A null value means that no authorizer is in use and access is 
     * always allowed.
     */
    public WikiAuthorizer getAuthorizer()
    {
        return( m_authorizer );
    }
    
}
