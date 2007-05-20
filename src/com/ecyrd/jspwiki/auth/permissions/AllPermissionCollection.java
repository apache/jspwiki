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
package com.ecyrd.jspwiki.auth.permissions;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * A collection of AllPermission objects.
 * @author Andrew R. Jaquith
 */
public class AllPermissionCollection extends PermissionCollection
{

    private static final long serialVersionUID = 1L;

    private boolean           m_notEmpty      = false;

    private boolean           m_readOnly      = false;

    protected final Hashtable m_permissions    = new Hashtable();

    /**
     * Adds an AllPermission object to this AllPermissionCollection. If this
     * collection was previously marked read-only, or if the permission supplied
     * is not of type {@link AllPermission}, a {@link SecurityException} is
     * thrown.
     * @see java.security.PermissionCollection#add(java.security.Permission)
     */
    public void add( Permission permission )
    {
        if ( !AllPermission.isJSPWikiPermission( permission ) )
        {
            throw new IllegalArgumentException(
                    "Permission must be of type com.ecyrd.jspwiki.permissions.*Permission." );
        }

        if ( m_readOnly )
        {
            throw new SecurityException( "attempt to add a Permission to a readonly PermissionCollection" );
        }

        m_notEmpty = true;

        // This is a filthy hack, but it keeps us from having to write our own
        // Enumeration implementation
        m_permissions.put( permission, permission );
    }

    /**
     * Returns an enumeration of all AllPermission objects stored in this
     * collection.
     * @see java.security.PermissionCollection#elements()
     */
    public Enumeration elements()
    {
        return m_permissions.elements();
    }

    /**
     * Iterates through the AllPermission objects stored by this
     * AllPermissionCollection and determines if any of them imply a supplied
     * Permission. If the Permission is not of type {@link AllPermission},
     * {@link PagePermission} or {@link WikiPermission}, this method will
     * return <code>false</code>. If none of the AllPermissions stored in
     * this collection imply the permission, the method returns
     * <code>false</code>; conversely, if one of the AllPermission objects
     * implies the permission, the method returns <code>true</code>.
     * @param permission the Permission to test. It may be any Permission type,
     * but only the AllPermission, PagePermission or WikiPermission types are
     * actually evaluated.
     * @see java.security.PermissionCollection#implies(java.security.Permission)
     */
    public boolean implies( Permission permission )
    {
        // If nothing in the collection yet, fail fast
        if ( !m_notEmpty )
        {
            return false;
        }

        // If not one of our permission types, it's not implied
        if ( !AllPermission.isJSPWikiPermission( permission ) )
        {
            return false;
        }

        // Step through each AllPermission
        Enumeration permEnum = m_permissions.elements();
        while( permEnum.hasMoreElements() )
        {
            Permission storedPermission = (Permission) permEnum.nextElement();
            if ( storedPermission.implies( permission ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @see java.security.PermissionCollection#isReadOnly()
     */
    public boolean isReadOnly()
    {
        return m_readOnly;
    }

    /**
     * @see java.security.PermissionCollection#setReadOnly()
     */
    public void setReadOnly()
    {
        m_readOnly = true;
    }
}
