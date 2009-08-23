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
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.WikiSessionTest;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.auth.acl.Acl;
import org.apache.wiki.auth.authorize.Group;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.auth.user.DuplicateUserException;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.auth.user.XMLUserDatabase;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.workflow.*;


/**
 */
public class UserManagerTest extends TestCase
{

  private TestEngine m_engine;
  private UserManager m_mgr;
  private UserDatabase m_db;
  private String m_groupName;
  
  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception
  {
    super.setUp();
    Properties props = new Properties();
    props.load( TestEngine.findTestProperties() );
    
    // Make sure user profile save workflow is OFF
    props.remove( "jspwiki.approver"+UserManager.SAVE_APPROVER );
    
    // Make sure we are using the XML user database
    props.put( XMLUserDatabase.PROP_USERDATABASE, "tests/etc/userdatabase.xml" );
    m_engine  = new TestEngine( props );
    m_mgr = m_engine.getUserManager();
    m_db = m_mgr.getUserDatabase();
    m_groupName = "Group" + System.currentTimeMillis();
  }
  
  protected void tearDown() throws Exception
  {
    GroupManager groupManager = m_engine.getGroupManager();
    if ( groupManager.findRole( m_groupName ) != null )
    {
        groupManager.removeGroup( m_groupName );
    }
    
    m_engine.shutdown();
  }

  /** Call this setup program to use the save-profile workflow. */
  protected void setUpWithWorkflow() throws Exception
  {
      Properties props = new Properties();
      props.load( TestEngine.findTestProperties() );
      
      // Turn on user profile saves by the Admin group
      props.put( "jspwiki.approver."+UserManager.SAVE_APPROVER, "Admin" );
      
      // Make sure we are using the XML user database
      props.put( XMLUserDatabase.PROP_USERDATABASE, "tests/etc/userdatabase.xml" );
      m_engine.shutdown();
      m_engine  = new TestEngine( props );
      m_mgr = m_engine.getUserManager();
      m_db = m_mgr.getUserDatabase();
  }
  
