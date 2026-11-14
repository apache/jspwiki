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

import org.apache.commons.io.FileUtils;
import org.apache.wiki.HttpMockFactory;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.auth.user.XMLUserDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

class AccountLifecycleTest {

    private static final long DAY = 86_400_000L;

    /** Remembers what it was asked to tell the users. */
    private static class RecordingNotifier implements AccountLifecycle.Notifier {

        final List< String > events = new ArrayList<>();
        final List< Date > dates = new ArrayList<>();
        boolean failForJanne;

        @Override
        public void passwordWillExpire( final UserProfile profile, final Date expires ) {
            if( failForJanne && "janne".equals( profile.getLoginName() ) ) {
                throw new IllegalStateException( "the mail server is down" );
            }
            events.add( "warn " + profile.getLoginName() );
            dates.add( expires );
        }

        @Override
        public void accountLocked( final UserProfile profile, final Date deleted ) {
            events.add( "lock " + profile.getLoginName() );
            dates.add( deleted );
        }

        @Override
        public void accountDeleted( final UserProfile profile ) {
            events.add( "delete " + profile.getLoginName() + " " + profile.getEmail() );
            dates.add( null );
        }
    }

    private TestEngine engine;
    private UserDatabase db;
    private RecordingNotifier notifier;
    private Date now;

    @BeforeEach
    void setUp() throws Exception {
        final Properties props = TestEngine.getTestProperties();
        final File file = new File( "target/AccountLifecycleTest" + UUID.randomUUID() + ".xml" );
        FileUtils.copyFile( new File( "src/test/resources/userdatabase.xml" ), file );
        props.put( XMLUserDatabase.PROP_USERDATABASE, file.getAbsolutePath() );
        engine = new TestEngine( props );
        db = engine.getManager( UserManager.class ).getUserDatabase();
        notifier = new RecordingNotifier();
        now = new Date();
    }

    private static Properties props( final String warn, final String expiry, final String lock, final String delete ) {
        final Properties props = new Properties();
        if( warn != null ) {
            props.put( AccountLifecycle.PROP_WARN_DAYS, warn );
        }
        if( expiry != null ) {
            props.put( PasswordExpiry.PROP_EXPIRY_DAYS, expiry );
        }
        if( lock != null ) {
            props.put( AccountLifecycle.PROP_LOCK_DAYS, lock );
        }
        if( delete != null ) {
            props.put( AccountLifecycle.PROP_DELETE_DAYS, delete );
        }
        return props;
    }

    private AccountLifecycle lifecycle( final Properties props ) {
        return new AccountLifecycle( props, () -> db, notifier );
    }

    private AccountLifecycle all() {
        return lifecycle( props( "60", "90", "120", "180" ) );
    }

    private void changedDaysAgo( final String loginName, final long days ) throws Exception {
        changed( loginName, new Date( now.getTime() - days * DAY ) );
    }

    private void changed( final String loginName, final Date date ) throws Exception {
        final UserProfile profile = db.findByLoginName( loginName );
        profile.setPasswordChanged( date );
        db.save( profile );
    }

    private UserProfile profile( final String loginName ) throws Exception {
        return db.findByLoginName( loginName );
    }

    /** Gives everybody a recent password, so only the profile a test is about is old. */
    private void everybodyYoung() throws Exception {
        for( final String name : new String[] { "janne", "Alice", "Bob", "admin" } ) {
            changedDaysAgo( name, 1 );
        }
    }

    /** What the owner of the account was told, as the notifier recorded it. */
    private List< String > eventsFor( final String loginName ) {
        return notifier.events.stream().filter( event -> event.split( " " )[ 1 ].equals( loginName ) ).collect( Collectors.toList() );
    }

    // configuration ...............................................................................................

    @Test
    void testNothingIsEnabledByDefault() {
        final AccountLifecycle lifecycle = lifecycle( new Properties() );

        Assertions.assertFalse( lifecycle.isEnabled() );
        Assertions.assertEquals( 0, lifecycle.getWarnDays() );
        Assertions.assertEquals( 0, lifecycle.getLockDays() );
        Assertions.assertEquals( 0, lifecycle.getDeleteDays() );
    }

    @Test
    void testTheStagesAreReadInDays() {
        final AccountLifecycle lifecycle = all();

        Assertions.assertTrue( lifecycle.isEnabled() );
        Assertions.assertEquals( 60, lifecycle.getWarnDays() );
        Assertions.assertEquals( 120, lifecycle.getLockDays() );
        Assertions.assertEquals( 180, lifecycle.getDeleteDays() );
    }

    @Test
    void testEachStageCanBeEnabledAlone() {
        Assertions.assertTrue( lifecycle( props( "60", "90", null, null ) ).isEnabled() );
        Assertions.assertTrue( lifecycle( props( null, null, "120", null ) ).isEnabled() );
        Assertions.assertTrue( lifecycle( props( null, null, null, "180" ) ).isEnabled() );
    }

