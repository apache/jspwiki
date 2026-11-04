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

import org.apache.wiki.HsqlDbUtils;
import org.apache.wiki.TestJDBCDataSource;
import org.apache.wiki.TestJNDIContext;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.util.CryptoUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.sql.DataSource;
import java.io.File;
import java.io.Serializable;
import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Date;
import java.util.Properties;
import org.apache.wiki.TestEngine;
import org.apache.wiki.auth.AbstractPasswordReuseTest;
import static org.apache.wiki.auth.UserManager.PROP_DATABASE;
import org.apache.wiki.auth.authorize.JDBCGroupDatabase;

/**
 *
 */
public class JDBCUserDatabaseTest extends AbstractPasswordReuseTest {
    private final HsqlDbUtils m_hu = new HsqlDbUtils();

    private JDBCUserDatabase m_db;

    private static final String TEST_ATTRIBUTES = "rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAACdAAKYXR0cmlidXRlMXQAEXNvbWUgcmFuZG9tIHZhbHVldAAKYXR0cmlidXRlMnQADWFub3RoZXIgdmFsdWV4";

    public static final String INSERT_JANNE = "INSERT INTO users (" +
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
            "'" + new Timestamp( new Timestamp( System.currentTimeMillis() ).getTime() ) + "'," +
            "'" + TEST_ATTRIBUTES + "'" + ");";

    public static final String INSERT_USER = "INSERT INTO users (" +
            JDBCUserDatabase.DEFAULT_DB_UID + "," +
            JDBCUserDatabase.DEFAULT_DB_EMAIL + "," +
            JDBCUserDatabase.DEFAULT_DB_LOGIN_NAME + "," +
            JDBCUserDatabase.DEFAULT_DB_PASSWORD + "," +
            JDBCUserDatabase.DEFAULT_DB_CREATED + ") VALUES (" +
            "'-8629747547991531672'," + "'jspwiki.tests@mailinator.com'," + "'user'," +
            "'{SHA}5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8'," +
            "'" + new Timestamp( new Timestamp( System.currentTimeMillis() ).getTime() ) + "'" + ");";

