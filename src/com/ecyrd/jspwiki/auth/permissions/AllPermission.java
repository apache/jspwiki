package com.ecyrd.jspwiki.auth.permissions;

import java.security.Permission;
import java.security.PermissionCollection;

/**
 * <p>
 * Permission to perform all operations on a given wiki.
 * </p>
 * @author Andrew Jaquith
 * @version $Revision: 1.3 $ $Date: 2006-03-30 04:51:37 $
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
        return ( p.m_wiki != null && p.m_wiki.equals( m_wiki ) );
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
        // Permission must be a WikiPermission, PagePermission or AllPermission
        if ( !( permission instanceof WikiPermission ) && !( permission instanceof PagePermission )
                && !( permission instanceof AllPermission ) )
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

        // If the wiki is implied, it's allowed
        return PagePermission.isSubset( m_wiki, wiki );
    }

    /**
     * Creates a new {@link AllPermissionCollection}.
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

}