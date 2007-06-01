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

    /**
     * Adds the specified permission to this ACL entry. The permission
     * <em>must</em> be of type
     * {@link com.ecyrd.jspwiki.auth.permissions.PagePermission}. Note: An entry
     * can have multiple permissions.
     * @param permission the permission to be associated with the principal in
     *            this entry
     * @return <code>true</code> if the permission was added, 
     *         <code>false</code> if the permission was
     *         already part of this entry's permission set, and <code>false</code> if
     *         the permission is not of type PagePermission
     */
    public synchronized boolean addPermission( Permission permission )
    {
        if ( permission instanceof PagePermission && findPermission( permission ) == null )
        {
            m_permissions.add( permission );
            return true;
        }

        return false;
    }

    /**
     * Checks if the specified permission is part of the permission set in this
     * entry.
     * @param permission the permission to be checked for.
     * @return true if the permission is part of the permission set in this entry,
     *         false otherwise.
     */
    public boolean checkPermission( Permission permission )
    {
        return findPermission( permission ) != null;
    }

    /**
     * Returns the principal for which permissions are granted by this
     * ACL entry. Returns null if there is no principal set for this entry yet.
     * @return the principal associated with this entry.
     */
    public synchronized Principal getPrincipal()
    {
        return m_principal;
    }

    /**
     * Returns an enumeration of the permissions in this ACL entry.
     * @return an enumeration of the permissions
     */
    public Enumeration permissions()
    {
        return m_permissions.elements();
    }

    /**
     * Removes the specified permission from this ACL entry.
     * @param permission the permission to be removed from this entry.
     * @return true if the permission is removed, false if the permission was not
     *         part of this entry's permission set.
     */
    public synchronized boolean removePermission( Permission permission )
    {
        Permission p = findPermission( permission );

        if ( p != null )
        {
            m_permissions.remove( p );
            return true;
        }

        return false;
    }

    /**
     * Specifies the principal for which permissions are granted or denied by
     * this ACL entry. If a principal was already set for this ACL entry, false
     * is returned, otherwise true is returned.
     * @param user the principal to be set for this entry
     * @return true if the principal is set, false if there was already a
     *         principal set for this entry
     */
    public synchronized boolean setPrincipal( Principal user )
    {
        if ( m_principal != null || user == null )
            return false;

        m_principal = user;

        return true;
    }

    /**
     * Returns a string representation of the contents of this ACL entry.
     * @return a string representation of the contents.
     */
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

