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
    boolean addEntry( AclEntry entry );

    /**
     * Returns an enumeration of the entries in this ACL. Each element in the
     * enumeration is of type AclEntry.
     * @return an enumeration of the entries in this ACL.
     */
    Enumeration< AclEntry > entries();

    /**
     * Returns <code>true</code>, if this Acl is empty.
     * @return the result
     * @since 2.4.68
     */
    boolean isEmpty();

    /**
     * Returns all Principal objects assigned a given Permission in the access
     * control list. The Princiapls returned are those that have been granted
     * either the supplied permission, or a permission implied by the supplied
     * permission. Principals are not "expanded" if they are a role or group.
     * @param permission the permission to search for
     * @return an array of Principals posessing the permission
     */
    Principal[] findPrincipals( Permission permission );

    /**
     * Returns an AclEntry for a supplied Principal, or <code>null</code> if
     * the Principal does not have a matching AclEntry.
     * @param principal the principal to search for
     * @return the AclEntry associated with the principal, or <code>null</code>
     */
    AclEntry getEntry( Principal principal );

    /**
     * Removes an ACL entry from this ACL.
     * @param entry the ACL entry to be removed from this ACL
     * @return true on success, false if the entry is not part of this ACL
     */
    boolean removeEntry( AclEntry entry );

    /**
     * Returns a string representation of the contents of this Acl.
     * @return the string representation
     */
    String toString();

}
