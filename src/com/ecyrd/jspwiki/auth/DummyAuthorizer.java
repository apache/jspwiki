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
import com.ecyrd.jspwiki.*;

/**
 *
 */
public class DummyAuthorizer
    implements WikiAuthorizer
{

    static Category log = Category.getInstance( DummyAuthorizer.class );

    public static final String PROP_DUMMYROLES = Authorizer.PROP_AUTHORIZER + ".dummy.roles";
    public static final String PROP_DUMMYPERMS = Authorizer.PROP_AUTHORIZER + ".dummy.permissions";
    
    private ArrayList m_dummyPermissions;
    private ArrayList m_dummyRoles;

    public DummyAuthorizer()
    {
    }

    /**
     * Dummy initialization does nothing.
     */
    public void initialize( Properties props )
        throws NoRequiredPropertyException
    {
        m_dummyRoles = new ArrayList();
        m_dummyPermissions = new ArrayList();

        String roles = props.getProperty( PROP_DUMMYROLES );
        String perms = props.getProperty( PROP_DUMMYPERMS );
        log.info( "Using dummy authorization. Everyone has roles " + roles + 
                  " and permissions " + perms );
        if( roles != null )
        {
            StringTokenizer tok = new StringTokenizer( roles, "," );
            while( tok.hasMoreTokens() )
            {
                String role = tok.nextToken();
                m_dummyRoles.add( role.trim() );
            }
        }
        if( perms != null )
        {
            StringTokenizer tok = new StringTokenizer( perms, "," );
            while( tok.hasMoreTokens() )
            {
                String permission = tok.nextToken();
                m_dummyPermissions.add( permission.trim() );
            }
        }
    }

    
    /**
     * Copies the dummy permissions to the user principal.
     */
    public void loadPermissions( UserProfile wup )
    {
        if( m_dummyRoles != null )
        {
            Iterator dummies = m_dummyRoles.iterator();
            while( dummies.hasNext() )
            {
                String role = (String)dummies.next();
                addRole( wup, role );
            }
        }

        if( m_dummyPermissions != null )
        {
            Iterator dummies = m_dummyPermissions.iterator();
            while( dummies.hasNext() )
            {
                String perm = (String)dummies.next();
                addPermission( wup, perm );
            }
        }
    }

    /**
     * Dummy method adds the role without attempting to look 
     * up corresponding permissions.
     */
    public void addRole( UserProfile wup, String roleName )
    {
        if( wup == null || roleName == null )
            return;

        wup.addRole( roleName );
    }

    /**
     * Explicitly adds a given permission to a UserProfile.
     * (Provided here just for uniformity; calls 
     * UserProfile.addPermission().)
     */
    public void addPermission( UserProfile wup, String permName )
    {
        if( wup == null || permName == null )
            return;

        wup.addPermission( permName );
    }

    /**
     * Returns null.
     */
    public AccessRuleSet getDefaultPermissions()
    {
        return( null );
    }

}








