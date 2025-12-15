/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */
package org.apache.wiki.auth.acl;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Acl;
import org.apache.wiki.api.core.AclEntry;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.pages.PageManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.regex.Matcher;
import org.apache.wiki.auth.acl.adv.AdvancedAcl;
import org.apache.wiki.auth.acl.adv.RuleNode;
import org.mockito.internal.util.collections.Sets;

public class DefaultAclManagerTest
{
    TestEngine m_engine = TestEngine.build();

    @BeforeEach
    public void setUp() throws Exception {
        m_engine.saveText( "TestDefaultPage", "Foo" );
        m_engine.saveText( "TestAclPage", "Bar. [{ALLOW edit Charlie, Herman}] " );
        
        m_engine.saveText("TestAdvAclPage", "Bar. [{ALLOW edit Charlie OR Herman}] [{ALLOW view Charlie OR Herman}] ");
        
        m_engine.saveText("TestAdvAclPage2", "Bar. [{ALLOW edit Charlie OR Admin OR NOT Accounting}] ");
        m_engine.saveText("TestAdvAclPage3", "Bar. [{ALLOW edit Group1 AND (Group2 OR Group3)}] ");
 
    }

    @AfterEach
    public void tearDown() {
        try {
            m_engine.getManager( PageManager.class ).deletePage( "TestDefaultPage" );
            m_engine.getManager( PageManager.class ).deletePage( "TestAclPage" );
            m_engine.getManager( PageManager.class ).deletePage( "TestAdvAclPage" );
            m_engine.getManager( PageManager.class ).deletePage( "TestAdvAclPage2" );
            m_engine.getManager( PageManager.class ).deletePage( "TestAdvAclPage3" );
            
            
        } catch ( final ProviderException e ) {
        }
    }

    @Test
    public void testGetPermissions()
    {
        Page page = m_engine.getManager( PageManager.class ).getPage( "TestDefaultPage" );
        Acl acl = m_engine.getManager( AclManager.class ).getPermissions( page );
        Assertions.assertNotNull( page.getAcl() );
        Assertions.assertTrue(page.getAcl().isEmpty());

        page = m_engine.getManager( PageManager.class ).getPage( "TestAclPage" );
        acl = m_engine.getManager( AclManager.class ).getPermissions( page );
        Assertions.assertNotNull( page.getAcl() );
        Assertions.assertFalse(page.getAcl().isEmpty());

        Principal[] p;

        // Charlie is an editor; reading is therefore implied
        p = acl.findPrincipals( PermissionFactory.getPagePermission(page, "view") );
        Assertions.assertEquals( 2, p.length );
        Assertions.assertTrue( ArrayUtils.contains( p, new WikiPrincipal("Charlie") ) );

        // Charlie should be in the ACL as an editor
        p = acl.findPrincipals( PermissionFactory.getPagePermission(page, "edit") );
        Assertions.assertEquals( 2, p.length );
        Assertions.assertTrue( ArrayUtils.contains( p, new WikiPrincipal("Charlie") ) );

        // Charlie should not be able to delete this page
        p = acl.findPrincipals( PermissionFactory.getPagePermission(page, "delete") );
        Assertions.assertEquals( 0, p.length );

        // Herman is an unregistered user and editor; reading is implied
        p = acl.findPrincipals( PermissionFactory.getPagePermission(page, "view") );
        Assertions.assertEquals( 2, p.length );
        Assertions.assertTrue( ArrayUtils.contains( p, new UnresolvedPrincipal("Herman") ) );

        // Herman should be in the ACL as an editor
        p = acl.findPrincipals( PermissionFactory.getPagePermission(page, "edit") );
        Assertions.assertEquals( 2, p.length );
        Assertions.assertTrue( ArrayUtils.contains( p, new UnresolvedPrincipal("Herman") ) );

        // Herman should not be able to delete this page
        p = acl.findPrincipals( PermissionFactory.getPagePermission(page, "delete") );
        Assertions.assertEquals( 0, p.length );
    }
    
