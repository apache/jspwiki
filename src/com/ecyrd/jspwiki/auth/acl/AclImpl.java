/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2004 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

/**
 *  JSPWiki implementation of an Access Control List.
 *  @author Janne Jalkanen
 *  @author Andrew Jaquith
 *  @since 2.3
 */
public class AclImpl implements Acl
{
    private final Vector<AclEntry> m_entries = new Vector<AclEntry>();

    /**
     * Constructs a new AclImpl instance.
     */
    public AclImpl()
    {
    }
    
    /**
     * Returns all Principal objects assigned a given Permission in the access
     * control list. The Princiapls returned are those that have been granted
     * either the supplied permission, or a permission implied by the supplied
     * permission. Principals are not "expanded" if they are a role or group.
     * @param permission the permission to search for
     * @return an array of Principals posessing the permission
     */
    public Principal[] findPrincipals( Permission permission )
    {
        Vector<Principal> principals = new Vector<Principal>();
        Enumeration entries = entries();
        
        while (entries.hasMoreElements()) 
        {
            AclEntry entry = (AclEntry)entries.nextElement();
            Enumeration permissions = entry.permissions();
            while ( permissions.hasMoreElements() ) 
            {
                Permission perm = (Permission)permissions.nextElement();
                if ( perm.implies( permission ) ) 
                {
                    principals.add( entry.getPrincipal() );
                }
            }
        }
        return principals.toArray( new Principal[principals.size()] );
    }
  
    private boolean hasEntry( AclEntry entry )
    {
        if( entry == null )
        {
            return false;
        }

        for( Iterator i = m_entries.iterator(); i.hasNext(); )
        {
            AclEntry e = (AclEntry) i.next();

            Principal ep     = e.getPrincipal();
            Principal entryp = entry.getPrincipal();

            if( ep == null || entryp == null )
            {
                throw new IllegalArgumentException( "Entry is null; check code, please (entry="+entry+"; e="+e+")" );
            }
            
            if( ep.getName().equals( entryp.getName() ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds an ACL entry to this ACL. An entry associates a principal (e.g., an
     * individual or a group) with a set of permissions. Each principal can have
     * at most one positive ACL entry, specifying permissions to be granted to
     * the principal. If there is already an ACL entry already in the ACL, false
     * is returned.
     * @param entry - the ACL entry to be added to this ACL
     * @return true on success, false if an entry of the same type (positive or
     *         negative) for the same principal is already present in this ACL
     */
    public synchronized boolean addEntry( AclEntry entry )
    {
        if( entry.getPrincipal() == null )
        {
            throw new IllegalArgumentException( "Entry principal cannot be null" );
        }

        if( hasEntry( entry ) )
        {
            return false;
        }
        
        m_entries.add( entry );

        return true;
    }

    /**
     * Removes an ACL entry from this ACL.
     * @param entry the ACL entry to be removed from this ACL
     * @return true on success, false if the entry is not part of this ACL
     */
    public synchronized boolean removeEntry( AclEntry entry )
    {
        return m_entries.remove( entry );
    }

    /**
     * Returns an enumeration of the entries in this ACL. Each element in the
     * enumeration is of type AclEntry.
     * @return an enumeration of the entries in this ACL.
     */
    public Enumeration entries()
    {
        return m_entries.elements();
    }

    /**
     * Returns an AclEntry for a supplied Principal, or <code>null</code> if
     * the Principal does not have a matching AclEntry.
     * @param principal the principal to search for
     * @return the AclEntry associated with the principal, or <code>null</code>
     */
    public AclEntry getEntry( Principal principal )
    {
        for( Enumeration e = m_entries.elements(); e.hasMoreElements(); )
        {
            AclEntry entry = (AclEntry) e.nextElement();
        
            if( entry.getPrincipal().getName().equals( principal.getName() ) )
            {
                return entry;
            }
        }

        return null;
    }

    /**
     * Returns a string representation of the contents of this Acl.
     * @return the string representation
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        for( Enumeration myEnum = entries(); myEnum.hasMoreElements(); )
        {
            AclEntry entry = (AclEntry) myEnum.nextElement();

            Principal pal = entry.getPrincipal();

            if( pal != null )
                sb.append( "  user = "+pal.getName()+": " );
            else
                sb.append( "  user = null: " );

            sb.append( "(" );
            for( Enumeration perms = entry.permissions(); perms.hasMoreElements(); )
            {
                Permission perm = (Permission) perms.nextElement();
                sb.append( perm.toString() );
            }
            sb.append( ")\n" );
        }

        return sb.toString();
    }

    /**
     * Returns <code>true</code>, if this Acl is empty.
     * @return the result
     * @since 2.4.68
     */
    public boolean isEmpty()
    {
        return m_entries.isEmpty();
    }

}
    
