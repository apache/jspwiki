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
 * Represents one entry in an Access Control List (ACL).
 * </p>
 * <p>
 * An ACL can be thought of as a data structure with multiple ACL entry objects.
 * Each ACL entry object contains a set of positive page permissions associated
 * with a particular principal. (A principal represents an entity such as an
 * individual user, group, or role). Each principal can have at most one ACL
 * entry; that is, multiple ACL entries are not allowed for any principal.
 * </p>
 * <p>
 * This interface is functionally equivalent to the java.security.acl.AclEntry
 * interface, minus negative permissions.
 * </p>
 * @see Acl
 * @since 2.3
 */
public interface AclEntry
{

    /**
     * Adds the specified permission to this ACL entry. The permission
     * <em>must</em> be of type
     * {@link org.apache.wiki.auth.permissions.PagePermission}. Note: An entry
     * can have multiple permissions.
     * @param permission the permission to be associated with the principal in
     *            this entry
     * @return <code>true</code> if the permission was added, 
     *         <code>false</code> if the permission was
     *         already part of this entry's permission set, and <code>false</code> if
     *         the permission is not of type PagePermission
     */
    boolean addPermission(Permission permission);

    /**
     * Checks if the specified permission is part of the permission set in this
     * entry.
     * @param permission the permission to be checked for.
     * @return true if the permission is part of the permission set in this entry,
     *         false otherwise.
     */
    boolean checkPermission(Permission permission);

    /**
     * Returns the principal for which permissions are granted by this
     * ACL entry. Returns null if there is no principal set for this entry yet.
     * @return the principal associated with this entry.
     */
    Principal getPrincipal();

    /**
     * Returns an enumeration of the permissions in this ACL entry.
     * @return an enumeration of the permissions
     */
    Enumeration< Permission > permissions();

    /**
     * Removes the specified permission from this ACL entry.
     * @param permission the permission to be removed from this entry.
     * @return true if the permission is removed, false if the permission was not
     *         part of this entry's permission set.
     */
    boolean removePermission(Permission permission);

    /**
     * Specifies the principal for which permissions are granted or denied by
     * this ACL entry. If a principal was already set for this ACL entry, false
     * is returned, otherwise true is returned.
     * @param user the principal to be set for this entry
     * @return true if the principal is set, false if there was already a
     *         principal set for this entry
     */
    boolean setPrincipal(Principal user);

    /**
     * Returns a string representation of the contents of this ACL entry.
     * @return a string representation of the contents.
     */
    String toString();
}