    @Test
    void testTheReminderNeedsTheExpiryOfThePassword() {
        Assertions.assertEquals( 0, lifecycle( props( "60", null, null, null ) ).getWarnDays() );
        Assertions.assertFalse( lifecycle( props( "60", null, null, null ) ).isEnabled() );
    }

    @Test
    void testTheReminderComesBeforeTheExpiry() {
        Assertions.assertEquals( 0, lifecycle( props( "90", "90", null, null ) ).getWarnDays() );
        Assertions.assertEquals( 0, lifecycle( props( "100", "90", null, null ) ).getWarnDays() );
    }

    @Test
    void testTheLockComesAfterTheExpiry() {
        Assertions.assertEquals( 0, lifecycle( props( null, "90", "90", null ) ).getLockDays() );
        Assertions.assertEquals( 0, lifecycle( props( null, "90", "60", null ) ).getLockDays() );
        Assertions.assertEquals( 91, lifecycle( props( null, "90", "91", null ) ).getLockDays() );
    }

    @Test
    void testTheDeletionComesAfterTheLockAndTheExpiry() {
        Assertions.assertEquals( 0, lifecycle( props( null, null, "120", "120" ) ).getDeleteDays() );
        Assertions.assertEquals( 0, lifecycle( props( null, null, "120", "100" ) ).getDeleteDays() );
        Assertions.assertEquals( 0, lifecycle( props( null, "90", null, "60" ) ).getDeleteDays() );
        Assertions.assertEquals( 121, lifecycle( props( null, null, "120", "121" ) ).getDeleteDays() );
    }

    @Test
    void testAStageThatIsNotInOrderIsDisabledButTheOthersStay() {
        final AccountLifecycle lifecycle = lifecycle( props( "60", "90", "50", "180" ) );

        Assertions.assertEquals( 60, lifecycle.getWarnDays() );
        Assertions.assertEquals( 0, lifecycle.getLockDays() );
        Assertions.assertEquals( 180, lifecycle.getDeleteDays() );
    }

    @Test
    void testInvalidValuesDisableTheStage() {
        for( final String value : new String[] { "abc", "-5", "", "0", "1.5" } ) {
            final AccountLifecycle lifecycle = lifecycle( props( null, null, value, value ) );

            Assertions.assertEquals( 0, lifecycle.getLockDays(), value );
            Assertions.assertEquals( 0, lifecycle.getDeleteDays(), value );
            Assertions.assertFalse( lifecycle.isEnabled(), value );
        }
    }

    // sweeps ......................................................................................................

    @Test
    void testNothingHappensWhenNothingIsEnabled() throws Exception {
        changedDaysAgo( "janne", 1000 );

        lifecycle( new Properties() ).apply( now );

        Assertions.assertTrue( notifier.events.isEmpty() );
        Assertions.assertFalse( profile( "janne" ).isLocked() );
    }

    @Test
    void testProfilesWithoutADateGetItAndNothingElseHappens() throws Exception {
        Assertions.assertNull( profile( "janne" ).getPasswordChanged() );

        all().apply( now );

        final Date date = profile( "janne" ).getPasswordChanged();
        Assertions.assertNotNull( date );
        Assertions.assertTrue( Math.abs( date.getTime() - now.getTime() ) < 5_000 );
        Assertions.assertTrue( notifier.events.isEmpty() );
    }

    @Test
    void testYoungPasswordsAreLeftAlone() throws Exception {
        everybodyYoung();
        changedDaysAgo( "janne", 5 );

        all().apply( now );

        Assertions.assertTrue( notifier.events.isEmpty() );
        Assertions.assertFalse( profile( "janne" ).isLocked() );
    }

    @Test
    void testTheReminderIsSentOnceWhenThePasswordIsOldEnough() throws Exception {
        everybodyYoung();
        changedDaysAgo( "janne", 70 );

        all().apply( now );
        all().apply( now );

        Assertions.assertEquals( List.of( "warn janne" ), notifier.events );
        final Date expires = notifier.dates.get( 0 );
        Assertions.assertEquals( profile( "janne" ).getPasswordChanged().getTime() + 90 * DAY, expires.getTime() );
    }

    @Test
    void testNoReminderBeforeItsTime() throws Exception {
        everybodyYoung();
        changedDaysAgo( "janne", 59 );

        all().apply( now );

        Assertions.assertTrue( notifier.events.isEmpty() );
    }

    @Test
    void testTheReminderIsSentAgainForANewPassword() throws Exception {
        everybodyYoung();
        changedDaysAgo( "janne", 70 );
        all().apply( now );
        Assertions.assertEquals( 1, notifier.events.size() );

        // the password is changed ten days later, and gets old again
        final Date changedAgain = new Date( now.getTime() + 10 * DAY );
        changed( "janne", changedAgain );
        all().apply( new Date( changedAgain.getTime() + 70 * DAY ) );

        Assertions.assertEquals( List.of( "warn janne", "warn janne" ), eventsFor( "janne" ) );
    }

