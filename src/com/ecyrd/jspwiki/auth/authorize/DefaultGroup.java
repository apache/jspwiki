package com.ecyrd.jspwiki.auth.authorize;

import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

/**
 * Provides a concrete implementation of the {@link Group} interface.
 * @author Andrew Jaquith
 * @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 * @since 2.3
 */
public class DefaultGroup implements Group

{
    private final Vector       m_members = new Vector();

    private final String       m_name;

    public DefaultGroup( String name )
    {
        m_name = name;
    }

    public boolean add( Principal user )
    {
        if ( isMember( user ) )
        {
            return false;
        }

        m_members.add( user );

        return true;
    }

    public void clear()
    {
        m_members.clear();
    }

    /**
     * Two DefaultGroups are equal if they contain identical member Principals
     * and have the same name.
     */
    public boolean equals( Object o )
    {
        if ( o == null || !( o instanceof DefaultGroup ) )
            return false;

        DefaultGroup g = (DefaultGroup) o; // Just a shortcut.

        if ( g.m_members.size() != m_members.size() )
            return false;

        if ( getName() != null && !getName().equals( g.getName() ) )
        {
            return false;
        }
        else if ( getName() == null && g.getName() != null )
        {
            return false;
        }

        for( Iterator i = m_members.iterator(); i.hasNext(); )
        {
            if ( !( g.isMember( (Principal) i.next() ) ) )
            {
                return false;
            }
        }

        return true;
    }

    public String getName()
    {
        return m_name;
    }

    /**
     * Returns <code>true</code> if a Principal is a member of the group.
     * Specifically, the Principal's <code>getName()</code>
     * method must return the same value as one of the Principals in the group
     * member list. The Principal's type does <em>not</em> need to match.
     */
    public boolean isMember( Principal principal )
    {
        return findMember( principal.getName() ) != null;
    }

    public Enumeration members()
    {
        return m_members.elements();
    }

    public boolean remove( Principal user )
    {
        user = findMember( user.getName() );

        if ( user == null )
            return false;

        m_members.remove( user );

        return true;
    }

    /**
     * Returns a string representation of the group.
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "[DefaultGroup: " + getName() + ", members=" );

        for( Iterator i = m_members.iterator(); i.hasNext(); )
        {
            sb.append( i.next() );
            sb.append( ", " );
        }

        sb.append( "]" );

        return sb.toString();
    }

    private Principal findMember( String name )
    {
        for( Iterator i = m_members.iterator(); i.hasNext(); )
        {
            Principal member = (Principal) i.next();

            if ( member.getName().equals( name ) )
            {
                return member;
            }
        }

        return null;
    }
}