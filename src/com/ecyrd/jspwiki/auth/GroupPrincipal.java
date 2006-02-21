package com.ecyrd.jspwiki.auth;

import java.security.Principal;

import com.ecyrd.jspwiki.auth.authorize.Group;

/**
 * Implements an immutable Principal that represents a Group. GroupPrincipals
 * are injected into a Subject's principal list at the time of authentication
 * (login), and serve as proxies for Group objects for the purposes of making
 * Java 2 security policy decisions. We add GroupPrincipals instead of the
 * actual Groups because calling classes should never be able to obtain a
 * volatile Principal. Administrators who wish to grant privileges to wiki
 * groups via the security policy file should always specify principals of type
 * GroupPrincipal, <em>not</em>{@link com.ecyrd.jspwiki.auth.authorize.Group}.
 * @author Andrew Jaquith
 * @version $Revision: 1.1 $ $Date: 2006-02-21 08:32:06 $
 * @since 2.3.79
 */
public class GroupPrincipal implements Principal
{
    private final String m_name;

    /**
     * Constructs a new GroupPrincipal object.
     * @param group the wiki group; cannot be <code>null</code>
     */
    public GroupPrincipal( Group group )
    {
        if ( group == null || group.getName() == null )
        {
            throw new IllegalArgumentException( "Group parameter cannot be null." );
        }
        m_name = group.getName();
    }
    
    /**
     * Constructs a new GroupPrincipal object.
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
     * @see java.security.Principal#getName()
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * Two GroupPrincipals are equal if their names are equal.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object obj )
    {
        if ( !( obj instanceof GroupPrincipal ) )
        {
            return false;
        }
        return ( (GroupPrincipal)obj ).m_name.equals( m_name );
    }

    /**
     * Returns the hashcode for this object.
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return m_name.hashCode();
    }

    /**
     * Returns a string representation of this object.
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "[GroupPrincipal: " + m_name + "]";
    }

}
