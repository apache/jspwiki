/*
 * Copyright 2025 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wiki.auth.acl.adv;

import java.util.HashSet;
import java.util.Set;

/**
 * AND/OR operator
 *
 * @since 3.0.0
 *
 */
public class OperatorNode extends RuleNode {

    @Override
    public Set<String> getAllRoles() {
        Set<String> set = new HashSet<>();
        set.addAll(left.getAllRoles());
        set.addAll(right.getAllRoles());
        return set;
    }

    public enum Operator {
        AND, OR
    }

    private final Operator operator;
    private final RuleNode left, right;

    public Operator getOperator() {
        return operator;
    }

    public RuleNode getLeft() {
        return left;
    }

    public RuleNode getRight() {
        return right;
    }

    public OperatorNode(Operator operator, RuleNode left, RuleNode right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean evaluate(java.util.Set<String> userRoles) {
        boolean l = left.evaluate(userRoles);
        boolean r = right.evaluate(userRoles);
        return operator == Operator.AND ? (l && r) : (l || r);
    }

    @Override
    public String toString() {
        return "(" + left + " " + operator + " " + right + ")";
    }
}
