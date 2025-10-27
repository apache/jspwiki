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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.apache.wiki.api.core.AclEntry;
import org.apache.wiki.auth.AdvancedAuthorizationManager;
import org.apache.wiki.auth.acl.Acl;
import org.apache.wiki.auth.acl.AdvancedAclManager;

/**
 * an extension to the base ACL classes that provides boolean logic, tyical use
 * case is for role/group membership
 *
 * @author AO
 * @since 3.0.0
 * @see AdvancedAclManager
 * @see AdvancedAuthorizationManager
 */
//org.apache.wiki.auth.acl.Acl
public class AdvancedAcl implements org.apache.wiki.api.core.Acl, Acl {

    private Map<String, RuleNode> nodes = new HashMap<>();

    @Override
    public boolean addEntry(AclEntry entry) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Enumeration<AclEntry> aclEntries() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    @Override
    public Principal[] findPrincipals(Permission permission) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public AclEntry getAclEntry(Principal principal) {
        //walk the tree looking for the specific prinicipal

        //not found
        return null;
    }

    @Override
    public boolean removeEntry(AclEntry entry) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void addRuleNode(RuleNode node, String actions) {
        nodes.put(actions, node);
    }

    public RuleNode getNode(Permission permission) {
        return nodes.get(permission.getActions());
    }

}
