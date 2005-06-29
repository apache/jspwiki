package com.ecyrd.jspwiki.auth.user;
import java.util.Properties;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiSecurityException;



/**
 * @author Andrew Jaquith
 */
public class XMLUserDatabaseTest extends TestCase {

  private XMLUserDatabase db;
  
  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    Properties props = new Properties();
    props.put(XMLUserDatabase.PROP_USERDATABASE, "tests/etc/userdatabase.xml");
    db = new XMLUserDatabase();
    db.initialize(null, props);
  }
  
  public void testFindByEmail() {
    try {
        UserProfile profile = db.findByEmail("janne@ecyrd.com");
        assertEquals("janne",           profile.getLoginName());
        assertEquals("Janne Jalkanen",  profile.getFullname());
        assertEquals("JanneJalkanen",   profile.getWikiName());
        assertEquals("{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword());
        assertEquals("janne@ecyrd.com", profile.getEmail());
    }
    catch (NoSuchPrincipalException e) {
        assertTrue(false);
    }
    try {
        UserProfile profile = db.findByEmail("foo@bar.org");
        // We should never get here
        assertTrue(false);
    }
    catch (NoSuchPrincipalException e) {
        assertTrue(true);
    }
  }
  
  public void testFindByWikiName() {
      try {
          UserProfile profile = db.findByWikiName("JanneJalkanen");
          assertEquals("janne",           profile.getLoginName());
          assertEquals("Janne Jalkanen",  profile.getFullname());
          assertEquals("JanneJalkanen",   profile.getWikiName());
          assertEquals("{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword());
          assertEquals("janne@ecyrd.com", profile.getEmail());
      }
      catch (NoSuchPrincipalException e) {
          assertTrue(false);
      }
      try {
          UserProfile profile = db.findByEmail("foo");
          // We should never get here
          assertTrue(false);
      }
      catch (NoSuchPrincipalException e) {
          assertTrue(true);
      }
    }

  public void testFindByLoginName() {
      try {
          UserProfile profile = db.findByLoginName("janne");
          assertEquals("janne",           profile.getLoginName());
          assertEquals("Janne Jalkanen",  profile.getFullname());
          assertEquals("JanneJalkanen",   profile.getWikiName());
          assertEquals("{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword());
          assertEquals("janne@ecyrd.com", profile.getEmail());
      }
      catch (NoSuchPrincipalException e) {
          assertTrue(false);
      }
      try {
          UserProfile profile = db.findByEmail("FooBar");
          // We should never get here
          assertTrue(false);
      }
      catch (NoSuchPrincipalException e) {
          assertTrue(true);
      }
    }

  public void testSave() {
      try {
          UserProfile profile = new DefaultUserProfile();
          profile.setEmail("user@example.com");
          profile.setLoginName("user");
          profile.setPassword("password");
          db.save(profile);
          profile = db.findByEmail("user@example.com");
          assertEquals("user@example.com", profile.getEmail());
          assertEquals("{SHA}5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8", profile.getPassword());
          db.commit();
      }
      catch (NoSuchPrincipalException e) {
          assertTrue(false);
      }
      catch (WikiSecurityException e) {
          assertTrue(false);
      }
  }
  
  public void testValidatePassword() {
      assertFalse(db.validatePassword("janne", "test"));
      assertTrue(db.validatePassword("janne", "myP@5sw0rd"));
      assertTrue(db.validatePassword("user", "password"));
  }

}
