/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.wiki.auth.acl.adv;

import java.security.Principal;
import java.util.List;
import java.util.Set;

/**
 * A role, group or user name in an advanced access rule.
 *
 * @since 3.0.2
 */
public class RoleNode extends RuleNode {

    private static final long serialVersionUID = 1L;

    private final String role;
    private transient volatile Principal principal;

    /**
     * Creates a node for a name.
     *
     * @param role the name of a role, group or user.
     */
    public RoleNode( final String role ) {
        this.role = role.trim();
    }

    /**
     * Returns the name this node stands for.
     *
     * @return the name.
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the principal the name resolved to.
     *
     * @param principal the principal.
     */
    public void setPrincipal( final Principal principal ) {
        this.principal = principal;
    }

    /**
     * Returns the principal the name resolved to.
     *
     * @return the principal, or {@code null} if the name was not resolved yet.
     */
    public Principal getPrincipal() {
        return principal;
    }

    /** {@inheritDoc} */
    @Override
    public boolean evaluate( final Set< String > userRoles ) {
        return userRoles.contains( role );
    }

    /** {@inheritDoc} */
    @Override
    public RuleNode rename( final Set< String > oldNames, final Principal newPrincipal ) {
        if( !oldNames.contains( role ) ) {
            return this;
        }
        final RoleNode renamed = new RoleNode( newPrincipal.getName() );
        renamed.setPrincipal( newPrincipal );
        return renamed;
    }

    /** {@inheritDoc} */
    @Override
    public List< RoleNode > getRoleNodes() {
        return List.of( this );
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return role;
    }

}
