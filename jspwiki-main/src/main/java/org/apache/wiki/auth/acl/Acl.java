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

import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;


/**
 * <p>
 * Deprecated, interface kept in order to keep backwards compatibility with versions up to 2.11.0.M6. {@link org.apache.wiki.api.core.Acl}
 * should be used instead.
 * </p>
 * {@inheritDoc}
 * @since 2.3
 * @deprecated use {@link org.apache.wiki.api.core.Acl} insteaad
 * @see org.apache.wiki.api.core.Acl
 */
@Deprecated
public interface Acl extends org.apache.wiki.api.core.Acl {

    /**
     * Adds an ACL entry to this ACL. An entry associates a principal (e.g., an individual or a group) with a set of permissions. Each
     * principal can have at most one positive ACL entry, specifying permissions to be granted to the principal. If there is already an
     * ACL entry already in the ACL, false is returned.
     *
     * @param entry - the ACL entry to be added to this ACL
     * @return true on success, false if an entry of the same type (positive or negative) for the same principal is already present in this ACL
     * @deprecated use {@link #addEntry(org.apache.wiki.api.core.AclEntry)} instead.
     * @see #addEntry(org.apache.wiki.api.core.AclEntry
     */
    @Deprecated
    default boolean addEntry( final AclEntry entry ) {
        return addEntry( ( org.apache.wiki.api.core.AclEntry )entry );
    }

    /**
     * Returns an enumeration of the entries in this ACL. Each element in the enumeration is of type AclEntry.
     *
     * @return an enumeration of the entries in this ACL.
     * @deprecated use {@link #aclEntries()} instead.
     * @see #aclEntries()
     */
    @Deprecated
    default Enumeration< AclEntry > entries() {
        final List< AclEntry> entries = Collections.list( aclEntries() ) // iterates list two times - this is ok as we don't expect too many elements inside aclEntries()
                                                   .stream()
                                                   .map( entry -> ( AclEntry )entry )
                                                   .collect( Collectors.toList() );
        return Collections.enumeration( entries );
    }

    /**
     * Returns an AclEntry for a supplied Principal, or <code>null</code> if the Principal does not have a matching AclEntry.
     *
     * @param principal the principal to search for
     * @return the AclEntry associated with the principal, or <code>null</code>
     * @deprecated use {@link #getAclEntry(Principal)} instead.
     * @see #getAclEntry(Principal)
     */
    @Deprecated
    default AclEntry getEntry( final Principal principal ) {
        return ( AclEntry )getAclEntry( principal );
    }

    /**
     * Removes an ACL entry from this ACL.
     *
     * @param entry the ACL entry to be removed from this ACL
     * @return true on success, false if the entry is not part of this ACL
     * @deprecated use {@link #removeEntry(org.apache.wiki.api.core.AclEntry)} instead.
     * @see #removeEntry(org.apache.wiki.api.core.AclEntry
     */
    @Deprecated
    default boolean removeEntry( final AclEntry entry ) {
        return removeEntry( ( org.apache.wiki.api.core.AclEntry )entry );
    }

}
