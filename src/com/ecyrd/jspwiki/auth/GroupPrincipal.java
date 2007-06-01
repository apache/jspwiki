/*
 * JSPWiki - a JSP-based WikiWiki clone. Copyright (C) 2001-2003 Janne Jalkanen
 * (Janne.Jalkanen@iki.fi) This program is free software; you can redistribute
 * it and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. You should have
 * received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.ecyrd.jspwiki.auth;

import java.security.Principal;

/**
 * Immutable Principal that represents a Group. GroupPrincipals are injected
 * into a Subject's principal list at the time of authentication (login), and
 * serve as proxies for Group objects for the purposes of making Java 2 security
 * policy decisions. We add GroupPrincipals instead of the actual Groups because
 * calling classes should never be able to obtain a mutable object (Group
 * memberships can be changed by callers). Administrators who wish to grant
 * privileges to specific wiki groups via the security policy file should always specify
 * principals of type GroupPrincipal.
 * @see com.ecyrd.jspwiki.auth.authorize.Group
 * @author Andrew Jaquith
 * @since 2.3.79
 */
public final class GroupPrincipal implements Principal
{
    private final String m_name;

    /**
     * Constructs a new GroupPrincipal object with a supplied name.
     *
     * @param group the wiki group; cannot be <code>null</code>
     */
    public GroupPrincipal( String group )
    {
        if ( group == null )
        {
            throw new IllegalArgumentException( "Group parameter cannot be null." );
        }
        m_name = group;
    }
    
    /**
     * Returns the name of the group principal.
     * @return the name
     * @see java.security.Principal#getName()
     */
    public final String getName()
    {
        return m_name;
    }

    /**
     * Two GroupPrincipals are equal if their names are equal.
     * @param obj the object to compare
     * @return the result of the equality test
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public final boolean equals( Object obj )
    {
        if ( !( obj instanceof GroupPrincipal ) )
        {
            return false;
        }
        GroupPrincipal p = (GroupPrincipal)obj;
        return p.m_name.equals( m_name );
    }

    /**
     * Returns the hashcode for this object.
     * @return the hash code
     * @see java.lang.Object#hashCode()
     */
    public final int hashCode()
    {
        return m_name.hashCode();
    }

    /**
     * Returns a string representation of this object.
     * @return the string
     * @see java.lang.Object#toString()
     */
    public final String toString()
    {
        return "[GroupPrincipal " + m_name + "]";
    }

}