    @Test
    void testAReminderThatCouldNotBeSentIsTriedAgain() throws Exception {
        everybodyYoung();
        changedDaysAgo( "janne", 70 );
        notifier.failForJanne = true;
        all().apply( now );
        Assertions.assertTrue( notifier.events.isEmpty() );

        notifier.failForJanne = false;
        all().apply( now );

        Assertions.assertEquals( List.of( "warn janne" ), notifier.events );
    }

    @Test
    void testAFailureWithOneProfileDoesNotStopTheOthers() throws Exception {
        everybodyYoung();
        changedDaysAgo( "janne", 70 );
        changedDaysAgo( "Alice", 70 );
        notifier.failForJanne = true;

        all().apply( now );

        Assertions.assertEquals( List.of( "warn Alice" ), notifier.events );
    }

    @Test
    void testTheAccountIsLockedAfterTheLockLimit() throws Exception {
        everybodyYoung();
        changedDaysAgo( "janne", 130 );

        all().apply( now );

        Assertions.assertTrue( profile( "janne" ).isLocked() );
        Assertions.assertEquals( List.of( "lock janne" ), notifier.events );
        Assertions.assertEquals( profile( "janne" ).getPasswordChanged().getTime() + 180 * DAY, notifier.dates.get( 0 ).getTime() );
    }

    @Test
    void testALockedAccountIsNotLockedOrToldAgain() throws Exception {
        everybodyYoung();
        changedDaysAgo( "janne", 130 );

        all().apply( now );
        all().apply( now );

        Assertions.assertEquals( List.of( "lock janne" ), notifier.events );
    }

    @Test
    void testALockedAccountCannotLogIn() throws Exception {
        everybodyYoung();
        changedDaysAgo( "janne", 130 );
        all().apply( now );
        final Session session = Wiki.session().find( engine, HttpMockFactory.createHttpRequest() );

        Assertions.assertFalse( engine.getManager( AuthenticationManager.class ).login( session, null, "janne", "myP@5sw0rd" ) );
    }

    @Test
    void testTheLockWithoutADeleteLimitSaysSo() throws Exception {
        everybodyYoung();
        changedDaysAgo( "janne", 130 );

        lifecycle( props( null, null, "120", null ) ).apply( now );

        Assertions.assertEquals( List.of( "lock janne" ), notifier.events );
        Assertions.assertNull( notifier.dates.get( 0 ) );
    }

    @Test
    void testALockedAccountGetsNoReminder() throws Exception {
        everybodyYoung();
        changedDaysAgo( "janne", 130 );

        all().apply( now );

        Assertions.assertFalse( notifier.events.contains( "warn janne" ) );
    }

    @Test
    void testTheAccountIsDeletedAfterTheDeleteLimit() throws Exception {
        everybodyYoung();
        changedDaysAgo( "janne", 200 );
        final String email = profile( "janne" ).getEmail();

        all().apply( now );

        Assertions.assertEquals( List.of( "delete janne " + email ), notifier.events );
        Assertions.assertThrows( NoSuchPrincipalException.class, () -> db.findByLoginName( "janne" ) );
    }

    @Test
    void testDeletingWinsOverLocking() throws Exception {
        everybodyYoung();
        changedDaysAgo( "janne", 200 );

        all().apply( now );

        Assertions.assertEquals( 1, notifier.events.size() );
        Assertions.assertTrue( notifier.events.get( 0 ).startsWith( "delete janne" ) );
    }

    @Test
    void testALockedAccountIsDeletedLater() throws Exception {
        everybodyYoung();
        changedDaysAgo( "janne", 130 );
        all().apply( now );

        all().apply( new Date( now.getTime() + 60 * DAY ) );

        final List< String > events = eventsFor( "janne" );
        Assertions.assertEquals( 2, events.size() );
        Assertions.assertTrue( events.get( 1 ).startsWith( "delete janne" ) );
        Assertions.assertThrows( NoSuchPrincipalException.class, () -> db.findByLoginName( "janne" ) );
    }

    @Test
    void testOtherProfilesAreNotAffected() throws Exception {
        everybodyYoung();
        changedDaysAgo( "janne", 200 );

        all().apply( now );

        Assertions.assertNotNull( profile( "Alice" ) );
        Assertions.assertFalse( profile( "Alice" ).isLocked() );
        Assertions.assertNotNull( profile( "admin" ) );
    }

    @Test
    void testTheDeletionIsOffUnlessItIsConfigured() throws Exception {
        everybodyYoung();
        changedDaysAgo( "janne", 10_000 );

        lifecycle( props( "60", "90", "120", null ) ).apply( now );

        Assertions.assertNotNull( profile( "janne" ) );
        Assertions.assertTrue( profile( "janne" ).isLocked() );
    }

}
