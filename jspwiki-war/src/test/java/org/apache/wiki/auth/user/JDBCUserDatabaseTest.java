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
package org.apache.wiki.auth.user;
import java.io.File;
import java.io.Serializable;
import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.sql.DataSource;

import org.apache.wiki.HsqlDbUtils;
import org.apache.wiki.TestJDBCDataSource;
import org.apache.wiki.TestJNDIContext;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.util.CryptoUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class JDBCUserDatabaseTest
{
    private HsqlDbUtils      m_hu   = new HsqlDbUtils();

    private JDBCUserDatabase m_db   = null;

    private static final String TEST_ATTRIBUTES = "rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAACdAAKYXR0cmlidXRlMXQAEXNvbWUgcmFuZG9tIHZhbHVldAAKYXR0cmlidXRlMnQADWFub3RoZXIgdmFsdWV4";

    private static final String INSERT_JANNE = "INSERT INTO users (" +
          JDBCUserDatabase.DEFAULT_DB_UID + "," +
          JDBCUserDatabase.DEFAULT_DB_EMAIL + "," +
          JDBCUserDatabase.DEFAULT_DB_FULL_NAME + "," +
          JDBCUserDatabase.DEFAULT_DB_LOGIN_NAME + "," +
          JDBCUserDatabase.DEFAULT_DB_PASSWORD + "," +
          JDBCUserDatabase.DEFAULT_DB_WIKI_NAME + "," +
          JDBCUserDatabase.DEFAULT_DB_CREATED + "," +
          JDBCUserDatabase.DEFAULT_DB_ATTRIBUTES + ") VALUES (" +
          "'-7739839977499061014'," + "'janne@ecyrd.com'," + "'Janne Jalkanen'," + "'janne'," +
          "'{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee'," +
          "'JanneJalkanen'," +
          "'" + new Timestamp( new Timestamp( System.currentTimeMillis() ).getTime() ).toString() + "'," +
          "'" + TEST_ATTRIBUTES +"'" + ");";

    private static final String INSERT_USER = "INSERT INTO users (" +
        JDBCUserDatabase.DEFAULT_DB_UID + "," +
        JDBCUserDatabase.DEFAULT_DB_EMAIL + "," +
        JDBCUserDatabase.DEFAULT_DB_LOGIN_NAME + "," +
        JDBCUserDatabase.DEFAULT_DB_PASSWORD + "," +
        JDBCUserDatabase.DEFAULT_DB_CREATED + ") VALUES (" +
        "'-8629747547991531672'," + "'jspwiki.tests@mailinator.com'," + "'user'," +
        "'{SHA}5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8'," +
        "'" + new Timestamp( new Timestamp( System.currentTimeMillis() ).getTime() ).toString() + "'" + ");";

    /**
     *
     */
    @Before
    public void setUp() throws Exception
    {
        m_hu.setUp();
        // Set up the mock JNDI initial context
        TestJNDIContext.initialize();
        Context initCtx = new InitialContext();
        try
        {
            initCtx.bind( "java:comp/env", new TestJNDIContext() );
        }
        catch( NameAlreadyBoundException e )
        {
            // ignore
        }
        Context ctx = (Context) initCtx.lookup( "java:comp/env" );
        DataSource ds = new TestJDBCDataSource( new File( "target/test-classes/jspwiki-custom.properties" ) );
        ctx.bind( JDBCUserDatabase.DEFAULT_DB_JNDI_NAME, ds );

        // Get the JDBC connection and init tables
        try
        {
            Connection conn = ds.getConnection();
            Statement stmt = conn.createStatement();
            String sql;

            sql = "DELETE FROM " + JDBCUserDatabase.DEFAULT_DB_TABLE + ";";
            stmt.executeUpdate( sql );

            // Create a new test user 'janne'
            stmt.executeUpdate( INSERT_JANNE );

            // Create a new test user 'user'
            stmt.executeUpdate( INSERT_USER );
            stmt.close();

            conn.close();

            // Initialize the user database
            m_db = new JDBCUserDatabase();
            m_db.initialize( null, new Properties() );
        }
        catch( SQLException e )
        {
            Assert.fail("Looks like your database could not be connected to - "+
                  "please make sure that you have started your database, exception: " + e.getMessage());
        }
    }

    @After
    public void tearDown() throws Exception
    {
        m_hu.tearDown();
    }

    @Test
    public void testDeleteByLoginName() throws WikiSecurityException
    {
        // First, count the number of users in the db now.
        int oldUserCount = m_db.getWikiNames().length;

        // Create a new user with random name
        String loginName = "TestUser" + String.valueOf( System.currentTimeMillis() );
        UserProfile profile = m_db.newProfile();
        profile.setEmail("jspwiki.tests@mailinator.com");
        profile.setLoginName( loginName );
        profile.setFullname( "FullName"+loginName );
        profile.setPassword("password");
        m_db.save(profile);

        // Make sure the profile saved successfully
        profile = m_db.findByLoginName( loginName );
        Assert.assertEquals( loginName, profile.getLoginName() );
        Assert.assertEquals( oldUserCount+1, m_db.getWikiNames().length );

        // Now delete the profile; should be back to old count
        m_db.deleteByLoginName( loginName );
        Assert.assertEquals( oldUserCount, m_db.getWikiNames().length );
    }

    @Test
    public void testAttributes() throws Exception
    {
        UserProfile profile = m_db.findByEmail( "janne@ecyrd.com" );

        Map<String,Serializable> attributes = profile.getAttributes();
        Assert.assertEquals( 2, attributes.size() );
        Assert.assertTrue( attributes.containsKey( "attribute1" ) );
        Assert.assertTrue( attributes.containsKey( "attribute2" ) );
        Assert.assertEquals( "some random value", attributes.get( "attribute1" ) );
        Assert.assertEquals( "another value", attributes.get( "attribute2" ) );

        // Change attribute 1, and add another one
        attributes.put( "attribute1", "replacement value" );
        attributes.put( "attribute the third", "some value" );
        m_db.save( profile );

        // Retrieve the profile again and make sure our values got saved
        profile = m_db.findByEmail( "janne@ecyrd.com" );
        attributes = profile.getAttributes();
        Assert.assertEquals( 3, attributes.size() );
        Assert.assertTrue( attributes.containsKey( "attribute1" ) );
        Assert.assertTrue( attributes.containsKey( "attribute2" ) );
        Assert.assertTrue( attributes.containsKey( "attribute the third" ) );
        Assert.assertEquals( "replacement value", attributes.get( "attribute1" ) );
        Assert.assertEquals( "another value", attributes.get( "attribute2" ) );
        Assert.assertEquals( "some value", attributes.get( "attribute the third" ) );

        // Restore the original attributes and re-save
        attributes.put( "attribute1", "some random value" );
        attributes.remove( "attribute the third" );
        m_db.save( profile );
    }

    @Test
    public void testFindByEmail()
    {
        try
        {
            UserProfile profile = m_db.findByEmail( "janne@ecyrd.com" );
            Assert.assertEquals( "-7739839977499061014", profile.getUid() );
            Assert.assertEquals( "janne", profile.getLoginName() );
            Assert.assertEquals( "Janne Jalkanen", profile.getFullname() );
            Assert.assertEquals( "JanneJalkanen", profile.getWikiName() );
            Assert.assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            Assert.assertEquals( "janne@ecyrd.com", profile.getEmail() );
            Assert.assertNotNull( profile.getCreated() );
            Assert.assertNull( profile.getLastModified() );
        }
        catch( NoSuchPrincipalException e )
        {
            Assert.assertTrue( false );
        }
        try
        {
            m_db.findByEmail( "foo@bar.org" );
            // We should never get here
            Assert.assertTrue( false );
        }
        catch( NoSuchPrincipalException e )
        {
            Assert.assertTrue( true );
        }
    }

    @Test
    public void testFindByFullName()
    {
        try
        {
            UserProfile profile = m_db.findByFullName( "Janne Jalkanen" );
            Assert.assertEquals( "-7739839977499061014", profile.getUid() );
            Assert.assertEquals( "janne", profile.getLoginName() );
            Assert.assertEquals( "Janne Jalkanen", profile.getFullname() );
            Assert.assertEquals( "JanneJalkanen", profile.getWikiName() );
            Assert.assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            Assert.assertEquals( "janne@ecyrd.com", profile.getEmail() );
            Assert.assertNotNull( profile.getCreated() );
            Assert.assertNull( profile.getLastModified() );
        }
        catch( NoSuchPrincipalException e )
        {
            Assert.assertTrue( false );
        }
        try
        {
            m_db.findByEmail( "foo@bar.org" );
            // We should never get here
            Assert.assertTrue( false );
        }
        catch( NoSuchPrincipalException e )
        {
            Assert.assertTrue( true );
        }
    }

    @Test
    public void testFindByUid()
    {
        try
        {
            UserProfile profile = m_db.findByUid( "-7739839977499061014" );
            Assert.assertEquals( "-7739839977499061014", profile.getUid() );
            Assert.assertEquals( "janne", profile.getLoginName() );
            Assert.assertEquals( "Janne Jalkanen", profile.getFullname() );
            Assert.assertEquals( "JanneJalkanen", profile.getWikiName() );
            Assert.assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            Assert.assertEquals( "janne@ecyrd.com", profile.getEmail() );
            Assert.assertNotNull( profile.getCreated() );
            Assert.assertNull( profile.getLastModified() );
        }
        catch( NoSuchPrincipalException e )
        {
            Assert.assertTrue( false );
        }
        try
        {
            m_db.findByEmail( "foo@bar.org" );
            // We should never get here
            Assert.assertTrue( false );
        }
        catch( NoSuchPrincipalException e )
        {
            Assert.assertTrue( true );
        }
    }

    @Test
    public void testFindByWikiName()
    {
        try
        {
            UserProfile profile = m_db.findByWikiName( "JanneJalkanen" );
            Assert.assertEquals( "-7739839977499061014", profile.getUid() );
            Assert.assertEquals( "janne", profile.getLoginName() );
            Assert.assertEquals( "Janne Jalkanen", profile.getFullname() );
            Assert.assertEquals( "JanneJalkanen", profile.getWikiName() );
            Assert.assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            Assert.assertEquals( "janne@ecyrd.com", profile.getEmail() );
            Assert.assertNotNull( profile.getCreated() );
            Assert.assertNull( profile.getLastModified() );
        }
        catch( NoSuchPrincipalException e )
        {
            Assert.assertTrue( false );
        }
        try
        {
            m_db.findByEmail( "foo" );
            // We should never get here
            Assert.assertTrue( false );
        }
        catch( NoSuchPrincipalException e )
        {
            Assert.assertTrue( true );
        }
    }

    @Test
    public void testFindByLoginName()
    {
        try
        {
            UserProfile profile = m_db.findByLoginName( "janne" );
            Assert.assertEquals( "-7739839977499061014", profile.getUid() );
            Assert.assertEquals( "janne", profile.getLoginName() );
            Assert.assertEquals( "Janne Jalkanen", profile.getFullname() );
            Assert.assertEquals( "JanneJalkanen", profile.getWikiName() );
            Assert.assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            Assert.assertEquals( "janne@ecyrd.com", profile.getEmail() );
            Assert.assertNotNull( profile.getCreated() );
            Assert.assertNull( profile.getLastModified() );
        }
        catch( NoSuchPrincipalException e )
        {
            Assert.assertTrue( false );
        }
        try
        {
            m_db.findByEmail( "FooBar" );
            // We should never get here
            Assert.assertTrue( false );
        }
        catch( NoSuchPrincipalException e )
        {
            Assert.assertTrue( true );
        }
    }

    @Test
    public void testGetWikiName() throws WikiSecurityException
    {
        Principal[] principals = m_db.getWikiNames();
        Assert.assertEquals( 1, principals.length );
    }

    @Test
    public void testRename() throws Exception
    {
        // Try renaming a non-existent profile; it should Assert.fail
        try
        {
            m_db.rename( "nonexistentname", "renameduser" );
            Assert.fail( "Should not have allowed rename..." );
        }
        catch ( NoSuchPrincipalException e )
        {
            // Cool; that's what we expect
        }

        // Create new user & verify it saved ok
        UserProfile profile = m_db.newProfile();
        profile.setEmail( "renamed@mailinator.com" );
        profile.setFullname( "Renamed User" );
        profile.setLoginName( "olduser" );
        profile.setPassword( "password" );
        m_db.save( profile );
        profile = m_db.findByLoginName( "olduser" );
        Assert.assertNotNull( profile );

        // Try renaming to a login name that's already taken; it should Assert.fail
        try
        {
            m_db.rename( "olduser", "janne" );
            Assert.fail( "Should not have allowed rename..." );
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
            Assert.fail( "Old user was found, but it shouldn't have been." );
        }
        catch ( NoSuchPrincipalException e )
        {
            // Cool, it's gone
        }

        // The new profile should be found, and its properties should match the old ones
        profile = m_db.findByLoginName( "renameduser" );
        Assert.assertEquals( "renamed@mailinator.com", profile.getEmail() );
        Assert.assertEquals( "Renamed User", profile.getFullname() );
        Assert.assertEquals( "renameduser", profile.getLoginName() );
        Assert.assertTrue( CryptoUtil.verifySaltedPassword( "password".getBytes(), profile.getPassword() ) );

        // Delete the user
        m_db.deleteByLoginName( "renameduser" );
    }

    @Test
    public void testSave() throws Exception
    {
        try
        {
            // Overwrite existing user
            UserProfile profile = m_db.newProfile();
            profile.setEmail( "jspwiki.tests@mailinator.com" );
            profile.setFullname( "Test User" );
            profile.setLoginName( "user" );
            profile.setPassword( "password" );
            m_db.save( profile );
            profile = m_db.findByEmail( "jspwiki.tests@mailinator.com" );
            Assert.assertEquals( "jspwiki.tests@mailinator.com", profile.getEmail() );
            Assert.assertEquals( "Test User", profile.getFullname() );
            Assert.assertEquals( "user", profile.getLoginName() );
            Assert.assertTrue( CryptoUtil.verifySaltedPassword( "password".getBytes(), profile.getPassword() ) );
            Assert.assertEquals( "TestUser", profile.getWikiName() );
            Assert.assertNotNull( profile.getCreated() );
            Assert.assertNotNull( profile.getLastModified() );
            Assert.assertNotSame( profile.getCreated(), profile.getLastModified() );

            // Create new user
            profile = m_db.newProfile();
            profile.setEmail( "jspwiki.tests2@mailinator.com" );
            profile.setFullname( "Test User 2" );
            profile.setLoginName( "user2" );
            profile.setPassword( "password" );
            m_db.save( profile );
            profile = m_db.findByEmail( "jspwiki.tests2@mailinator.com" );
            Assert.assertEquals( "jspwiki.tests2@mailinator.com", profile.getEmail() );
            Assert.assertEquals( "Test User 2", profile.getFullname() );
            Assert.assertEquals( "user2", profile.getLoginName() );
            Assert.assertTrue( CryptoUtil.verifySaltedPassword( "password".getBytes(), profile.getPassword() ) );
            Assert.assertEquals( "TestUser2", profile.getWikiName() );
            Assert.assertNotNull( profile.getCreated() );
            Assert.assertNotNull( profile.getLastModified() );
            Assert.assertEquals( profile.getCreated(), profile.getLastModified() );

            // Make sure we can find it by uid
            String uid = profile.getUid();
            Assert.assertNotNull( m_db.findByUid( uid ) );

        }
        catch( NoSuchPrincipalException e )
        {
            Assert.assertTrue( false );
        }
        catch( WikiSecurityException e )
        {
            Assert.assertTrue( false );
        }
    }

    @Test
    public void testValidatePassword()
    {
        Assert.assertFalse( m_db.validatePassword( "janne", "test" ) );
        Assert.assertTrue( m_db.validatePassword( "janne", "myP@5sw0rd" ) );
        Assert.assertTrue( m_db.validatePassword( "user", "password" ) );
    }

}
