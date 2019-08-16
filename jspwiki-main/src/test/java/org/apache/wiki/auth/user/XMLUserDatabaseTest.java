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
import java.security.Principal;
import java.util.Map;
import java.util.Properties;


public class XMLUserDatabaseTest {

    private XMLUserDatabase m_db;

    /**
     *
     */
    @BeforeEach
    public void setUp() throws Exception {
        Properties props = TestEngine.getTestProperties();
        props.put( XMLUserDatabase.PROP_USERDATABASE, "target/test-classes/userdatabase.xml" );
        WikiEngine engine = new TestEngine( props );
        m_db = new XMLUserDatabase();
        m_db.initialize( engine, props );
    }

    @Test
    public void testDeleteByLoginName() throws WikiSecurityException {
        // First, count the number of users in the db now.
        int oldUserCount = m_db.getWikiNames().length;

        // Create a new user with random name
        String loginName = "TestUser" + String.valueOf( System.currentTimeMillis() );
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
            UserProfile profile = m_db.findByEmail( "janne@ecyrd.com" );
            Assertions.assertEquals( "-7739839977499061014", profile.getUid() );
            Assertions.assertEquals( "janne", profile.getLoginName() );
            Assertions.assertEquals( "Janne Jalkanen", profile.getFullname() );
            Assertions.assertEquals( "JanneJalkanen", profile.getWikiName() );
            Assertions.assertEquals( "{SSHA}1WFv9OV11pD5IySgVH3sFa2VlCyYjbLrcVT/qw==", profile.getPassword() );
            Assertions.assertEquals( "janne@ecyrd.com", profile.getEmail() );
        } catch( NoSuchPrincipalException e ) {
            Assertions.assertTrue( false );
        }
        try {
            m_db.findByEmail( "foo@bar.org" );
            // We should never get here
            Assertions.assertTrue( false );
        } catch( NoSuchPrincipalException e ) {
            Assertions.assertTrue( true );
        }
    }

    @Test
    public void testFindByFullName() {
        try {
            UserProfile profile = m_db.findByFullName( "Janne Jalkanen" );
            Assertions.assertEquals( "-7739839977499061014", profile.getUid() );
            Assertions.assertEquals( "janne", profile.getLoginName() );
            Assertions.assertEquals( "Janne Jalkanen", profile.getFullname() );
            Assertions.assertEquals( "JanneJalkanen", profile.getWikiName() );
            Assertions.assertEquals( "{SSHA}1WFv9OV11pD5IySgVH3sFa2VlCyYjbLrcVT/qw==", profile.getPassword() );
            Assertions.assertEquals( "janne@ecyrd.com", profile.getEmail() );
            Assertions.assertNotNull( profile.getCreated() );
            Assertions.assertNotNull( profile.getLastModified() );
        } catch( NoSuchPrincipalException e ) {
            Assertions.assertTrue( false );
        }
        try {
            m_db.findByEmail( "foo@bar.org" );
            // We should never get here
            Assertions.assertTrue( false );
        } catch( NoSuchPrincipalException e ) {
            Assertions.assertTrue( true );
        }
    }

    @Test
    public void testFindByUid() {
        try {
            UserProfile profile = m_db.findByUid( "-7739839977499061014" );
            Assertions.assertEquals( "-7739839977499061014", profile.getUid() );
            Assertions.assertEquals( "janne", profile.getLoginName() );
            Assertions.assertEquals( "Janne Jalkanen", profile.getFullname() );
            Assertions.assertEquals( "JanneJalkanen", profile.getWikiName() );
            Assertions.assertEquals( "{SSHA}1WFv9OV11pD5IySgVH3sFa2VlCyYjbLrcVT/qw==", profile.getPassword() );
            Assertions.assertEquals( "janne@ecyrd.com", profile.getEmail() );
            Assertions.assertNotNull( profile.getCreated() );
            Assertions.assertNotNull( profile.getLastModified() );
        } catch( NoSuchPrincipalException e ) {
            Assertions.assertTrue( false );
        }
        try {
            m_db.findByEmail( "foo@bar.org" );
            // We should never get here
            Assertions.assertTrue( false );
        } catch( NoSuchPrincipalException e ) {
            Assertions.assertTrue( true );
        }
    }

    @Test
    public void testFindByWikiName() {
        try {
            UserProfile profile = m_db.findByWikiName( "JanneJalkanen" );
            Assertions.assertEquals( "-7739839977499061014", profile.getUid() );
            Assertions.assertEquals( "janne", profile.getLoginName() );
            Assertions.assertEquals( "Janne Jalkanen", profile.getFullname() );
            Assertions.assertEquals( "JanneJalkanen", profile.getWikiName() );
            Assertions.assertEquals( "{SSHA}1WFv9OV11pD5IySgVH3sFa2VlCyYjbLrcVT/qw==", profile.getPassword() );
            Assertions.assertEquals( "janne@ecyrd.com", profile.getEmail() );
        } catch( NoSuchPrincipalException e ) {
            Assertions.assertTrue( false );
        }
        try {
            m_db.findByEmail( "foo" );
            // We should never get here
            Assertions.assertTrue( false );
        } catch( NoSuchPrincipalException e ) {
            Assertions.assertTrue( true );
        }
    }

    @Test
    public void testFindByLoginName() {
        try {
            UserProfile profile = m_db.findByLoginName( "janne" );
            Assertions.assertEquals( "-7739839977499061014", profile.getUid() );
            Assertions.assertEquals( "janne", profile.getLoginName() );
            Assertions.assertEquals( "Janne Jalkanen", profile.getFullname() );
            Assertions.assertEquals( "JanneJalkanen", profile.getWikiName() );
            Assertions.assertEquals( "{SSHA}1WFv9OV11pD5IySgVH3sFa2VlCyYjbLrcVT/qw==", profile.getPassword() );
            Assertions.assertEquals( "janne@ecyrd.com", profile.getEmail() );
        } catch( NoSuchPrincipalException e ) {
            Assertions.assertTrue( false );
        }
        try {
            m_db.findByEmail( "FooBar" );
            // We should never get here
            Assertions.assertTrue( false );
        } catch( NoSuchPrincipalException e ) {
            Assertions.assertTrue( true );
        }
    }

    @Test
    public void testGetWikiNames() throws WikiSecurityException {
        // There are 8 test users in the database
        Principal[] p = m_db.getWikiNames();
        Assertions.assertEquals( 8, p.length );
        Assertions.assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "JanneJalkanen", WikiPrincipal.WIKI_NAME ) ) );
        Assertions.assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "", WikiPrincipal.WIKI_NAME ) ) );
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
        } catch( NoSuchPrincipalException e ) {
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
        } catch( DuplicateUserException e ) {
            // Cool; that's what we expect
        }

        // Now, rename it to an unused name
        m_db.rename( "olduser", "renameduser" );

        // The old user shouldn't be found
        try {
            profile = m_db.findByLoginName( "olduser" );
            Assertions.fail( "Old user was found, but it shouldn't have been." );
        } catch( NoSuchPrincipalException e ) {
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
            String uid = profile.getUid();
            Assertions.assertNotNull( m_db.findByUid( uid ) );
        } catch( NoSuchPrincipalException e ) {
            Assertions.assertTrue( false );
        } catch( WikiSecurityException e ) {
            Assertions.assertTrue( false );
        }
    }

    @Test
    public void testValidatePassword() {
        Assertions.assertFalse( m_db.validatePassword( "janne", "test" ) );
        Assertions.assertTrue( m_db.validatePassword( "janne", "myP@5sw0rd" ) );
        Assertions.assertTrue( m_db.validatePassword( "user", "password" ) );
    }

}
