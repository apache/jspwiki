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
 * @version $Revision: 1.2 $ $Date: 2006-07-25 03:46:41 $
 * @since 2.3.79
 */
public final class GroupPrincipal implements Principal
{
    private final String m_name;
    private final String m_wiki;

    /**
     * Constructs a new GroupPrincipal object for all wikis (*).
     * @param wiki the name of the wiki this group is for; cannot be <code>null</code>
     * @param group the wiki group; cannot be <code>null</code>
     */
    public GroupPrincipal( String group )
    {
        this( "*", group );
    }
    
    /**
     * Constructs a new GroupPrincipal object.
     * @param wiki the name of the wiki this group is for; cannot be <code>null</code>
     * @param group the wiki group; cannot be <code>null</code>
     */
    public GroupPrincipal( String wiki, String group )
    {
        if ( wiki == null )
        {
            throw new IllegalArgumentException( "Wiki parameter cannot be null." );
        }
        m_wiki = wiki;
        
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
    public final String getName()
    {
        return m_name;
    }

    /**
     * Returns the wiki for this group principal.
     */
    public final String getWiki()
    {
        return m_wiki;
    }
    
    /**
     * Two GroupPrincipals are equal if their names and wikis are equal.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public final boolean equals( Object obj )
    {
        if ( !( obj instanceof GroupPrincipal ) )
        {
            return false;
        }
        GroupPrincipal p = (GroupPrincipal)obj;
        return ( p.m_name.equals( m_name ) && p.m_wiki.equals( m_wiki ) );
    }

    /**
     * Returns the hashcode for this object.
     * @see java.lang.Object#hashCode()
     */
    public final int hashCode()
    {
        return m_name.hashCode() + 13 * m_wiki.hashCode();
    }

    /**
     * Returns a string representation of this object.
     * @see java.lang.Object#toString()
     */
    public final String toString()
    {
        return "[GroupPrincipal " + m_wiki + ":" + m_name + "]";
    }

}
