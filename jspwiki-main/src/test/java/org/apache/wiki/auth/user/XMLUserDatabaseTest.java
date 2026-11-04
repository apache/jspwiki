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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.Users;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.util.CryptoUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.commons.io.FileUtils;


public class XMLUserDatabaseTest {

    private XMLUserDatabase m_db;

    private WikiEngine m_engine;

    private Properties m_props;

    @BeforeEach
    public void setUp() throws Exception {
        m_props = TestEngine.getTestProperties();
        File target = new File("target/XMLUserDatabaseTest" + UUID.randomUUID().toString() + ".xml");
        FileUtils.copyFile(new File("src/test/resources/userdatabase.xml" ), target);
        m_props.put( XMLUserDatabase.PROP_USERDATABASE, target.getAbsolutePath() );
        m_engine = new TestEngine( m_props );
        m_db = new XMLUserDatabase();
        m_db.initialize( m_engine, m_props );
    }

    @Test
    public void testSaveQuotedEmailKeepsDatabaseWellFormed() throws Exception {
        // An RFC-2822 quoted local part passes EmailValidator; it used to be written into
        // userdatabase.xml without XML escaping, corrupting the whole authentication store.
        final String email = "\"x\"@example.com";
        UserProfile profile = m_db.newProfile();
        profile.setEmail( email );
        profile.setLoginName( "xmlescape" );
        profile.setFullname( "Xml Escape" );
        profile.setPassword( "password" );
        m_db.save( profile );

        // Reload the database from disk: a corrupted file comes back empty (or unparseable),
        // locking every user out of authentication.
        final XMLUserDatabase db2 = new XMLUserDatabase();
        db2.initialize( m_engine, m_props );
        final UserProfile saved = db2.findByLoginName( "xmlescape" );
        Assertions.assertEquals( email, saved.getEmail() );

        // The pre-existing users must have survived the round-trip as well.
        Assertions.assertNotNull( db2.findByLoginName( "janne" ) );
    }

    @Test
    public void testDeleteByLoginName() throws WikiSecurityException {
        // First, count the number of users in the db now.
        final String loginName = "TestUser" + System.currentTimeMillis();
        synchronized (m_db) {
            final int oldUserCount = m_db.getWikiNames().length;
            try {

                // Create a new user with random name
                UserProfile profile = m_db.newProfile();
                profile.setEmail("jspwiki.tests@mailinator.com");
                profile.setLoginName(loginName);
                profile.setFullname("FullName" + loginName);
                profile.setPassword("password");
                m_db.save(profile);

                // Make sure the profile saved successfully
                profile = m_db.findByLoginName(loginName);
                Assertions.assertEquals(loginName, profile.getLoginName());
                Assertions.assertEquals(oldUserCount + 1, m_db.getWikiNames().length);
            } finally {
                // Now delete the profile; should be back to old count
                m_db.deleteByLoginName(loginName);
                Assertions.assertEquals(oldUserCount, m_db.getWikiNames().length);
            }
        }
    }

