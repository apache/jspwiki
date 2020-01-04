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
package org.apache.wiki.auth;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiSession;
import org.apache.wiki.WikiSessionTest;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.auth.acl.UnresolvedPrincipal;
import org.apache.wiki.auth.authorize.Group;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.auth.permissions.WikiPermission;
import org.apache.wiki.auth.user.UserProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.security.Permission;
import java.security.Principal;
import java.util.Properties;

/**
 * Tests the AuthorizationManager class.
 */
public class AuthorizationManagerTest
{
    private AuthorizationManager m_auth;

    private TestEngine           m_engine;

    private GroupManager         m_groupMgr;

    private WikiSession          m_session;

    private static class TestPrincipal implements Principal
    {
        private final String m_name;

        public TestPrincipal( String name )
        {
            m_name = name;
        }

        public String getName()
        {
            return m_name;
        }
    }

    @BeforeEach
    public void setUp() throws Exception
    {
        Properties props = TestEngine.getTestProperties();

        // Make sure we are using the default security policy file jspwiki.policy
        props.put( AuthorizationManager.POLICY, AuthorizationManager.DEFAULT_POLICY );

        // Initialize the test engine
        m_engine = new TestEngine( props );
        m_auth = m_engine.getAuthorizationManager();
        m_groupMgr = m_engine.getGroupManager();
        m_session = WikiSessionTest.adminSession( m_engine );
    }

    /**
     * Tests the default policy. Anonymous users can read, Authenticated can
     * edit, etc. Uses the default tests/etc/jspwiki.policy file installed by
     * the JRE at startup.
     * @throws Exception
     */
    @Test
    public void testDefaultPermissions() throws Exception
    {
        // Save a page without an ACL
        m_engine.saveText( "TestDefaultPage", "Foo" );
        Permission view = PermissionFactory.getPagePermission( "*:TestDefaultPage", "view" );
        Permission edit = PermissionFactory.getPagePermission( "*:TestDefaultPage", "edit" );
        WikiSession session;

        // Alice is asserted
        session = WikiSessionTest.assertedSession( m_engine, Users.ALICE );
        Assertions.assertTrue( m_auth.checkPermission( session, view ), "Alice view" );
        Assertions.assertTrue( m_auth.checkPermission( session, edit ), "Alice edit" );

        // Bob is logged in
        session = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        Assertions.assertTrue( m_auth.checkPermission( session, view ), "Bob view" );
        Assertions.assertTrue( m_auth.checkPermission( session, edit ), "Bob edit" );

        // Delete the test page
        try
        {
            m_engine.getPageManager().deletePage( "TestDefaultPage" );
        }
        catch( ProviderException e )
        {
            Assertions.fail( e.getMessage() );
        }
    }

    @Test
    public void testGetRoles() throws Exception
    {
        WikiSession session;
        Principal[] principals;

        // Create a new "asserted" session for Bob
        session = WikiSessionTest.assertedSession( m_engine, Users.BOB );

        // Set up a group without Bob in it
        Group test = m_groupMgr.parseGroup( "Test", "Alice \n Charlie", true );
        m_groupMgr.setGroup( m_session, test );

        // Bob should have two roles: ASSERTED and ALL
        principals = session.getRoles();
        Assertions.assertTrue( ArrayUtils.contains( principals, Role.ALL ), "Bob in ALL" );
        Assertions.assertTrue( ArrayUtils.contains( principals, Role.ASSERTED ), "Bob in ASSERTED" );
        Assertions.assertFalse( ArrayUtils.contains( principals, Role.ANONYMOUS ), "Bob not in ANONYMOUS" );
        Assertions.assertFalse( ArrayUtils.contains( principals, test.getPrincipal() ), "Bob not in Test" );

        // Re-save group "Test" with Bob as a member
        test = m_groupMgr.parseGroup( "Test", "Alice \n Bob \nCharlie", true );
        m_groupMgr.setGroup( m_session, test );

        // Bob not authenticated: should still have only two romes
        principals = session.getRoles();
        Assertions.assertTrue( ArrayUtils.contains( principals, Role.ALL ), "Bob in ALL" );
        Assertions.assertTrue( ArrayUtils.contains( principals, Role.ASSERTED ), "Bob in ASSERTED" );
        Assertions.assertFalse( ArrayUtils.contains( principals, Role.ANONYMOUS ), "Bob not in ANONYMOUS" );
        Assertions.assertFalse( ArrayUtils.contains( principals, test.getPrincipal() ), "Bob in Test" );

        // Elevate Bob to "authenticated" status
        session = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );

