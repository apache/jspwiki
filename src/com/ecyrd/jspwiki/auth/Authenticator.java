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
 * Authenticator acts as a unified interface and simplified loader of 
 * a dynamically defined WikiAuthenticator object.
 *
 * <p>JSPWiki authentication features users, roles, and permissions.
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
 * <p>The source of user information depends on the actual
 * authenticator class used. The simplest case is the FileAuthenticator
 * with simple file-based entries.
 *
 * <p>A successful authentication modifies the checked UserProfile
 * by setting its status to VALIDATED and adding its roles and
 * permissions. (In practice, WikiEngine also creates a non-validated
 * UserProfile with certain default roles for guest users. This,
 * however, is not the concern of the auth package.)
 *
 * <p>There are certain special identifiers used in the 
 * authentication system. You need to be aware of these when
 * setting access controls.
 *
 * <ul>
 * <li><b>ALL</b> is a special group identifier; it is parsed
 *     separately and allows or denies access by all groups.
 *     Users should never be configured to belong to ALL.
 *     (See AccessRuleSet for more information.)
 * <li><b>admin</b> is a special group identifier that always
 *     has access to everything. Admin should never be used
 *     in the page access directives, and only trusted users
 *     should be configured into the admin group.
 * </ul>
 *
 * See AccessRuleSet for more information on how page-level
 * access configuration works.
 */
public class Authenticator
{
    public static final String PROP_AUTHENTICATOR = "jspwiki.authenticator";
    public static final String MSG_AUTH = "authmsg";

    static Category log = Category.getInstance( Authenticator.class );

    private WikiAuthenticator m_auth;


    /**
     * Creates a new Authenticator, using the class defined in the
     * properties (jspwiki.properties file). If none is defined, 
     * uses the default DummyAuthenticator.
     */
    public Authenticator( Properties props )
        throws WikiException, NoRequiredPropertyException
    {
        String classname = null;
        try
        {
            classname = props.getProperty( PROP_AUTHENTICATOR );
            if( classname == null )
            {
                log.warn( "jspwiki.authenticator not defined; using DummyAuthenticator." );
                classname = "DummyAuthenticator";
            }
            Class authClass = WikiEngine.findWikiClass( classname, "com.ecyrd.jspwiki.auth" );
            m_auth = (WikiAuthenticator)authClass.newInstance();
            m_auth.initialize( props );
        }
        catch( ClassNotFoundException e )
        {
            log.error( "Unable to locate authenticator class " + classname, e );
            throw new WikiException( "no authenticator class" );
        }
        catch( InstantiationException e )
        {
            log.error( "Unable to create authenticator class " + classname, e );
            throw new WikiException( "faulty provider class" );
        }
        catch( IllegalAccessException e )
        {
            log.error( "Illegal access to authenticator class " + classname, e );
            throw new WikiException( "illegal provider class" );
        }
        catch( NoRequiredPropertyException e )
        {
            log.error("Authenticator did not found a property it was looking for: " + e.getMessage(), e );
            throw e;  // Same exception works.
        }
    }


    /**
     * Calls the current authenticator's authenticate() method.
     */
    public boolean authenticate( UserProfile wup )
    {
        if( m_auth != null )
        {
            return( m_auth.authenticate( wup ) );
        }
        else
        {
            log.warn( "No authenticator has been initialized!" );
            return( false );
        }
    }


    /**
     * Returns the Authenticator in use.
     */
    public WikiAuthenticator getAuthenticator()
    {
        return( m_auth );
    }
    
}