    @Test
    public void testAttributes() throws Exception {
         final Principal[] p = m_db.getWikiNames();
        UserProfile profile = m_db.findByEmail( "janne@ecyrd.com" );

        Map< String, Serializable > attributes = profile.getAttributes();
        Assertions.assertTrue( attributes.size() >= 2 );
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
        Assertions.assertTrue( attributes.size() >= 3 );
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
            Assertions.assertEquals( "{SHA-256}AeJQgAgYDAf2WZiqPJ2l6cGdGC/PgWmkjZmkjrBEV6SW/HlclZGlIg==", profile.getPassword() );
            Assertions.assertEquals( "janne@ecyrd.com", profile.getEmail() );
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
            Assertions.assertEquals( "{SHA-256}AeJQgAgYDAf2WZiqPJ2l6cGdGC/PgWmkjZmkjrBEV6SW/HlclZGlIg==", profile.getPassword() );
            Assertions.assertEquals( "janne@ecyrd.com", profile.getEmail() );
            Assertions.assertNotNull( profile.getCreated() );
            Assertions.assertNotNull( profile.getLastModified() );
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
            Assertions.assertEquals( "{SHA-256}AeJQgAgYDAf2WZiqPJ2l6cGdGC/PgWmkjZmkjrBEV6SW/HlclZGlIg==", profile.getPassword() );
            Assertions.assertEquals( "janne@ecyrd.com", profile.getEmail() );
            Assertions.assertNotNull( profile.getCreated() );
            Assertions.assertNotNull( profile.getLastModified() );
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
            Assertions.assertEquals( "{SHA-256}AeJQgAgYDAf2WZiqPJ2l6cGdGC/PgWmkjZmkjrBEV6SW/HlclZGlIg==", profile.getPassword() );
            Assertions.assertEquals( "janne@ecyrd.com", profile.getEmail() );
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
            Assertions.assertEquals( "{SHA-256}AeJQgAgYDAf2WZiqPJ2l6cGdGC/PgWmkjZmkjrBEV6SW/HlclZGlIg==", profile.getPassword() );
            Assertions.assertEquals( "janne@ecyrd.com", profile.getEmail() );
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
    public void testGetWikiNames() throws WikiSecurityException {
        // There are 8 test users in the database
        final Principal[] p = m_db.getWikiNames();
        Assertions.assertEquals( 7, p.length );
        Assertions.assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "JanneJalkanen", WikiPrincipal.WIKI_NAME ) ) );
        Assertions.assertFalse( ArrayUtils.contains( p, new WikiPrincipal( "", WikiPrincipal.WIKI_NAME ) ) );
        Assertions.assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "Administrator", WikiPrincipal.WIKI_NAME ) ) );
        Assertions.assertTrue( ArrayUtils.contains( p, new WikiPrincipal( Users.ALICE, WikiPrincipal.WIKI_NAME ) ) );
        Assertions.assertTrue( ArrayUtils.contains( p, new WikiPrincipal( Users.BOB, WikiPrincipal.WIKI_NAME ) ) );
        Assertions.assertTrue( ArrayUtils.contains( p, new WikiPrincipal( Users.CHARLIE, WikiPrincipal.WIKI_NAME ) ) );
        Assertions.assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "FredFlintstone", WikiPrincipal.WIKI_NAME ) ) );
        Assertions.assertTrue( ArrayUtils.contains( p, new WikiPrincipal( Users.BIFF, WikiPrincipal.WIKI_NAME ) ) );
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
            UserProfile profile = m_db.newProfile();
            profile.setEmail( "jspwiki.tests@mailinator.com" );
            profile.setLoginName( "user" );
            profile.setPassword( "password" );
            m_db.save( profile );
            profile = m_db.findByEmail( "jspwiki.tests@mailinator.com" );
            Assertions.assertEquals( "jspwiki.tests@mailinator.com", profile.getEmail() );
            Assertions.assertTrue( CryptoUtil.verifySaltedPassword( "password".getBytes(), profile.getPassword() ) );

            // Make sure we can find it by uid
            final String uid = profile.getUid();
            Assertions.assertNotNull( m_db.findByUid( uid ) );
        } catch( final WikiSecurityException e ) {
            Assertions.fail( e.getMessage() );
        }
    }

    private XMLUserDatabase newDatabase( final File file, final String algorithm, final String iterations ) throws Exception {
        final Properties props = TestEngine.getTestProperties();
        props.put( XMLUserDatabase.PROP_USERDATABASE, file.getAbsolutePath() );
        props.put( "jspwiki.credentials.reuseCount", "3" );
        if( algorithm != null ) {
            props.put( "jspwiki.credentials.hash.algorithm", algorithm );
        }
        if( iterations != null ) {
            props.put( "jspwiki.credentials.hash.iterations", iterations );
        }
        final XMLUserDatabase db = new XMLUserDatabase();
        db.initialize( m_engine, props );
        return db;
    }

    private File newDatabaseFile() throws Exception {
        final File file = new File( "target/XMLUserDatabaseTest" + UUID.randomUUID() + ".xml" );
        FileUtils.copyFile( new File( "src/test/resources/userdatabase.xml" ), file );
        return file;
    }

    private void saveUser( final XMLUserDatabase db, final String loginName, final String password ) throws Exception {
        final UserProfile profile = db.newProfile();
        profile.setEmail( loginName + "@example.com" );
        profile.setLoginName( loginName );
        profile.setFullname( "Full " + loginName );
        profile.setPassword( password );
        db.save( profile );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testNewPasswordsUseTheConfiguredAlgorithmAndIterations() throws Exception {
        final XMLUserDatabase db = newDatabase( newDatabaseFile(), "PBKDF2-SHA512", "2000" );

        saveUser( db, "sha512user", "Secret#1pass" );

        final String stored = db.findByLoginName( "sha512user" ).getPassword();
        Assertions.assertTrue( stored.startsWith( "{PBKDF2-SHA512}2000$" ), stored );
        Assertions.assertTrue( db.validatePassword( "sha512user", "Secret#1pass" ) );
        Assertions.assertFalse( db.validatePassword( "sha512user", "Secret#2pass" ) );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testDefaultSettingsKeepTheCurrentFormat() throws Exception {
        final XMLUserDatabase db = newDatabase( newDatabaseFile(), null, null );

        saveUser( db, "defaultuser", "Secret#1pass" );

        final String stored = db.findByLoginName( "defaultuser" ).getPassword();
        Assertions.assertTrue( stored.startsWith( "{PBKDF2-SHA256}600000$" ), stored );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testLoginHashesAgainWhenTheSettingsChanged() throws Exception {
        final File file = newDatabaseFile();
        saveUser( newDatabase( file, "PBKDF2-SHA256", "2000" ), "changeuser", "Secret#1pass" );

        final XMLUserDatabase db = newDatabase( file, "PBKDF2-SHA512", "3000" );
        Assertions.assertTrue( db.findByLoginName( "changeuser" ).getPassword().startsWith( "{PBKDF2-SHA256}2000$" ) );

        Assertions.assertTrue( db.validatePassword( "changeuser", "Secret#1pass" ) );

        Assertions.assertTrue( db.findByLoginName( "changeuser" ).getPassword().startsWith( "{PBKDF2-SHA512}3000$" ) );
        Assertions.assertTrue( db.validatePassword( "changeuser", "Secret#1pass" ) );
        Assertions.assertFalse( db.validatePassword( "changeuser", "Secret#2pass" ) );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testLoginHashesAgainWhenOnlyTheIterationsChanged() throws Exception {
        final File file = newDatabaseFile();
        saveUser( newDatabase( file, "PBKDF2-SHA512", "2000" ), "iteruser", "Secret#1pass" );

        final XMLUserDatabase db = newDatabase( file, "PBKDF2-SHA512", "4000" );
        Assertions.assertTrue( db.validatePassword( "iteruser", "Secret#1pass" ) );

        Assertions.assertTrue( db.findByLoginName( "iteruser" ).getPassword().startsWith( "{PBKDF2-SHA512}4000$" ) );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testLoginKeepsTheHashWhenTheSettingsAreTheSame() throws Exception {
        final XMLUserDatabase db = newDatabase( newDatabaseFile(), "PBKDF2-SHA512", "2000" );
        saveUser( db, "sameuser", "Secret#1pass" );
        final String before = db.findByLoginName( "sameuser" ).getPassword();

        Assertions.assertTrue( db.validatePassword( "sameuser", "Secret#1pass" ) );

        Assertions.assertEquals( before, db.findByLoginName( "sameuser" ).getPassword() );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testFailedLoginDoesNotHashAgain() throws Exception {
        final File file = newDatabaseFile();
        saveUser( newDatabase( file, "PBKDF2-SHA256", "2000" ), "failuser", "Secret#1pass" );
        final XMLUserDatabase db = newDatabase( file, "PBKDF2-SHA512", "3000" );

        Assertions.assertFalse( db.validatePassword( "failuser", "Secret#2pass" ) );

        Assertions.assertTrue( db.findByLoginName( "failuser" ).getPassword().startsWith( "{PBKDF2-SHA256}2000$" ) );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testLegacyHashesAreHashedAgainWithTheConfiguredSettings() throws Exception {
        final XMLUserDatabase db = newDatabase( newDatabaseFile(), "PBKDF2-SHA512", "2000" );
        Assertions.assertFalse( db.findByLoginName( "janne" ).getPassword().startsWith( "{PBKDF2" ) );

        Assertions.assertTrue( db.validatePassword( "janne", "myP@5sw0rd" ) );

        Assertions.assertTrue( db.findByLoginName( "janne" ).getPassword().startsWith( "{PBKDF2-SHA512}2000$" ) );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testUnsupportedAlgorithmFallsBackToTheDefault() throws Exception {
        final XMLUserDatabase db = newDatabase( newDatabaseFile(), "PBKDF2-MD5", "2000" );

        saveUser( db, "fallbackuser", "Secret#1pass" );

        final String stored = db.findByLoginName( "fallbackuser" ).getPassword();
        Assertions.assertTrue( stored.startsWith( "{PBKDF2-SHA256}600000$" ), stored );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testInvalidIterationsFallBackToTheDefault() throws Exception {
        for( final String iterations : new String[] { "abc", "0", "-5", "" } ) {
            final XMLUserDatabase db = newDatabase( newDatabaseFile(), "PBKDF2-SHA512", iterations );

            saveUser( db, "iterationsuser", "Secret#1pass" );

            final String stored = db.findByLoginName( "iterationsuser" ).getPassword();
            Assertions.assertTrue( stored.startsWith( "{PBKDF2-SHA512}210000$" ), iterations + " -> " + stored );
        }
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testPasswordReuseIsDetectedWithEveryAlgorithm() throws Exception {
        final File file = newDatabaseFile();
        final XMLUserDatabase sha256 = newDatabase( file, "PBKDF2-SHA256", "2000" );
        saveUser( sha256, "reuseuser", "Secret#1pass" );
        final XMLUserDatabase sha512 = newDatabase( file, "PBKDF2-SHA512", "2000" );
        final UserProfile profile = sha512.findByLoginName( "reuseuser" );
        profile.setPassword( "Secret#2pass" );
        sha512.save( profile );

        Assertions.assertFalse( sha512.validatePasswordReuse( "reuseuser", "Secret#1pass" ) );
        Assertions.assertFalse( sha512.validatePasswordReuse( "reuseuser", "Secret#2pass" ) );
        Assertions.assertTrue( sha512.validatePasswordReuse( "reuseuser", "Secret#3pass" ) );
    }

    @Test
    public void testValidatePassword() {
        Assertions.assertFalse( m_db.validatePassword( "janne", "test" ) );
        Assertions.assertTrue( m_db.validatePassword( "janne", "myP@5sw0rd" ) );
        Assertions.assertTrue( m_db.validatePassword( "user", "password" ) );
    }

    
        @Test
    public void JSPWIKI_130() throws Exception {
        final Properties props = TestEngine.getTestProperties();
        File target = new File("target/JSPWIKI_130" + UUID.randomUUID().toString() + ".xml");
        FileUtils.copyFile(new File("src/test/resources/userdatabase.xml"), target);
        props.put(XMLUserDatabase.PROP_USERDATABASE, target.getAbsolutePath());
        final WikiEngine engine = new TestEngine(props);
        XMLUserDatabase m_db = new XMLUserDatabase();
        m_db.initialize(engine, props);
        //create a user and save it the changes.
         // Create new user & verify it saved ok
        UserProfile profile = m_db.newProfile();
        profile.setEmail( "renamed@mailinator.com" );
        profile.setFullname( "Renamed User" );
        profile.setLoginName( "olduser" );
        profile.setPassword( "password" );
        m_db.save( profile );
        
        //confirm the check file exists
        File f = new File(target.getParent(), target.getName() + ".check");
        Assertions.assertTrue(f.exists());
        
        //shutdown the database manager
        
        //change a bit or two in the check file.
        FileUtils.writeStringToFile(f, "you've been hacked", StandardCharsets.UTF_8);
        
        
        //start it back up again, expecting it to barf
        Assertions.assertThrows(RuntimeException.class, () -> {
            m_db.initialize(engine, props);
        });
    }
}
