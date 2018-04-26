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
import java.io.File;
import java.security.Permission;
import java.security.Principal;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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

    @Before
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
        Assert.assertTrue( "Alice view", m_auth.checkPermission( session, view ) );
        Assert.assertTrue( "Alice edit", m_auth.checkPermission( session, edit ) );

        // Bob is logged in
        session = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        Assert.assertTrue( "Bob view", m_auth.checkPermission( session, view ) );
        Assert.assertTrue( "Bob edit", m_auth.checkPermission( session, edit ) );

        // Delete the test page
        try
        {
            m_engine.deletePage( "TestDefaultPage" );
        }
        catch( ProviderException e )
        {
            Assert.assertTrue( false );
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
        Assert.assertTrue( "Bob in ALL", ArrayUtils.contains( principals, Role.ALL ) );
        Assert.assertTrue( "Bob in ASSERTED", ArrayUtils.contains( principals, Role.ASSERTED ) );
        Assert.assertFalse( "Bob not in ANONYMOUS", ArrayUtils.contains( principals, Role.ANONYMOUS ) );
        Assert.assertFalse( "Bob not in Test", ArrayUtils.contains( principals, test.getPrincipal() ) );

        // Re-save group "Test" with Bob as a member
        test = m_groupMgr.parseGroup( "Test", "Alice \n Bob \nCharlie", true );
        m_groupMgr.setGroup( m_session, test );

        // Bob not authenticated: should still have only two romes
        principals = session.getRoles();
        Assert.assertTrue( "Bob in ALL", ArrayUtils.contains( principals, Role.ALL ) );
        Assert.assertTrue( "Bob in ASSERTED", ArrayUtils.contains( principals, Role.ASSERTED ) );
        Assert.assertFalse( "Bob not in ANONYMOUS", ArrayUtils.contains( principals, Role.ANONYMOUS ) );
        Assert.assertFalse( "Bob in Test", ArrayUtils.contains( principals, test.getPrincipal() ) );

        // Elevate Bob to "authenticated" status
        session = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );

        // Re-save the group; Bob should possess the role now
        test = m_groupMgr.parseGroup( "Test", "Alice \n Bob \n Charlie", true );
        m_groupMgr.setGroup( m_session, test );
        principals = session.getRoles();
        Assert.assertTrue( "Bob in ALL", ArrayUtils.contains( principals, Role.ALL ) );
        Assert.assertFalse( "Bob in ASSERTED", ArrayUtils.contains( principals, Role.ASSERTED ) );
        Assert.assertFalse( "Bob not in ANONYMOUS", ArrayUtils.contains( principals, Role.ANONYMOUS ) );
        Assert.assertTrue( "Bob in Test", ArrayUtils.contains( principals, test.getPrincipal() ) );

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
        Assert.assertFalse ( "Alice has Alice", m_auth.hasRoleOrPrincipal( session, new WikiPrincipal( Users.ALICE ) ) );
        Assert.assertFalse ( "Alice has Alice", m_auth.hasRoleOrPrincipal( session, new TestPrincipal( Users.ALICE ) ) );
        Assert.assertFalse( "Alice not has Bob", m_auth.hasRoleOrPrincipal( session, new WikiPrincipal( Users.BOB ) ) );
        Assert.assertFalse( "Alice not has Bob", m_auth.hasRoleOrPrincipal( session, new TestPrincipal( Users.BOB ) ) );

        // Built-in role memberships
        Assert.assertTrue( "Alice in ALL", m_auth.hasRoleOrPrincipal( session, Role.ALL ) );
        Assert.assertFalse( "Alice not in ANONYMOUS", m_auth.hasRoleOrPrincipal( session, Role.ANONYMOUS ) );
        Assert.assertTrue( "Alice in ASSERTED", m_auth.hasRoleOrPrincipal( session, Role.ASSERTED ) );
        Assert.assertFalse( "Alice not in AUTHENTICATED", m_auth.hasRoleOrPrincipal( session, Role.AUTHENTICATED ) );

        // Custom roles should be FALSE because Alice is asserted
        Assert.assertFalse( "Alice not in IT", m_auth.hasRoleOrPrincipal( session, it ) );
        Assert.assertFalse( "Alice not in Engineering", m_auth.hasRoleOrPrincipal( session, engineering ) );
        Assert.assertFalse( "Alice not in Finance", m_auth.hasRoleOrPrincipal( session, finance ) );

        // Group memberships should be FALSE because Alice is asserted
        Assert.assertFalse( "Alice not in Foo", m_auth.hasRoleOrPrincipal( session, fooGroup.getPrincipal() ) );
        Assert.assertFalse( "Alice not in Bar", m_auth.hasRoleOrPrincipal( session, barGroup.getPrincipal() ) );

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
        Assert.assertTrue( "Alice has Alice", m_auth.hasRoleOrPrincipal( session, new WikiPrincipal( Users.ALICE ) ) );
        Assert.assertTrue( "Alice has Alice", m_auth.hasRoleOrPrincipal( session, new TestPrincipal( Users.ALICE ) ) );
        Assert.assertFalse( "Alice not has Bob", m_auth.hasRoleOrPrincipal( session, new WikiPrincipal( Users.BOB ) ) );
        Assert.assertFalse( "Alice not has Bob", m_auth.hasRoleOrPrincipal( session, new TestPrincipal( Users.BOB ) ) );

        // Built-in role membership
        Assert.assertTrue( "Alice in ALL", m_auth.hasRoleOrPrincipal( session, Role.ALL ) );
        Assert.assertFalse( "Alice not in ANONYMOUS", m_auth.hasRoleOrPrincipal( session, Role.ANONYMOUS ) );
        Assert.assertFalse( "Alice not in ASSERTED", m_auth.hasRoleOrPrincipal( session, Role.ASSERTED ) );
        Assert.assertTrue( "Alice in AUTHENTICATED", m_auth.hasRoleOrPrincipal( session, Role.AUTHENTICATED ) );

        // Custom roles
        Assert.assertTrue( "Alice in IT", m_auth.hasRoleOrPrincipal( session, it ) );
        Assert.assertTrue( "Alice in Engineering", m_auth.hasRoleOrPrincipal( session, engineering ) );
        Assert.assertFalse( "Alice not in Finance", m_auth.hasRoleOrPrincipal( session, finance ) );

        // Group memberships
        Assert.assertFalse( "Alice not in Foo", m_auth.hasRoleOrPrincipal( session, fooGroup.getPrincipal() ) );
        Assert.assertTrue( "Alice in Bar", m_auth.hasRoleOrPrincipal( session, barGroup.getPrincipal() ) );

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

        Attachment p = (Attachment) m_engine.getPage( "Test/test1.txt" );
        Permission view = PermissionFactory.getPagePermission( p, "view" );
        Permission edit = PermissionFactory.getPagePermission( p, "edit" );

        // Create authenticated session with user 'Alice', who can read & edit (in ACL)
        WikiSession session;
        session = WikiSessionTest.authenticatedSession( m_engine, Users.ALICE, Users.ALICE_PASS );
        Assert.assertTrue( "Alice view Test/test1.txt", m_auth.checkPermission( session, view ) );
        Assert.assertTrue( "Alice edit Test/test1.txt", m_auth.checkPermission( session, edit ) );

        // Create authenticated session with user 'Bob', who can't read or edit (not in ACL)
        session = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        Assert.assertFalse( "Bob !view Test/test1.txt", m_auth.checkPermission( session, view ) );
        Assert.assertFalse( "Bob !edit Test/test1.txt", m_auth.checkPermission( session, edit ) );

        // Delete test page & attachment
        m_engine.getAttachmentManager().deleteAttachment( att );
        m_engine.deletePage( "Test" );
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

        Attachment p = (Attachment) m_engine.getPage( "Test/test1.txt" );
        Permission view = PermissionFactory.getPagePermission( p, "view" );
        Permission edit = PermissionFactory.getPagePermission( p, "edit" );

        // Create session with user 'Alice', who can read (in ACL)
        WikiSession session;
        session = WikiSessionTest.authenticatedSession( m_engine, Users.ALICE, Users.ALICE_PASS );
        Assert.assertTrue( "Foo view Test", m_auth.checkPermission( session, view ) );
        Assert.assertFalse( "Foo !edit Test", m_auth.checkPermission( session, edit ) );

        // Create session with user 'Bob', who can't read or edit (not in ACL)
        session = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        Assert.assertFalse( "Bar !view Test", m_auth.checkPermission( session, view ) );
        Assert.assertFalse( "Bar !edit Test", m_auth.checkPermission( session, view ) );

        // Delete test page & attachment
        m_engine.getAttachmentManager().deleteAttachment( att );
        m_engine.deletePage( "Test" );
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
        Assert.assertTrue ( "Anon anonymous", m_auth.hasRoleOrPrincipal( session, Role.ANONYMOUS ) );
        Assert.assertFalse( "Anon not asserted", m_auth.hasRoleOrPrincipal( session, Role.ASSERTED ) );
        Assert.assertFalse( "Anon not authenticated", m_auth.hasRoleOrPrincipal( session, Role.AUTHENTICATED ) );
        Assert.assertFalse( "Alice not in Anon", m_auth.hasRoleOrPrincipal( session, alice ) );
        Assert.assertFalse( "Anon not in IT", m_auth.hasRoleOrPrincipal( session, it ) );
        Assert.assertFalse( "Anon not in Finance", m_auth.hasRoleOrPrincipal( session, finance ) );
        Assert.assertFalse( "Anon not in Group1", m_auth.hasRoleOrPrincipal( session, group1 ) );
        Assert.assertFalse( "Anon not in Group2", m_auth.hasRoleOrPrincipal( session, group2 ) );

        // Create asserted session with 1 GroupPrincipal & 1 custom Role
        // Alice is asserted, and thus not in ANY custom roles or groups
        session = WikiSessionTest.assertedSession( m_engine, Users.ALICE, new Principal[] { it } );
        Assert.assertFalse( "Alice not anonymous", m_auth.hasRoleOrPrincipal( session, Role.ANONYMOUS ) );
        Assert.assertTrue ( "Alice asserted", m_auth.hasRoleOrPrincipal( session, Role.ASSERTED ) );
        Assert.assertFalse( "Alice not authenticated", m_auth.hasRoleOrPrincipal( session, Role.AUTHENTICATED ) );
        Assert.assertFalse( "Alice not in Alice", m_auth.hasRoleOrPrincipal( session, alice ) );
        Assert.assertFalse( "Alice not in IT", m_auth.hasRoleOrPrincipal( session, it ) );
        Assert.assertFalse( "Alice not in Finance", m_auth.hasRoleOrPrincipal( session, finance ) );
        Assert.assertFalse( "Alice not in Group1", m_auth.hasRoleOrPrincipal( session, group1 ) );
        Assert.assertFalse( "Alice not in Group2", m_auth.hasRoleOrPrincipal( session, group2 ) );

        // Create authenticated session with 1 GroupPrincipal & 1 custom Role
        // Alice is authenticated, and thus part of custom roles and groups
        session = WikiSessionTest.containerAuthenticatedSession( m_engine, Users.ALICE, new Principal[] { it } );
        Assert.assertFalse( "Alice not anonymous", m_auth.hasRoleOrPrincipal( session, Role.ANONYMOUS ) );
        Assert.assertFalse( "Alice not asserted", m_auth.hasRoleOrPrincipal( session, Role.ASSERTED ) );
        Assert.assertTrue ( "Alice authenticated", m_auth.hasRoleOrPrincipal( session, Role.AUTHENTICATED ) );
        Assert.assertTrue ( "Alice in Ernie", m_auth.hasRoleOrPrincipal( session, alice ) );
        Assert.assertTrue ( "Alice in IT", m_auth.hasRoleOrPrincipal( session, it ) );
        Assert.assertFalse( "Alice not in Finance", m_auth.hasRoleOrPrincipal( session, finance ) );
        Assert.assertTrue ( "Alice in Group1", m_auth.hasRoleOrPrincipal( session, group1 ) );
        Assert.assertFalse( "Alice not in Group2", m_auth.hasRoleOrPrincipal( session, group2 ) );

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
        Assert.assertTrue ( "Anon anonymous", m_auth.isUserInRole( session, Role.ANONYMOUS ) );
        Assert.assertFalse( "Anon not asserted", m_auth.isUserInRole( session, Role.ASSERTED ) );
        Assert.assertFalse( "Anon not authenticated", m_auth.isUserInRole( session, Role.AUTHENTICATED ) );
        Assert.assertFalse( "Anon not in Ernie", m_auth.isUserInRole( session, alice ) );
        Assert.assertFalse( "Anon not in IT", m_auth.isUserInRole( session, it ) );
        Assert.assertFalse( "Anon not in Finance", m_auth.isUserInRole( session, finance ) );
        Assert.assertFalse( "Anon not in Group1", m_auth.isUserInRole( session, group1 ) );
        Assert.assertFalse( "Anon not in Group2", m_auth.isUserInRole( session, group2 ) );

        // Create asserted session with 1 GroupPrincipal & 1 custom Role
        // Alice is asserted, and thus not in ANY custom roles or groups
        session = WikiSessionTest.assertedSession( m_engine, Users.ALICE, new Principal[] { it } );
        Assert.assertFalse( "Alice not anonymous", m_auth.isUserInRole( session, Role.ANONYMOUS ) );
        Assert.assertTrue ( "Alice asserted", m_auth.isUserInRole( session, Role.ASSERTED ) );
        Assert.assertFalse( "Alice not authenticated", m_auth.isUserInRole( session, Role.AUTHENTICATED ) );
        Assert.assertFalse( "Alice not in Alice", m_auth.isUserInRole( session, alice ) );
        Assert.assertFalse( "Alice not in IT", m_auth.isUserInRole( session, it ) );
        Assert.assertFalse( "Alice not in Finance", m_auth.isUserInRole( session, finance ) );
        Assert.assertFalse( "Alice not in Group1", m_auth.isUserInRole( session, group1 ) );
        Assert.assertFalse( "Alice not in Group2", m_auth.isUserInRole( session, group2 ) );

        // Create authenticated session with 1 GroupPrincipal & 1 custom Role
        // Ernie is authenticated, and thus part of custom roles and groups
        session = WikiSessionTest.containerAuthenticatedSession( m_engine, Users.ALICE, new Principal[] { it } );
        Assert.assertFalse( "Alice not anonymous", m_auth.isUserInRole( session, Role.ANONYMOUS ) );
        Assert.assertFalse( "Alice not asserted", m_auth.isUserInRole( session, Role.ASSERTED ) );
        Assert.assertTrue ( "Alice not authenticated", m_auth.isUserInRole( session, Role.AUTHENTICATED ) );
        Assert.assertFalse( "Alice not in Alice", m_auth.isUserInRole( session, alice ) );
        Assert.assertTrue ( "Alice in IT", m_auth.isUserInRole( session, it ) );
        Assert.assertFalse( "Alice not in Finance", m_auth.isUserInRole( session, finance ) );
        Assert.assertTrue ( "Alice in Group1", m_auth.isUserInRole( session, group1 ) );
        Assert.assertFalse( "Alice not in Group2", m_auth.isUserInRole( session, group2 ) );

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

        WikiPage p = m_engine.getPage( "Test" );
        Permission view = PermissionFactory.getPagePermission( p, "view" );
        Permission edit = PermissionFactory.getPagePermission( p, "edit" );

        // Create session with authenticated user 'Alice', who can read & edit (in ACL)
        WikiSession session;
        session = WikiSessionTest.authenticatedSession( m_engine, Users.ALICE, Users.ALICE_PASS );
        Assert.assertTrue( "Alice view Test", m_auth.checkPermission( session, view ) );
        Assert.assertTrue( "Alice edit Test", m_auth.checkPermission( session, edit ) );

        // Create session with authenticated user 'Bob', who can't read or edit (not in ACL)
        session = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        Assert.assertFalse( "Bob !view Test", m_auth.checkPermission( session, view ) );
        Assert.assertFalse( "Bob !edit Test", m_auth.checkPermission( session, edit ) );

        // Cleanup
        try
        {
            m_engine.deletePage( "Test" );
        }
        catch( ProviderException e )
        {
            Assert.fail( "Could not delete page" );
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
        Assert.assertEquals( principal, m_auth.resolvePrincipal( "Authenticated" ) );
        principal = Role.ASSERTED;
        Assert.assertEquals( principal, m_auth.resolvePrincipal( "Asserted" ) );
        principal = Role.ALL;
        Assert.assertEquals( principal, m_auth.resolvePrincipal( "All" ) );
        principal = Role.ANONYMOUS;
        Assert.assertEquals( principal, m_auth.resolvePrincipal( "Anonymous" ) );

        // This should not resolve because there's no built-in role Admin
        principal = new WikiPrincipal( "Admin" );
        Assert.assertFalse( principal.equals( m_auth.resolvePrincipal( "Admin" ) ) );
    }

    @Test
    public void testResolveGroups() throws WikiException
    {
        Group group1 = m_groupMgr.parseGroup( "SampleGroup", "", true );
        m_groupMgr.setGroup( m_session, group1 );

        Assert.assertEquals( group1.getPrincipal(), m_auth.resolvePrincipal( "SampleGroup" ) );
        m_groupMgr.removeGroup( "SampleGroup" );

        // We shouldn't be able to spoof a built-in role
        try
        {
            Group group2 = m_groupMgr.parseGroup( "Authenticated", "", true );
            Assert.assertNotSame( group2.getPrincipal(), m_auth.resolvePrincipal( "Authenticated" ) );
        }
        catch ( WikiSecurityException e )
        {
            Assert.assertTrue ( "Authenticated not allowed as group name.", true );
        }
        Assert.assertEquals( Role.AUTHENTICATED, m_auth.resolvePrincipal( "Authenticated" ) );
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
            Assert.fail( "Failed save: " + e.getLocalizedMessage() );
        }
        Assert.assertEquals( new WikiPrincipal( "authmanagertest",  WikiPrincipal.LOGIN_NAME ), m_auth.resolvePrincipal( "authmanagertest" ) );
        Assert.assertEquals( new WikiPrincipal( "AuthorizationManagerTest User", WikiPrincipal.FULL_NAME ), m_auth.resolvePrincipal( "AuthorizationManagerTest User" ) );
        Assert.assertEquals( new WikiPrincipal( "AuthorizationManagerTestUser", WikiPrincipal.WIKI_NAME ), m_auth.resolvePrincipal( "AuthorizationManagerTestUser" ) );
        try
        {
            m_engine.getUserManager().getUserDatabase().deleteByLoginName( "authmanagertest" );
        }
        catch( WikiSecurityException e )
        {
            Assert.fail( "Failed delete: " + e.getLocalizedMessage() );
        }


        // A wiki group should resolve to itself
        Group group1 = m_groupMgr.parseGroup( "SampleGroup", "", true );
        m_groupMgr.setGroup( m_session, group1 );
        Assert.assertEquals( group1.getPrincipal(), m_auth.resolvePrincipal( "SampleGroup" ) );
        m_groupMgr.removeGroup( "SampleGroup" );

        // A built-in role should resolve to itself
        Assert.assertEquals( Role.AUTHENTICATED, m_auth.resolvePrincipal( "Authenticated" ) );

        // We shouldn't be able to spoof a built-in role
        Assert.assertNotSame( new WikiPrincipal( "Authenticated" ), m_auth.resolvePrincipal( "Authenticated" ) );

        // An unknown user should resolve to a generic UnresolvedPrincipal
        Principal principal = new UnresolvedPrincipal( "Bart Simpson" );
        Assert.assertEquals( principal, m_auth.resolvePrincipal( "Bart Simpson" ) );
    }

    @Test
    public void testRoleAcl() throws Exception
    {
        // Create test page & attachment
        String src = "[{ALLOW edit Authenticated}] ";
        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage( "Test" );
        Permission view = PermissionFactory.getPagePermission( p, "view" );
        Permission edit = PermissionFactory.getPagePermission( p, "edit" );

        // Create session with authenticated user 'Alice', who can read & edit
        WikiSession session;
        session = WikiSessionTest.authenticatedSession( m_engine, Users.ALICE, Users.ALICE_PASS );
        Assert.assertTrue( "Alice view Test", m_auth.checkPermission( session, view ) );
        Assert.assertTrue( "Alice edit Test", m_auth.checkPermission( session, edit ) );

        // Create session with asserted user 'Bob', who can't read or edit (not in ACL)
        session = WikiSessionTest.assertedSession( m_engine, Users.BOB );
        Assert.assertFalse( "Bob !view Test", m_auth.checkPermission( session, view ) );
        Assert.assertFalse( "Bob !edit Test", m_auth.checkPermission( session, edit ) );

        // Cleanup
        try
        {
            m_engine.deletePage( "Test" );
        }
        catch( ProviderException e )
        {
            Assert.assertTrue( false );
        }
    }

    @Test
    public void testStaticPermission() throws Exception
    {
        WikiSession s = WikiSessionTest.anonymousSession( m_engine );
        Assert.assertTrue( "Anonymous view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        Assert.assertTrue( "Anonymous edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        Assert.assertTrue( "Anonymous comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        Assert.assertFalse( "Anonymous modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        Assert.assertFalse( "Anonymous upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        Assert.assertFalse( "Anonymous rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        Assert.assertFalse( "Anonymous delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        Assert.assertTrue( "Anonymous prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        Assert.assertTrue( "Anonymous profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        Assert.assertTrue( "Anonymous pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        Assert.assertFalse( "Anonymous groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );

        s = WikiSessionTest.assertedSession( m_engine, "Jack Sparrow" );
        Assert.assertTrue( "Asserted view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        Assert.assertTrue( "Asserted edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        Assert.assertTrue( "Asserted comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        Assert.assertFalse( "Asserted modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        Assert.assertFalse( "Asserted upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        Assert.assertFalse( "Asserted rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        Assert.assertFalse( "Asserted delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        Assert.assertTrue( "Asserted prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        Assert.assertTrue( "Asserted profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        Assert.assertTrue( "Asserted pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        Assert.assertFalse( "Asserted groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );

        s = WikiSessionTest.authenticatedSession( m_engine, Users.JANNE, Users.JANNE_PASS );
        Assert.assertTrue( "Authenticated view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        Assert.assertTrue( "Authenticated edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        Assert.assertTrue( "Authenticated comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        Assert.assertTrue( "Authenticated modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        Assert.assertTrue( "Authenticated upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        Assert.assertTrue( "Authenticated rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        Assert.assertFalse( "Authenticated delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        Assert.assertTrue( "Authenticated prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        Assert.assertTrue( "Authenticated profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        Assert.assertTrue( "Authenticated pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        Assert.assertTrue( "Authenticated groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );

        s = WikiSessionTest.adminSession( m_engine );
        Assert.assertTrue( "Admin view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        Assert.assertTrue( "Admin edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        Assert.assertTrue( "Admin comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        Assert.assertTrue( "Admin modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        Assert.assertTrue( "Admin upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        Assert.assertTrue( "Admin rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        // Even though we grant AllPermission in the policy, 'delete' isn't explicit so the check
        // for delete privileges will Assert.fail (but it will succeed if requested via the checkPermission())
        Assert.assertFalse( "Admin delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        Assert.assertTrue( "Admin prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        Assert.assertTrue( "Admin profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        Assert.assertTrue( "Admin pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        Assert.assertTrue( "Admin groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );
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

        Assert.assertTrue( "Alice has AllPermission", m_auth.checkPermission( session,
                                                                       new AllPermission( m_engine.getApplicationName() )));
        Assert.assertTrue( "Alice cannot read", m_auth.checkPermission( session,
                                                                 new PagePermission("TestDefaultPage","view") ) );
    }

    @Test
    public void testAdminView2() throws Exception
    {
        m_engine.saveText( "TestDefaultPage", "Foo [{ALLOW view FooBar}]" );

        WikiSession session = WikiSessionTest.adminSession(m_engine);

        Assert.assertTrue( "Alice has AllPermission", m_auth.checkPermission( session,
                                                                       new AllPermission( m_engine.getApplicationName() )));
        Assert.assertTrue( "Alice cannot read", m_auth.checkPermission( session,
                                                                 new PagePermission("TestDefaultPage","view") ) );
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
        Assert.assertFalse( "Anonymous view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        Assert.assertFalse( "Anonymous edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        Assert.assertFalse( "Anonymous comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        Assert.assertFalse( "Anonymous modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        Assert.assertFalse( "Anonymous upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        Assert.assertFalse( "Anonymous rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        Assert.assertFalse( "Anonymous delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        Assert.assertFalse( "Anonymous prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        Assert.assertFalse( "Anonymous profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        Assert.assertFalse( "Anonymous pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        Assert.assertFalse( "Anonymous groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );

        s = WikiSessionTest.assertedSession( m_engine, "Jack Sparrow" );
        Assert.assertFalse( "Asserted view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        Assert.assertFalse( "Asserted edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        Assert.assertFalse( "Asserted comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        Assert.assertFalse( "Asserted modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        Assert.assertFalse( "Asserted upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        Assert.assertFalse( "Asserted rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        Assert.assertFalse( "Asserted delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        Assert.assertFalse( "Asserted prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        Assert.assertFalse( "Asserted profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        Assert.assertFalse( "Asserted pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        Assert.assertFalse( "Asserted groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );

        s = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        Assert.assertTrue( "Bob  view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        Assert.assertTrue( "Bob edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        Assert.assertTrue( "Bob comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        Assert.assertTrue( "Bob modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        Assert.assertTrue( "Bob upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        Assert.assertFalse( "Bob rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        Assert.assertTrue( "Bob delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        Assert.assertFalse( "Bob prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        Assert.assertFalse( "Bob profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        Assert.assertFalse( "Bob pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        Assert.assertFalse( "Bob groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );

        s = WikiSessionTest.authenticatedSession( m_engine, Users.JANNE, Users.JANNE_PASS );
        Assert.assertTrue( "Janne  view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        Assert.assertTrue( "Janne edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        Assert.assertTrue( "Janne comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        Assert.assertTrue( "Janne modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        Assert.assertTrue( "Janne upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        Assert.assertFalse( "Janne rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        Assert.assertTrue( "Janne delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        Assert.assertFalse( "Janne prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        Assert.assertFalse( "Janne profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        Assert.assertFalse( "Janne pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        Assert.assertFalse( "Janne groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );

        s = WikiSessionTest.adminSession( m_engine );
        Assert.assertTrue( "Admin view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        Assert.assertFalse( "Admin edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        Assert.assertFalse( "Admin comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        Assert.assertFalse( "Admin modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        Assert.assertFalse( "Admin upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        Assert.assertFalse( "Admin rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        Assert.assertFalse( "Admin delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        Assert.assertFalse( "Admin prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        Assert.assertFalse( "Admin profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        Assert.assertFalse( "Admin pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        Assert.assertFalse( "Admin groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );
    }

}