  public void testSetRenamedUserProfile() throws Exception
  {
      // First, count the number of users, groups, and pages
      int oldUserCount = m_db.getWikiNames().length;
      GroupManager groupManager = m_engine.getGroupManager();
      ContentManager contentManager = m_engine.getContentManager();
      AuthorizationManager authManager = m_engine.getAuthorizationManager();
      int oldGroupCount = groupManager.getRoles().length;
      
      // Setup Step 1: create a new user with random name
      WikiSession session = m_engine.guestSession();
      long now = System.currentTimeMillis();
      String oldLogin     = "TestLogin" + now;
      String oldName      = "Test User " + now;
      String newLogin     = "RenamedLogin" + now;
      String newName      = "Renamed User " + now;
      UserProfile profile = m_db.newProfile();
      profile.setEmail( "testuser@testville.com" );
      profile.setLoginName( oldLogin );
      profile.setFullname ( oldName );
      profile.setPassword ( "password" );
      m_mgr.setUserProfile( session, profile );
      
      // 1a. Make sure the profile saved successfully and that we're logged in
      profile = m_mgr.getUserProfile( session );
      assertEquals( oldLogin, profile.getLoginName() );
      assertEquals( oldName, profile.getFullname() );
      assertEquals( oldUserCount+1, m_db.getWikiNames().length );
      assertTrue( session.isAuthenticated() );
      
      // Setup Step 2: create a new group with our test user in it
      Group group = groupManager.parseGroup( m_groupName, "Alice \n Bob \n Charlie \n " + oldLogin + "\n" + oldName, true );
      groupManager.setGroup( session, group );
      
      // 2a. Make sure the group is created with the user in it, and the role is added to the Subject
      assertEquals( oldGroupCount+1, groupManager.getRoles().length );
      assertTrue  ( group.isMember( new WikiPrincipal( oldLogin ) ) );
      assertTrue  ( group.isMember( new WikiPrincipal( oldName  ) ) );
      assertFalse ( group.isMember( new WikiPrincipal( newLogin ) ) );
      assertFalse ( group.isMember( new WikiPrincipal( newName  ) ) );
      assertTrue  ( groupManager.isUserInRole( session, group.getPrincipal() ) );
      
      // Setup Step 3: create a new page with our test user in the ACL
      int oldPageCount = contentManager.getTotalPageCount( null );
      String pageName = "TestPage" + now;
      m_engine.saveText( pageName, "Test text. [{ALLOW view " + oldName + ", " + oldLogin + ", Alice}] More text." );
      
      // 3a. Make sure the page got saved, and that ONLY our test user has permission to read it.
      WikiPage p = m_engine.getPage( pageName );
      assertEquals ( oldPageCount+1, contentManager.getTotalPageCount( null ) );
      assertNotNull( p.getAcl().getEntry( new WikiPrincipal( oldLogin ) ) );
      assertNotNull( p.getAcl().getEntry( new WikiPrincipal( oldName  ) ) );
      assertNull   ( p.getAcl().getEntry( new WikiPrincipal( newLogin ) ) );
      assertNull   ( p.getAcl().getEntry( new WikiPrincipal( newName  ) ) );
      assertTrue   ( "Test User view page", authManager.checkPermission( session, PermissionFactory.getPagePermission( p, "view" ) ) );
      WikiSession bobSession = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
      assertFalse  ( "Bob !view page", authManager.checkPermission( bobSession, PermissionFactory.getPagePermission( p, "view" ) ) );
      
      // Setup Step 4: change the user name in the profile and see what happens
      profile = m_db.newProfile();
      profile.setEmail    ( "testuser@testville.com" );
      profile.setLoginName( oldLogin );
      profile.setFullname ( newName );
      profile.setPassword ( "password" );
      m_mgr.setUserProfile( session, profile );
      
      // Test 1: the wiki session should have the new wiki name in Subject
      Principal[] principals = session.getPrincipals();
      assertTrue ( ArrayUtils.contains( principals, new WikiPrincipal( oldLogin ) ) );
      assertFalse( ArrayUtils.contains( principals, new WikiPrincipal( oldName  ) ) );
      assertFalse( ArrayUtils.contains( principals, new WikiPrincipal( newLogin ) ) );
      assertTrue ( ArrayUtils.contains( principals, new WikiPrincipal( newName  ) ) );
      
      // Test 2: our group should not contain the old name OR login name any more
      // (the full name is always used)
      group = groupManager.getGroup( m_groupName );
      assertFalse( group.isMember( new WikiPrincipal( oldLogin ) ) );
      assertFalse( group.isMember( new WikiPrincipal( oldName  ) ) );
      assertFalse( group.isMember( new WikiPrincipal( newLogin ) ) );
      assertTrue ( group.isMember( new WikiPrincipal( newName  ) ) );
      
      // Test 3: our page should not contain the old wiki name OR login name
      // in the ACL any more (the full name is always used)
      p = m_engine.getPage( pageName );
      Acl acl = p.getAcl();
      assertNotNull ( acl );
      assertNull   ( acl.getEntry( new WikiPrincipal( oldLogin ) ) );
      assertNull   ( acl.getEntry( new WikiPrincipal( oldName  ) ) );
      assertNull   ( acl.getEntry( new WikiPrincipal( newLogin ) ) );
      assertNotNull( acl.getEntry( new WikiPrincipal( newName  ) ) );
      assertTrue( "Test User view page", authManager.checkPermission( session, PermissionFactory.getPagePermission( p, "view" ) ) );
      assertFalse( "Bob !view page", authManager.checkPermission( bobSession, PermissionFactory.getPagePermission( p, "view" ) ) );
      
      // Test 4: our page text should have been re-written
      // (with the ACL text surgically removed)
      String expectedText = "Test text.  More text.\r\n";
      String actualText = m_engine.getText( pageName );
      assertEquals( expectedText, actualText );
      
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
      assertEquals ( oldPageCount+1, contentManager.getTotalPageCount( null ) );
      assertNotNull( p.getAcl().getEntry( new WikiPrincipal( oldLogin ) ) );
      assertNotNull( p.getAcl().getEntry( new WikiPrincipal( oldName  ) ) );
      assertNull   ( p.getAcl().getEntry( new WikiPrincipal( newLogin ) ) );
      assertNull   ( p.getAcl().getEntry( new WikiPrincipal( newName  ) ) );
      assertTrue   ( "Test User view page", authManager.checkPermission( session, PermissionFactory.getPagePermission( p, "view" ) ) );
      assertFalse  ( "Bob !view page", authManager.checkPermission( bobSession, PermissionFactory.getPagePermission( p, "view" ) ) );
      
      // Setup Step 8: re-save the profile with the new login name
      profile = m_db.newProfile();
      profile.setEmail    ( "testuser@testville.com" );
      profile.setLoginName( newLogin );
      profile.setFullname ( oldName );
      profile.setPassword ( "password" );
      m_mgr.setUserProfile( session, profile );

      // Test 5: the wiki session should have the new login name in Subject
      principals = session.getPrincipals();
      assertFalse( ArrayUtils.contains( principals, new WikiPrincipal( oldLogin ) ) );
      assertTrue ( ArrayUtils.contains( principals, new WikiPrincipal( oldName  ) ) );
      assertTrue ( ArrayUtils.contains( principals, new WikiPrincipal( newLogin ) ) );
      assertFalse( ArrayUtils.contains( principals, new WikiPrincipal( newName  ) ) );
      
      // Test 6: our group should not contain the old name OR login name any more
      // (the full name is always used)
      group = groupManager.getGroup( m_groupName );
      assertFalse( group.isMember( new WikiPrincipal( oldLogin ) ) );
      assertTrue ( group.isMember( new WikiPrincipal( oldName  ) ) );
      assertFalse( group.isMember( new WikiPrincipal( newLogin ) ) );
      assertFalse( group.isMember( new WikiPrincipal( newName  ) ) );
      
      // Test 7: our page should not contain the old wiki name OR login name
      // in the ACL any more (the full name is always used)
      p = m_engine.getPage( pageName );
      assertNull   ( p.getAcl().getEntry( new WikiPrincipal( oldLogin ) ) );
      assertNotNull( p.getAcl().getEntry( new WikiPrincipal( oldName  ) ) );
      assertNull   ( p.getAcl().getEntry( new WikiPrincipal( newLogin ) ) );
      assertNull   ( p.getAcl().getEntry( new WikiPrincipal( newName  ) ) );
      assertTrue( "Test User view page", authManager.checkPermission( session, PermissionFactory.getPagePermission( p, "view" ) ) );
      assertFalse( "Bob !view page", authManager.checkPermission( bobSession, PermissionFactory.getPagePermission( p, "view" ) ) );
      
      // Test 8: our page text should have been re-written
      // (with the ACL text surgically removed)
      expectedText = "More test text.  More text.\r\n";
      actualText = m_engine.getText( pageName );
      assertEquals( expectedText, actualText );
      
      // CLEANUP: delete the profile; user and page; should be back to old counts
      m_db.deleteByLoginName( newLogin );
      assertEquals( oldUserCount, m_db.getWikiNames().length );
      
      groupManager.removeGroup( group.getName() );
      assertEquals( oldGroupCount, groupManager.getRoles().length );
      
      m_engine.deletePage( pageName );
      assertEquals( oldPageCount, contentManager.getTotalPageCount( null ) );
  }

