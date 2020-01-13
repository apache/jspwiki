/* 
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package org.apache.wiki.auth.acl;

import java.io.Serializable;
import java.security.Permission;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Vector;

/**
 *  JSPWiki implementation of an Access Control List.
 *
 *  @since 2.3
 */
public class AclImpl implements Acl, Serializable
{
    private static final long serialVersionUID = 1L;
    private final Vector<AclEntry> m_entries = new Vector<>();

    /**
     * Constructs a new AclImpl instance.
     */
    public AclImpl()
    {
    }
    
    /**
     * Returns all Principal objects assigned a given Permission in the access control list. The Principals returned are those that have
     * been granted either the supplied permission, or a permission implied by the supplied permission. Principals are not "expanded" if
     * they are a role or group.
     *
     * @param permission the permission to search for
     * @return an array of Principals possessing the permission
     */
    public Principal[] findPrincipals( final Permission permission ) {
        final Vector< Principal > principals = new Vector<>();
        final Enumeration< AclEntry > entries = entries();
        
        while( entries.hasMoreElements() ) {
            final AclEntry entry = entries.nextElement();
            final Enumeration< Permission > permissions = entry.permissions();
            while( permissions.hasMoreElements() ) {
                final Permission perm = permissions.nextElement();
                if ( perm.implies( permission ) ) {
                    principals.add( entry.getPrincipal() );
                }
            }
        }
        return principals.toArray( new Principal[ principals.size() ] );
    }
  
    private boolean hasEntry( final AclEntry entry ) {
        if( entry == null ) {
            return false;
        }

        for( final AclEntry e : m_entries ) {
            final Principal ep     = e.getPrincipal();
            final Principal entryp = entry.getPrincipal();

            if( ep == null || entryp == null ) {
                throw new IllegalArgumentException( "Entry is null; check code, please (entry="+entry+"; e="+e+")" );
            }
            
            if( ep.getName().equals( entryp.getName() ) ) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds an ACL entry to this ACL. An entry associates a principal (e.g., an individual or a group) with a set of permissions. Each
     * principal can have at most one positive ACL entry, specifying permissions to be granted to the principal. If there is already an
     * ACL entry already in the ACL, false is returned.
     *
     * @param entry - the ACL entry to be added to this ACL
     * @return true on success, false if an entry of the same type (positive or negative) for the same principal is already present in
     * this ACL
     */
    public synchronized boolean addEntry( final AclEntry entry ) {
        if( entry.getPrincipal() == null ) {
            throw new IllegalArgumentException( "Entry principal cannot be null" );
        }

        if( hasEntry( entry ) ) {
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
    public synchronized boolean removeEntry( final AclEntry entry )
    {
        return m_entries.remove( entry );
    }

    /**
     * Returns an enumeration of the entries in this ACL. Each element in the
     * enumeration is of type AclEntry.
     * @return an enumeration of the entries in this ACL.
     */
    public Enumeration< AclEntry > entries()
    {
        return m_entries.elements();
    }

    /**
     * Returns an AclEntry for a supplied Principal, or <code>null</code> if the Principal does not have a matching AclEntry.
     *
     * @param principal the principal to search for
     * @return the AclEntry associated with the principal, or <code>null</code>
     */
    public AclEntry getEntry( final Principal principal ) {
        for( final AclEntry entry : m_entries ) {
            if( entry.getPrincipal().getName().equals( principal.getName() ) ) {
                return entry;
            }
        }

        return null;
    }

    /**
     * Returns a string representation of the contents of this Acl.
     *
     * @return the string representation
     */
    public String toString() {
    	final StringBuilder sb = new StringBuilder();
        for( final AclEntry entry : m_entries ) {
            final Principal pal = entry.getPrincipal();
            if( pal != null ) {
                sb.append( "  user = " ).append( pal.getName() ).append( ": " );
            } else {
                sb.append( "  user = null: " );
            }
            sb.append( "(" );
            for( final Enumeration< Permission > perms = entry.permissions(); perms.hasMoreElements(); ) {
                final Permission perm = perms.nextElement();
                sb.append( perm.toString() );
            }
            sb.append( ")\n" );
        }

        return sb.toString();
    }

    /**
     * Returns <code>true</code>, if this Acl is empty.
     *
     * @return the result
     * @since 2.4.68
     */
    public boolean isEmpty() {
        return m_entries.isEmpty();
    }

}
    
