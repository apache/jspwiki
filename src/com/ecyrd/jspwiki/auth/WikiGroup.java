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
        if( !isMember(user) )
        {
            return false;
        }

        m_members.remove( user );

        return true;
    }

    public boolean isMember(Principal member)
    {
        return m_members.contains( member );
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
        return "[Group: "+getName()+"]";
    }
}