    @Test
    public void testGetPermissionsAdvancedAcl() {
        Page page = m_engine.getManager(PageManager.class).getPage("TestDefaultPage");
        AclManager mgr = m_engine.getManager(AclManager.class);

        page = m_engine.getManager(PageManager.class).getPage("TestAdvAclPage");
        AdvancedAcl aacl = (AdvancedAcl) mgr.getPermissions(page);
        Assertions.assertNotNull(page.getAcl());
        Assertions.assertFalse(page.getAcl().isEmpty());

        // Charlie is an editor; reading is therefore implied
        RuleNode node = aacl.getNode(PermissionFactory.getPagePermission(page, "view"));
        Assertions.assertTrue(node.evaluate(Sets.newSet("Charlie")));
        Assertions.assertTrue(node.evaluate(Sets.newSet("Herman")));

        // Charlie should not be able to delete this page
        Assertions.assertTrue(node.evaluate(Sets.newSet("Herman")));

        // Herman should be in the ACL as an editor
        node = aacl.getNode(PermissionFactory.getPagePermission(page, "edit"));
        Assertions.assertTrue(node.evaluate(Sets.newSet("Herman")));
       
    }

     @Test
    public void testGetPermissionsAdvancedAcl2() {
        Page page = m_engine.getManager(PageManager.class).getPage("TestAdvAclPage2");
        Assertions.assertNotNull(page.getAcl());
        Assertions.assertFalse(page.getAcl().isEmpty());
        AclManager mgr = m_engine.getManager(AclManager.class);

        AdvancedAcl acl = (AdvancedAcl) mgr.getPermissions(page);
        Assertions.assertNotNull(page.getAcl());
        Assertions.assertFalse(page.getAcl().isEmpty());

        // Charlie is an editor; reading is therefore implied
        RuleNode node = acl.getNode(PermissionFactory.getPagePermission(page, "edit"));
        Assertions.assertTrue(node.evaluate(Sets.newSet("Charlie")));
        Assertions.assertTrue(node.evaluate(Sets.newSet("Admin")));
        Assertions.assertFalse(node.evaluate(Sets.newSet("Accounting")));


    }
    
