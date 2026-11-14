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
package org.apache.wiki.ui.admin.beans;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.apache.wiki.HttpMockFactory;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.auth.user.XMLUserDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

class UserBeanTest {

    private TestEngine engine;
    private UserDatabase db;

    @BeforeEach
    void setUp() throws Exception {
        final Properties props = TestEngine.getTestProperties();
        final File file = new File( "target/UserBeanTest" + UUID.randomUUID() + ".xml" );
        FileUtils.copyFile( new File( "src/test/resources/userdatabase.xml" ), file );
        props.put( XMLUserDatabase.PROP_USERDATABASE, file.getAbsolutePath() );
        engine = new TestEngine( props );
        db = engine.getManager( UserManager.class ).getUserDatabase();
    }

    @AfterEach
    void tearDown() {
        engine.stop();
    }

    private void lockJanne() throws Exception {
        final UserProfile profile = db.findByLoginName( "janne" );
        profile.setLockExpiry( new Date( System.currentTimeMillis() + 3_600_000L ) );
        db.save( profile );
    }

    /** Posts the form of the user administration for janne. */
    private Context post( final String password, final String unlock ) throws Exception {
        final HttpServletRequest request = HttpMockFactory.createHttpRequest();
        Mockito.doReturn( "save" ).when( request ).getParameter( "action" );
        Mockito.doReturn( "janne" ).when( request ).getParameter( "loginid" );
        Mockito.doReturn( "janne" ).when( request ).getParameter( "loginname" );
        Mockito.doReturn( "Janne Jalkanen" ).when( request ).getParameter( "fullname" );
        Mockito.doReturn( "janne@ecyrd.com" ).when( request ).getParameter( "email" );
        Mockito.doReturn( password ).when( request ).getParameter( "password" );
        Mockito.doReturn( password ).when( request ).getParameter( "password2" );
        Mockito.doReturn( unlock ).when( request ).getParameter( "unlock" );
        final Context context = Wiki.context().create( engine, request, ContextEnum.PAGE_VIEW.getRequestContext() );
        new UserBean( engine ).doPost( context );
        return context;
    }

    @Test
    void testALockedProfileIsUnlockedWithANewPassword() throws Exception {
        lockJanne();
        Assertions.assertTrue( db.findByLoginName( "janne" ).isLocked() );

        post( "Another#Secret1", "true" );

        final UserProfile janne = db.findByLoginName( "janne" );
        Assertions.assertFalse( janne.isLocked() );
        Assertions.assertTrue( db.validatePassword( "janne", "Another#Secret1" ) );
        Assertions.assertTrue( janne.getPasswordChanged().getTime() > System.currentTimeMillis() - 60_000L );
    }

    @Test
    void testALockedProfileIsNotUnlockedWithoutANewPassword() throws Exception {
        lockJanne();

        final Context context = post( "", "true" );

        Assertions.assertTrue( db.findByLoginName( "janne" ).isLocked() );
        Assertions.assertTrue( Arrays.stream( context.getWikiSession().getMessages() ).anyMatch( message -> message.contains( "new password" ) ),
                               Arrays.toString( context.getWikiSession().getMessages() ) );
        Assertions.assertTrue( Arrays.stream( context.getWikiSession().getMessages() ).noneMatch( message -> message.contains( "has been updated" ) ) );
    }

    @Test
    void testSavingWithoutAskingToUnlockKeepsTheLock() throws Exception {
        lockJanne();

        post( "Another#Secret1", null );

        Assertions.assertTrue( db.findByLoginName( "janne" ).isLocked() );
    }

    @Test
    void testAskingToUnlockAProfileThatIsNotLockedChangesNothingElse() throws Exception {
        final String before = db.findByLoginName( "janne" ).getPassword();

        post( "", "true" );

        Assertions.assertEquals( before.startsWith( "{PBKDF2" ), db.findByLoginName( "janne" ).getPassword().startsWith( "{PBKDF2" ) );
        Assertions.assertFalse( db.findByLoginName( "janne" ).isLocked() );
    }

}
