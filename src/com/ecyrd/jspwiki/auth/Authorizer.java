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
 * Authorizer acts as a unified interface and simplified loader of 
 * a dynamically defined WikiAuthorizer object. Also provides 
 * utility methods for building authorization rules and comparing
 * them agains a WikiUserPrincipal's permissions. 
 *
 *  
 * <p>Currently, the structure of access rules only permits 
 * a simple AND of positive or negative matches. We may want
 * to expand on this, and modify the AccessRule chain structure
 * appropriately.
 */
public class Authorizer
{
    /** In jspwiki.properties, the class name to use as authorizer. */
    public static final String PROP_AUTHORIZER    = "jspwiki.authorizer";

    static Category log = Category.getInstance( Authorizer.class );

    private WikiAuthorizer m_authorizer;

    /** The default role assigned to people who have bothered to name themselves. */
    public static final String AUTH_PARTICIPANT_ROLE = "participant";

    /** The default role assigned to people who haven't bothered to name themselves. */
    public static final String AUTH_GUEST_ROLE = "guest";

    /** The default name assigned to people who haven't bothered to name themselves. */
    public static final String AUTH_UNKNOWN_UID = "unknown";

    /** Special permission key for including all users. */
    public static final String ROLE_ALL = "ALL";

    /** Special role identifier for uninhibited access. */
    public static final String ROLE_ADMIN = "admin";

    /**
     * Creates a new Authorizer, using the class defined in the
     * properties (jspwiki.properties file). If none is defined, 
     * uses the default DummyAuthorizer.
     */
    public Authorizer( Properties props )
        throws WikiException, NoRequiredPropertyException
    {
        String classname = null;
        try
        {
            classname = props.getProperty( PROP_AUTHORIZER );
            if( classname == null )
            {
                log.warn( PROP_AUTHORIZER + " not defined; using DummyAuthorizer." );
                classname = "DummyAuthorizer";
            }
            Class authClass = WikiEngine.findWikiClass( classname, "com.ecyrd.jspwiki.auth" );
            m_authorizer = (WikiAuthorizer)authClass.newInstance();
            m_authorizer.initialize( props );
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
            log.warn( "No authorizer has been initialized!" );
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
            log.warn( "No authorizer has been initialized!" );
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
            log.warn( "No authorizer has been initialized!" );
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
            log.warn( "No authorizer has been initialized!" );
        }

       return( new AccessRuleSet() );
     }

    /**
     * Returns the Authorizer in use.
     */
    public WikiAuthorizer getAuthorizer()
    {
        return( m_authorizer );
    }
    
}
