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

import org.apache.wiki.TestEngine;
import org.apache.wiki.auth.user.UserProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.mail.MessagingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

class AccountLifecycleMailerTest {

    private static class Message {
        final String to;
        final String subject;
        final String content;

        Message( final String to, final String subject, final String content ) {
            this.to = to;
            this.subject = subject;
            this.content = content;
        }
    }

    private final List< Message > sent = new ArrayList<>();
    private TestEngine engine;
    private AccountLifecycleMailer mailer;
    private UserProfile profile;
    private final TimeZone original = TimeZone.getDefault();

    @BeforeEach
    void setUp() throws Exception {
        TimeZone.setDefault( TimeZone.getTimeZone( "UTC" ) );
        engine = TestEngine.build();
        mailer = new AccountLifecycleMailer( engine, Locale.ENGLISH, ( props, to, subject, content ) -> sent.add( new Message( to, subject, content ) ) );
        profile = engine.getManager( UserManager.class ).getUserDatabase().newProfile();
        profile.setLoginName( "jdoe" );
        profile.setFullname( "John Doe" );
        profile.setEmail( "jdoe@example.com" );
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        TimeZone.setDefault( original );
        engine.stop();
    }

    @Test
    void testTheOwnerIsToldThatThePasswordWillExpire() {
        mailer.passwordWillExpire( profile, new Date( 1_800_000_000_000L ) );

        Assertions.assertEquals( 1, sent.size() );
        final Message message = sent.get( 0 );
        Assertions.assertEquals( "jdoe@example.com", message.to );
        Assertions.assertTrue( message.subject.contains( engine.getApplicationName() ), message.subject );
        Assertions.assertTrue( message.subject.contains( "expire" ), message.subject );
        Assertions.assertTrue( message.content.contains( "John Doe" ), message.content );
        Assertions.assertTrue( message.content.contains( "jdoe" ), message.content );
        Assertions.assertTrue( message.content.contains( "January 15, 2027" ), message.content );
    }

    @Test
    void testTheOwnerIsToldThatTheAccountHasBeenLocked() {
        mailer.accountLocked( profile, null );

        Assertions.assertEquals( 1, sent.size() );
        Assertions.assertTrue( sent.get( 0 ).subject.contains( "locked" ), sent.get( 0 ).subject );
        Assertions.assertTrue( sent.get( 0 ).content.contains( "administrator" ), sent.get( 0 ).content );
        Assertions.assertFalse( sent.get( 0 ).content.contains( "deleted" ), sent.get( 0 ).content );
    }

    @Test
    void testTheOwnerIsToldWhenTheLockedAccountWillBeDeleted() {
        mailer.accountLocked( profile, new Date( 1_800_000_000_000L ) );

        Assertions.assertEquals( 1, sent.size() );
        Assertions.assertTrue( sent.get( 0 ).content.contains( "deleted" ), sent.get( 0 ).content );
        Assertions.assertTrue( sent.get( 0 ).content.contains( "January 15, 2027" ), sent.get( 0 ).content );
    }

    @Test
    void testTheOwnerIsToldThatTheAccountHasBeenDeleted() {
        mailer.accountDeleted( profile );

        Assertions.assertEquals( 1, sent.size() );
        Assertions.assertEquals( "jdoe@example.com", sent.get( 0 ).to );
        Assertions.assertTrue( sent.get( 0 ).subject.contains( "deleted" ), sent.get( 0 ).subject );
    }

    @Test
    void testNothingIsSentToSomeoneWithoutAnAddress() {
        profile.setEmail( null );
        mailer.passwordWillExpire( profile, new Date() );
        profile.setEmail( "" );
        mailer.accountLocked( profile, null );
        mailer.accountDeleted( profile );

        Assertions.assertTrue( sent.isEmpty() );
    }

    @Test
    void testAFailureToSendIsReported() {
        final AccountLifecycleMailer failing = new AccountLifecycleMailer( engine, Locale.ENGLISH, ( props, to, subject, content ) -> {
            throw new MessagingException( "no mail server" );
        } );

        Assertions.assertThrows( IllegalStateException.class, () -> failing.passwordWillExpire( profile, new Date() ) );
    }

    @Test
    void testTheMailIsSentWithTheProperties() {
        final List< Properties > seen = new ArrayList<>();
        final AccountLifecycleMailer recording = new AccountLifecycleMailer( engine, Locale.ENGLISH, ( props, to, subject, content ) -> seen.add( props ) );

        recording.accountDeleted( profile );

        Assertions.assertEquals( 1, seen.size() );
        Assertions.assertSame( engine.getWikiProperties(), seen.get( 0 ) );
    }

}
