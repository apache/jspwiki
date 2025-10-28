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
package org.apache.wiki.auth;

import java.security.Permission;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Acl;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.auth.acl.AclManager;
import org.apache.wiki.auth.acl.AdvancedAclManager;
import org.apache.wiki.auth.acl.adv.AdvancedAcl;
import org.apache.wiki.auth.acl.adv.RuleNode;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.event.WikiSecurityEvent;
import org.apache.wiki.pages.PageManager;

/**
 * Advanced Authorization manager. This is the component that enforces the
 * access control rules defined by {@link AdvancedAcl} boolean logic.
 *
 * @since 3.0.0
 * @see AdvancedAcl
 * @see AdvancedAclManager
 */
public class AdvancedAuthorizationManager extends DefaultAuthorizationManager {

    private static final Logger LOG = LogManager.getLogger(AdvancedAuthorizationManager.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkPermission(final Session session, final Permission permission) {
        // A slight sanity check.
        if (session == null || permission == null) {
            fireEvent(WikiSecurityEvent.ACCESS_DENIED, null, permission);
            return false;
        }

        final Principal user = session.getLoginPrincipal();

        // Always allow the action if user has AllPermission
        final Permission allPermission = new AllPermission(m_engine.getApplicationName());
        final boolean hasAllPermission = checkStaticPermission(session, allPermission);
        if (hasAllPermission) {
            fireEvent(WikiSecurityEvent.ACCESS_ALLOWED, user, permission);
            return true;
        }

        // If the user doesn't have *at least* the permission granted by policy, return false.
        final boolean hasPolicyPermission = checkStaticPermission(session, permission);
        if (!hasPolicyPermission) {
            fireEvent(WikiSecurityEvent.ACCESS_DENIED, user, permission);
            return false;
        }

        // If this isn't a PagePermission, it's allowed
        if (!(permission instanceof PagePermission)) {
            fireEvent(WikiSecurityEvent.ACCESS_ALLOWED, user, permission);
            return true;
        }

        // If the page or ACL is null, it's allowed.
        final String pageName = ((PagePermission) permission).getPage();
        final Page page = m_engine.getManager(PageManager.class).getPage(pageName);
        final Acl acl = (page == null) ? null : m_engine.getManager(AclManager.class).getPermissions(page);
        if (page == null || acl == null || acl.isEmpty()) {
            fireEvent(WikiSecurityEvent.ACCESS_ALLOWED, user, permission);
            return true;
        }

        // Next, iterate through the Principal objects assigned this permission. If the context's subject possesses
        // any of these, the action is allowed.
        if (acl instanceof AdvancedAcl) {
            AdvancedAcl a2 = (AdvancedAcl) acl;
            Set<String> roles = new HashSet<>();
            roles.add(session.getLoginPrincipal().getName());
            if (session.getRoles() != null) {
                for (Principal p : session.getRoles()) {
                    roles.add(p.getName());
                }
            }
            RuleNode node = a2.getNode(permission);
            if (node == null) {
                fireEvent(WikiSecurityEvent.ACCESS_ALLOWED, user, permission);
                return true;
            }
            Set<String> potentialRoles = node.getAllRoles();
            for (String s : potentialRoles) {
                if (hasRoleOrPrincipal(session, new WikiPrincipal(s))) {
                    roles.add(s);
                }
            }

            if (node.evaluate(roles)) {
                //granted..
                fireEvent(WikiSecurityEvent.ACCESS_ALLOWED, user, permission);
                return true;
            }
        } else {
            LOG.warn("Usage of the AdvantedAuthorizationManager without also using the AdvancedAclManager is NOT supported. Everytyhing will be denied!");
        }
        fireEvent(WikiSecurityEvent.ACCESS_DENIED, user, permission);
        return false;

    }
}
