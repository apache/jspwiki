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
package org.apache.wiki.auth.authorize;

import org.apache.wiki.auth.GroupPrincipal;
import org.apache.wiki.util.Synchronizer;

import java.security.Principal;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * Groups are a specialized type of ad-hoc role used by the wiki system. Unlike
 * externally-provided roles (such as those provided by an LDAP server or web
 * container), JSPWiki groups can be created dynamically by wiki users, without
 * requiring special container privileges or administrator intervention. They
 * are designed to provide a lightweight role-based access control system that
 * complements existing role systems.
 * </p>
 * <p>
 * Group names are case-insensitive, and have a few naming restrictions, which
 * are enforced by the {@link GroupManager}:
 * </p>
 * <ul>
 * <li>Groups cannot have the same name as a built-in Role (e.g., "Admin",
 * "Authenticated" etc.)</li>
 * <li>Groups cannot have the same name as an existing user</li>
 * </ul>
 * <p>
 * <em>Note: prior to JSPWiki 2.4.19, Group was an interface; it
 * is now a concrete, final class.</em>
 * </p>
 * <p>
 * Groups are related to {@link GroupPrincipal}s. A GroupPrincipal, when
 * injected into the Principal set of a Session's Subject, means that the
 * user is a member of a Group of the same name -- it is, in essence, an
 * "authorization token." GroupPrincipals, unlike Groups, are thread-safe,
 * lightweight and immutable. That's why we use them in Subjects rather than the
 * Groups themselves.
 * </p>
 *
 * @since 2.3
 */
public class Group {

    static final String[]  RESTRICTED_GROUPNAMES = new String[] { "Anonymous", "All", "Asserted", "Authenticated" };

    private final Vector<Principal>    m_members = new Vector<>();

    private String          m_creator;

    private Date            m_created;

    private String          m_modifier;

    private Date            m_modified;

    private final String    m_name;

    private final Principal m_principal;

    private final String    m_wiki;

    /**
     * A lock used to ensure thread safety when accessing shared resources.
     * This lock provides more flexibility and capabilities than the intrinsic locking mechanism,
     * such as the ability to attempt to acquire a lock with a timeout, or to interrupt a thread
     * waiting to acquire a lock.
     *
     * @see java.util.concurrent.locks.ReentrantLock
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Protected constructor to prevent direct instantiation except by other
     * package members. Callers should use
     * {@link GroupManager#parseGroup(String, String, boolean)} or
     * {@link GroupManager#parseGroup(org.apache.wiki.api.core.Context, boolean)}.
     * instead.
     * @param name the name of the group
     * @param wiki the wiki the group belongs to
     */
    protected Group( final String name, final String wiki ) {
        m_name = name;
        m_wiki = wiki;
        m_principal = new GroupPrincipal( name );
    }

    /**
     * Adds a Principal to the group. 
     * 
     * @param user the principal to add
     * @return <code>true</code> if the operation was successful
     */
    public boolean add( final Principal user ) {
        return Synchronizer.synchronize(lock, () -> {
            if( isMember( user ) ) {
                return false;
            }

            m_members.add( user );
            return true;
        });
    }

    /**
     * Clears all Principals from the group list. 
     */
    public void clear() {
        Synchronizer.synchronize(lock, m_members::clear);
    }

    /**
     * Two DefaultGroups are equal if they contain identical member Principals
     * and have the same name.
     * @param o the object to compare
     * @return the comparison
     */
    @Override
    public boolean equals( final Object o ) {
        if( !( o instanceof Group ) ) {
            return false;
        }

        final Group g = ( Group )o; // Just a shortcut.

        if( g.m_members.size() != m_members.size() ) {
            return false;
        }

        if( getName() != null && !getName().equals( g.getName() ) ) {
            return false;
        } else if( getName() == null && g.getName() != null ) {
            return false;
        }

        return m_members.stream().allMatch(g::isMember);
    }

    /**
     *  The hashcode is calculated as a XOR sum over all members of the Group.
     *
     *  @return the hash code
     */
    @Override
    public int hashCode() {
        return m_members.stream().mapToInt(Principal::hashCode).reduce(0, (a, b) -> a ^ b);
    }
    
    /**
     * Returns the creation date.
     *
     * @return the creation date
     */
    public Date getCreated() {
        return Synchronizer.synchronize(lock, () -> m_created);
    }

    /**
     * Returns the creator of this Group.
     *
     * @return the creator
     */
    public final String getCreator() {
        return Synchronizer.synchronize(lock, () -> m_creator);
    }

    /**
     * Returns the last-modified date.
     *
     * @return the date and time of last modification
     */
    public Date getLastModified() {
        return Synchronizer.synchronize(lock, () -> m_modified);
    }

    /**
     * Returns the name of the user who last modified this group.
     *
     * @return the modifier
     */
    public final String getModifier() {
        return Synchronizer.synchronize(lock, () -> m_modifier);
    }

    /**
     * The name of the group. This is set in the class constructor.
     *
     * @return the name of the Group
     */
    public String getName() {
        return m_name;
    }

    /**
     * Returns the GroupPrincipal that represents this Group.
     *
     * @return the group principal
     */
    public Principal getPrincipal() {
        return m_principal;
    }

    /**
     * Returns the wiki name.
     *
     * @return the wiki name
     */
    public String getWiki() {
        return m_wiki;
    }

    /**
     * Returns <code>true</code> if a Principal is a member of the group. Specifically, the Principal's <code>getName()</code> method must
     * return the same value as one of the Principals in the group member list. The Principal's type does <em>not</em> need to match.
     *
     * @param principal the principal about whom membership status is sought
     * @return the result of the operation
     */
    public boolean isMember( final Principal principal ) {
        return findMember( principal.getName() ) != null;
    }

    /**
     * Returns the members of the group as an array of Principal objects.
     *
     * @return the members
     */
    public Principal[] members() {
        return m_members.toArray( new Principal[0] );
    }

    /**
     * Removes a Principal from the group.
     *
     * @param user the principal to remove
     * @return <code>true</code> if the operation was successful
     */
    public boolean remove(Principal user) {
        final AtomicReference<Principal> userRef = new AtomicReference<>(user);
        return Synchronizer.synchronize(lock, () -> {
            Principal updatedUser = findMember(userRef.get().getName());
            userRef.set(updatedUser);
            if (updatedUser == null) {
                return false;
            }
            m_members.remove(updatedUser);
            return true;
        });
    }

    /**
     * Sets the created date.
     *
     * @param date the creation date
     */
    public void setCreated( final Date date ) {
        Synchronizer.synchronize(lock, () -> {
            m_created = date;
        });
    }

    /**
     * Sets the creator of this Group.
     * @param creator the creator
     */
    public final void setCreator( final String creator ) {
        Synchronizer.synchronize(lock, () -> {
            this.m_creator = creator;
        });
    }

    /**
     * Sets the last-modified date
     *
     * @param date the last-modified date
     */
    public void setLastModified( final Date date ) {
        Synchronizer.synchronize(lock, () -> {
            m_modified = date;
        });
    }

    /**
     * Sets the name of the user who last modified this group.
     *
     * @param modifier the modifier
     */
    public final void setModifier( final String modifier ) {
        Synchronizer.synchronize(lock, () -> {
            this.m_modifier = modifier;
        });
    }

    /**
     * Returns a string representation of the Group.
     *
     * @return the string
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "(Group " + getName() + ")";
    }

    private Principal findMember( final String name ) {
        return m_members.stream().filter(member -> member.getName().equals(name)).findFirst().orElse(null);

    }

}
