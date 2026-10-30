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
package org.apache.wiki.notification;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import org.apache.commons.io.FileUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.auth.user.XMLUserDatabase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

class DefaultSubscriptionManagerTest {

    private static SimpleSmtpServer smtp;

    private TestEngine engine;
    private SubscriptionManager manager;
    private UserDatabase db;

    @BeforeAll
    static void startMailServer() throws Exception {
        smtp = SimpleSmtpServer.start( SimpleSmtpServer.AUTO_SMTP_PORT );
    }

    @AfterAll
    static void stopMailServer() {
        smtp.close();
    }

    @BeforeEach
    void setUp() throws Exception {
        smtp.reset();
        final Properties props = TestEngine.getTestProperties();
        props.setProperty( "mail.smtp.port", String.valueOf( smtp.getPort() ) );
        props.setProperty( DefaultSubscriptionManager.PROP_BASEURL, "http://wiki.example.org/" );
        final File target = new File( "target/SubscriptionManagerTest" + UUID.randomUUID() + ".xml" );
        FileUtils.copyFile( new File( "src/test/resources/userdatabase.xml" ), target );
        props.put( XMLUserDatabase.PROP_USERDATABASE, target.getAbsolutePath() );

        engine = new TestEngine( props );
        manager = engine.getManager( SubscriptionManager.class );
        db = engine.getManager( UserManager.class ).getUserDatabase();
        final UserProfile profile = db.newProfile();
        profile.setLoginName( "subscriber" );
        profile.setFullname( "Sub Scriber" );
        profile.setEmail( "subscriber@example.com" );
        db.save( profile );
    }

    @AfterEach
    void tearDown() {
        engine.deleteTestPage( "SubscribedPage" );
        engine.deleteTestPage( "OtherPage" );
        engine.deleteTestPage( "RestrictedPage" );
        engine.stop();
    }

    @Test
    void subscriptionsAreKeptInTheUserDatabase() throws Exception {
        Assertions.assertFalse( manager.isSubscribed( "subscriber", "SubscribedPage" ) );

        manager.subscribe( "subscriber", "SubscribedPage" );
        manager.subscribe( "subscriber", "SubscribedPage" );
        manager.subscribe( "subscriber", "OtherPage" );

        Assertions.assertTrue( manager.isSubscribed( "subscriber", "SubscribedPage" ) );
        Assertions.assertEquals( 2, manager.getSubscriptions( "subscriber" ).size() );

        manager.unsubscribe( "subscriber", "OtherPage" );
        manager.unsubscribe( "subscriber", "OtherPage" );
        Assertions.assertFalse( manager.isSubscribed( "subscriber", "OtherPage" ) );
        Assertions.assertTrue( manager.isSubscribed( "subscriber", "SubscribedPage" ) );

        manager.unsubscribe( "subscriber", "SubscribedPage" );
        Assertions.assertTrue( manager.getSubscriptions( "subscriber" ).isEmpty() );
        Assertions.assertTrue( db.findByLoginName( "subscriber" ).getAttributes().isEmpty() );
    }

    @Test
    void anUnknownUserHasNoSubscriptions() {
        Assertions.assertFalse( manager.isSubscribed( "nobody", "SubscribedPage" ) );
        Assertions.assertTrue( manager.getSubscriptions( "nobody" ).isEmpty() );
    }

    @Test
    void subscribingAnUnknownUserFails() {
        Assertions.assertThrows( WikiException.class, () -> manager.subscribe( "nobody", "SubscribedPage" ) );
    }

    @Test
    void savingASubscribedPageSendsAMail() throws Exception {
        manager.subscribe( "subscriber", "SubscribedPage" );

        engine.saveText( "SubscribedPage", "first text" );
        engine.saveText( "OtherPage", "other text" );

        final List< SmtpMessage > mails = awaitMails( 1 );
        Assertions.assertEquals( 1, mails.size() );
        final SmtpMessage mail = mails.get( 0 );
        Assertions.assertTrue( mail.getHeaderValue( "To" ).contains( "subscriber@example.com" ) );
        Assertions.assertTrue( mail.getHeaderValue( "Subject" ).contains( "SubscribedPage" ) );
        Assertions.assertTrue( mail.getBody().contains( "SubscribedPage" ) );
        Assertions.assertFalse( mail.getBody().contains( "first text" ) );
        Assertions.assertTrue( mail.getBody().matches( "(?s).*http://wiki\\.example\\.org/\\S+page=SubscribedPage.*" ), mail.getBody() );
    }

    @Test
    void aPageWithRestrictedViewIsNotNotified() throws Exception {
        manager.subscribe( "subscriber", "RestrictedPage" );
        manager.subscribe( "subscriber", "SubscribedPage" );

        engine.saveText( "RestrictedPage", "[{ALLOW view Admin}]\nsecret" );
        engine.saveText( "SubscribedPage", "public" );

        final List< SmtpMessage > mails = awaitMails( 1 );
        Assertions.assertEquals( 1, mails.size() );
        Assertions.assertTrue( mails.get( 0 ).getHeaderValue( "Subject" ).contains( "SubscribedPage" ) );
    }

    @Test
    void aSubscriberWithoutEmailIsSkipped() throws Exception {
        final UserProfile profile = db.findByLoginName( "subscriber" );
        profile.setEmail( "" );
        db.save( profile );
        manager.subscribe( "subscriber", "SubscribedPage" );

        engine.saveText( "SubscribedPage", "text" );
        engine.saveText( "OtherPage", "text" );
        Thread.sleep( 1000 );

        Assertions.assertEquals( 0, smtp.getReceivedEmails().size() );
    }

    /** Waits for the background sender, giving up after ten seconds. */
    private static List< SmtpMessage > awaitMails( final int expected ) throws InterruptedException {
        final long end = System.currentTimeMillis() + 10_000;
        while( smtp.getReceivedEmails().size() < expected && System.currentTimeMillis() < end ) {
            Thread.sleep( 50 );
        }
        Thread.sleep( 500 ); // would show an unexpected extra mail
        return smtp.getReceivedEmails();
    }

}