    /**
     *
     */
    @BeforeEach
    public void setUp() throws Exception {
        m_hu.setUp();
        // Set up the mock JNDI initial context
        TestJNDIContext.initialize();
        final Context initCtx = new InitialContext();
        try {
            initCtx.bind( "java:comp/env", new TestJNDIContext() );
        } catch( final NameAlreadyBoundException e ) {
            // ignore
        }
        final Context ctx = ( Context ) initCtx.lookup( "java:comp/env" );
        final DataSource ds = new TestJDBCDataSource( new File( "target/test-classes/jspwiki-custom.properties" ), m_hu.getDriverUrl() );
        ctx.bind( JDBCUserDatabase.DEFAULT_DB_JNDI_NAME, ds );

        // Get the JDBC connection and init tables
        try {
            final Connection conn = ds.getConnection();
            final Statement stmt = conn.createStatement();
            final String sql;

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
        } catch( final SQLException e ) {
            Assertions.fail( "Looks like your database could not be connected to - " +
                             "please make sure that you have started your database, exception: " + e.getMessage() );
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        m_hu.tearDown();
    }

    @Test
    public void testDeleteByLoginName() throws WikiSecurityException {
        // First, count the number of users in the db now.
        final int oldUserCount = m_db.getWikiNames().length;

        // Create a new user with random name
        final String loginName = "TestUser" + System.currentTimeMillis();
        UserProfile profile = m_db.newProfile();
        profile.setEmail( "jspwiki.tests@mailinator.com" );
        profile.setLoginName( loginName );
        profile.setFullname( "FullName" + loginName );
        profile.setPassword( "password" );
        m_db.save( profile );

        // Make sure the profile saved successfully
        profile = m_db.findByLoginName( loginName );
        Assertions.assertEquals( loginName, profile.getLoginName() );
        Assertions.assertEquals( oldUserCount + 1, m_db.getWikiNames().length );

        // Now delete the profile; should be back to old count
        m_db.deleteByLoginName( loginName );
        Assertions.assertEquals( oldUserCount, m_db.getWikiNames().length );
    }

    @Test
    public void testAttributes() throws Exception {
        UserProfile profile = m_db.findByEmail( "janne@ecyrd.com" );

        Map< String, Serializable > attributes = profile.getAttributes();
        Assertions.assertEquals( 2, attributes.size() );
        Assertions.assertTrue( attributes.containsKey( "attribute1" ) );
        Assertions.assertTrue( attributes.containsKey( "attribute2" ) );
        Assertions.assertEquals( "some random value", attributes.get( "attribute1" ) );
        Assertions.assertEquals( "another value", attributes.get( "attribute2" ) );

        // Change attribute 1, and add another one
        attributes.put( "attribute1", "replacement value" );
        attributes.put( "attribute the third", "some value" );
        m_db.save( profile );

        // Retrieve the profile again and make sure our values got saved
        profile = m_db.findByEmail( "janne@ecyrd.com" );
        attributes = profile.getAttributes();
        Assertions.assertEquals( 3, attributes.size() );
        Assertions.assertTrue( attributes.containsKey( "attribute1" ) );
        Assertions.assertTrue( attributes.containsKey( "attribute2" ) );
        Assertions.assertTrue( attributes.containsKey( "attribute the third" ) );
        Assertions.assertEquals( "replacement value", attributes.get( "attribute1" ) );
        Assertions.assertEquals( "another value", attributes.get( "attribute2" ) );
        Assertions.assertEquals( "some value", attributes.get( "attribute the third" ) );

        // Restore the original attributes and re-save
        attributes.put( "attribute1", "some random value" );
        attributes.remove( "attribute the third" );
        m_db.save( profile );
    }

    @Test
    public void testFindByEmail() {
        try {
            final UserProfile profile = m_db.findByEmail( "janne@ecyrd.com" );
            Assertions.assertEquals( "-7739839977499061014", profile.getUid() );
            Assertions.assertEquals( "janne", profile.getLoginName() );
            Assertions.assertEquals( "Janne Jalkanen", profile.getFullname() );
            Assertions.assertEquals( "JanneJalkanen", profile.getWikiName() );
            Assertions.assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            Assertions.assertEquals( "janne@ecyrd.com", profile.getEmail() );
            Assertions.assertNotNull( profile.getCreated() );
            Assertions.assertNull( profile.getLastModified() );
        } catch( final NoSuchPrincipalException e ) {
            Assertions.fail();
        }
        try {
            m_db.findByEmail( "foo@bar.org" );
            // We should never get here
            Assertions.fail();
        } catch( final NoSuchPrincipalException e ) {
            Assertions.assertTrue( true );
        }
    }

    @Test
    public void testFindByFullName() {
        try {
            final UserProfile profile = m_db.findByFullName( "Janne Jalkanen" );
            Assertions.assertEquals( "-7739839977499061014", profile.getUid() );
            Assertions.assertEquals( "janne", profile.getLoginName() );
            Assertions.assertEquals( "Janne Jalkanen", profile.getFullname() );
            Assertions.assertEquals( "JanneJalkanen", profile.getWikiName() );
            Assertions.assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            Assertions.assertEquals( "janne@ecyrd.com", profile.getEmail() );
            Assertions.assertNotNull( profile.getCreated() );
            Assertions.assertNull( profile.getLastModified() );
        } catch( final NoSuchPrincipalException e ) {
            Assertions.fail();
        }
        try {
            m_db.findByEmail( "foo@bar.org" );
            // We should never get here
            Assertions.fail();
        } catch( final NoSuchPrincipalException e ) {
            Assertions.assertTrue( true );
        }
    }

    @Test
    public void testFindByUid() {
        try {
            final UserProfile profile = m_db.findByUid( "-7739839977499061014" );
            Assertions.assertEquals( "-7739839977499061014", profile.getUid() );
            Assertions.assertEquals( "janne", profile.getLoginName() );
            Assertions.assertEquals( "Janne Jalkanen", profile.getFullname() );
            Assertions.assertEquals( "JanneJalkanen", profile.getWikiName() );
            Assertions.assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            Assertions.assertEquals( "janne@ecyrd.com", profile.getEmail() );
            Assertions.assertNotNull( profile.getCreated() );
            Assertions.assertNull( profile.getLastModified() );
        } catch( final NoSuchPrincipalException e ) {
            Assertions.fail();
        }
        try {
            m_db.findByEmail( "foo@bar.org" );
            // We should never get here
            Assertions.fail();
        } catch( final NoSuchPrincipalException e ) {
            Assertions.assertTrue( true );
        }
    }

    @Test
    public void testFindByWikiName() {
        try {
            final UserProfile profile = m_db.findByWikiName( "JanneJalkanen" );
            Assertions.assertEquals( "-7739839977499061014", profile.getUid() );
            Assertions.assertEquals( "janne", profile.getLoginName() );
            Assertions.assertEquals( "Janne Jalkanen", profile.getFullname() );
            Assertions.assertEquals( "JanneJalkanen", profile.getWikiName() );
            Assertions.assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            Assertions.assertEquals( "janne@ecyrd.com", profile.getEmail() );
            Assertions.assertNotNull( profile.getCreated() );
            Assertions.assertNull( profile.getLastModified() );
        } catch( final NoSuchPrincipalException e ) {
            Assertions.fail();
        }
        try {
            m_db.findByEmail( "foo" );
            // We should never get here
            Assertions.fail();
        } catch( final NoSuchPrincipalException e ) {
            Assertions.assertTrue( true );
        }
    }

    @Test
    public void testFindByLoginName() {
        try {
            final UserProfile profile = m_db.findByLoginName( "janne" );
            Assertions.assertEquals( "-7739839977499061014", profile.getUid() );
            Assertions.assertEquals( "janne", profile.getLoginName() );
            Assertions.assertEquals( "Janne Jalkanen", profile.getFullname() );
            Assertions.assertEquals( "JanneJalkanen", profile.getWikiName() );
            Assertions.assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            Assertions.assertEquals( "janne@ecyrd.com", profile.getEmail() );
            Assertions.assertNotNull( profile.getCreated() );
            Assertions.assertNull( profile.getLastModified() );
        } catch( final NoSuchPrincipalException e ) {
            Assertions.fail();
        }
        try {
            m_db.findByEmail( "FooBar" );
            // We should never get here
            Assertions.fail();
        } catch( final NoSuchPrincipalException e ) {
            Assertions.assertTrue( true );
        }
    }

    @Test
    public void testGetWikiName() throws WikiSecurityException {
        final Principal[] principals = m_db.getWikiNames();
        Assertions.assertEquals( 1, principals.length );
    }

    @Test
    public void testRename() throws Exception {
        // Try renaming a non-existent profile; it should Assertions.fail
        try {
            m_db.rename( "nonexistentname", "renameduser" );
            Assertions.fail( "Should not have allowed rename..." );
        } catch( final NoSuchPrincipalException e ) {
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
        Assertions.assertNotNull( profile );

        // Try renaming to a login name that's already taken; it should Assertions.fail
        try {
            m_db.rename( "olduser", "janne" );
            Assertions.fail( "Should not have allowed rename..." );
        } catch( final DuplicateUserException e ) {
            // Cool; that's what we expect
        }

        // Now, rename it to an unused name
        m_db.rename( "olduser", "renameduser" );

        // The old user shouldn't be found
        try {
            m_db.findByLoginName( "olduser" );
            Assertions.fail( "Old user was found, but it shouldn't have been." );
        } catch( final NoSuchPrincipalException e ) {
            // Cool, it's gone
        }

        // The new profile should be found, and its properties should match the old ones
        profile = m_db.findByLoginName( "renameduser" );
        Assertions.assertEquals( "renamed@mailinator.com", profile.getEmail() );
        Assertions.assertEquals( "Renamed User", profile.getFullname() );
        Assertions.assertEquals( "renameduser", profile.getLoginName() );
        Assertions.assertTrue( CryptoUtil.verifySaltedPassword( "password".getBytes(), profile.getPassword() ) );

        // Delete the user
        m_db.deleteByLoginName( "renameduser" );
    }

    @Test
    public void testSave() throws Exception {
        try {
            // Overwrite existing user
            UserProfile profile = m_db.newProfile();
            profile.setEmail( "jspwiki.tests@mailinator.com" );
            profile.setFullname( "Test User" );
            profile.setLoginName( "user" );
            profile.setPassword( "password" );
            m_db.save( profile );
            profile = m_db.findByEmail( "jspwiki.tests@mailinator.com" );
            Assertions.assertEquals( "jspwiki.tests@mailinator.com", profile.getEmail() );
            Assertions.assertEquals( "Test User", profile.getFullname() );
            Assertions.assertEquals( "user", profile.getLoginName() );
            Assertions.assertTrue( CryptoUtil.verifySaltedPassword( "password".getBytes(), profile.getPassword() ) );
            Assertions.assertEquals( "TestUser", profile.getWikiName() );
            Assertions.assertNotNull( profile.getCreated() );
            Assertions.assertNotNull( profile.getLastModified() );
            Assertions.assertNotSame( profile.getCreated(), profile.getLastModified() );

            // Create new user
            profile = m_db.newProfile();
            profile.setEmail( "jspwiki.tests2@mailinator.com" );
            profile.setFullname( "Test User 2" );
            profile.setLoginName( "user2" );
            profile.setPassword( "password" );
            m_db.save( profile );
            profile = m_db.findByEmail( "jspwiki.tests2@mailinator.com" );
            Assertions.assertEquals( "jspwiki.tests2@mailinator.com", profile.getEmail() );
            Assertions.assertEquals( "Test User 2", profile.getFullname() );
            Assertions.assertEquals( "user2", profile.getLoginName() );
            Assertions.assertTrue( CryptoUtil.verifySaltedPassword( "password".getBytes(), profile.getPassword() ) );
            Assertions.assertEquals( "TestUser2", profile.getWikiName() );
            Assertions.assertNotNull( profile.getCreated() );
            Assertions.assertNotNull( profile.getLastModified() );
            Assertions.assertEquals( profile.getCreated(), profile.getLastModified() );

            // Make sure we can find it by uid
            final String uid = profile.getUid();
            Assertions.assertNotNull( m_db.findByUid( uid ) );

        } catch( final WikiSecurityException e ) {
            Assertions.fail();
        }
    }

    private Properties sha512Properties() {
        final Properties props = new Properties();
        props.put( "jspwiki.credentials.hash.algorithm", "PBKDF2-SHA512" );
        props.put( "jspwiki.credentials.hash.iterations", "2000" );
        return props;
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testPasswordHashedWithSha512IsStored() throws Exception {
        m_db.initialize( null, sha512Properties() );

        final UserProfile profile = m_db.newProfile();
        profile.setLoginName( "sha512user" );
        profile.setFullname( "Sha512 User" );
        profile.setEmail( "sha512user@example.com" );
        profile.setPassword( "Secret#1pass" );
        m_db.save( profile );

        Assertions.assertTrue( m_db.findByLoginName( "sha512user" ).getPassword().startsWith( "{PBKDF2-SHA512}2000$" ) );
        Assertions.assertTrue( m_db.validatePassword( "sha512user", "Secret#1pass" ) );
        Assertions.assertFalse( m_db.validatePassword( "sha512user", "Secret#2pass" ) );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testLoginHashesAgainWithTheConfiguredSettings() throws Exception {
        m_db.initialize( null, sha512Properties() );
        Assertions.assertFalse( m_db.findByLoginName( "janne" ).getPassword().startsWith( "{PBKDF2" ) );

        Assertions.assertTrue( m_db.validatePassword( "janne", "myP@5sw0rd" ) );

        Assertions.assertTrue( m_db.findByLoginName( "janne" ).getPassword().startsWith( "{PBKDF2-SHA512}2000$" ) );
        Assertions.assertTrue( m_db.validatePassword( "janne", "myP@5sw0rd" ) );
    }

    @Test
    public void testLockExpiryIsSavedAndRead() throws Exception {
        final Date expiry = new Date( ( System.currentTimeMillis() + 3_600_000L ) / 1000 * 1000 );
        final UserProfile profile = m_db.findByLoginName( "janne" );
        profile.setLockExpiry( expiry );
        m_db.save( profile );

        final UserProfile reloaded = m_db.findByLoginName( "janne" );
        Assertions.assertTrue( reloaded.isLocked() );
        Assertions.assertEquals( expiry, reloaded.getLockExpiry() );

        reloaded.setLockExpiry( null );
        m_db.save( reloaded );
        Assertions.assertFalse( m_db.findByLoginName( "janne" ).isLocked() );
    }

    private void withPasswordChangedColumn() throws Exception {
        final Properties props = new Properties();
        props.put( "jspwiki.userdatabase.passwordChanged", "password_changed" );
        m_db.initialize( null, props );
    }

    private void saveUser( final String loginName, final String password ) throws Exception {
        final UserProfile profile = m_db.newProfile();
        profile.setLoginName( loginName );
        profile.setFullname( "Full " + loginName );
        profile.setEmail( loginName + "@example.com" );
        profile.setPassword( password );
        m_db.save( profile );
    }

    private Date daysAgo( final int days ) {
        return new Date( ( System.currentTimeMillis() - 86_400_000L * days ) / 1000 * 1000 );
    }

    @Test
    public void testPasswordChangeDateIsNotKeptWithoutTheColumn() throws Exception {
        saveUser( "nocolumn", "Secret#1pass" );

        Assertions.assertNull( m_db.findByLoginName( "nocolumn" ).getPasswordChanged() );
    }

    @Test
    public void testNewProfileGetsThePasswordChangeDate() throws Exception {
        withPasswordChangedColumn();
        final long before = System.currentTimeMillis() - 2000;

        saveUser( "datenew", "Secret#1pass" );

        final Date changed = m_db.findByLoginName( "datenew" ).getPasswordChanged();
        Assertions.assertNotNull( changed );
        Assertions.assertTrue( changed.getTime() >= before && changed.getTime() <= System.currentTimeMillis() + 2000 );
    }

    @Test
    public void testChangingThePasswordUpdatesTheDate() throws Exception {
        withPasswordChangedColumn();
        saveUser( "datechange", "Secret#1pass" );
        final UserProfile profile = m_db.findByLoginName( "datechange" );
        final Date old = daysAgo( 40 );
        profile.setPasswordChanged( old );
        m_db.save( profile );
        Assertions.assertEquals( old, m_db.findByLoginName( "datechange" ).getPasswordChanged() );

        final UserProfile again = m_db.findByLoginName( "datechange" );
        again.setPassword( "Secret#2pass" );
        m_db.save( again );

        final Date changed = m_db.findByLoginName( "datechange" ).getPasswordChanged();
        Assertions.assertTrue( changed.after( old ) );
        Assertions.assertTrue( changed.getTime() > System.currentTimeMillis() - 60_000L );
    }

    @Test
    public void testSavingTheProfileWithoutChangingThePasswordKeepsTheDate() throws Exception {
        withPasswordChangedColumn();
        saveUser( "datekeep", "Secret#1pass" );
        final UserProfile profile = m_db.findByLoginName( "datekeep" );
        final Date old = daysAgo( 40 );
        profile.setPasswordChanged( old );
        m_db.save( profile );

        final UserProfile again = m_db.findByLoginName( "datekeep" );
        again.setFullname( "Another Name" );
        m_db.save( again );

        Assertions.assertEquals( old, m_db.findByLoginName( "datekeep" ).getPasswordChanged() );
    }

    @Test
    public void testSettingTheSamePasswordAgainDoesNotUpdateTheDate() throws Exception {
        withPasswordChangedColumn();
        saveUser( "datesame", "Secret#1pass" );
        final UserProfile profile = m_db.findByLoginName( "datesame" );
        final Date old = daysAgo( 40 );
        profile.setPasswordChanged( old );
        m_db.save( profile );

        final UserProfile again = m_db.findByLoginName( "datesame" );
        again.setPassword( "Secret#1pass" );
        m_db.save( again );

        Assertions.assertEquals( old, m_db.findByLoginName( "datesame" ).getPasswordChanged() );
    }

    @Test
    public void testHashingTheLegacyPasswordAgainAtLoginDoesNotStartTheClockAgain() throws Exception {
        withPasswordChangedColumn();
        final UserProfile janne = m_db.findByLoginName( "janne" );
        final Date old = daysAgo( 40 );
        janne.setPasswordChanged( old );
        m_db.save( janne );
        Assertions.assertFalse( m_db.findByLoginName( "janne" ).getPassword().startsWith( "{PBKDF2" ) );

        Assertions.assertTrue( m_db.validatePassword( "janne", "myP@5sw0rd" ) );

        Assertions.assertTrue( m_db.findByLoginName( "janne" ).getPassword().startsWith( "{PBKDF2" ) );
        Assertions.assertEquals( old, m_db.findByLoginName( "janne" ).getPasswordChanged() );
    }

    @Test
    public void testProfilesWithoutADateHaveNone() throws Exception {
        withPasswordChangedColumn();

        Assertions.assertNull( m_db.findByLoginName( "janne" ).getPasswordChanged() );
    }

    @Test
    public void testValidatePassword() {
        Assertions.assertFalse( m_db.validatePassword( "janne", "test" ) );
        Assertions.assertTrue( m_db.validatePassword( "janne", "myP@5sw0rd" ) );
        Assertions.assertTrue( m_db.validatePassword( "user", "password" ) );
    }

     @Override
    public Properties getTestProps() throws Exception {
        final Properties props = TestEngine.getTestProperties();

        props.put(PROP_DATABASE, JDBCUserDatabase.class.getCanonicalName());
        props.put("jspwiki.groupdatabase", JDBCGroupDatabase.class.getCanonicalName());
        props.put("jspwiki.groupdatabase.datasource", JDBCUserDatabase.DEFAULT_DB_JNDI_NAME);
        props.put("jspwiki.userdatabase.datasource", JDBCUserDatabase.DEFAULT_DB_JNDI_NAME);
        return props;
    }

}
