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

import java.io.File;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

/** What a user who logs in to a locked account is told (JSPWIKI-1387). */
class LockedAccountLoginTest {

    private TestEngine newEngine() throws Exception {
        final Properties props = TestEngine.getTestProperties();
        final File file = new File( "target/LockedAccountLoginTest" + UUID.randomUUID() + ".xml" );
        FileUtils.copyFile( new File( "src/test/resources/userdatabase.xml" ), file );
        props.put( XMLUserDatabase.PROP_USERDATABASE, file.getAbsolutePath() );
        return new TestEngine( props );
    }

    private void lock( final TestEngine engine, final String loginName, final Date until ) throws Exception {
        final UserDatabase db = engine.getManager( UserManager.class ).getUserDatabase();
        final UserProfile profile = db.findByLoginName( loginName );
        profile.setLockExpiry( until );
        db.save( profile );
    }

    private Session newSession( final TestEngine engine ) {
        final HttpServletRequest request = HttpMockFactory.createHttpRequest();
        return Wiki.session().find( engine, request );
    }

    private boolean login( final TestEngine engine, final Session session, final String loginName, final String password ) throws Exception {
        return engine.getManager( AuthenticationManager.class ).login( session, HttpMockFactory.createHttpRequest(), loginName, password );
    }

    @Test
    void testALockedAccountTellsItsOwnerThatItIsLocked() throws Exception {
        final TestEngine engine = newEngine();
        lock( engine, "janne", new Date( System.currentTimeMillis() + 3_600_000L ) );
        final Session session = newSession( engine );

        Assertions.assertFalse( login( engine, session, "janne", "myP@5sw0rd" ) );

        Assertions.assertFalse( session.isAuthenticated() );
        Assertions.assertEquals( 1, session.getMessages( "login" ).length );
        Assertions.assertTrue( session.getMessages( "login" )[ 0 ].contains( "locked" ), session.getMessages( "login" )[ 0 ] );
    }

    @Test
    void testALockedAccountDoesNotShowToSomeoneWhoDoesNotKnowThePassword() throws Exception {
        final TestEngine engine = newEngine();
        lock( engine, "janne", new Date( System.currentTimeMillis() + 3_600_000L ) );
        final Session session = newSession( engine );

        Assertions.assertFalse( login( engine, session, "janne", "wrong" ) );

        Assertions.assertEquals( 0, session.getMessages( "login" ).length );
    }

    @Test
    void testAnAccountThatIsNotLockedLogsIn() throws Exception {
        final TestEngine engine = newEngine();
        final Session session = newSession( engine );

        Assertions.assertTrue( login( engine, session, "janne", "myP@5sw0rd" ) );

        Assertions.assertEquals( 0, session.getMessages( "login" ).length );
    }

}
