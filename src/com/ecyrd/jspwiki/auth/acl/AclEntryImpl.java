package com.ecyrd.jspwiki.auth.acl;

import java.security.Permission;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import com.ecyrd.jspwiki.auth.permissions.PagePermission;

/**
 * Implementation of a JSPWiki AclEntry.
 * @author Janne Jalkanen
 * @author Andrew Jaquith
 * @version $Revision: 1.3 $ $Date: 2005-08-07 22:06:09 $
 * @since 2.3
 */
public class AclEntryImpl implements AclEntry
{

    private Vector    m_permissions = new Vector();
    private Principal m_principal;

    /**
     * Constructs a new AclEntryImpl instance.
     */
    public AclEntryImpl()
    {
    }
    
    public boolean addPermission( Permission permission )
    {
        if ( permission instanceof PagePermission && findPermission( permission ) == null )
        {
            m_permissions.add( permission );
            return true;
        }

        return false;
    }

    public boolean checkPermission( Permission permission )
    {
        return findPermission( permission ) != null;
    }

    public Object clone()
    {
        AclEntryImpl aei = new AclEntryImpl();

        aei.setPrincipal( m_principal );

        aei.m_permissions = (Vector) m_permissions.clone();

        return aei;
    }

    public Principal getPrincipal()
    {
        return m_principal;
    }

    public Enumeration permissions()
    {
        return m_permissions.elements();
    }

    public boolean removePermission( Permission permission )
    {
        Permission p = findPermission( permission );

        if ( p != null )
        {
            m_permissions.remove( p );
            return true;
        }

        return false;
    }

    public boolean setPrincipal( Principal user )
    {
        if ( m_principal != null || user == null )
            return false;

        m_principal = user;

        return true;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        Principal p = getPrincipal();

        sb.append( "[AclEntry ALLOW " + ( p != null ? p.getName() : "null" ) );
        sb.append( " " );

        for( Iterator i = m_permissions.iterator(); i.hasNext(); )
        {
            Permission pp = (Permission) i.next();

            sb.append( pp.toString() );
            sb.append( "," );
        }

        sb.append( "]" );

        return sb.toString();
    }

    /**
     * Looks through the permission list and finds a permission that matches the
     * permission.
     */
    private Permission findPermission( Permission p )
    {
        for( Iterator i = m_permissions.iterator(); i.hasNext(); )
        {
            Permission pp = (Permission) i.next();

            if ( pp.implies( p ) )
            {
                return pp;
            }
        }

        return null;
    }
}