  public void testSetUserProfile() throws Exception
  {
      // First, count the number of users in the db now.
      int oldUserCount = m_db.getWikiNames().length;
      
      // Create a new user with random name
      WikiSession session = m_engine.guestSession();
      String loginName = "TestUser" + String.valueOf( System.currentTimeMillis() );
      UserProfile profile = m_db.newProfile();
      profile.setEmail( "testuser@testville.com" );
      profile.setLoginName( loginName );
      profile.setFullname( "FullName"+loginName );
      profile.setPassword( "password");
      m_mgr.setUserProfile( session, profile );
      
      // Make sure the profile saved successfully
      profile = m_mgr.getUserProfile( session );
      assertEquals( loginName, profile.getLoginName() );
      assertEquals( oldUserCount+1, m_db.getWikiNames().length );

      // Now delete the profile; should be back to old count
      m_db.deleteByLoginName( loginName );
      assertEquals( oldUserCount, m_db.getWikiNames().length );
  }

  public void testSetUserProfileWithApproval() throws Exception
  {
      setUpWithWorkflow();
      
      // First, count the number of users in the db now.
      int oldUserCount = m_db.getWikiNames().length;
      
      // Create a new user with random name
      WikiSession session = m_engine.guestSession();
      String loginName = "TestUser" + String.valueOf( System.currentTimeMillis() );
      UserProfile profile = m_db.newProfile();
      profile.setEmail( "testuser@testville.com" );
      profile.setLoginName( loginName );
      profile.setFullname( "FullName"+loginName );
      profile.setPassword( "password");
      
      // Because user profile saves require approvals, we will catch a Redirect
      try 
      {
          m_mgr.setUserProfile( session, profile );
          fail( "We should have caught a DecisionRequiredException caused by approval!" );
      }
      catch ( DecisionRequiredException e )
      {
      }
      
      // The user should NOT be saved yet
      assertEquals( oldUserCount, m_db.getWikiNames().length );
      
      // Now, look in Admin's queue, and verify there's a pending Decision there
      DecisionQueue dq = m_engine.getWorkflowManager().getDecisionQueue();
      Collection<Decision> decisions = dq.getActorDecisions( m_engine.adminSession() );
      assertEquals( 1, decisions.size() );

      // Verify that the Decision has all the facts and attributes we need
      Decision d = decisions.iterator().next();
      List<Fact> facts = d.getFacts();
      assertEquals( new Fact( UserManager.PREFS_FULL_NAME, profile.getFullname() ), facts.get(0) );
      assertEquals( new Fact( UserManager.PREFS_LOGIN_NAME, profile.getLoginName() ), facts.get(1) );
      assertEquals( new Fact( UserManager.FACT_SUBMITTER, session.getUserPrincipal().getName() ), facts.get(2) );
      assertEquals( new Fact( UserManager.PREFS_EMAIL, profile.getEmail() ), facts.get(3) );
      assertEquals( profile, d.getWorkflow().getAttribute( UserManager.SAVED_PROFILE ) );
      
      // Approve the profile
      d.decide( Outcome.DECISION_APPROVE );
      
      // Make sure the profile saved successfully
      assertEquals( oldUserCount+1, m_db.getWikiNames().length );

      // Now delete the profile; should be back to old count
      m_db.deleteByLoginName( loginName );
      assertEquals( oldUserCount, m_db.getWikiNames().length );
  }
  