        // Re-save the group; Bob should possess the role now
        test = m_groupMgr.parseGroup( "Test", "Alice \n Bob \n Charlie", true );
        m_groupMgr.setGroup( m_session, test );
        principals = session.getRoles();
        Assertions.assertTrue( ArrayUtils.contains( principals, Role.ALL ), "Bob in ALL" );
        Assertions.assertFalse( ArrayUtils.contains( principals, Role.ASSERTED ), "Bob in ASSERTED" );
        Assertions.assertFalse( ArrayUtils.contains( principals, Role.ANONYMOUS ), "Bob not in ANONYMOUS" );
        Assertions.assertTrue( ArrayUtils.contains( principals, test.getPrincipal() ), "Bob in Test" );

        // Cleanup
        m_groupMgr.removeGroup( "Test" );
    }

    @Test
    public void testAssertedSession() throws Exception
    {
        // Create Alice and her roles
        Principal alice = new WikiPrincipal( Users.ALICE );
        Role it = new Role( "IT" );
        Role engineering = new Role( "Engineering" );
        Role finance = new Role( "Finance" );
        Principal admin = new GroupPrincipal( "Admin" );
        WikiSession session = WikiSessionTest.assertedSession(
                m_engine,
                Users.ALICE,
                new Principal[] { it, engineering, admin } );

        // Create two groups: Alice should be part of group Bar, but not Foo
        Group fooGroup = m_groupMgr.parseGroup( "Foo", "", true );
        Group barGroup = m_groupMgr.parseGroup( "Bar", "", true );
        barGroup.add( alice );
        m_groupMgr.setGroup( m_session, fooGroup );
        m_groupMgr.setGroup( m_session, barGroup );

        // Test user principal posession: Alice isn't considered to
        // have the "Alice" principal because she's not authenticated
        Assertions.assertFalse ( m_auth.hasRoleOrPrincipal( session, new WikiPrincipal( Users.ALICE ) ), "Alice has Alice" );
        Assertions.assertFalse ( m_auth.hasRoleOrPrincipal( session, new TestPrincipal( Users.ALICE ) ), "Alice has Alice" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, new WikiPrincipal( Users.BOB ) ), "Alice not has Bob" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, new TestPrincipal( Users.BOB ) ), "Alice not has Bob" );

        // Built-in role memberships
        Assertions.assertTrue( m_auth.hasRoleOrPrincipal( session, Role.ALL ), "Alice in ALL" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, Role.ANONYMOUS ), "Alice not in ANONYMOUS" );
        Assertions.assertTrue( m_auth.hasRoleOrPrincipal( session, Role.ASSERTED ), "Alice in ASSERTED" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, Role.AUTHENTICATED ), "Alice not in AUTHENTICATED" );

        // Custom roles should be FALSE because Alice is asserted
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, it ), "Alice not in IT" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, engineering ), "Alice not in Engineering" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, finance ), "Alice not in Finance" );

        // Group memberships should be FALSE because Alice is asserted
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, fooGroup.getPrincipal() ), "Alice not in Foo" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, barGroup.getPrincipal() ), "Alice not in Bar" );

        // Clean up
        m_groupMgr.removeGroup( "Foo" );
        m_groupMgr.removeGroup( "Bar" );
    }

    @Test
    public void testAuthenticatedSession() throws Exception
    {
        // Create Alice and her roles
        Principal alice = new WikiPrincipal( Users.ALICE );
        Role it = new Role( "IT" );
        Role engineering = new Role( "Engineering" );
        Role finance = new Role( "Finance" );
        Principal admin = new GroupPrincipal( "Admin" );
        WikiSession session = WikiSessionTest.containerAuthenticatedSession(
                m_engine,
                Users.ALICE,
                new Principal[] { it, engineering, admin } );

        // Create two groups: Alice should be part of group Bar, but not Foo
        Group fooGroup = m_groupMgr.parseGroup( "Foo", "", true );
        Group barGroup = m_groupMgr.parseGroup( "Bar", "", true );
        barGroup.add( alice );
        m_groupMgr.setGroup( m_session, fooGroup );
        m_groupMgr.setGroup( m_session, barGroup );

        // Test user principal posession: user principals of different
        // types should still be "the same" if their names are equal
        Assertions.assertTrue( m_auth.hasRoleOrPrincipal( session, new WikiPrincipal( Users.ALICE ) ), "Alice has Alice" );
        Assertions.assertTrue( m_auth.hasRoleOrPrincipal( session, new TestPrincipal( Users.ALICE ) ), "Alice has Alice" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, new WikiPrincipal( Users.BOB ) ), "Alice not has Bob" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, new TestPrincipal( Users.BOB ) ), "Alice not has Bob" );

        // Built-in role membership
        Assertions.assertTrue( m_auth.hasRoleOrPrincipal( session, Role.ALL ), "Alice in ALL" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, Role.ANONYMOUS ), "Alice not in ANONYMOUS" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, Role.ASSERTED ), "Alice not in ASSERTED" );
        Assertions.assertTrue( m_auth.hasRoleOrPrincipal( session, Role.AUTHENTICATED ), "Alice in AUTHENTICATED" );

        // Custom roles
        Assertions.assertTrue( m_auth.hasRoleOrPrincipal( session, it ), "Alice in IT" );
        Assertions.assertTrue( m_auth.hasRoleOrPrincipal( session, engineering ), "Alice in Engineering" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, finance ), "Alice not in Finance" );

        // Group memberships
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, fooGroup.getPrincipal() ), "Alice not in Foo" );
        Assertions.assertTrue( m_auth.hasRoleOrPrincipal( session, barGroup.getPrincipal() ), "Alice in Bar" );

        // Cleanup
        m_groupMgr.removeGroup( "Foo" );
        m_groupMgr.removeGroup( "Bar" );
    }

    @Test
    public void testInheritedPermissions() throws Exception
    {
        // Create test page & attachment
        String src = "[{ALLOW edit Alice}] ";
        m_engine.saveText( "Test", src );

        File f = m_engine.makeAttachmentFile();
        Attachment att = new Attachment( m_engine, "Test", "test1.txt" );
        att.setAuthor( "FirstPost" );
        m_engine.getAttachmentManager().storeAttachment( att, f );

        Attachment p = (Attachment) m_engine.getPageManager().getPage( "Test/test1.txt" );
        Permission view = PermissionFactory.getPagePermission( p, "view" );
        Permission edit = PermissionFactory.getPagePermission( p, "edit" );

        // Create authenticated session with user 'Alice', who can read & edit (in ACL)
        WikiSession session;
        session = WikiSessionTest.authenticatedSession( m_engine, Users.ALICE, Users.ALICE_PASS );
        Assertions.assertTrue( m_auth.checkPermission( session, view ), "Alice view Test/test1.txt" );
        Assertions.assertTrue( m_auth.checkPermission( session, edit ), "Alice view Test/test1.txt" );

        // Create authenticated session with user 'Bob', who can't read or edit (not in ACL)
        session = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        Assertions.assertFalse( m_auth.checkPermission( session, view ), "Bob !view Test/test1.txt" );
        Assertions.assertFalse( m_auth.checkPermission( session, edit ), "Bob !view Test/test1.txt" );

        // Delete test page & attachment
        m_engine.getAttachmentManager().deleteAttachment( att );
        m_engine.getPageManager().deletePage( "Test" );
    }

    @Test
    public void testInheritedAclPermissions() throws Exception
    {
        // Create test page & attachment
        String src = "[{ALLOW view Alice}] ";
        m_engine.saveText( "Test", src );

        File f = m_engine.makeAttachmentFile();
        Attachment att = new Attachment( m_engine, "Test", "test1.txt" );
        att.setAuthor( "FirstPost" );
        m_engine.getAttachmentManager().storeAttachment( att, f );

        Attachment p = (Attachment) m_engine.getPageManager().getPage( "Test/test1.txt" );
        Permission view = PermissionFactory.getPagePermission( p, "view" );
        Permission edit = PermissionFactory.getPagePermission( p, "edit" );

        // Create session with user 'Alice', who can read (in ACL)
        WikiSession session;
        session = WikiSessionTest.authenticatedSession( m_engine, Users.ALICE, Users.ALICE_PASS );
        Assertions.assertTrue( m_auth.checkPermission( session, view ), "Foo view Test" );
        Assertions.assertFalse( m_auth.checkPermission( session, edit ),"Foo !edit Test" );

        // Create session with user 'Bob', who can't read or edit (not in ACL)
        session = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        Assertions.assertFalse( m_auth.checkPermission( session, view ),"Bar !view Test" );
        Assertions.assertFalse( m_auth.checkPermission( session, view ), "Bar !edit Test" );

        // Delete test page & attachment
        m_engine.getAttachmentManager().deleteAttachment( att );
        m_engine.getPageManager().deletePage( "Test" );
    }

    @Test
    public void testHasRoleOrPrincipal() throws Exception
    {
        // Create new user Alice and 2 sample roles
        Principal alice = new WikiPrincipal( Users.ALICE );
        Role it = new Role( "IT" );
        Role finance = new Role( "Finance" );

        // Create Group1 with Alice in it, Group2 without
        WikiSession session = WikiSessionTest.adminSession( m_engine );
        Group g1 = m_groupMgr.parseGroup( "Group1", "Alice", true );
        m_groupMgr.setGroup( session, g1 );
        Principal group1 = g1.getPrincipal();
        Group g2 = m_groupMgr.parseGroup( "Group2", "Bob", true );
        m_groupMgr.setGroup( session, g2 );
        Principal group2 = g2.getPrincipal();

        // Create anonymous session; not in ANY custom roles or groups
        session = WikiSessionTest.anonymousSession( m_engine );
        Assertions.assertTrue ( m_auth.hasRoleOrPrincipal( session, Role.ANONYMOUS ), "Anon anonymous" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, Role.ASSERTED ), "Anon not asserted" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, Role.AUTHENTICATED ), "Anon not authenticated" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, alice ), "Alice not in Anon" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, it ), "Anon not in IT" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, finance ), "Anon not in Finance" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, group1 ), "Anon not in Group1" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, group2 ), "Anon not in Group2" );

        // Create asserted session with 1 GroupPrincipal & 1 custom Role
        // Alice is asserted, and thus not in ANY custom roles or groups
        session = WikiSessionTest.assertedSession( m_engine, Users.ALICE, new Principal[] { it } );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, Role.ANONYMOUS ), "Alice not anonymous" );
        Assertions.assertTrue ( m_auth.hasRoleOrPrincipal( session, Role.ASSERTED ), "Alice asserted" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, Role.AUTHENTICATED ), "Alice not authenticated" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, alice ), "Alice not in Alice" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, it ), "Alice not in IT" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, finance ), "Alice not in Finance" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, group1 ), "Alice not in Group1" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, group2 ), "Alice not in Group2" );

        // Create authenticated session with 1 GroupPrincipal & 1 custom Role
        // Alice is authenticated, and thus part of custom roles and groups
        session = WikiSessionTest.containerAuthenticatedSession( m_engine, Users.ALICE, new Principal[] { it } );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, Role.ANONYMOUS ), "Alice not anonymous" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, Role.ASSERTED ), "Alice not asserted" );
        Assertions.assertTrue ( m_auth.hasRoleOrPrincipal( session, Role.AUTHENTICATED ), "Alice authenticated" );
        Assertions.assertTrue ( m_auth.hasRoleOrPrincipal( session, alice ), "Alice in Ernie" );
        Assertions.assertTrue ( m_auth.hasRoleOrPrincipal( session, it ), "Alice in IT" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, finance ), "Alice not in Finance" );
        Assertions.assertTrue ( m_auth.hasRoleOrPrincipal( session, group1 ), "Alice in Group1" );
        Assertions.assertFalse( m_auth.hasRoleOrPrincipal( session, group2 ), "Alice not in Group2" );

        // Clean up
        m_groupMgr.removeGroup( "Group1" );
        m_groupMgr.removeGroup( "Group2" );
    }

    @Test
    public void testIsUserInRole() throws Exception
    {
        // Create new user Alice and 2 sample roles
        Principal alice = new WikiPrincipal( Users.ALICE );
        Role it = new Role( "IT" );
        Role finance = new Role( "Finance" );

        // Create Group1 with Alice in it, Group2 without
        WikiSession session = WikiSessionTest.adminSession( m_engine );
        Group g1 = m_groupMgr.parseGroup( "Group1", "Alice", true );
        m_groupMgr.setGroup( session, g1 );
        Principal group1 = g1.getPrincipal();
        Group g2 = m_groupMgr.parseGroup( "Group2", "Bob", true );
        m_groupMgr.setGroup( session, g2 );
        Principal group2 = g2.getPrincipal();

        // Create anonymous session; not in ANY custom roles or groups
        session = WikiSessionTest.anonymousSession( m_engine );
        Assertions.assertTrue ( m_auth.isUserInRole( session, Role.ANONYMOUS ), "Anon anonymous" );
        Assertions.assertFalse( m_auth.isUserInRole( session, Role.ASSERTED ), "Anon not asserted" );
        Assertions.assertFalse( m_auth.isUserInRole( session, Role.AUTHENTICATED ), "Anon not authenticated" );
        Assertions.assertFalse( m_auth.isUserInRole( session, alice ), "Anon not in Ernie" );
        Assertions.assertFalse( m_auth.isUserInRole( session, it ), "Anon not in IT" );
        Assertions.assertFalse( m_auth.isUserInRole( session, finance ), "Anon not in Finance" );
        Assertions.assertFalse( m_auth.isUserInRole( session, group1 ), "Anon not in Group1" );
        Assertions.assertFalse( m_auth.isUserInRole( session, group2 ), "Anon not in Group2" );

        // Create asserted session with 1 GroupPrincipal & 1 custom Role
        // Alice is asserted, and thus not in ANY custom roles or groups
        session = WikiSessionTest.assertedSession( m_engine, Users.ALICE, new Principal[] { it } );
        Assertions.assertFalse( m_auth.isUserInRole( session, Role.ANONYMOUS ), "Alice not anonymous" );
        Assertions.assertTrue ( m_auth.isUserInRole( session, Role.ASSERTED ), "Alice asserted" );
        Assertions.assertFalse( m_auth.isUserInRole( session, Role.AUTHENTICATED ), "Alice not authenticated" );
        Assertions.assertFalse( m_auth.isUserInRole( session, alice ), "Alice not in Alice" );
        Assertions.assertFalse( m_auth.isUserInRole( session, it ), "Alice not in IT" );
        Assertions.assertFalse( m_auth.isUserInRole( session, finance ), "Alice not in Finance" );
        Assertions.assertFalse( m_auth.isUserInRole( session, group1 ), "Alice not in Group1" );
        Assertions.assertFalse( m_auth.isUserInRole( session, group2 ), "Alice not in Group2" );

        // Create authenticated session with 1 GroupPrincipal & 1 custom Role
        // Ernie is authenticated, and thus part of custom roles and groups
        session = WikiSessionTest.containerAuthenticatedSession( m_engine, Users.ALICE, new Principal[] { it } );
        Assertions.assertFalse( m_auth.isUserInRole( session, Role.ANONYMOUS ), "Alice not anonymous" );
        Assertions.assertFalse( m_auth.isUserInRole( session, Role.ASSERTED ), "Alice not asserted" );
        Assertions.assertTrue ( m_auth.isUserInRole( session, Role.AUTHENTICATED ), "Alice not authenticated" );
        Assertions.assertFalse( m_auth.isUserInRole( session, alice ), "Alice not in Alice" );
        Assertions.assertTrue ( m_auth.isUserInRole( session, it ), "Alice in IT" );
        Assertions.assertFalse( m_auth.isUserInRole( session, finance ), "Alice not in Finance" );
        Assertions.assertTrue ( m_auth.isUserInRole( session, group1 ), "Alice in Group1" );
        Assertions.assertFalse( m_auth.isUserInRole( session, group2 ), "Alice not in Group2" );

        // Clean up
        m_groupMgr.removeGroup( "Group1" );
        m_groupMgr.removeGroup( "Group2" );
    }

    @Test
    public void testPrincipalAcl() throws Exception
    {
        // Create test page & attachment
        String src = "[{ALLOW edit Alice}] ";
        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPageManager().getPage( "Test" );
        Permission view = PermissionFactory.getPagePermission( p, "view" );
        Permission edit = PermissionFactory.getPagePermission( p, "edit" );

        // Create session with authenticated user 'Alice', who can read & edit (in ACL)
        WikiSession session;
        session = WikiSessionTest.authenticatedSession( m_engine, Users.ALICE, Users.ALICE_PASS );
        Assertions.assertTrue( m_auth.checkPermission( session, view ), "Alice view Test" );
        Assertions.assertTrue( m_auth.checkPermission( session, edit ), "Alice edit Test" );

        // Create session with authenticated user 'Bob', who can't read or edit (not in ACL)
        session = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        Assertions.assertFalse( m_auth.checkPermission( session, view ), "Bob !view Test" );
        Assertions.assertFalse( m_auth.checkPermission( session, edit ), "Bob !edit Test" );

        // Cleanup
        try
        {
            m_engine.getPageManager().deletePage( "Test" );
        }
        catch( ProviderException e )
        {
            Assertions.fail( "Could not delete page" );
        }
    }

    /**
     * Any principal strings that have same names as built-in roles should
     * resolve as built-in roles!
     */
    @Test
    public void testResolveBuiltInRoles()
    {
        Principal principal = Role.AUTHENTICATED;
        Assertions.assertEquals( principal, m_auth.resolvePrincipal( "Authenticated" ) );
        principal = Role.ASSERTED;
        Assertions.assertEquals( principal, m_auth.resolvePrincipal( "Asserted" ) );
        principal = Role.ALL;
        Assertions.assertEquals( principal, m_auth.resolvePrincipal( "All" ) );
        principal = Role.ANONYMOUS;
        Assertions.assertEquals( principal, m_auth.resolvePrincipal( "Anonymous" ) );

        // This should not resolve because there's no built-in role Admin
        principal = new WikiPrincipal( "Admin" );
        Assertions.assertFalse( principal.equals( m_auth.resolvePrincipal( "Admin" ) ) );
    }

    @Test
    public void testResolveGroups() throws WikiException
    {
        Group group1 = m_groupMgr.parseGroup( "SampleGroup", "", true );
        m_groupMgr.setGroup( m_session, group1 );

        Assertions.assertEquals( group1.getPrincipal(), m_auth.resolvePrincipal( "SampleGroup" ) );
        m_groupMgr.removeGroup( "SampleGroup" );

        // We shouldn't be able to spoof a built-in role
        try
        {
            Group group2 = m_groupMgr.parseGroup( "Authenticated", "", true );
            Assertions.assertNotSame( group2.getPrincipal(), m_auth.resolvePrincipal( "Authenticated" ) );
        }
        catch ( WikiSecurityException e )
        {
            Assertions.assertTrue ( true, "Authenticated not allowed as group name." );
        }
        Assertions.assertEquals( Role.AUTHENTICATED, m_auth.resolvePrincipal( "Authenticated" ) );
    }

    @Test
    public void testResolveUsers() throws WikiException
    {
        // We should be able to resolve a user by login, user, or wiki name
        UserProfile profile = m_engine.getUserManager().getUserDatabase().newProfile();
        profile.setEmail( "authmanagertest@tester.net" );
        profile.setFullname( "AuthorizationManagerTest User" );
        profile.setLoginName( "authmanagertest" );
        try
        {
            m_engine.getUserManager().getUserDatabase().save( profile );
        }
        catch( WikiSecurityException e )
        {
            Assertions.fail( "Failed save: " + e.getLocalizedMessage() );
        }
        Assertions.assertEquals( new WikiPrincipal( "authmanagertest",  WikiPrincipal.LOGIN_NAME ), m_auth.resolvePrincipal( "authmanagertest" ) );
        Assertions.assertEquals( new WikiPrincipal( "AuthorizationManagerTest User", WikiPrincipal.FULL_NAME ), m_auth.resolvePrincipal( "AuthorizationManagerTest User" ) );
        Assertions.assertEquals( new WikiPrincipal( "AuthorizationManagerTestUser", WikiPrincipal.WIKI_NAME ), m_auth.resolvePrincipal( "AuthorizationManagerTestUser" ) );
        try
        {
            m_engine.getUserManager().getUserDatabase().deleteByLoginName( "authmanagertest" );
        }
        catch( WikiSecurityException e )
        {
            Assertions.fail( "Failed delete: " + e.getLocalizedMessage() );
        }


        // A wiki group should resolve to itself
        Group group1 = m_groupMgr.parseGroup( "SampleGroup", "", true );
        m_groupMgr.setGroup( m_session, group1 );
        Assertions.assertEquals( group1.getPrincipal(), m_auth.resolvePrincipal( "SampleGroup" ) );
        m_groupMgr.removeGroup( "SampleGroup" );

        // A built-in role should resolve to itself
        Assertions.assertEquals( Role.AUTHENTICATED, m_auth.resolvePrincipal( "Authenticated" ) );

        // We shouldn't be able to spoof a built-in role
        Assertions.assertNotSame( new WikiPrincipal( "Authenticated" ), m_auth.resolvePrincipal( "Authenticated" ) );

        // An unknown user should resolve to a generic UnresolvedPrincipal
        Principal principal = new UnresolvedPrincipal( "Bart Simpson" );
        Assertions.assertEquals( principal, m_auth.resolvePrincipal( "Bart Simpson" ) );
    }

    @Test
    public void testRoleAcl() throws Exception
    {
        // Create test page & attachment
        String src = "[{ALLOW edit Authenticated}] ";
        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPageManager().getPage( "Test" );
        Permission view = PermissionFactory.getPagePermission( p, "view" );
        Permission edit = PermissionFactory.getPagePermission( p, "edit" );

        // Create session with authenticated user 'Alice', who can read & edit
        WikiSession session;
        session = WikiSessionTest.authenticatedSession( m_engine, Users.ALICE, Users.ALICE_PASS );
        Assertions.assertTrue( m_auth.checkPermission( session, view ), "Alice view Test" );
        Assertions.assertTrue( m_auth.checkPermission( session, edit ), "Alice edit Test" );

        // Create session with asserted user 'Bob', who can't read or edit (not in ACL)
        session = WikiSessionTest.assertedSession( m_engine, Users.BOB );
        Assertions.assertFalse( m_auth.checkPermission( session, view ), "Bob !view Test" );
        Assertions.assertFalse( m_auth.checkPermission( session, edit ), "Bob !edit Test" );

        // Cleanup
        try
        {
            m_engine.getPageManager().deletePage( "Test" );
        }
        catch( ProviderException e )
        {
            Assertions.fail( e.getMessage() );
        }
    }

    @Test
    public void testStaticPermission() throws Exception
    {
        WikiSession s = WikiSessionTest.anonymousSession( m_engine );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.VIEW ), "Anonymous view" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.EDIT ), "Anonymous edit" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.COMMENT ), "Anonymous comment" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.MODIFY ), "Anonymous modify" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.UPLOAD ), "Anonymous upload" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.RENAME ), "Anonymous rename" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.DELETE ), "Anonymous delete" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ), "Anonymous prefs" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ), "Anonymous profile" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ), "Anonymous pages" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ), "Anonymous groups" );

        s = WikiSessionTest.assertedSession( m_engine, "Jack Sparrow" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.VIEW ), "Asserted view" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.EDIT ), "Asserted edit" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.COMMENT ), "Asserted comment" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.MODIFY ), "Asserted modify" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.UPLOAD ), "Asserted upload" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.RENAME ), "Asserted rename" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.DELETE ), "Asserted delete" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ), "Asserted prefs" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ), "Asserted profile" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ), "Asserted pages" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ), "Asserted groups" );

        s = WikiSessionTest.authenticatedSession( m_engine, Users.JANNE, Users.JANNE_PASS );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.VIEW ), "Authenticated view" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.EDIT ), "Authenticated edit" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.COMMENT ), "Authenticated comment" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.MODIFY ), "Authenticated modify" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.UPLOAD ), "Authenticated upload" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.RENAME ), "Authenticated rename" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.DELETE ),"Authenticated delete" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ), "Authenticated prefs" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ), "Authenticated profile" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ), "Authenticated pages" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ), "Authenticated groups" );

        s = WikiSessionTest.adminSession( m_engine );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.VIEW ), "Admin view" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.EDIT ), "Admin edit" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.COMMENT ), "Admin comment" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.MODIFY ), "Admin modify" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.UPLOAD ), "Admin upload" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.RENAME ), "Admin rename" );
        // Even though we grant AllPermission in the policy, 'delete' isn't explicit so the check
        // for delete privileges will Assertions.fail (but it will succeed if requested via the checkPermission())
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.DELETE ), "Admin delete" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ), "Admin prefs" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ), "Admin profile" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ), "Admin pages" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ), "Admin groups" );
    }

    @Test
    public void testAdminView()
       throws Exception
    {
        m_engine.saveText( "TestDefaultPage", "Foo [{ALLOW view FooBar}]" );

        Principal admin = new GroupPrincipal( "Admin" );
        WikiSession session = WikiSessionTest.containerAuthenticatedSession(
                m_engine,
                Users.ALICE,
                new Principal[] { admin } );

        Assertions.assertTrue( m_auth.checkPermission( session, new AllPermission( m_engine.getApplicationName() ) ), "Alice has AllPermission" );
        Assertions.assertTrue( m_auth.checkPermission( session, new PagePermission("TestDefaultPage","view") ), "Alice cannot read" );
    }

    @Test
    public void testAdminView2() throws Exception
    {
        m_engine.saveText( "TestDefaultPage", "Foo [{ALLOW view FooBar}]" );

        WikiSession session = WikiSessionTest.adminSession(m_engine);

        Assertions.assertTrue( m_auth.checkPermission( session, new AllPermission( m_engine.getApplicationName() ) ), "Alice has AllPermission" );
        Assertions.assertTrue( m_auth.checkPermission( session, new PagePermission("TestDefaultPage","view") ),"Alice cannot read" );
    }

    @Test
    public void testUserPolicy() throws Exception
    {
        Properties props = TestEngine.getTestProperties();

        // Make sure we are using the default security policy file jspwiki.policy
        props.put( AuthorizationManager.POLICY, "jspwiki-testUserPolicy.policy" );

        // Initialize the test engine
        m_engine = new TestEngine( props );
        m_auth = m_engine.getAuthorizationManager();
        m_groupMgr = m_engine.getGroupManager();
        m_session = WikiSessionTest.adminSession( m_engine );

        WikiSession s = WikiSessionTest.anonymousSession( m_engine );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.VIEW ), "Anonymous view" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.EDIT ), "Anonymous edit" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.COMMENT ), "Anonymous comment" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.MODIFY ), "Anonymous modify" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.UPLOAD ), "Anonymous upload" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.RENAME ), "Anonymous rename" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.DELETE ), "Anonymous delete" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ), "Anonymous prefs" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ), "Anonymous profile" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ), "Anonymous pages" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ), "Anonymous groups" );

        s = WikiSessionTest.assertedSession( m_engine, "Jack Sparrow" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.VIEW ), "Asserted view" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.EDIT ), "Asserted edit" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.COMMENT ), "Asserted comment" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.MODIFY ), "Asserted modify" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.UPLOAD ), "Asserted upload" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.RENAME ), "Asserted rename" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.DELETE ), "Asserted delete" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ), "Asserted prefs" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ), "Asserted profile" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ), "Asserted pages" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ), "Asserted groups" );

        s = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.VIEW ), "Bob view" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.EDIT ), "Bob edit" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.COMMENT ), "Bob comment" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.MODIFY ), "Bob modify" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.UPLOAD ), "Bob upload" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.RENAME ), "Bob rename" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.DELETE ), "Bob delete" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ), "Bob prefs" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ), "Bob profile" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ), "Bob pages" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ), "Bob groups" );

        s = WikiSessionTest.authenticatedSession( m_engine, Users.JANNE, Users.JANNE_PASS );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.VIEW ), "Janne view" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.EDIT ), "Janne edit" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.COMMENT ), "Janne comment" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.MODIFY ), "Janne modify" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.UPLOAD ), "Janne upload" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.RENAME ), "Janne rename" );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.DELETE ), "Janne delete" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ), "Janne prefs" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ), "Janne profile" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ), "Janne pages" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ), "Janne groups" );

        s = WikiSessionTest.adminSession( m_engine );
        Assertions.assertTrue( m_auth.checkStaticPermission( s, PagePermission.VIEW ), "Admin view" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.EDIT ), "Admin edit" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.COMMENT ), "Admin comment" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.MODIFY ), "Admin modify" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.UPLOAD ), "Admin upload" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.RENAME ), "Admin rename" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, PagePermission.DELETE ), "Admin delete" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ), "Admin prefs" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ), "Admin profile" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ), "Admin pages" );
        Assertions.assertFalse( m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ), "Admin groups" );
    }

}
