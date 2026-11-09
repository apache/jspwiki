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

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.apache.wiki.HttpMockFactory;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.auth.user.XMLUserDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

class PasswordExpiryTest {

    private static final long DAY = 86_400_000L;

    private static Date daysAgo( final int days ) {
        return new Date( System.currentTimeMillis() - DAY * days );
    }

    private static PasswordExpiry expiry( final String days ) {
        final Properties props = new Properties();
        if( days != null ) {
            props.put( PasswordExpiry.PROP_EXPIRY_DAYS, days );
        }
        return new PasswordExpiry( props );
    }

    private static UserProfile profileChanged( final Date date ) {
        final UserProfile profile = Mockito.mock( UserProfile.class );
        Mockito.when( profile.getPasswordChanged() ).thenReturn( date );
        return profile;
    }

    @Test
    void testPasswordsDoNotExpireByDefault() {
        Assertions.assertFalse( expiry( null ).isEnabled() );
        Assertions.assertFalse( expiry( null ).isExpired( profileChanged( daysAgo( 10_000 ) ), new Date() ) );
    }

    @Test
    void testZeroMeansThatPasswordsDoNotExpire() {
        Assertions.assertFalse( expiry( "0" ).isEnabled() );
        Assertions.assertFalse( expiry( "0" ).isExpired( profileChanged( daysAgo( 10_000 ) ), new Date() ) );
    }

    @Test
    void testOnlyPasswordsOlderThanTheLimitExpire() {
        final PasswordExpiry expiry = expiry( "90" );

        Assertions.assertTrue( expiry.isEnabled() );
        Assertions.assertFalse( expiry.isExpired( profileChanged( daysAgo( 89 ) ), new Date() ) );
        final Date now = new Date();
        Assertions.assertFalse( expiry.isExpired( profileChanged( new Date( now.getTime() - 90 * DAY ) ), now ) );
        Assertions.assertTrue( expiry.isExpired( profileChanged( daysAgo( 91 ) ), new Date() ) );
    }

    @Test
    void testTheLimitIsMeasuredFromTheGivenTime() {
        final Date changed = new Date( 1_000_000_000_000L );

        Assertions.assertFalse( expiry( "30" ).isExpired( profileChanged( changed ), new Date( changed.getTime() + 30 * DAY ) ) );
        Assertions.assertTrue( expiry( "30" ).isExpired( profileChanged( changed ), new Date( changed.getTime() + 30 * DAY + 1 ) ) );
    }

    @Test
    void testAPasswordWithoutADateDoesNotExpire() {
        Assertions.assertFalse( expiry( "90" ).isExpired( profileChanged( null ), new Date() ) );
    }

    @Test
    void testInvalidValuesMeanThatPasswordsDoNotExpire() {
        for( final String value : new String[] { "abc", "-5", "", "1.5", "9999999999999999999999" } ) {
            Assertions.assertFalse( expiry( value ).isEnabled(), value );
        }
    }

    @Test
    void testSurroundingBlanksAreIgnored() {
        Assertions.assertTrue( expiry( " 90 " ).isEnabled() );
    }

    // login .......................................................................................................

    private TestEngine newEngine( final String days ) throws Exception {
        final Properties props = TestEngine.getTestProperties();
        final File file = new File( "target/PasswordExpiryTest" + UUID.randomUUID() + ".xml" );
        FileUtils.copyFile( new File( "src/test/resources/userdatabase.xml" ), file );
        props.put( XMLUserDatabase.PROP_USERDATABASE, file.getAbsolutePath() );
        if( days != null ) {
            props.put( PasswordExpiry.PROP_EXPIRY_DAYS, days );
        }
        return new TestEngine( props );
    }

    private void setPasswordChanged( final TestEngine engine, final String loginName, final Date date ) throws Exception {
        final UserDatabase db = engine.getManager( UserManager.class ).getUserDatabase();
        final UserProfile profile = db.findByLoginName( loginName );
        profile.setPasswordChanged( date );
        db.save( profile );
    }

    private Date passwordChanged( final TestEngine engine, final String loginName ) throws Exception {
        return engine.getManager( UserManager.class ).getUserDatabase().findByLoginName( loginName ).getPasswordChanged();
    }

