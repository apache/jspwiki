package com.ecyrd.jspwiki.auth.user;
import java.security.Principal;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.Users;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.WikiSecurityException;



/**
 * @author Andrew Jaquith
 */
public class XMLUserDatabaseTest extends TestCase
{

  private XMLUserDatabase m_db;

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception
  {
      super.setUp();
      Properties props = new Properties();
      props.load( TestEngine.findTestProperties() );
      props.put(XMLUserDatabase.PROP_USERDATABASE, "tests/etc/userdatabase.xml");
      WikiEngine engine  = new TestEngine(props);
      m_db = new XMLUserDatabase();
      m_db.initialize(engine, props);
  }

  public void testDeleteByLoginName() throws WikiSecurityException
  {
      // First, count the number of users in the db now.
      int oldUserCount = m_db.getWikiNames().length;

      // Create a new user with random name
      String loginName = "TestUser" + String.valueOf( System.currentTimeMillis() );
      UserProfile profile = new DefaultUserProfile();
      profile.setEmail("testuser@testville.com");
      profile.setLoginName( loginName );
      profile.setFullname( "FullName"+loginName );
      profile.setPassword("password");
      m_db.save(profile);

      // Make sure the profile saved successfully
      profile = m_db.findByLoginName( loginName );
      assertEquals( loginName, profile.getLoginName() );
      assertEquals( oldUserCount+1, m_db.getWikiNames().length );

      // Now delete the profile; should be back to old count
      m_db.deleteByLoginName( loginName );
      assertEquals( oldUserCount, m_db.getWikiNames().length );
  }

  public void testFindByEmail()
  {
    try
    {
        UserProfile profile = m_db.findByEmail("janne@ecyrd.com");
        assertEquals("janne",           profile.getLoginName());
        assertEquals("Janne Jalkanen",  profile.getFullname());
        assertEquals("JanneJalkanen",   profile.getWikiName());
        assertEquals("{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword());
        assertEquals("janne@ecyrd.com", profile.getEmail());
    }
    catch (NoSuchPrincipalException e)
    {
        assertTrue(false);
    }
    try
    {
        m_db.findByEmail("foo@bar.org");
        // We should never get here
        assertTrue(false);
    }
    catch (NoSuchPrincipalException e)
    {
        assertTrue(true);
    }
  }

  public void testFindByWikiName()
  {
      try
      {
          UserProfile profile = m_db.findByWikiName("JanneJalkanen");
          assertEquals("janne",           profile.getLoginName());
          assertEquals("Janne Jalkanen",  profile.getFullname());
          assertEquals("JanneJalkanen",   profile.getWikiName());
          assertEquals("{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword());
          assertEquals("janne@ecyrd.com", profile.getEmail());
      }
      catch (NoSuchPrincipalException e)
      {
          assertTrue(false);
      }
      try
      {
          m_db.findByEmail("foo");
          // We should never get here
          assertTrue(false);
      }
      catch (NoSuchPrincipalException e)
      {
          assertTrue(true);
      }
    }

  public void testFindByLoginName()
  {
      try
      {
          UserProfile profile = m_db.findByLoginName("janne");
          assertEquals("janne",           profile.getLoginName());
          assertEquals("Janne Jalkanen",  profile.getFullname());
          assertEquals("JanneJalkanen",   profile.getWikiName());
          assertEquals("{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword());
          assertEquals("janne@ecyrd.com", profile.getEmail());
      }
      catch (NoSuchPrincipalException e)
      {
          assertTrue(false);
      }
      try
      {
          m_db.findByEmail("FooBar");
          // We should never get here
          assertTrue(false);
      }
      catch (NoSuchPrincipalException e)
      {
          assertTrue(true);
      }
    }

  public void testGetWikiNames() throws WikiSecurityException
  {
      // There are 8 test users in the database
      Principal[] p = m_db.getWikiNames();
      assertEquals( 8, p.length );
      assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "JanneJalkanen", WikiPrincipal.WIKI_NAME ) ) );
      assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "", WikiPrincipal.WIKI_NAME ) ) );
      assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "Administrator", WikiPrincipal.WIKI_NAME ) ) );
      assertTrue( ArrayUtils.contains( p, new WikiPrincipal( Users.ALICE, WikiPrincipal.WIKI_NAME ) ) );
      assertTrue( ArrayUtils.contains( p, new WikiPrincipal( Users.BOB, WikiPrincipal.WIKI_NAME ) ) );
      assertTrue( ArrayUtils.contains( p, new WikiPrincipal( Users.CHARLIE, WikiPrincipal.WIKI_NAME ) ) );
      assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "FredFlintstone", WikiPrincipal.WIKI_NAME ) ) );
      assertTrue( ArrayUtils.contains( p, new WikiPrincipal( Users.BIFF, WikiPrincipal.WIKI_NAME ) ) );
  }

  public void testRename() throws Exception
  {
      // Try renaming a non-existent profile; it should fail
      try
      {
          m_db.rename( "nonexistentname", "renameduser" );
          fail( "Should not have allowed rename..." );
      }
      catch ( NoSuchPrincipalException e )
      {
          // Cool; that's what we expect
      }

      // Create new user & verify it saved ok
      UserProfile profile = new DefaultUserProfile();
      profile.setEmail( "renamed@example.com" );
      profile.setFullname( "Renamed User" );
      profile.setLoginName( "olduser" );
      profile.setPassword( "password" );
      m_db.save( profile );
      profile = m_db.findByLoginName( "olduser" );
      assertNotNull( profile );

      // Try renaming to a login name that's already taken; it should fail
      try
      {
          m_db.rename( "olduser", "janne" );
          fail( "Should not have allowed rename..." );
      }
      catch ( DuplicateUserException e )
      {
          // Cool; that's what we expect
      }

      // Now, rename it to an unused name
      m_db.rename( "olduser", "renameduser" );

      // The old user shouldn't be found
      try
      {
          profile = m_db.findByLoginName( "olduser" );
          fail( "Old user was found, but it shouldn't have been." );
      }
      catch ( NoSuchPrincipalException e )
      {
          // Cool, it's gone
      }

      // The new profile should be found, and its properties should match the old ones
      profile = m_db.findByLoginName( "renameduser" );
      assertEquals( "renamed@example.com", profile.getEmail() );
      assertEquals( "Renamed User", profile.getFullname() );
      assertEquals( "renameduser", profile.getLoginName() );
      assertEquals( "{SHA}5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8", profile.getPassword() );

      // Delete the user
      m_db.deleteByLoginName( "renameduser" );
  }

  public void testSave()
  {
      try
      {
          UserProfile profile = new DefaultUserProfile();
          profile.setEmail("user@example.com");
          profile.setLoginName("user");
          profile.setPassword("password");
          m_db.save(profile);
          profile = m_db.findByEmail("user@example.com");
          assertEquals("user@example.com", profile.getEmail());
          assertEquals("{SHA}5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8", profile.getPassword());
      }
      catch (NoSuchPrincipalException e)
      {
          assertTrue(false);
      }
      catch (WikiSecurityException e)
      {
          assertTrue(false);
      }
  }

  public void testValidatePassword()
  {
      assertFalse(m_db.validatePassword("janne", "test"));
      assertTrue(m_db.validatePassword("janne", "myP@5sw0rd"));
      assertTrue(m_db.validatePassword("user", "password"));
  }

}
