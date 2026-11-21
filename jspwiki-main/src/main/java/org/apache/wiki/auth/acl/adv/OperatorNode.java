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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Combines two expressions with AND or OR.
 *
 * @since 3.0.2
 */
public class OperatorNode extends RuleNode {

    private static final long serialVersionUID = 1L;

    /** The operators of an advanced access rule. */
    public enum Operator { AND, OR }

    private final Operator operator;
    private final RuleNode left;
    private final RuleNode right;

    /**
     * Creates a node.
     *
     * @param operator how the two expressions are combined.
     * @param left the left expression.
     * @param right the right expression.
     */
    public OperatorNode( final Operator operator, final RuleNode left, final RuleNode right ) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    /**
     * Returns the operator.
     *
     * @return the operator.
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * Returns the left expression.
     *
     * @return the expression.
     */
    public RuleNode getLeft() {
        return left;
    }

    /**
     * Returns the right expression.
     *
     * @return the expression.
     */
    public RuleNode getRight() {
        return right;
    }

    /** {@inheritDoc} */
    @Override
    public boolean evaluate( final Set< String > userRoles ) {
        return operator == Operator.AND ? left.evaluate( userRoles ) && right.evaluate( userRoles )
                                        : left.evaluate( userRoles ) || right.evaluate( userRoles );
    }

    /** {@inheritDoc} */
    @Override
    public List< RoleNode > getRoleNodes() {
        final List< RoleNode > nodes = new ArrayList<>( left.getRoleNodes() );
        nodes.addAll( right.getRoleNodes() );
        return nodes;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "(" + left + " " + operator + " " + right + ")";
    }

}
