package com.ecyrd.jspwiki.auth;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.user.*;
import com.ecyrd.jspwiki.filters.RedirectException;
import com.ecyrd.jspwiki.workflow.Decision;
import com.ecyrd.jspwiki.workflow.DecisionQueue;
import com.ecyrd.jspwiki.workflow.Fact;
import com.ecyrd.jspwiki.workflow.Outcome;



/**
 * @author Andrew Jaquith
 */
public class UserManagerTest extends TestCase
{

  private TestEngine m_engine;
  private UserManager m_mgr;
  private UserDatabase m_db;
  
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
      m_engine  = new TestEngine( props );
      m_mgr = m_engine.getUserManager();
      m_db = m_mgr.getUserDatabase();
  }
  
  public void testSetUserProfile() throws Exception
  {
      // First, count the number of users in the db now.
      int oldUserCount = m_db.getWikiNames().length;
      
      // Create a new user with random name
      WikiSession session = m_engine.guestSession();
      String loginName = "TestUser" + String.valueOf( System.currentTimeMillis() );
      UserProfile profile = new DefaultUserProfile();
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
      m_db.commit();
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
      UserProfile profile = new DefaultUserProfile();
      profile.setEmail( "testuser@testville.com" );
      profile.setLoginName( loginName );
      profile.setFullname( "FullName"+loginName );
      profile.setPassword( "password");
      
      // Because user profile saves require approvals, we will catch a Redirect
      try 
      {
          m_mgr.setUserProfile( session, profile );
          fail( "We should have caught a RedirectException caused by approval!" );
      }
      catch ( RedirectException e )
      {
      }
      
      // The user should NOT be saved yet
      assertEquals( oldUserCount, m_db.getWikiNames().length );
      
      // Now, look in Admin's queue, and verify there's a pending Decision there
      DecisionQueue dq = m_engine.getWorkflowManager().getDecisionQueue();
      Collection decisions = dq.getActorDecisions( m_engine.adminSession() );
      assertEquals( 1, decisions.size() );

      // Verify that the Decision has all the facts and attributes we need
      Decision d = (Decision)decisions.iterator().next();
      List facts = d.getFacts();
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
      m_db.commit();
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
      UserProfile profile = new DefaultUserProfile();
      profile.setEmail( "testuser@testville.com" );
      profile.setLoginName( loginName );
      profile.setFullname( "FullName"+loginName );
      profile.setPassword( "password");
      
      // Because user profile saves require approvals, we will catch a Redirect
      try 
      {
          m_mgr.setUserProfile( session, profile );
          fail( "We should have caught a RedirectException caused by approval!" );
      }
      catch ( RedirectException e )
      {
      }
      
      // The user should NOT be saved yet
      assertEquals( oldUserCount, m_db.getWikiNames().length );
      
      // Now, look in Admin's queue, and verify there's a pending Decision there
      DecisionQueue dq = m_engine.getWorkflowManager().getDecisionQueue();
      Collection decisions = dq.getActorDecisions( m_engine.adminSession() );
      assertEquals( 1, decisions.size() );

      // Verify that the Decision has all the facts and attributes we need
      Decision d = (Decision)decisions.iterator().next();
      List facts = d.getFacts();
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
      UserProfile profile = new DefaultUserProfile();
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
