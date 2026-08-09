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
 * A rule node is a tree like representation of a boolean logic statement used
 * for access controls on a wiki page.
 *
 * @author AO
 * @since 3.0.0
 */
public abstract class RuleNode {

    /**
     * evaluates if a given user/request is allowed to a specific page given
     * their roles/attributes etc
     *
     * @param userRoles
     * @return true if allowed, false otherwise
     */
    public abstract boolean evaluate(java.util.Set<String> userRoles);

    /**
     * gets the complete list of all roles defined within the ACL statement.
     *
     * @return set of roles/usernames/attributes etc
     */
    public abstract Set<String> getAllRoles();

}