    private Session newSession( final TestEngine engine ) {
        final HttpServletRequest request = HttpMockFactory.createHttpRequest();
        return Wiki.session().find( engine, request );
    }

    private boolean login( final TestEngine engine, final Session session, final String loginName, final String password ) throws Exception {
        return engine.getManager( AuthenticationManager.class ).login( session, HttpMockFactory.createHttpRequest(), loginName, password );
    }

    @Test
    void testAPasswordThatHasNotExpiredLogsIn() throws Exception {
        final TestEngine engine = newEngine( "90" );
        setPasswordChanged( engine, "janne", daysAgo( 30 ) );
        final Session session = newSession( engine );

        Assertions.assertTrue( login( engine, session, "janne", "myP@5sw0rd" ) );

        Assertions.assertTrue( session.isAuthenticated() );
    }

    @Test
    void testAnExpiredPasswordDoesNotLogIn() throws Exception {
        final TestEngine engine = newEngine( "90" );
        setPasswordChanged( engine, "janne", daysAgo( 100 ) );
        final Session session = newSession( engine );

        Assertions.assertFalse( login( engine, session, "janne", "myP@5sw0rd" ) );

        Assertions.assertFalse( session.isAuthenticated() );
        Assertions.assertEquals( 1, session.getMessages( "login" ).length );
        Assertions.assertTrue( session.getMessages( "login" )[ 0 ].contains( "expired" ), session.getMessages( "login" )[ 0 ] );
    }

    @Test
    void testAnExpiredPasswordDoesNotShowToSomeoneWhoDoesNotKnowIt() throws Exception {
        final TestEngine engine = newEngine( "90" );
        setPasswordChanged( engine, "janne", daysAgo( 100 ) );
        final Session session = newSession( engine );

        Assertions.assertFalse( login( engine, session, "janne", "wrong" ) );

        Assertions.assertEquals( 0, session.getMessages( "login" ).length );
    }

    @Test
    void testChangingTheExpiredPasswordAllowsTheLoginAgain() throws Exception {
        final TestEngine engine = newEngine( "90" );
        setPasswordChanged( engine, "janne", daysAgo( 100 ) );
        Assertions.assertFalse( login( engine, newSession( engine ), "janne", "myP@5sw0rd" ) );

        final UserDatabase db = engine.getManager( UserManager.class ).getUserDatabase();
        final UserProfile profile = db.findByLoginName( "janne" );
        profile.setPassword( "Another#Secret1" );
        db.save( profile );

        Assertions.assertTrue( login( engine, newSession( engine ), "janne", "Another#Secret1" ) );
    }

    @Test
    void testAProfileWithoutADateStartsItsClockAtTheFirstLogin() throws Exception {
        final TestEngine engine = newEngine( "90" );
        Assertions.assertNull( passwordChanged( engine, "janne" ) );
        final long before = System.currentTimeMillis() - 1000;

        Assertions.assertTrue( login( engine, newSession( engine ), "janne", "myP@5sw0rd" ) );

        final Date changed = passwordChanged( engine, "janne" );
        Assertions.assertNotNull( changed );
        Assertions.assertTrue( changed.getTime() >= before && changed.getTime() <= System.currentTimeMillis() + 1000 );
    }

    @Test
    void testPasswordsDoNotExpireWhenNoLimitIsSet() throws Exception {
        final TestEngine engine = newEngine( null );
        setPasswordChanged( engine, "janne", daysAgo( 10_000 ) );

        Assertions.assertTrue( login( engine, newSession( engine ), "janne", "myP@5sw0rd" ) );
    }

    @Test
    void testTheDateIsNotSetWhenNoLimitIsSet() throws Exception {
        final TestEngine engine = newEngine( null );

        Assertions.assertTrue( login( engine, newSession( engine ), "janne", "myP@5sw0rd" ) );

        Assertions.assertNull( passwordChanged( engine, "janne" ) );
    }

    @Test
    void testOtherUsersAreNotAffected() throws Exception {
        final TestEngine engine = newEngine( "90" );
        setPasswordChanged( engine, "janne", daysAgo( 100 ) );

        Assertions.assertTrue( login( engine, newSession( engine ), "user", "password" ) );
    }

}
