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

import java.util.List;
import java.util.Set;

/**
 * Negates an expression. It is true for every user that does not satisfy the expression, including users that are not logged in.
 *
 * @since 3.0.2
 */
public class NotNode extends RuleNode {

    private static final long serialVersionUID = 1L;

    private final RuleNode child;

    /**
     * Creates a negation.
     *
     * @param child the expression to negate.
     */
    public NotNode( final RuleNode child ) {
        this.child = child;
    }

    /**
     * Returns the negated expression.
     *
     * @return the expression.
     */
    public RuleNode getChild() {
        return child;
    }

    /** {@inheritDoc} */
    @Override
    public boolean evaluate( final Set< String > userRoles ) {
        return !child.evaluate( userRoles );
    }

    /** {@inheritDoc} */
    @Override
    public List< RoleNode > getRoleNodes() {
        return child.getRoleNodes();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "(NOT " + child + ")";
    }

}
