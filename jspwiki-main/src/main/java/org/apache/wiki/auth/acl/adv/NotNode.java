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

import java.util.Set;

/**
 *
 * @author AO
 */
public class NotNode extends RuleNode {
    private final RuleNode child;

    public RuleNode getChild() {
        return child;
    }

    public NotNode(RuleNode child) {
        this.child = child;
    }

    @Override
    public boolean evaluate(java.util.Set<String> userRoles) {
       return !child.evaluate(userRoles);
    }

    @Override
    public String toString() {
        return "(NOT " + child + ")";
    }

    @Override
    public Set<String> getAllRoles() {
        return child.getAllRoles();
    }
}
