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
 * @version $Revision: 1.3 $ $Date: 2005-08-07 22:06:09 $
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
     * @see java.security.Principal#getName()
     */
    public final String getName()
    {
        return m_name;
    }
    
    public final String toString()
    {
        return "[UnresolvedPrincipal: " + m_name + "]";
    }

    /**
     * An unresolved principal is equal to another
     * unresolved principal if their names match.
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

}