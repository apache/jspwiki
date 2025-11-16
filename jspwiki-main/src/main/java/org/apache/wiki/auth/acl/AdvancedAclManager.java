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
package org.apache.wiki.auth.acl;

import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.acl.adv.AdvancedAcl;
import org.apache.wiki.auth.acl.adv.NotNode;
import org.apache.wiki.auth.acl.adv.OperatorNode;
import org.apache.wiki.auth.acl.adv.RoleNode;
import org.apache.wiki.auth.acl.adv.RuleNode;
import org.apache.wiki.auth.acl.adv.RuleParser;

/**
 * Advanced ACL manager. This is the component that parses the access control
 * rules defined by {@link AdvancedAcl} boolean logic.
 *
 * @since 3.0.0
 * @see AdvancedAcl
 * @see AdvancedAuthorizationManager
 */
public class AdvancedAclManager extends DefaultAclManager {

    private static final Logger LOG = LogManager.getLogger(AdvancedAclManager.class);
    private AuthorizationManager m_auth;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(final Engine engine, final Properties props) {
        super.initialize(engine, props);
        m_auth = engine.getManager(AuthorizationManager.class);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.apache.wiki.api.core.Acl parseAcl(final Page page, final String ruleLine) throws WikiSecurityException {
        org.apache.wiki.api.core.Acl acl = page.getAcl();
        if (acl == null || !(acl instanceof AdvancedAcl)) {
            acl = new AdvancedAcl();
        }
        try {
            final StringTokenizer fieldToks = new StringTokenizer(ruleLine);
            //burn off the allow tag
            fieldToks.nextToken();
            //get the permission flag. i.e. edit, view, etc
            final String actions = fieldToks.nextToken();
            StringBuilder sb = new StringBuilder();
            while (fieldToks.hasMoreTokens()) {
                sb.append(fieldToks.nextToken() + " ");
            }
            RuleParser parser = new RuleParser(sb.toString());
            RuleNode node = parser.parse();
            ((AdvancedAcl) acl).addRuleNode(node, actions);
            recursiveResolve(node);
            page.setAcl(acl);
            LOG.debug(acl.toString());
        } catch (final NoSuchElementException nsee) {
            LOG.warn("Invalid access rule: " + ruleLine + " - defaults will be used.");
            throw new WikiSecurityException("Invalid access rule: " + ruleLine, nsee);
        } catch (final IllegalArgumentException iae) {
            throw new WikiSecurityException("Invalid permission type: " + ruleLine, iae);
        }

        return acl;
    }

    private void recursiveResolve(RuleNode node) {
        if (node == null) {
            return;
        }
        if (node instanceof OperatorNode) {
            recursiveResolve(((OperatorNode) node).getLeft());
            recursiveResolve(((OperatorNode) node).getRight());
        } else if (node instanceof NotNode) {
            recursiveResolve(((NotNode) node).getChild());
        } else if (node instanceof RoleNode) {
            ((RoleNode) node).setPrincipal(m_auth.resolvePrincipal(((RoleNode) node).getRole()));
        }
    }

}