  public void testSetUserProfileWithDenial() throws Exception
  {
      setUpWithWorkflow();
      
      // First, count the number of users in the db now.
      int oldUserCount = m_db.getWikiNames().length;
      
      // Create a new user with random name
      WikiSession session = m_engine.guestSession();
      String loginName = "TestUser" + String.valueOf( System.currentTimeMillis() );
      UserProfile profile = m_db.newProfile();
      profile.setEmail( "testuser@testville.com" );
      profile.setLoginName( loginName );
      profile.setFullname( "FullName"+loginName );
      profile.setPassword( "password");
      
      // Because user profile saves require approvals, we will catch a Redirect
      try 
      {
          m_mgr.setUserProfile( session, profile );
          fail( "We should have caught a DecisionRequiredException caused by approval!" );
      }
      catch ( DecisionRequiredException e )
      {
      }
      
      // The user should NOT be saved yet
      assertEquals( oldUserCount, m_db.getWikiNames().length );
      
      // Now, look in Admin's queue, and verify there's a pending Decision there
      DecisionQueue dq = m_engine.getWorkflowManager().getDecisionQueue();
      Collection<Decision> decisions = dq.getActorDecisions( m_engine.adminSession() );
      assertEquals( 1, decisions.size() );

      // Verify that the Decision has all the facts and attributes we need
      Decision d = decisions.iterator().next();
      List<Fact> facts = d.getFacts();
      assertEquals( new Fact( UserManager.PREFS_FULL_NAME, profile.getFullname() ), facts.get(0) );
      assertEquals( new Fact( UserManager.PREFS_LOGIN_NAME, profile.getLoginName() ), facts.get(1) );
      assertEquals( new Fact( UserManager.FACT_SUBMITTER, session.getUserPrincipal().getName() ), facts.get(2) );
      assertEquals( new Fact( UserManager.PREFS_EMAIL, profile.getEmail() ), facts.get(3) );
      assertEquals( profile, d.getWorkflow().getAttribute( UserManager.SAVED_PROFILE ) );
      
      // Approve the profile
      d.decide( Outcome.DECISION_DENY );
      
      // Make sure the profile did NOT save
      assertEquals( oldUserCount, m_db.getWikiNames().length );
  }
  
  public void testSetCollidingUserProfile() throws Exception
  {
      // First, count the number of users in the db now.
      int oldUserCount = m_db.getWikiNames().length;
      
      // Create a new user with random name
      WikiSession session = m_engine.guestSession();
      String loginName = "TestUser" + String.valueOf( System.currentTimeMillis() );
      UserProfile profile = m_db.newProfile();
      profile.setEmail( "testuser@testville.com" );
      profile.setLoginName( loginName );
      profile.setFullname( "FullName"+loginName );
      profile.setPassword( "password");
      
      // Set the login name to collide with Janne's: should prohibit saving
      profile.setLoginName( "janne" );
      try 
      {
          m_mgr.setUserProfile( session, profile );
          fail( "UserManager allowed saving of user with login name 'janne', but it shouldn't have." ); 
      }
      catch ( DuplicateUserException e )
      {
          // Good! That's what we expected; reset for next test
          profile.setLoginName( loginName );
      }
            
      // Set the login name to collide with Janne's: should prohibit saving
      profile.setFullname( "Janne Jalkanen" );
      try 
      {
          m_mgr.setUserProfile( session, profile );
          fail( "UserManager allowed saving of user with login name 'janne', but it shouldn't have." ); 
      }
      catch ( DuplicateUserException e )
      {
          // Good! That's what we expected
      }
      
      // There shouldn't have been any users added
      assertEquals( oldUserCount, m_db.getWikiNames().length );
  }
  
}
