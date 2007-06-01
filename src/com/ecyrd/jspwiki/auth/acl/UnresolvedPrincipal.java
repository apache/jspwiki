/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.auth.acl;

import java.security.Principal;

/**
 * Represents a Principal, typically read from an ACL, that cannot
 * be resolved based on the current state of the user database, group
 * manager, and built-in role definitions.
 * Creating a principal marked "unresolved" allows
 * delayed resolution, which enables principals to be resolved
 * lazily during a later access control check. Conceptuallly,
 * UnresolvedPrincipal performs a function similar to
 * {@link java.security.UnresolvedPermission}.
 * 
 * @author Andrew Jaquith
 * @since 2.3
 */
public final class UnresolvedPrincipal implements Principal
{

    private final String m_name;

    /**
     * Constructs a new UnresolvedPrincipal instance.
     * @param name the name of the Principal
     */
    public UnresolvedPrincipal( String name )
    {
        m_name = name;
    }

    /**
     * Returns the name of the principal.
     * @return the name
     * @see java.security.Principal#getName()
     */
    public final String getName()
    {
        return m_name;
    }
    
    /**
     * Returns a String representation of the UnresolvedPrincipal.
     * @return the String
     */
    public final String toString()
    {
        return "[UnresolvedPrincipal: " + m_name + "]";
    }

    /**
     * An unresolved principal is equal to another
     * unresolved principal if their names match.
     * @param obj the object to compare to this one
     * @return the result of the equality test
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public final boolean equals( Object obj )
    {
        if ( obj instanceof UnresolvedPrincipal )
        {
            return m_name.equals( ( (UnresolvedPrincipal) obj ).m_name );
        }
        return false;
    }

    /**
     *  The hashCode of this object is equal to the hash code of its name.
     *  @return the hash code
     */
    public final int hashCode()
    {
        return m_name.hashCode();
    }
}
