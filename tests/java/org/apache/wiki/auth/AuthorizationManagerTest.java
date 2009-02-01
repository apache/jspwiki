/*
    JSPWiki - a JSP-based WikiWiki clone.

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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wiki.*;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.GroupPrincipal;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.acl.UnresolvedPrincipal;
import org.apache.wiki.auth.authorize.Group;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.auth.permissions.WikiPermission;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.providers.ProviderException;


/**
 * Tests the AuthorizationManager class.
 * @author Janne Jalkanen
 */
public class AuthorizationManagerTest extends TestCase
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

    public AuthorizationManagerTest( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( "Authorization Manager test" );
        suite.addTestSuite( AuthorizationManagerTest.class );
        return suite;
    }

    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        
        // Make sure we are using the default security policy file jspwiki.policy
        props.put( AuthorizationManager.POLICY, AuthorizationManager.DEFAULT_POLICY );
        
        // Initialize the test engine
        m_engine = new TestEngine( props );
        m_auth = m_engine.getAuthorizationManager();
        m_groupMgr = m_engine.getGroupManager();
        m_session = WikiSessionTest.adminSession( m_engine );
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        m_engine.shutdown();
    }

    /**
     * Tests the default policy. Anonymous users can read, Authenticated can
     * edit, etc. Uses the default tests/etc/jspwiki.policy file installed by
     * the JRE at startup.
     * @throws Exception
     */
    public void testDefaultPermissions() throws Exception
    {
        // Save a page without an ACL
        m_engine.saveText( "TestDefaultPage", "Foo" );
        Permission view = PermissionFactory.getPagePermission( "*:TestDefaultPage", "view" );
        Permission edit = PermissionFactory.getPagePermission( "*:TestDefaultPage", "edit" );
        WikiSession session;

        // Alice is asserted
        session = WikiSessionTest.assertedSession( m_engine, Users.ALICE );
        assertTrue( "Alice view", m_auth.checkPermission( session, view ) );
        assertTrue( "Alice edit", m_auth.checkPermission( session, edit ) );

        // Bob is logged in
        session = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        assertTrue( "Bob view", m_auth.checkPermission( session, view ) );
        assertTrue( "Bob edit", m_auth.checkPermission( session, edit ) );

        // Delete the test page
        try
        {
            m_engine.deletePage( "TestDefaultPage" );
        }
        catch( ProviderException e )
        {
            assertTrue( false );
        }
    }

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
        assertTrue( "Bob in ALL", ArrayUtils.contains( principals, Role.ALL ) );
        assertTrue( "Bob in ASSERTED", ArrayUtils.contains( principals, Role.ASSERTED ) );
        assertFalse( "Bob not in ANONYMOUS", ArrayUtils.contains( principals, Role.ANONYMOUS ) );
        assertFalse( "Bob not in Test", ArrayUtils.contains( principals, test.getPrincipal() ) );

        // Re-save group "Test" with Bob as a member
        test = m_groupMgr.parseGroup( "Test", "Alice \n Bob \nCharlie", true );
        m_groupMgr.setGroup( m_session, test );

        // Bob not authenticated: should still have only two romes
        principals = session.getRoles();
        assertTrue( "Bob in ALL", ArrayUtils.contains( principals, Role.ALL ) );
        assertTrue( "Bob in ASSERTED", ArrayUtils.contains( principals, Role.ASSERTED ) );
        assertFalse( "Bob not in ANONYMOUS", ArrayUtils.contains( principals, Role.ANONYMOUS ) );
        assertFalse( "Bob in Test", ArrayUtils.contains( principals, test.getPrincipal() ) );

        // Elevate Bob to "authenticated" status
        session = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );

        // Re-save the group; Bob should possess the role now
        test = m_groupMgr.parseGroup( "Test", "Alice \n Bob \n Charlie", true );
        m_groupMgr.setGroup( m_session, test );
        principals = session.getRoles();
        assertTrue( "Bob in ALL", ArrayUtils.contains( principals, Role.ALL ) );
        assertFalse( "Bob in ASSERTED", ArrayUtils.contains( principals, Role.ASSERTED ) );
        assertFalse( "Bob not in ANONYMOUS", ArrayUtils.contains( principals, Role.ANONYMOUS ) );
        assertTrue( "Bob in Test", ArrayUtils.contains( principals, test.getPrincipal() ) );

        // Cleanup
        m_groupMgr.removeGroup( "Test" );
    }

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
        assertFalse ( "Alice has Alice", m_auth.hasRoleOrPrincipal( session, new WikiPrincipal( Users.ALICE ) ) );
        assertFalse ( "Alice has Alice", m_auth.hasRoleOrPrincipal( session, new TestPrincipal( Users.ALICE ) ) );
        assertFalse( "Alice not has Bob", m_auth.hasRoleOrPrincipal( session, new WikiPrincipal( Users.BOB ) ) );
        assertFalse( "Alice not has Bob", m_auth.hasRoleOrPrincipal( session, new TestPrincipal( Users.BOB ) ) );

        // Built-in role memberships
        assertTrue( "Alice in ALL", m_auth.hasRoleOrPrincipal( session, Role.ALL ) );
        assertFalse( "Alice not in ANONYMOUS", m_auth.hasRoleOrPrincipal( session, Role.ANONYMOUS ) );
        assertTrue( "Alice in ASSERTED", m_auth.hasRoleOrPrincipal( session, Role.ASSERTED ) );
        assertFalse( "Alice not in AUTHENTICATED", m_auth.hasRoleOrPrincipal( session, Role.AUTHENTICATED ) );

        // Custom roles should be FALSE because Alice is asserted
        assertFalse( "Alice not in IT", m_auth.hasRoleOrPrincipal( session, it ) );
        assertFalse( "Alice not in Engineering", m_auth.hasRoleOrPrincipal( session, engineering ) );
        assertFalse( "Alice not in Finance", m_auth.hasRoleOrPrincipal( session, finance ) );

        // Group memberships should be FALSE because Alice is asserted
        assertFalse( "Alice not in Foo", m_auth.hasRoleOrPrincipal( session, fooGroup.getPrincipal() ) );
        assertFalse( "Alice not in Bar", m_auth.hasRoleOrPrincipal( session, barGroup.getPrincipal() ) );

        // Clean up
        m_groupMgr.removeGroup( "Foo" );
        m_groupMgr.removeGroup( "Bar" );
    }

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
        assertTrue( "Alice has Alice", m_auth.hasRoleOrPrincipal( session, new WikiPrincipal( Users.ALICE ) ) );
        assertTrue( "Alice has Alice", m_auth.hasRoleOrPrincipal( session, new TestPrincipal( Users.ALICE ) ) );
        assertFalse( "Alice not has Bob", m_auth.hasRoleOrPrincipal( session, new WikiPrincipal( Users.BOB ) ) );
        assertFalse( "Alice not has Bob", m_auth.hasRoleOrPrincipal( session, new TestPrincipal( Users.BOB ) ) );

        // Built-in role membership
        assertTrue( "Alice in ALL", m_auth.hasRoleOrPrincipal( session, Role.ALL ) );
        assertFalse( "Alice not in ANONYMOUS", m_auth.hasRoleOrPrincipal( session, Role.ANONYMOUS ) );
        assertFalse( "Alice not in ASSERTED", m_auth.hasRoleOrPrincipal( session, Role.ASSERTED ) );
        assertTrue( "Alice in AUTHENTICATED", m_auth.hasRoleOrPrincipal( session, Role.AUTHENTICATED ) );

        // Custom roles
        assertTrue( "Alice in IT", m_auth.hasRoleOrPrincipal( session, it ) );
        assertTrue( "Alice in Engineering", m_auth.hasRoleOrPrincipal( session, engineering ) );
        assertFalse( "Alice not in Finance", m_auth.hasRoleOrPrincipal( session, finance ) );

        // Group memberships
        assertFalse( "Alice not in Foo", m_auth.hasRoleOrPrincipal( session, fooGroup.getPrincipal() ) );
        assertTrue( "Alice in Bar", m_auth.hasRoleOrPrincipal( session, barGroup.getPrincipal() ) );

        // Cleanup
        m_groupMgr.removeGroup( "Foo" );
        m_groupMgr.removeGroup( "Bar" );
    }

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
        assertTrue( "Alice view Test/test1.txt", m_auth.checkPermission( session, view ) );
        assertTrue( "Alice edit Test/test1.txt", m_auth.checkPermission( session, edit ) );

        // Create authenticated session with user 'Bob', who can't read or edit (not in ACL)
        session = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        assertFalse( "Bob !view Test/test1.txt", m_auth.checkPermission( session, view ) );
        assertFalse( "Bob !edit Test/test1.txt", m_auth.checkPermission( session, edit ) );

        // Delete test page & attachment
        m_engine.getAttachmentManager().deleteAttachment( att );
        m_engine.deletePage( "Test" );
    }

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
        assertTrue( "Foo view Test", m_auth.checkPermission( session, view ) );
        assertFalse( "Foo !edit Test", m_auth.checkPermission( session, edit ) );

        // Create session with user 'Bob', who can't read or edit (not in ACL)
        session = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        assertFalse( "Bar !view Test", m_auth.checkPermission( session, view ) );
        assertFalse( "Bar !edit Test", m_auth.checkPermission( session, view ) );

        // Delete test page & attachment
        m_engine.getAttachmentManager().deleteAttachment( att );
        m_engine.deletePage( "Test" );
    }

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
        assertTrue ( "Anon anonymous", m_auth.hasRoleOrPrincipal( session, Role.ANONYMOUS ) );
        assertFalse( "Anon not asserted", m_auth.hasRoleOrPrincipal( session, Role.ASSERTED ) );
        assertFalse( "Anon not authenticated", m_auth.hasRoleOrPrincipal( session, Role.AUTHENTICATED ) );
        assertFalse( "Alice not in Anon", m_auth.hasRoleOrPrincipal( session, alice ) );
        assertFalse( "Anon not in IT", m_auth.hasRoleOrPrincipal( session, it ) );
        assertFalse( "Anon not in Finance", m_auth.hasRoleOrPrincipal( session, finance ) );
        assertFalse( "Anon not in Group1", m_auth.hasRoleOrPrincipal( session, group1 ) );
        assertFalse( "Anon not in Group2", m_auth.hasRoleOrPrincipal( session, group2 ) );

        // Create asserted session with 1 GroupPrincipal & 1 custom Role
        // Alice is asserted, and thus not in ANY custom roles or groups
        session = WikiSessionTest.assertedSession( m_engine, Users.ALICE, new Principal[] { it } );
        assertFalse( "Alice not anonymous", m_auth.hasRoleOrPrincipal( session, Role.ANONYMOUS ) );
        assertTrue ( "Alice asserted", m_auth.hasRoleOrPrincipal( session, Role.ASSERTED ) );
        assertFalse( "Alice not authenticated", m_auth.hasRoleOrPrincipal( session, Role.AUTHENTICATED ) );
        assertFalse( "Alice not in Alice", m_auth.hasRoleOrPrincipal( session, alice ) );
        assertFalse( "Alice not in IT", m_auth.hasRoleOrPrincipal( session, it ) );
        assertFalse( "Alice not in Finance", m_auth.hasRoleOrPrincipal( session, finance ) );
        assertFalse( "Alice not in Group1", m_auth.hasRoleOrPrincipal( session, group1 ) );
        assertFalse( "Alice not in Group2", m_auth.hasRoleOrPrincipal( session, group2 ) );

        // Create authenticated session with 1 GroupPrincipal & 1 custom Role
        // Alice is authenticated, and thus part of custom roles and groups
        session = WikiSessionTest.containerAuthenticatedSession( m_engine, Users.ALICE, new Principal[] { it } );
        assertFalse( "Alice not anonymous", m_auth.hasRoleOrPrincipal( session, Role.ANONYMOUS ) );
        assertFalse( "Alice not asserted", m_auth.hasRoleOrPrincipal( session, Role.ASSERTED ) );
        assertTrue ( "Alice authenticated", m_auth.hasRoleOrPrincipal( session, Role.AUTHENTICATED ) );
        assertTrue ( "Alice in Ernie", m_auth.hasRoleOrPrincipal( session, alice ) );
        assertTrue ( "Alice in IT", m_auth.hasRoleOrPrincipal( session, it ) );
        assertFalse( "Alice not in Finance", m_auth.hasRoleOrPrincipal( session, finance ) );
        assertTrue ( "Alice in Group1", m_auth.hasRoleOrPrincipal( session, group1 ) );
        assertFalse( "Alice not in Group2", m_auth.hasRoleOrPrincipal( session, group2 ) );

        // Clean up
        m_groupMgr.removeGroup( "Group1" );
        m_groupMgr.removeGroup( "Group2" );
    }

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
        assertTrue ( "Anon anonymous", m_auth.isUserInRole( session, Role.ANONYMOUS ) );
        assertFalse( "Anon not asserted", m_auth.isUserInRole( session, Role.ASSERTED ) );
        assertFalse( "Anon not authenticated", m_auth.isUserInRole( session, Role.AUTHENTICATED ) );
        assertFalse( "Anon not in Ernie", m_auth.isUserInRole( session, alice ) );
        assertFalse( "Anon not in IT", m_auth.isUserInRole( session, it ) );
        assertFalse( "Anon not in Finance", m_auth.isUserInRole( session, finance ) );
        assertFalse( "Anon not in Group1", m_auth.isUserInRole( session, group1 ) );
        assertFalse( "Anon not in Group2", m_auth.isUserInRole( session, group2 ) );

        // Create asserted session with 1 GroupPrincipal & 1 custom Role
        // Alice is asserted, and thus not in ANY custom roles or groups
        session = WikiSessionTest.assertedSession( m_engine, Users.ALICE, new Principal[] { it } );
        assertFalse( "Alice not anonymous", m_auth.isUserInRole( session, Role.ANONYMOUS ) );
        assertTrue ( "Alice asserted", m_auth.isUserInRole( session, Role.ASSERTED ) );
        assertFalse( "Alice not authenticated", m_auth.isUserInRole( session, Role.AUTHENTICATED ) );
        assertFalse( "Alice not in Alice", m_auth.isUserInRole( session, alice ) );
        assertFalse( "Alice not in IT", m_auth.isUserInRole( session, it ) );
        assertFalse( "Alice not in Finance", m_auth.isUserInRole( session, finance ) );
        assertFalse( "Alice not in Group1", m_auth.isUserInRole( session, group1 ) );
        assertFalse( "Alice not in Group2", m_auth.isUserInRole( session, group2 ) );

        // Create authenticated session with 1 GroupPrincipal & 1 custom Role
        // Ernie is authenticated, and thus part of custom roles and groups
        session = WikiSessionTest.containerAuthenticatedSession( m_engine, Users.ALICE, new Principal[] { it } );
        assertFalse( "Alice not anonymous", m_auth.isUserInRole( session, Role.ANONYMOUS ) );
        assertFalse( "Alice not asserted", m_auth.isUserInRole( session, Role.ASSERTED ) );
        assertTrue ( "Alice not authenticated", m_auth.isUserInRole( session, Role.AUTHENTICATED ) );
        assertFalse( "Alice not in Alice", m_auth.isUserInRole( session, alice ) );
        assertTrue ( "Alice in IT", m_auth.isUserInRole( session, it ) );
        assertFalse( "Alice not in Finance", m_auth.isUserInRole( session, finance ) );
        assertTrue ( "Alice in Group1", m_auth.isUserInRole( session, group1 ) );
        assertFalse( "Alice not in Group2", m_auth.isUserInRole( session, group2 ) );

        // Clean up
        m_groupMgr.removeGroup( "Group1" );
        m_groupMgr.removeGroup( "Group2" );
    }

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
        assertTrue( "Alice view Test", m_auth.checkPermission( session, view ) );
        assertTrue( "Alice edit Test", m_auth.checkPermission( session, edit ) );

        // Create session with authenticated user 'Bob', who can't read or edit (not in ACL)
        session = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        assertFalse( "Bob !view Test", m_auth.checkPermission( session, view ) );
        assertFalse( "Bob !edit Test", m_auth.checkPermission( session, edit ) );

        // Cleanup
        try
        {
            m_engine.deletePage( "Test" );
        }
        catch( ProviderException e )
        {
            fail( "Could not delete page" );
        }
    }

    /**
     * Any principal strings that have same names as built-in roles should
     * resolve as built-in roles!
     */
    public void testResolveBuiltInRoles()
    {
        Principal principal = Role.AUTHENTICATED;
        assertEquals( principal, m_auth.resolvePrincipal( "Authenticated" ) );
        principal = Role.ASSERTED;
        assertEquals( principal, m_auth.resolvePrincipal( "Asserted" ) );
        principal = Role.ALL;
        assertEquals( principal, m_auth.resolvePrincipal( "All" ) );
        principal = Role.ANONYMOUS;
        assertEquals( principal, m_auth.resolvePrincipal( "Anonymous" ) );

        // This should not resolve because there's no built-in role Admin
        principal = new WikiPrincipal( "Admin" );
        assertFalse( principal.equals( m_auth.resolvePrincipal( "Admin" ) ) );
    }

    public void testResolveGroups() throws WikiException
    {
        Group group1 = m_groupMgr.parseGroup( "SampleGroup", "", true );
        m_groupMgr.setGroup( m_session, group1 );

        assertEquals( group1.getPrincipal(), m_auth.resolvePrincipal( "SampleGroup" ) );
        m_groupMgr.removeGroup( "SampleGroup" );

        // We shouldn't be able to spoof a built-in role
        try
        {
            Group group2 = m_groupMgr.parseGroup( "Authenticated", "", true );
            assertNotSame( group2.getPrincipal(), m_auth.resolvePrincipal( "Authenticated" ) );
        }
        catch ( WikiSecurityException e )
        {
            assertTrue ( "Authenticated not allowed as group name.", true );
        }
        assertEquals( Role.AUTHENTICATED, m_auth.resolvePrincipal( "Authenticated" ) );
    }

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
            fail( "Failed save: " + e.getLocalizedMessage() );
        }
        assertEquals( new WikiPrincipal( "authmanagertest",  WikiPrincipal.LOGIN_NAME ), m_auth.resolvePrincipal( "authmanagertest" ) );
        assertEquals( new WikiPrincipal( "AuthorizationManagerTest User", WikiPrincipal.FULL_NAME ), m_auth.resolvePrincipal( "AuthorizationManagerTest User" ) );
        assertEquals( new WikiPrincipal( "AuthorizationManagerTestUser", WikiPrincipal.WIKI_NAME ), m_auth.resolvePrincipal( "AuthorizationManagerTestUser" ) );
        try
        {
            m_engine.getUserManager().getUserDatabase().deleteByLoginName( "authmanagertest" );
        }
        catch( WikiSecurityException e )
        {
            fail( "Failed delete: " + e.getLocalizedMessage() );
        }


        // A wiki group should resolve to itself
        Group group1 = m_groupMgr.parseGroup( "SampleGroup", "", true );
        m_groupMgr.setGroup( m_session, group1 );
        assertEquals( group1.getPrincipal(), m_auth.resolvePrincipal( "SampleGroup" ) );
        m_groupMgr.removeGroup( "SampleGroup" );

        // A built-in role should resolve to itself
        assertEquals( Role.AUTHENTICATED, m_auth.resolvePrincipal( "Authenticated" ) );

        // We shouldn't be able to spoof a built-in role
        assertNotSame( new WikiPrincipal( "Authenticated" ), m_auth.resolvePrincipal( "Authenticated" ) );

        // An unknown user should resolve to a generic UnresolvedPrincipal
        Principal principal = new UnresolvedPrincipal( "Bart Simpson" );
        assertEquals( principal, m_auth.resolvePrincipal( "Bart Simpson" ) );
    }

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
        assertTrue( "Alice view Test", m_auth.checkPermission( session, view ) );
        assertTrue( "Alice edit Test", m_auth.checkPermission( session, edit ) );

        // Create session with asserted user 'Bob', who can't read or edit (not in ACL)
        session = WikiSessionTest.assertedSession( m_engine, Users.BOB );
        assertFalse( "Bob !view Test", m_auth.checkPermission( session, view ) );
        assertFalse( "Bob !edit Test", m_auth.checkPermission( session, edit ) );

        // Cleanup
        try
        {
            m_engine.deletePage( "Test" );
        }
        catch( ProviderException e )
        {
            assertTrue( false );
        }
    }

    public void testStaticPermission() throws Exception
    {
        WikiSession s = WikiSessionTest.anonymousSession( m_engine );
        assertTrue( "Anonymous view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertTrue( "Anonymous edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertTrue( "Anonymous comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertFalse( "Anonymous modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        assertFalse( "Anonymous upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertFalse( "Anonymous rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        assertFalse( "Anonymous delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertTrue( "Anonymous prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        assertTrue( "Anonymous profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        assertTrue( "Anonymous pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        assertFalse( "Anonymous groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );

        s = WikiSessionTest.assertedSession( m_engine, "Jack Sparrow" );
        assertTrue( "Asserted view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertTrue( "Asserted edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertTrue( "Asserted comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertFalse( "Asserted modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        assertFalse( "Asserted upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertFalse( "Asserted rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        assertFalse( "Asserted delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertTrue( "Asserted prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        assertTrue( "Asserted profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        assertTrue( "Asserted pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        assertFalse( "Asserted groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );

        s = WikiSessionTest.authenticatedSession( m_engine, Users.JANNE, Users.JANNE_PASS );
        assertTrue( "Authenticated view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertTrue( "Authenticated edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertTrue( "Authenticated comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertTrue( "Authenticated modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        assertTrue( "Authenticated upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertTrue( "Authenticated rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        assertFalse( "Authenticated delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertTrue( "Authenticated prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        assertTrue( "Authenticated profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        assertTrue( "Authenticated pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        assertTrue( "Authenticated groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );

        s = WikiSessionTest.adminSession( m_engine );
        assertTrue( "Admin view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertTrue( "Admin edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertTrue( "Admin comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertTrue( "Admin modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        assertTrue( "Admin upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertTrue( "Admin rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        // Even though we grant AllPermission in the policy, 'delete' isn't explicit so the check
        // for delete privileges will fail (but it will succeed if requested via the checkPermission())
        assertFalse( "Admin delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertTrue( "Admin prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        assertTrue( "Admin profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        assertTrue( "Admin pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        assertTrue( "Admin groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );
    }
    
    public void testAdminView()
       throws Exception
    {
        m_engine.saveText( "TestDefaultPage", "Foo [{ALLOW view FooBar}]" );
        
        Principal admin = new GroupPrincipal( "Admin" );
        WikiSession session = WikiSessionTest.containerAuthenticatedSession(
                m_engine,
                Users.ALICE,
                new Principal[] { admin } );

        assertTrue( "Alice has AllPermission", m_auth.checkPermission( session, 
                                                                       new AllPermission( m_engine.getApplicationName() )));
        assertTrue( "Alice cannot read", m_auth.checkPermission( session, 
                                                                 new PagePermission("TestDefaultPage","view") ) );
    }

    public void testAdminView2() throws Exception 
    {
        m_engine.saveText( "TestDefaultPage", "Foo [{ALLOW view FooBar}]" );
     
        WikiSession session = WikiSessionTest.adminSession(m_engine);

        assertTrue( "Alice has AllPermission", m_auth.checkPermission( session, 
                                                                       new AllPermission( m_engine.getApplicationName() )));
        assertTrue( "Alice cannot read", m_auth.checkPermission( session, 
                                                                 new PagePermission("TestDefaultPage","view") ) );
    }
    
    public void testUserPolicy() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        
        // Make sure we are using the default security policy file jspwiki.policy
        props.put( AuthorizationManager.POLICY, "jspwiki-testUserPolicy.policy" );
        
        // Initialize the test engine
        m_engine.shutdown();
        m_engine = new TestEngine( props );
        m_auth = m_engine.getAuthorizationManager();
        m_groupMgr = m_engine.getGroupManager();
        m_session = WikiSessionTest.adminSession( m_engine );
        
        WikiSession s = WikiSessionTest.anonymousSession( m_engine );
        assertFalse( "Anonymous view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertFalse( "Anonymous edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertFalse( "Anonymous comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertFalse( "Anonymous modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        assertFalse( "Anonymous upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertFalse( "Anonymous rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        assertFalse( "Anonymous delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertFalse( "Anonymous prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        assertFalse( "Anonymous profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        assertFalse( "Anonymous pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        assertFalse( "Anonymous groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );

        s = WikiSessionTest.assertedSession( m_engine, "Jack Sparrow" );
        assertFalse( "Asserted view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertFalse( "Asserted edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertFalse( "Asserted comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertFalse( "Asserted modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        assertFalse( "Asserted upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertFalse( "Asserted rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        assertFalse( "Asserted delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertFalse( "Asserted prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        assertFalse( "Asserted profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        assertFalse( "Asserted pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        assertFalse( "Asserted groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );

        s = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        assertTrue( "Bob  view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertTrue( "Bob edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertTrue( "Bob comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertTrue( "Bob modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        assertTrue( "Bob upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertFalse( "Bob rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        assertTrue( "Bob delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertFalse( "Bob prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        assertFalse( "Bob profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        assertFalse( "Bob pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        assertFalse( "Bob groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );

        s = WikiSessionTest.authenticatedSession( m_engine, Users.JANNE, Users.JANNE_PASS );
        assertTrue( "Janne  view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertTrue( "Janne edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertTrue( "Janne comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertTrue( "Janne modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        assertTrue( "Janne upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertFalse( "Janne rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        assertTrue( "Janne delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertFalse( "Janne prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        assertFalse( "Janne profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        assertFalse( "Janne pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        assertFalse( "Janne groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );

        s = WikiSessionTest.adminSession( m_engine );
        assertTrue( "Admin view", m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertFalse( "Admin edit", m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertFalse( "Admin comment", m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertFalse( "Admin modify", m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        assertFalse( "Admin upload", m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertFalse( "Admin rename", m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        assertFalse( "Admin delete", m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertFalse( "Admin prefs", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        assertFalse( "Admin profile", m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        assertFalse( "Admin pages", m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        assertFalse( "Admin groups", m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );
    }

}
