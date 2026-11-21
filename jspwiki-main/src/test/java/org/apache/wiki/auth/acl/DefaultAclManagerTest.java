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
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.filters.BasePageFilter;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.filters.FilterManager;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.render.RenderingManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import org.apache.wiki.auth.acl.adv.RuleNode;

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

    /** A page filter that asks for the permissions of the page it is filtering must not make the rendering of that page loop. */
    @Test
    public void testFilterCheckingPermissionsOfItsOwnPageDoesNotLoop() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        m_engine.getManager( FilterManager.class ).addPageFilter( new BasePageFilter() {
            @Override
            public String preTranslate( final Context context, final String content ) {
                calls.incrementAndGet();
                context.getEngine().getManager( AuthorizationManager.class )
                       .checkPermission( context.getWikiSession(), PermissionFactory.getPagePermission( context.getPage(), "view" ) );
                return content;
            }
        }, 100 );

        final String html = m_engine.getManager( RenderingManager.class ).getHTML( "TestDefaultPage" );

        Assertions.assertTrue( html.contains( "Foo" ) );
        Assertions.assertTrue( calls.get() < 10, "the filter ran " + calls.get() + " times" );
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
    
    private static RuleNode onlyRule( final Acl acl, final Page page, final String action ) {
        final List< RuleNode > rules = ( ( AclImpl ) acl ).findRules( PermissionFactory.getPagePermission( page, action ) );
        Assertions.assertEquals( 1, rules.size() );
        return rules.get( 0 );
    }

    @Test
    public void testGetPermissionsAdvancedAcl() {
        final Page page = m_engine.getManager( PageManager.class ).getPage( "TestAdvAclPage" );
        final Acl acl = m_engine.getManager( AclManager.class ).getPermissions( page );
        Assertions.assertFalse( acl.isEmpty() );

        // The page has a rule for view and another for edit, and reading is implied by editing, so both grant it
        final List< RuleNode > view = ( ( AclImpl ) acl ).findRules( PermissionFactory.getPagePermission( page, "view" ) );
        Assertions.assertEquals( 2, view.size() );
        Assertions.assertTrue( view.get( 0 ).evaluate( Set.of( "Charlie" ) ) );
        Assertions.assertTrue( view.get( 1 ).evaluate( Set.of( "Herman" ) ) );
        Assertions.assertFalse( view.get( 0 ).evaluate( Set.of( "Alice" ) ) );

        final RuleNode edit = onlyRule( acl, page, "edit" );
        Assertions.assertTrue( edit.evaluate( Set.of( "Herman" ) ) );
        Assertions.assertFalse( edit.evaluate( Set.of( "Alice" ) ) );

        // the rule does not grant what editing does not imply
        Assertions.assertTrue( ( ( AclImpl ) acl ).findRules( PermissionFactory.getPagePermission( page, "delete" ) ).isEmpty() );
    }

    @Test
    public void testGetPermissionsAdvancedAcl2() {
        final Page page = m_engine.getManager( PageManager.class ).getPage( "TestAdvAclPage2" );
        final Acl acl = m_engine.getManager( AclManager.class ).getPermissions( page );
        Assertions.assertFalse( acl.isEmpty() );

        final RuleNode node = onlyRule( acl, page, "edit" );
        Assertions.assertTrue( node.evaluate( Set.of( "Charlie" ) ) );
        Assertions.assertTrue( node.evaluate( Set.of( "Admin" ) ) );
        Assertions.assertFalse( node.evaluate( Set.of( "Accounting" ) ) );
        Assertions.assertTrue( node.evaluate( Set.of() ) );
    }

    @Test
    public void testGetPermissionsAdvancedAcl3() {
        final Page page = m_engine.getManager( PageManager.class ).getPage( "TestAdvAclPage3" );
        final Acl acl = m_engine.getManager( AclManager.class ).getPermissions( page );
        Assertions.assertFalse( acl.isEmpty() );

        final RuleNode node = onlyRule( acl, page, "edit" );
        Assertions.assertFalse( node.evaluate( Set.of( "Group1" ) ) );
        Assertions.assertFalse( node.evaluate( Set.of( "Group2" ) ) );
        Assertions.assertFalse( node.evaluate( Set.of( "Group3" ) ) );
        Assertions.assertTrue( node.evaluate( Set.of( "Group1", "Group2" ) ) );
        Assertions.assertTrue( node.evaluate( Set.of( "Group1", "Group3" ) ) );
        Assertions.assertTrue( node.evaluate( Set.of( "Group1", "Group2", "Group3" ) ) );
        Assertions.assertFalse( node.evaluate( Set.of( "Admin" ) ) );
    }

    /** A simple rule and an advanced one on the same page keep both, whatever their order. */
    @Test
    public void testSimpleAndAdvancedRulesAreKept() throws Exception {
        final AclManager mgr = m_engine.getManager( AclManager.class );
        for( final String text : new String[] { "[{ALLOW view Alice}] [{ALLOW edit Bob OR Charlie}]",
                                                "[{ALLOW edit Bob OR Charlie}] [{ALLOW view Alice}]" } ) {
            m_engine.saveText( "TestMixedAclPage", text );
            final Page page = m_engine.getManager( PageManager.class ).getPage( "TestMixedAclPage" );
            final Acl acl = mgr.getPermissions( page );
            final Principal[] viewers = acl.findPrincipals( PermissionFactory.getPagePermission( page, "view" ) );
            Assertions.assertEquals( 1, viewers.length, text );
            Assertions.assertEquals( "Alice", viewers[ 0 ].getName(), text );
            Assertions.assertEquals( 1, ( ( AclImpl ) acl ).findRules( PermissionFactory.getPagePermission( page, "edit" ) ).size(), text );
            m_engine.getManager( PageManager.class ).deletePage( "TestMixedAclPage" );
        }
    }

    /** Two advanced rules for the same action add up, as simple rules do. */
    @Test
    public void testAdvancedRulesOfTheSameActionAddUp() throws Exception {
        m_engine.saveText( "TestTwoRulesPage", "[{ALLOW edit Alice OR Bob}] [{ALLOW edit Charlie AND Herman}]" );
        final Page page = m_engine.getManager( PageManager.class ).getPage( "TestTwoRulesPage" );
        final Acl acl = m_engine.getManager( AclManager.class ).getPermissions( page );
        Assertions.assertEquals( 2, ( ( AclImpl ) acl ).findRules( PermissionFactory.getPagePermission( page, "edit" ) ).size() );
        m_engine.getManager( PageManager.class ).deletePage( "TestTwoRulesPage" );
    }

    @Test
    public void testInvalidAdvancedRuleIsRejected() {
        final Page page = m_engine.getManager( PageManager.class ).getPage( "TestDefaultPage" );
        final AclManager mgr = m_engine.getManager( AclManager.class );
        for( final String rule : new String[] { "Alice AND", "(Alice OR Bob", "Alice OR Bob, Charlie", "NOT", "AND OR" } ) {
            final WikiSecurityException e = Assertions.assertThrows( WikiSecurityException.class,
                                                                     () -> mgr.parseAcl( page, "ALLOW edit " + rule ), rule );
            Assertions.assertTrue( e.getMessage().startsWith( "Invalid access rule: ALLOW edit " + rule ), e.getMessage() );
        }
        Assertions.assertTrue( page.getAcl() == null || page.getAcl().isEmpty() );
    }

    /** Names that only contain the words of the operators are names, and a lower case operator is a name too. */
    @Test
    public void testOperatorWordsInsideNamesAreNames() throws Exception {
        m_engine.saveText( "TestWordsPage", "[{ALLOW edit ANDY, ORACLE, NOTARY, order, Brand}]" );
        final Page page = m_engine.getManager( PageManager.class ).getPage( "TestWordsPage" );
        final Acl acl = m_engine.getManager( AclManager.class ).getPermissions( page );
        Assertions.assertEquals( 5, acl.findPrincipals( PermissionFactory.getPagePermission( page, "edit" ) ).length );
        Assertions.assertTrue( ( ( AclImpl ) acl ).findRules( PermissionFactory.getPagePermission( page, "edit" ) ).isEmpty() );
        m_engine.getManager( PageManager.class ).deletePage( "TestWordsPage" );
    }

    @Test
    public void testPrintAclKeepsAdvancedRules() throws Exception {
        m_engine.saveText( "TestPrintPage", "[{ALLOW view Alice}] [{ALLOW edit Bob AND (Charlie OR NOT Herman)}]" );
        final Page page = m_engine.getManager( PageManager.class ).getPage( "TestPrintPage" );
        final String printed = DefaultAclManager.printAcl( m_engine.getManager( AclManager.class ).getPermissions( page ) );
        Assertions.assertTrue( printed.contains( "[{ALLOW view Alice}]" ), printed );
        Assertions.assertTrue( printed.contains( "[{ALLOW edit (Bob AND (Charlie OR (NOT Herman)))}]" ), printed );

        // what is printed reads back as the same rule
        m_engine.saveText( "TestPrintPage2", printed );
        final Page page2 = m_engine.getManager( PageManager.class ).getPage( "TestPrintPage2" );
        Assertions.assertEquals( printed, DefaultAclManager.printAcl( m_engine.getManager( AclManager.class ).getPermissions( page2 ) ) );
        m_engine.getManager( PageManager.class ).deletePage( "TestPrintPage" );
        m_engine.getManager( PageManager.class ).deletePage( "TestPrintPage2" );
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
