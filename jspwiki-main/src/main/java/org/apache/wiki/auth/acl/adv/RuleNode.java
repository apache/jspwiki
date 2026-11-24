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

import java.io.Serializable;
import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A node of the expression tree of an advanced access rule, such as {@code Admin AND (Editors OR NOT Banned)}.
 *
 * @since 3.0.2
 */
public abstract class RuleNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Evaluates this node against the names a user holds.
     *
     * @param userRoles the names of the roles, groups and users the current user holds.
     * @return {@code true} if the user satisfies the rule.
     */
    public abstract boolean evaluate( Set< String > userRoles );

    /**
     * Returns the leaves of this expression, in the order they were written.
     *
     * @return the role nodes of this expression.
     */
    public abstract List< RoleNode > getRoleNodes();

    /**
     * Returns a copy of this expression in which the given names stand for another principal, as when a user renames a profile.
     *
     * @param oldNames the names to replace.
     * @param newPrincipal the principal the names are replaced with.
     * @return the new expression, or this one if it does not mention any of the names.
     */
    public abstract RuleNode rename( Set< String > oldNames, Principal newPrincipal );

    /**
     * Returns the names of all the roles, groups and users this expression mentions.
     *
     * @return the names, without duplicates.
     */
    public Set< String > getAllRoles() {
        final Set< String > names = new LinkedHashSet<>();
        for( final RoleNode node : getRoleNodes() ) {
            names.add( node.getRole() );
        }
        return names;
    }

}
