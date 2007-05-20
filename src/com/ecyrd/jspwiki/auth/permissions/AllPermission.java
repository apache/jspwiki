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
package com.ecyrd.jspwiki.auth.permissions;

import java.security.Permission;
import java.security.PermissionCollection;

/**
 * <p>
 * Permission to perform all operations on a given wiki.
 * </p>
 * @author Andrew Jaquith
 * @since 2.3.80
 */
public final class AllPermission extends Permission
{
    private static final long   serialVersionUID = 1L;

    private static final String WILDCARD         = "*";

    private final String        m_wiki;

    /**
     * Creates a new WikiPermission for a specified set of actions.
     * @param wiki the wiki to which the permission should apply
     */
    public AllPermission( String wiki )
    {
        super( wiki );
        m_wiki = ( wiki == null ) ? WILDCARD : wiki;
    }

    /**
     * Two AllPermission objects are considered equal if their wikis are equal.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public final boolean equals( Object obj )
    {
        if ( !( obj instanceof AllPermission ) )
        {
            return false;
        }
        AllPermission p = (AllPermission) obj;
        return p.m_wiki != null && p.m_wiki.equals( m_wiki );
    }

    /**
     * No-op; always returns <code>null</code>
     * @see java.security.Permission#getActions()
     */
    public final String getActions()
    {
        return null;
    }

    /**
     * Returns the name of the wiki containing the page represented by this
     * permission; may return the wildcard string.
     * @return the wiki
     */
    public final String getWiki()
    {
        return m_wiki;
    }

    /**
     * Returns the hash code for this WikiPermission.
     * @see java.lang.Object#hashCode()
     */
    public final int hashCode()
    {
        return m_wiki.hashCode();
    }

    /**
     * WikiPermission can only imply other WikiPermissions; no other permission
     * types are implied. One WikiPermission implies another if all of the other
     * WikiPermission's actions are equal to, or a subset of, those for this
     * permission.
     * @param permission the permission which may (or may not) be implied by
     *            this instance
     * @return <code>true</code> if the permission is implied,
     *         <code>false</code> otherwise
     * @see java.security.Permission#implies(java.security.Permission)
     */
    public final boolean implies( Permission permission )
    {
        // Permission must be a JSPWiki permission, PagePermission or AllPermission
        if ( !isJSPWikiPermission( permission ) )
        {
            return false;
        }
        String wiki = null;
        if ( permission instanceof AllPermission )
        {
            wiki = ( (AllPermission) permission ).getWiki();
        }
        else if ( permission instanceof PagePermission )
        {
            wiki = ( (PagePermission) permission ).getWiki();
        }
        if ( permission instanceof WikiPermission )
        {
            wiki = ( (WikiPermission) permission ).getWiki();
        }
        if ( permission instanceof GroupPermission )
        {
            wiki = ( (GroupPermission) permission ).getWiki();
        }

        // If the wiki is implied, it's allowed
        return PagePermission.isSubset( m_wiki, wiki );
    }

    /**
     * Returns a new {@link AllPermissionCollection}.
     * @see java.security.Permission#newPermissionCollection()
     */
    public PermissionCollection newPermissionCollection()
    {
        return new AllPermissionCollection();
    }

    /**
     * Prints a human-readable representation of this permission.
     * @see java.lang.Object#toString()
     */
    public final String toString()
    {
        return "(\"" + this.getClass().getName() + "\",\"" + m_wiki + "\")";
    }

    protected static final boolean isJSPWikiPermission( Permission permission )
    {
        return   permission instanceof WikiPermission ||
                 permission instanceof PagePermission ||
                 permission instanceof GroupPermission ||
                 permission instanceof AllPermission;
    }

}