      @Test
    public void testGetPermissionsAdvancedAcl3() {
        Page page = m_engine.getManager(PageManager.class).getPage("TestAdvAclPage3");
        Assertions.assertNotNull(page.getAcl());
        Assertions.assertFalse(page.getAcl().isEmpty());
        AclManager mgr = m_engine.getManager(AclManager.class);

        AdvancedAcl acl = (AdvancedAcl) mgr.getPermissions(page);
        Assertions.assertNotNull(page.getAcl());
        Assertions.assertFalse(page.getAcl().isEmpty());

        // Charlie is an editor; reading is therefore implied
        RuleNode node = acl.getNode(PermissionFactory.getPagePermission(page, "edit"));
        Assertions.assertFalse(node.evaluate(Sets.newSet("Group1")));
        Assertions.assertFalse(node.evaluate(Sets.newSet("Group2")));
        Assertions.assertFalse(node.evaluate(Sets.newSet("Group3")));
        Assertions.assertTrue(node.evaluate(Sets.newSet("Group1", "Group2")));
        Assertions.assertTrue(node.evaluate(Sets.newSet("Group1", "Group3")));
        Assertions.assertTrue(node.evaluate(Sets.newSet("Group1", "Group2", "Group3")));
        Assertions.assertFalse(node.evaluate(Sets.newSet("Admin")));
        Assertions.assertFalse(node.evaluate(Sets.newSet("Accounting")));


    }

    
    @Test
    public void testAclRegex()
    {
        String acl;
        Matcher m;

        acl = "[{ALLOW view Bob, Alice, Betty}] Test text.";
        m = DefaultAclManager.ACL_PATTERN.matcher( acl );
        Assertions.assertTrue ( m.find() );
        Assertions.assertEquals( 2, m.groupCount() );
        Assertions.assertEquals( "[{ALLOW view Bob, Alice, Betty}]", m.group(0) );
        Assertions.assertEquals( "view", m.group(1) );
        Assertions.assertEquals( "Bob, Alice, Betty", m.group(2) );
        Assertions.assertFalse( m.find() );

        acl = "[{ALLOW view Alice}] Test text.";
        m = DefaultAclManager.ACL_PATTERN.matcher( acl );
        Assertions.assertTrue ( m.find() );
//        System.out.println( m.group() );
        Assertions.assertEquals( 2, m.groupCount() );
        Assertions.assertEquals( "[{ALLOW view Alice}]", m.group(0) );
        Assertions.assertEquals( "view", m.group(1) );
        Assertions.assertEquals( "Alice", m.group(2) );
        Assertions.assertFalse( m.find() );

        acl = "Test text   [{   ALLOW   view   Alice  }]  Test text.";
        m = DefaultAclManager.ACL_PATTERN.matcher( acl );
        Assertions.assertTrue ( m.find() );
//        System.out.println( m.group() );
        Assertions.assertEquals( 2, m.groupCount() );
        Assertions.assertEquals( "[{   ALLOW   view   Alice  }]", m.group(0) );
        Assertions.assertEquals( "view", m.group(1) );
        Assertions.assertEquals( "Alice", m.group(2) );
        Assertions.assertFalse( m.find() );

        acl = "Test text   [{   ALLOW   view  Alice  ,  Bob  }]  Test text.";
        m = DefaultAclManager.ACL_PATTERN.matcher( acl );
        Assertions.assertTrue ( m.find() );
//        System.out.println( m.group() );
        Assertions.assertEquals( 2, m.groupCount() );
        Assertions.assertEquals( "[{   ALLOW   view  Alice  ,  Bob  }]", m.group(0) );
        Assertions.assertEquals( "view", m.group(1) );
        Assertions.assertEquals( "Alice  ,  Bob", m.group(2) );
        Assertions.assertFalse( m.find() );

        acl = "Test text   [{   ALLOW   view  Alice  ,  Bob  }]  Test text  [{ALLOW edit Betty}].";
        m = DefaultAclManager.ACL_PATTERN.matcher( acl );
        Assertions.assertTrue ( m.find() );
//        System.out.println( m.group() );
        Assertions.assertEquals( 2, m.groupCount() );
        Assertions.assertEquals( "[{   ALLOW   view  Alice  ,  Bob  }]", m.group(0) );
        Assertions.assertEquals( "view", m.group(1) );
        Assertions.assertEquals( "Alice  ,  Bob", m.group(2) );
        Assertions.assertTrue ( m.find() );
        Assertions.assertEquals( 2, m.groupCount() );
        Assertions.assertEquals( "[{ALLOW edit Betty}]", m.group(0) );
        Assertions.assertEquals( "edit", m.group(1) );
        Assertions.assertEquals( "Betty", m.group(2) );
        Assertions.assertFalse( m.find() );
    }

    @Test
    public void testPrintAcl()
    {
        // Verify that the printed Acl for the test page is OK
        final Page page = m_engine.getManager( PageManager.class ).getPage( "TestAclPage" );
        Acl acl = m_engine.getManager( AclManager.class ).getPermissions( page );
        final String aclString = DefaultAclManager.printAcl( acl );
        Assertions.assertEquals( "[{ALLOW edit Charlie,Herman}]\n", aclString );

        // Create an ACL from scratch
        acl = Wiki.acls().acl();
        AclEntry entry = Wiki.acls().entry();
        entry.setPrincipal( new WikiPrincipal( "Charlie" ) );
        entry.addPermission( PermissionFactory.getPagePermission( "Main:Foo", "view" ) );
        entry.addPermission( PermissionFactory.getPagePermission( "Main:Foo", "edit" ) );
        acl.addEntry( entry );
        entry = Wiki.acls().entry();
        entry.setPrincipal( new WikiPrincipal( "Devin" ) );
        entry.addPermission( PermissionFactory.getPagePermission( "Main:Foo", "edit" ) );
        entry.addPermission( PermissionFactory.getPagePermission( "Main:Foo", "delete" ) );
        acl.addEntry( entry );

        // Verify that the printed ACL is OK
        final String expectedValue = "[{ALLOW delete Devin}]\n[{ALLOW edit Charlie,Devin}]\n[{ALLOW view Charlie}]\n";
        Assertions.assertEquals( expectedValue, DefaultAclManager.printAcl( acl ) );
    }

}
