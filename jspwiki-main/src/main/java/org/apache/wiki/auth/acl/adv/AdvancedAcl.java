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

import java.security.Permission;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.wiki.api.core.AclEntry;
import org.apache.wiki.auth.acl.Acl;

/**
 * an extension to the base ACL classes that provides boolean logic, tyical use
 * case is for role/group membership
 *
 * @since 3.0.0
 * @see AclEntry
 * @see AclImpl
 * @see DefaultAclManager
 */
public class AdvancedAcl implements org.apache.wiki.api.core.Acl, Acl {

    private Map<String, RuleNode> nodes = new HashMap<>();

    @Override
    public boolean addEntry(AclEntry entry) {
        if (entry instanceof AdvancedAcl e) {
            this.nodes.putAll(e.nodes);
            return true;
        }
        return false;
    }

    @Override
    public Enumeration<AclEntry> aclEntries() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    @Override
    public Principal[] findPrincipals(Permission permission) {
        final List< Principal> principals = new ArrayList<>();
        final Enumeration< AclEntry> entries = aclEntries();
        while (entries.hasMoreElements()) {
            final AclEntry entry = entries.nextElement();
            final Enumeration< Permission> permissions = entry.permissions();
            while (permissions.hasMoreElements()) {
                final Permission perm = permissions.nextElement();
                if (perm.implies(permission)) {
                    principals.add(entry.getPrincipal());
                }
            }
        }
        return principals.toArray(new Principal[0]);
    }

    @Override
    public AclEntry getAclEntry(Principal principal) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean removeEntry(AclEntry entry) {
        boolean success = false;
        if (entry instanceof AdvancedAcl e) {
            for (String s : e.nodes.keySet()) {
                RuleNode remove = nodes.remove(s);
                if (remove != null) {
                    success = true;
                }
            }

        }
        return success;
    }

    public void addRuleNode(RuleNode node, String actions) {
        nodes.put(actions, node);
    }

    public RuleNode getNode(Permission permission) {
        return nodes.get(permission.getActions());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Entry<String, RuleNode> e : nodes.entrySet()) {
            sb.append("[{ALLOW ").append(e.getKey()).append(" ").append(e.toString()).append("}]\n");
        }
        return sb.toString();
    }

}
