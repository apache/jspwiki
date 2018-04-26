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

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wiki.PageManager;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiSession;
import org.apache.wiki.WikiSessionTest;
import org.apache.wiki.auth.authorize.Group;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.auth.user.DuplicateUserException;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.auth.user.XMLUserDatabase;
import org.apache.wiki.workflow.Decision;
import org.apache.wiki.workflow.DecisionQueue;
import org.apache.wiki.workflow.DecisionRequiredException;
import org.apache.wiki.workflow.Fact;
import org.apache.wiki.workflow.Outcome;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class UserManagerTest {

    private TestEngine m_engine;
    private UserManager m_mgr;
    private UserDatabase m_db;
    private String m_groupName;

    /**
     *
     */
    @Before
    public void setUp() throws Exception {
        Properties props = TestEngine.getTestProperties();

        // Make sure user profile save workflow is OFF
        props.remove( "jspwiki.approver" + UserManager.SAVE_APPROVER );

        // Make sure we are using the XML user database
        props.put( XMLUserDatabase.PROP_USERDATABASE, "target/test-classes/userdatabase.xml" );
        m_engine = new TestEngine( props );
        m_mgr = m_engine.getUserManager();
        m_db = m_mgr.getUserDatabase();
        m_groupName = "Group" + System.currentTimeMillis();
    }

    @After
    public void tearDown() throws Exception {
        GroupManager groupManager = m_engine.getGroupManager();
        if( groupManager.findRole( m_groupName ) != null ) {
            groupManager.removeGroup( m_groupName );
        }
    }

    /** Call this setup program to use the save-profile workflow. */
    protected void setUpWithWorkflow() throws Exception {
        Properties props = TestEngine.getTestProperties();

        // Turn on user profile saves by the Admin group
        props.put( "jspwiki.approver." + UserManager.SAVE_APPROVER, "Admin" );

        // Make sure we are using the XML user database
        props.put( XMLUserDatabase.PROP_USERDATABASE, "target/test-classes/userdatabase.xml" );
        m_engine = new TestEngine( props );
        m_mgr = m_engine.getUserManager();
        m_db = m_mgr.getUserDatabase();
    }

    @Test
    public void testSetRenamedUserProfile() throws Exception {
        // First, count the number of users, groups, and pages
        int oldUserCount = m_db.getWikiNames().length;
        GroupManager groupManager = m_engine.getGroupManager();
        PageManager pageManager = m_engine.getPageManager();
        AuthorizationManager authManager = m_engine.getAuthorizationManager();
        int oldGroupCount = groupManager.getRoles().length;
        int oldPageCount = pageManager.getTotalPageCount();

        // Setup Step 1: create a new user with random name
        WikiSession session = m_engine.guestSession();
        long now = System.currentTimeMillis();
        String oldLogin = "TestLogin" + now;
        String oldName = "Test User " + now;
        String newLogin = "RenamedLogin" + now;
        String newName = "Renamed User " + now;
        UserProfile profile = m_db.newProfile();
        profile.setEmail( "jspwiki.tests@mailinator.com" );
        profile.setLoginName( oldLogin );
        profile.setFullname( oldName );
        profile.setPassword( "password" );
        m_mgr.setUserProfile( session, profile );

        // 1a. Make sure the profile saved successfully and that we're logged in
        profile = m_mgr.getUserProfile( session );
        Assert.assertEquals( oldLogin, profile.getLoginName() );
        Assert.assertEquals( oldName, profile.getFullname() );
        Assert.assertEquals( oldUserCount + 1, m_db.getWikiNames().length );
        Assert.assertTrue( session.isAuthenticated() );

        // Setup Step 2: create a new group with our test user in it
        Group group = groupManager.parseGroup( m_groupName, "Alice \n Bob \n Charlie \n " + oldLogin + "\n" + oldName, true );
        groupManager.setGroup( session, group );

        // 2a. Make sure the group is created with the user in it, and the role is added to the Subject
        Assert.assertEquals( oldGroupCount + 1, groupManager.getRoles().length );
        Assert.assertTrue( group.isMember( new WikiPrincipal( oldLogin ) ) );
        Assert.assertTrue( group.isMember( new WikiPrincipal( oldName ) ) );
        Assert.assertFalse( group.isMember( new WikiPrincipal( newLogin ) ) );
        Assert.assertFalse( group.isMember( new WikiPrincipal( newName ) ) );
        Assert.assertTrue( groupManager.isUserInRole( session, group.getPrincipal() ) );

        // Setup Step 3: create a new page with our test user in the ACL
        String pageName = "TestPage" + now;
        m_engine.saveText( pageName, "Test text. [{ALLOW view " + oldName + ", " + oldLogin + ", Alice}] More text." );

        // 3a. Make sure the page got saved, and that ONLY our test user has permission to read it.
        WikiPage p = m_engine.getPage( pageName );
        Assert.assertEquals( oldPageCount + 1, pageManager.getTotalPageCount() );
        Assert.assertNotNull( p.getAcl().getEntry( new WikiPrincipal( oldLogin ) ) );
        Assert.assertNotNull( p.getAcl().getEntry( new WikiPrincipal( oldName ) ) );
        Assert.assertNull( p.getAcl().getEntry( new WikiPrincipal( newLogin ) ) );
        Assert.assertNull( p.getAcl().getEntry( new WikiPrincipal( newName ) ) );
        Assert.assertTrue( "Test User view page", authManager.checkPermission( session, PermissionFactory.getPagePermission( p, "view" ) ) );
        WikiSession bobSession = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        Assert.assertFalse( "Bob !view page", authManager.checkPermission( bobSession, PermissionFactory.getPagePermission( p, "view" ) ) );

        // Setup Step 4: change the user name in the profile and see what happens
        profile = m_db.newProfile();
        profile.setEmail( "jspwiki.tests@mailinator.com" );
        profile.setLoginName( oldLogin );
        profile.setFullname( newName );
        profile.setPassword( "password" );
        m_mgr.setUserProfile( session, profile );

        // Test 1: the wiki session should have the new wiki name in Subject
        Principal[] principals = session.getPrincipals();
        Assert.assertTrue( ArrayUtils.contains( principals, new WikiPrincipal( oldLogin ) ) );
        Assert.assertFalse( ArrayUtils.contains( principals, new WikiPrincipal( oldName ) ) );
        Assert.assertFalse( ArrayUtils.contains( principals, new WikiPrincipal( newLogin ) ) );
        Assert.assertTrue( ArrayUtils.contains( principals, new WikiPrincipal( newName ) ) );

        // Test 2: our group should not contain the old name OR login name any more
        // (the full name is always used)
        group = groupManager.getGroup( m_groupName );
        Assert.assertFalse( group.isMember( new WikiPrincipal( oldLogin ) ) );
        Assert.assertFalse( group.isMember( new WikiPrincipal( oldName ) ) );
        Assert.assertFalse( group.isMember( new WikiPrincipal( newLogin ) ) );
        Assert.assertTrue( group.isMember( new WikiPrincipal( newName ) ) );

        // Test 3: our page should not contain the old wiki name OR login name
        // in the ACL any more (the full name is always used)
        p = m_engine.getPage( pageName );
        Assert.assertNull( p.getAcl().getEntry( new WikiPrincipal( oldLogin ) ) );
        Assert.assertNull( p.getAcl().getEntry( new WikiPrincipal( oldName ) ) );
        Assert.assertNull( p.getAcl().getEntry( new WikiPrincipal( newLogin ) ) );
        Assert.assertNotNull( p.getAcl().getEntry( new WikiPrincipal( newName ) ) );
        Assert.assertTrue( "Test User view page", authManager.checkPermission( session, PermissionFactory.getPagePermission( p, "view" ) ) );
        Assert.assertFalse( "Bob !view page", authManager.checkPermission( bobSession, PermissionFactory.getPagePermission( p, "view" ) ) );

        // Test 4: our page text should have been re-written
        // (The new full name should be in the ACL, but the login name should have been removed)
        String expectedText = "[{ALLOW view Alice," + newName + "}]\nTest text.  More text.\r\n";
        String actualText = m_engine.getText( pageName );
        Assert.assertEquals( expectedText, actualText );

        // Remove our test page
        m_engine.deletePage( pageName );

        // Setup Step 6: re-create the group with our old test user names in it
        group = groupManager.parseGroup( m_groupName, "Alice \n Bob \n Charlie \n " + oldLogin + "\n" + oldName, true );
        groupManager.setGroup( session, group );

        // Setup Step 7: Save a new page with the old login/wiki names in the ACL again
        // The test user should still be able to see the page (because the login name matches...)
        pageName = "TestPage2" + now;
        m_engine.saveText( pageName, "More test text. [{ALLOW view " + oldName + ", " + oldLogin + ", Alice}] More text." );
        p = m_engine.getPage( pageName );
        Assert.assertEquals( oldPageCount + 1, pageManager.getTotalPageCount() );
        Assert.assertNotNull( p.getAcl().getEntry( new WikiPrincipal( oldLogin ) ) );
        Assert.assertNotNull( p.getAcl().getEntry( new WikiPrincipal( oldName ) ) );
        Assert.assertNull( p.getAcl().getEntry( new WikiPrincipal( newLogin ) ) );
        Assert.assertNull( p.getAcl().getEntry( new WikiPrincipal( newName ) ) );
        Assert.assertTrue( "Test User view page", authManager.checkPermission( session, PermissionFactory.getPagePermission( p, "view" ) ) );
        Assert.assertFalse( "Bob !view page", authManager.checkPermission( bobSession, PermissionFactory.getPagePermission( p, "view" ) ) );

        // Setup Step 8: re-save the profile with the new login name
        profile = m_db.newProfile();
        profile.setEmail( "jspwiki.tests@mailinator.com" );
        profile.setLoginName( newLogin );
        profile.setFullname( oldName );
        profile.setPassword( "password" );
        m_mgr.setUserProfile( session, profile );

        // Test 5: the wiki session should have the new login name in Subject
        principals = session.getPrincipals();
        Assert.assertFalse( ArrayUtils.contains( principals, new WikiPrincipal( oldLogin ) ) );
        Assert.assertTrue( ArrayUtils.contains( principals, new WikiPrincipal( oldName ) ) );
        Assert.assertTrue( ArrayUtils.contains( principals, new WikiPrincipal( newLogin ) ) );
        Assert.assertFalse( ArrayUtils.contains( principals, new WikiPrincipal( newName ) ) );

        // Test 6: our group should not contain the old name OR login name any more
        // (the full name is always used)
        group = groupManager.getGroup( m_groupName );
        Assert.assertFalse( group.isMember( new WikiPrincipal( oldLogin ) ) );
        Assert.assertTrue( group.isMember( new WikiPrincipal( oldName ) ) );
        Assert.assertFalse( group.isMember( new WikiPrincipal( newLogin ) ) );
        Assert.assertFalse( group.isMember( new WikiPrincipal( newName ) ) );

        // Test 7: our page should not contain the old wiki name OR login name
        // in the ACL any more (the full name is always used)
        p = m_engine.getPage( pageName );
        Assert.assertNull( p.getAcl().getEntry( new WikiPrincipal( oldLogin ) ) );
        Assert.assertNotNull( p.getAcl().getEntry( new WikiPrincipal( oldName ) ) );
        Assert.assertNull( p.getAcl().getEntry( new WikiPrincipal( newLogin ) ) );
        Assert.assertNull( p.getAcl().getEntry( new WikiPrincipal( newName ) ) );
        Assert.assertTrue( "Test User view page", authManager.checkPermission( session, PermissionFactory.getPagePermission( p, "view" ) ) );
        Assert.assertFalse( "Bob !view page", authManager.checkPermission( bobSession, PermissionFactory.getPagePermission( p, "view" ) ) );

        // Test 8: our page text should have been re-written
        // (The new full name should be in the ACL, but the login name should have been removed)
        expectedText = "[{ALLOW view Alice," + oldName + "}]\nMore test text.  More text.\r\n";
        actualText = m_engine.getText( pageName );
        Assert.assertEquals( expectedText, actualText );

        // CLEANUP: delete the profile; user and page; should be back to old counts
        m_db.deleteByLoginName( newLogin );
        Assert.assertEquals( oldUserCount, m_db.getWikiNames().length );

        groupManager.removeGroup( group.getName() );
        Assert.assertEquals( oldGroupCount, groupManager.getRoles().length );

        m_engine.deletePage( pageName );
        Assert.assertEquals( oldPageCount, pageManager.getTotalPageCount() );
    }

    @Test
    public void testSetUserProfile() throws Exception {
        // First, count the number of users in the db now.
        int oldUserCount = m_db.getWikiNames().length;

        // Create a new user with random name
        WikiSession session = m_engine.guestSession();
        String loginName = "TestUser" + String.valueOf( System.currentTimeMillis() );
        UserProfile profile = m_db.newProfile();
        profile.setEmail( "jspwiki.tests@mailinator.com" );
        profile.setLoginName( loginName );
        profile.setFullname( "FullName" + loginName );
        profile.setPassword( "password" );
        m_mgr.setUserProfile( session, profile );

        // Make sure the profile saved successfully
        profile = m_mgr.getUserProfile( session );
        Assert.assertEquals( loginName, profile.getLoginName() );
        Assert.assertEquals( oldUserCount + 1, m_db.getWikiNames().length );

        // Now delete the profile; should be back to old count
        m_db.deleteByLoginName( loginName );
        Assert.assertEquals( oldUserCount, m_db.getWikiNames().length );
    }

    @Test
    public void testSetUserProfileWithApproval() throws Exception {
        setUpWithWorkflow();

        // First, count the number of users in the db now.
        int oldUserCount = m_db.getWikiNames().length;

        // Create a new user with random name
        WikiSession session = m_engine.guestSession();
        String loginName = "TestUser" + String.valueOf( System.currentTimeMillis() );
        UserProfile profile = m_db.newProfile();
        profile.setEmail( "jspwiki.tests@mailinator.com" );
        profile.setLoginName( loginName );
        profile.setFullname( "FullName" + loginName );
        profile.setPassword( "password" );

        // Because user profile saves require approvals, we will catch a Redirect
        try {
            m_mgr.setUserProfile( session, profile );
            Assert.fail( "We should have caught a DecisionRequiredException caused by approval!" );
        } catch( DecisionRequiredException e ) {
        }

        // The user should NOT be saved yet
        Assert.assertEquals( oldUserCount, m_db.getWikiNames().length );

        // Now, look in Admin's queue, and verify there's a pending Decision there
        DecisionQueue dq = m_engine.getWorkflowManager().getDecisionQueue();
        Collection decisions = dq.getActorDecisions( m_engine.adminSession() );
        Assert.assertEquals( 1, decisions.size() );

        // Verify that the Decision has all the facts and attributes we need
        Decision d = ( Decision )decisions.iterator().next();
        List facts = d.getFacts();
        Assert.assertEquals( new Fact( UserManager.PREFS_FULL_NAME, profile.getFullname() ), facts.get( 0 ) );
        Assert.assertEquals( new Fact( UserManager.PREFS_LOGIN_NAME, profile.getLoginName() ), facts.get( 1 ) );
        Assert.assertEquals( new Fact( UserManager.FACT_SUBMITTER, session.getUserPrincipal().getName() ), facts.get( 2 ) );
        Assert.assertEquals( new Fact( UserManager.PREFS_EMAIL, profile.getEmail() ), facts.get( 3 ) );
        Assert.assertEquals( profile, d.getWorkflow().getAttribute( UserManager.SAVED_PROFILE ) );

        // Approve the profile
        d.decide( Outcome.DECISION_APPROVE );

        // Make sure the profile saved successfully
        Assert.assertEquals( oldUserCount + 1, m_db.getWikiNames().length );

        // Now delete the profile; should be back to old count
        m_db.deleteByLoginName( loginName );
        Assert.assertEquals( oldUserCount, m_db.getWikiNames().length );
    }

    @Test
    public void testSetUserProfileWithDenial() throws Exception {
        setUpWithWorkflow();

        // First, count the number of users in the db now.
        int oldUserCount = m_db.getWikiNames().length;

        // Create a new user with random name
        WikiSession session = m_engine.guestSession();
        String loginName = "TestUser" + String.valueOf( System.currentTimeMillis() );
        UserProfile profile = m_db.newProfile();
        profile.setEmail( "jspwiki.tests@mailinator.com" );
        profile.setLoginName( loginName );
        profile.setFullname( "FullName" + loginName );
        profile.setPassword( "password" );

        // Because user profile saves require approvals, we will catch a Redirect
        try {
            m_mgr.setUserProfile( session, profile );
            Assert.fail( "We should have caught a DecisionRequiredException caused by approval!" );
        } catch( DecisionRequiredException e ) {
        }

        // The user should NOT be saved yet
        Assert.assertEquals( oldUserCount, m_db.getWikiNames().length );

        // Now, look in Admin's queue, and verify there's a pending Decision there
        DecisionQueue dq = m_engine.getWorkflowManager().getDecisionQueue();
        Collection decisions = dq.getActorDecisions( m_engine.adminSession() );
        Assert.assertEquals( 1, decisions.size() );

        // Verify that the Decision has all the facts and attributes we need
        Decision d = ( Decision )decisions.iterator().next();
        List facts = d.getFacts();
        Assert.assertEquals( new Fact( UserManager.PREFS_FULL_NAME, profile.getFullname() ), facts.get( 0 ) );
        Assert.assertEquals( new Fact( UserManager.PREFS_LOGIN_NAME, profile.getLoginName() ), facts.get( 1 ) );
        Assert.assertEquals( new Fact( UserManager.FACT_SUBMITTER, session.getUserPrincipal().getName() ), facts.get( 2 ) );
        Assert.assertEquals( new Fact( UserManager.PREFS_EMAIL, profile.getEmail() ), facts.get( 3 ) );
        Assert.assertEquals( profile, d.getWorkflow().getAttribute( UserManager.SAVED_PROFILE ) );

        // Approve the profile
        d.decide( Outcome.DECISION_DENY );

        // Make sure the profile did NOT save
        Assert.assertEquals( oldUserCount, m_db.getWikiNames().length );
    }

    @Test
    public void testSetCollidingUserProfile() throws Exception {
        // First, count the number of users in the db now.
        int oldUserCount = m_db.getWikiNames().length;

        // Create a new user with random name
        WikiSession session = m_engine.guestSession();
        String loginName = "TestUser" + String.valueOf( System.currentTimeMillis() );
        UserProfile profile = m_db.newProfile();
        profile.setEmail( "jspwiki.tests@mailinator.com" );
        profile.setLoginName( loginName );
        profile.setFullname( "FullName" + loginName );
        profile.setPassword( "password" );

        // Set the login name to collide with Janne's: should prohibit saving
        profile.setLoginName( "janne" );
        try {
            m_mgr.setUserProfile( session, profile );
            Assert.fail( "UserManager allowed saving of user with login name 'janne', but it shouldn't have." );
        } catch( DuplicateUserException e ) {
            // Good! That's what we expected; reset for next test
            profile.setLoginName( loginName );
        }

        // Set the login name to collide with Janne's: should prohibit saving
        profile.setFullname( "Janne Jalkanen" );
        try {
            m_mgr.setUserProfile( session, profile );
            Assert.fail( "UserManager allowed saving of user with login name 'janne', but it shouldn't have." );
        } catch( DuplicateUserException e ) {
            // Good! That's what we expected
        }

        // There shouldn't have been any users added
        Assert.assertEquals( oldUserCount, m_db.getWikiNames().length );
    }

}
