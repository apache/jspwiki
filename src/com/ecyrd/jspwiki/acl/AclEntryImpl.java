package com.ecyrd.jspwiki.acl;

import java.security.acl.AclEntry;
import java.security.acl.Permission;
import java.security.Principal;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Iterator;

public class AclEntryImpl
    implements AclEntry
{
    private Principal m_principal;
    private boolean   m_negative = false;
    private Vector    m_permissions = new Vector();

    public boolean setPrincipal(Principal user)
    {
        if( m_principal != null ) return false;

        m_principal = user;

        return true;
    }

    public Principal getPrincipal()
    {
        return m_principal;
    }

    public void setNegativePermissions()
    {
        m_negative = true;
    }

    public boolean isNegative()
    {
        return m_negative;
    }

    /**
     *  Looks through the permission list and finds a permission that
     *  matches the permission.
     */
    private Permission findPermission( Permission p )
    {
        for( Iterator i = m_permissions.iterator(); i.hasNext(); )
        {
            Permission pp = (Permission) i.next();

            if( pp.equals( p ) )
            {
                return pp;
            }
        }

        return null;
    }

    public boolean addPermission(Permission permission)
    {
        if( findPermission( permission ) != null )
            return true;

        m_permissions.add( permission );

        return false;
    }

    public boolean removePermission(Permission permission)
    {
        Permission p = findPermission(permission);

        if( p != null ) 
        {
            m_permissions.remove( p );
            return true;
        }

        return false;
    }

    public boolean checkPermission(Permission permission)
    {
        return findPermission( permission ) != null;
    }

    public Enumeration permissions()
    {
        return m_permissions.elements();
    }

    public Object clone()
    {
        AclEntryImpl aei = new AclEntryImpl();

        aei.setPrincipal( m_principal );

        aei.m_permissions = (Vector)m_permissions.clone();
        aei.m_negative    = m_negative;

        return aei;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append("AclEntry: [User="+getPrincipal().getName());
        sb.append( m_negative ? " DENY " : " ALLOW " );        

        for( Iterator i = m_permissions.iterator(); i.hasNext(); )
        {
            Permission pp = (Permission) i.next();

            sb.append( pp.toString() );
            sb.append( "," );
        }

        sb.append("]");

        return sb.toString();
    }
}

