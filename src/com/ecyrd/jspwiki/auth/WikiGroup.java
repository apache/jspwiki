package com.ecyrd.jspwiki.auth;

import java.security.acl.Group;
import java.security.Principal;

import java.util.Vector;
import java.util.Enumeration;
import java.util.Iterator;

public class WikiGroup
    extends WikiPrincipal
    implements Group
{
    private Vector m_members = new Vector();

    public WikiGroup()
    {
    }

    public WikiGroup( String name )
    {
        setName( name );
    }

    public boolean addMember(Principal user)
    {
        if( isMember(user) )
        {
            return false;
        }

        m_members.add( user );

        return true;
    }


    public boolean removeMember(Principal user)
    {
        user = findMember( user.getName() );

        if( user == null ) return false;

        m_members.remove( user );

        return true;
    }

    public void clearMembers()
    {
	m_members.clear();
    }

    private Principal findMember( String name )
    {
        for( Iterator i = m_members.iterator(); i.hasNext(); )
        {
            Principal member = (Principal) i.next();

            if( member.getName().equals( name ) )
            {
                return member;
            }
        }

        return null;
    }

    public boolean isMember(Principal principal)
    {
        return findMember( principal.getName() ) != null;
    }

    public Enumeration members()
    {
        return m_members.elements();
    }

    /**
     *  Each and every element is checked that another Group contains
     *  the same Principals.
     */
    public boolean equals( Object o )
    {
        if( o == null || !(o instanceof WikiGroup) ) return false;

        WikiGroup g = (WikiGroup) o; // Just a shortcut.

        if( g.m_members.size() != m_members.size() ) return false;

        if( getName() != null && !getName().equals( g.getName() ) )
        {
            return false;
        } 
        else if( getName() == null && g.getName() != null ) 
        {
            return false;
        }

        for( Iterator i = m_members.iterator(); i.hasNext(); )
        {
            if( !(g.isMember( (Principal) i.next() ) ) )
            {
                return false;
            }
        }

        return true;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "[Group: "+getName()+", members=" );

        for( Iterator i = m_members.iterator(); i.hasNext(); )
        {
            sb.append( i.next() );
            sb.append(", ");
        }

        sb.append("]");

        return sb.toString();
    }
}
