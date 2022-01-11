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
package org.apache.wiki.auth.permissions;

import java.io.Serializable;
import java.security.Permission;
import java.security.PermissionCollection;

/**
 * <p>
 * Permission to perform all operations on a given wiki.
 * </p>
 * @since 2.3.80
 */
public final class AllPermission extends Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String WILDCARD = "*";

    private final String m_wiki;

    /** For serialization purposes. */
    AllPermission() {
        this( null );
    }

    /**
     * Creates a new AllPermission for the given wikis.
     *
     * @param wiki the wiki to which the permission should apply.  If null, will
     *             apply to all wikis.
     */
    public AllPermission( final String wiki ) {
        super( wiki );
        m_wiki = ( wiki == null ) ? WILDCARD : wiki;
    }

    /**
     * Two AllPermission objects are considered equal if their wikis are equal.
     *
     * @param obj {@inheritDoc}
     * @return {@inheritDoc}
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( final Object obj ) {
        if( !( obj instanceof AllPermission ) ) {
            return false;
        }
        final AllPermission p = ( AllPermission )obj;
        return p.m_wiki != null && p.m_wiki.equals( m_wiki );
    }

    /**
     * No-op; always returns <code>null</code>
     *
     * @return Always null.
     * @see java.security.Permission#getActions()
     */
    public String getActions() {
        return null;
    }

    /**
     * Returns the name of the wiki containing the page represented by this
     * permission; may return the wildcard string.
     *
     * @return The wiki
     */
    public String getWiki() {
        return m_wiki;
    }

    /**
     * Returns the hash code for this WikiPermission.
     *
     * @return {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return m_wiki.hashCode();
    }

    /**
     * WikiPermission can only imply other WikiPermissions; no other permission
     * types are implied. One WikiPermission implies another if all of the other
     * WikiPermission's actions are equal to, or a subset of, those for this
     * permission.
     *
     * @param permission the permission which may (or may not) be implied by
     *                   this instance
     * @return <code>true</code> if the permission is implied,
     * <code>false</code> otherwise
     * @see java.security.Permission#implies(java.security.Permission)
     */
    public boolean implies( final Permission permission ) {
        // Permission must be a JSPWiki permission, PagePermission or AllPermission
        if( !PermissionChecks.isJSPWikiPermission( permission ) ) {
            return false;
        }
        String wiki = null;
        if( permission instanceof AllPermission ) {
            wiki = ( ( AllPermission )permission ).getWiki();
        } else if( permission instanceof PagePermission ) {
            wiki = ( ( PagePermission )permission ).getWiki();
        }
        if( permission instanceof WikiPermission ) {
            wiki = ( ( WikiPermission )permission ).getWiki();
        }
        if( permission instanceof GroupPermission ) {
            wiki = ( ( GroupPermission )permission ).getWiki();
        }

        // If the wiki is implied, it's allowed
        return PagePermission.isSubset( m_wiki, wiki );
    }

    /**
     * Returns a new {@link AllPermissionCollection}.
     *
     * @return {@inheritDoc}
     * @see java.security.Permission#newPermissionCollection()
     */
    public PermissionCollection newPermissionCollection() {
        return new AllPermissionCollection();
    }

    /**
     * Prints a human-readable representation of this permission.
     *
     * @return {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "(\"" + this.getClass().getName() + "\",\"" + m_wiki + "\")";
    }

}
