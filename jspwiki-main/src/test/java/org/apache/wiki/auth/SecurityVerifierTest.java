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

import java.security.Principal;
import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import static org.apache.wiki.auth.SecurityVerifier.ERROR_DB;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Dad
 */
public class SecurityVerifierTest {

    TestEngine engine;
    WikiContext context;
    SecurityVerifier instance;

    public SecurityVerifierTest() {
        engine = TestEngine.build();
        context = new WikiContext(engine,
                new WikiPage(engine, "test"));
        instance = new SecurityVerifier(engine, context.getWikiSession());

    }

    @Test
    public void testPolicyPrincipals() {

        Principal[] result = instance.policyPrincipals();
        Assertions.assertNotNull(result);
    }

    @Test
    public void testPolicyRoleTable() {

        String result = instance.policyRoleTable();
        Assertions.assertNotNull(result);
    }

    @Test
    public void testContainerRoleTable() throws Exception {

        Assertions.assertThrows(IllegalStateException.class, () -> {
            //because we are not in a container
            instance.containerRoleTable();
        });
    }

    @Test
    public void testIsSecurityPolicyConfigured() {

        boolean result = instance.isSecurityPolicyConfigured();
        Assertions.assertTrue(result);
    }

    @Test
    public void testWebContainerRoles() throws Exception {

        Principal[] result = instance.webContainerRoles();
        Assertions.assertNotNull(result);
    }

    @Test
    public void testVerifyPolicyAndContainerRoles() throws Exception {

        instance.verifyPolicyAndContainerRoles();
        String[] messages = context.getWikiSession().getMessages(ERROR_DB);
        Assertions.assertTrue(messages == null || messages.length == 0, StringUtils.join(messages));
    }

    @Test
    public void testVerifyGroupDatabase() {

        instance.verifyGroupDatabase();
        String[] messages = context.getWikiSession().getMessages(ERROR_DB);
        Assertions.assertTrue(messages == null || messages.length == 0, StringUtils.join(messages));
    }

    @Test
    public void testVerifyJaas() {

        instance.verifyJaas();
        String[] messages = context.getWikiSession().getMessages(ERROR_DB);
        Assertions.assertTrue(messages == null || messages.length == 0, StringUtils.join(messages));
    }

    @Test
    public void testVerifyPolicy() {

        instance.verifyPolicy();
        String[] messages = context.getWikiSession().getMessages(ERROR_DB);
        Assertions.assertTrue(messages == null || messages.length == 0, StringUtils.join(messages));
    }

    @Test
    public void testVerifyUserDatabase() {

        instance.verifyUserDatabase();
        String[] messages = context.getWikiSession().getMessages(ERROR_DB);
        Assertions.assertTrue(messages == null || messages.length == 0, StringUtils.join(messages));

    }

}
