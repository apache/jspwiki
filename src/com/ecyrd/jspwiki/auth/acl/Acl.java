/* 
 JSPWiki - a JSP-based WikiWiki clone.

 Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

/**
 * <p>
 * Defines an access control list (ACL) for wiki pages. An Access Control List
 * is a data structure used to guard access to resources. An ACL can be thought
 * of as a data structure with multiple ACL entries. Each ACL entry, of
 * interface type AclEntry, contains a set of positive permissions associated
 * with a particular principal. (A principal represents an entity such as an
 * individual user or a group). The ACL Entries in each ACL observe the
 * following rules:
 * </p>
 * <ul>
 * <li>Each principal can have at most one ACL entry; that is, multiple ACL
 * entries are not allowed for any principal. Each entry specifies the set of
 * permissions that are to be granted</li>
 * <li>If there is no entry for a particular principal, then the principal is
 * considered to have a null (empty) permission set</li>
 * </ul>
 * <p>
 * This interface is a highly stripped-down derivation of the
 * java.security.acl.Acl interface. In particular, the notion of an Acl "owner"
 * has been eliminated, since JWPWiki pages do not have owners. An additional
 * simplification compared to the standard Java package is that negative
 * permissions have been eliminated. Instead, JSPWiki assumes a "default-deny"
 * security stance: principals are granted no permissions by default, and
 * posesses only those that have been explicitly granted to them. And finally,
 * the getPermissions() and checkPermission() methods have been eliminated due
 * to the complexities associated with resolving Role principal membership.
 * </p>
 * @author Janne Jalkanen
 * @author Andrew Jaquith
 * @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 * @since 2.3
 */
public interface Acl
{
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
    public boolean addEntry( AclEntry entry );

    /**
     * Returns an enumeration of the entries in this ACL. Each element in the
     * enumeration is of type AclEntry.
     * @return an enumeration of the entries in this ACL.
     */
    public Enumeration entries();

    /**
     * Returns all Principal objects assigned a given Permission in the access
     * control list. The Princiapls returned are those that have been granted
     * either the supplied permission, or a permission implied by the supplied
     * permission. Principals are not "expanded" if they are a role or group.
     * @param permission
     * @return an array of Principals posessing the permission
     */
    public Principal[] findPrincipals( Permission permission );

    /**
     * Returns an AclEntry for a supplied Principal, or <code>null</code> if
     * the Principal does not have a matching AclEntry.
     * @param principal
     * @return the AclEntry associated with the principal, or <code>null</code>
     */
    public AclEntry getEntry( Principal principal );

    /**
     * Removes an ACL entry from this ACL.
     * @param entry the ACL entry to be removed from this ACL
     * @return true on success, false if the entry is not part of this ACL
     */
    public boolean removeEntry( AclEntry entry );

    /**
     * Returns a string representation of the contents of this Acl.
     */
    public String toString();

}