/* 
 JSPWiki - a JSP-based WikiWiki clone.

 Copyright (C) 2001-2004 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.auth.authorize;

import java.security.Principal;

/**
 * A lightweight, immutable Principal that represents a built-in wiki role such
 * as Anonymous, Asserted and Authenticated. It can also represent dynamic roles
 * used by an external {@link com.ecyrd.jspwiki.auth.Authorizer}, such as a web
 * container.
 * @author Andrew Jaquith
 * @version $Revision: 1.3 $ $Date: 2005-08-07 22:06:09 $
 * @since 2.3
 */
public final class Role implements Principal
{

    /** If the user has authenticated and is an adminstrator */
    public static final Role ADMIN         = new Role( "Admin" );

    /** All users, regardless of authentication status */
    public static final Role ALL           = new Role( "All" );

    /** If the user hasn't supplied a name */
    public static final Role ANONYMOUS     = new Role( "Anonymous" );

    /** If the user has supplied a cookie with a username */
    public static final Role ASSERTED      = new Role( "Asserted" );

    /** If the user has authenticated with the Container or UserDatabase */
    public static final Role AUTHENTICATED = new Role( "Authenticated" );

    private final String   m_name;

    /**
     * Constructs a new Role with a given name.
     * @param name the name of the Role
     */
    public Role( String name )
    {
        m_name = name;
    }

    /**
     * Returns <code>true</code> if a supplied Role is a built-in Role:
     * {@link #ADMIN}, {@link #ALL}, {@link #ANONYMOUS}, {@link #ASSERTED},
     * or {@link #AUTHENTICATED}.
     * @param role the role to check
     * @return the result of the check
     */
    public static final boolean isBuiltInRole(Role role)
    {
        return (role.equals(ADMIN) || role.equals(ALL) || role.equals(ANONYMOUS) ||
                role.equals(ASSERTED) || role.equals(AUTHENTICATED));
        
    }
    
    /**
     * Returns <code>true</code> if the supplied name is identical to the name
     * of a built-in Role; that is, the value returned by <code>getName()</code>
     * for built-in Roles {@link #ADMIN}, {@link #ALL}, {@link #ANONYMOUS},
     * {@link #ASSERTED}, or {@link #AUTHENTICATED}.
     * @param name the name to be tested
     * @return <code>true</code> if the name is reserved; <code>false</code>
     *         if not
     */
    public static final boolean isReservedName(String name)
    {
        return (name.equals(ADMIN.m_name) || name.equals(ALL.m_name) || 
                name.equals(ANONYMOUS.m_name) || name.equals(ASSERTED.m_name) || 
                name.equals(AUTHENTICATED.m_name));
    }
    
    /**
     * Two Role objects are considered equal if their names are identical.
     * @return <code>true</code> if both objects are of type Role and have identical names
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public final boolean equals( Object obj )
    {
        if ( obj == null || !( obj instanceof Role ) ) { return false; }
        return ( m_name.equals( ( (Role) obj ).getName() ) );
    }

    /**
     *  Returns the name of the Principal.
     * @return the name of the Role
     */
    public final String getName()
    {
        return m_name;
    }
    
    /**
     * Returns a String representation of the role
     * @return the string representation of the role
     * @see java.lang.Object#toString()
     */
    public final String toString() {
        return "[" + this.getClass().getName() + ": " + m_name + "]"; 
    }

}