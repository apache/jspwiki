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
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.regex.Matcher;

public class DefaultAclManagerTest
{
    TestEngine m_engine = TestEngine.build();

    @BeforeEach
    public void setUp() throws Exception {
        m_engine.saveText( "TestDefaultPage", "Foo" );
        m_engine.saveText( "TestAclPage", "Bar. [{ALLOW edit Charlie, Herman}] " );
    }

    @AfterEach
    public void tearDown() {
        try {
            m_engine.getPageManager().deletePage( "TestDefaultPage" );
            m_engine.getPageManager().deletePage( "TestAclPage" );
        } catch ( final ProviderException e ) {
        }
    }

    @Test
    public void testGetPermissions()
    {
        WikiPage page = m_engine.getPageManager().getPage( "TestDefaultPage" );
        Acl acl = m_engine.getAclManager().getPermissions( page );
        Assertions.assertNotNull( page.getAcl() );
        Assertions.assertTrue(page.getAcl().isEmpty());

        page = m_engine.getPageManager().getPage( "TestAclPage" );
        acl = m_engine.getAclManager().getPermissions( page );
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
        WikiPage page = m_engine.getPageManager().getPage( "TestAclPage" );
        Acl acl = m_engine.getAclManager().getPermissions( page );
        String aclString = DefaultAclManager.printAcl( acl );
        Assertions.assertEquals( "[{ALLOW edit Charlie,Herman}]\n", aclString );

        // Create an ACL from scratch
        acl = new AclImpl();
        AclEntry entry = new AclEntryImpl();
        entry.setPrincipal( new WikiPrincipal( "Charlie" ) );
        entry.addPermission( PermissionFactory.getPagePermission( "Main:Foo", "view" ) );
        entry.addPermission( PermissionFactory.getPagePermission( "Main:Foo", "edit" ) );
        acl.addEntry( entry );
        entry = new AclEntryImpl();
        entry.setPrincipal( new WikiPrincipal( "Devin" ) );
        entry.addPermission( PermissionFactory.getPagePermission( "Main:Foo", "edit" ) );
        entry.addPermission( PermissionFactory.getPagePermission( "Main:Foo", "delete" ) );
        acl.addEntry( entry );

        // Verify that the printed ACL is OK
        String expectedValue = "[{ALLOW delete Devin}]\n[{ALLOW edit Charlie,Devin}]\n[{ALLOW view Charlie}]\n";
        Assertions.assertEquals( expectedValue, DefaultAclManager.printAcl( acl ) );
    }

}
