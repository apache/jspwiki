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

import java.util.Properties;
import org.apache.wiki.TestEngine;
import static org.apache.wiki.TestEngine.getTestProperties;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.auth.acl.adv.AdvancedAcl;
import org.apache.wiki.auth.acl.adv.RuleNode;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.pages.PageManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

/**
 *
 */
public class AdvancedAclManagerTest {

    TestEngine m_engine;

    public AdvancedAclManagerTest() {
        Properties props = TestEngine.getTestProperties();
        props.setProperty("jspwiki.aclManager", AdvancedAclManager.class.getCanonicalName());
        m_engine = TestEngine.build(props);
    }

    @BeforeEach
    public void setUp() throws Exception {
        m_engine.saveText("TestDefaultPage", "Foo");
        m_engine.saveText("TestAclPage", "Bar. [{ALLOW edit Charlie OR Herman}] [{ALLOW view Charlie OR Herman}] ");
    }

    @AfterEach
    public void tearDown() {
        try {
            m_engine.getManager(PageManager.class).deletePage("TestDefaultPage");
            m_engine.getManager(PageManager.class).deletePage("TestAclPage");
        } catch (final ProviderException e) {
        }
    }

    @Test
    public void testGetPermissions() {
        Page page = m_engine.getManager(PageManager.class).getPage("TestDefaultPage");
        AclManager mgr = m_engine.getManager(AclManager.class);
        Assertions.assertTrue(mgr instanceof AdvancedAclManager);
        //org.apache.wiki.api.core.Acl acl = (AdvancedAcl) m_engine.getManager(AclManager.class).getPermissions(page);
        //Assertions.assertNotNull(page.getAcl());
        //Assertions.assertTrue(page.getAcl().isEmpty());

        page = m_engine.getManager(PageManager.class).getPage("TestAclPage");
        AdvancedAcl aacl = (AdvancedAcl) mgr.getPermissions(page);
        Assertions.assertNotNull(page.getAcl());
        Assertions.assertFalse(page.getAcl().isEmpty());

        // Charlie is an editor; reading is therefore implied
        RuleNode node = aacl.getNode(PermissionFactory.getPagePermission(page, "view"));
        Assertions.assertTrue(node.evaluate(Sets.newSet("Charlie")));
        Assertions.assertTrue(node.evaluate(Sets.newSet("Herman")));

        // Charlie should not be able to delete this page
        //p = acl.findPrincipals( PermissionFactory.getPagePermission(page, "delete") );
        ///Assertions.assertEquals( 0, p.length );
        // Herman is an unregistered user and editor; reading is implied
        Assertions.assertTrue(node.evaluate(Sets.newSet("Herman")));

        // Herman should be in the ACL as an editor
        node = aacl.getNode(PermissionFactory.getPagePermission(page, "edit"));
        Assertions.assertTrue(node.evaluate(Sets.newSet("Herman")));
        //p = acl.findPrincipals( PermissionFactory.getPagePermission(page, "edit") );
        //Assertions.assertEquals( 2, p.length );
        //Assertions.assertTrue( ArrayUtils.contains( p, new UnresolvedPrincipal("Herman") ) );

        // Herman should not be able to delete this page
        //p = acl.findPrincipals(PermissionFactory.getPagePermission(page, "delete"));
        //Assertions.assertEquals(0, p.length);
    }

}
