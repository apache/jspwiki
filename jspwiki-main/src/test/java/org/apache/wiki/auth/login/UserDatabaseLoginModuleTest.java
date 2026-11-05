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
package org.apache.wiki.auth.login;
import java.io.File;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.commons.io.FileUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.auth.user.XMLUserDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

/**
 */
public class UserDatabaseLoginModuleTest
{
    WikiEngine m_engine;

    UserDatabase m_db;

    Subject      m_subject;

    @Test
    public final void testLogin()
    {
        try
        {
            // Log in with a user that isn't in the database
            CallbackHandler handler = new WikiCallbackHandler( m_engine, null, "user", "password" );
            LoginModule module = new UserDatabaseLoginModule();
            module.initialize( m_subject, handler,
                              new HashMap<String, Object>(),
                              new HashMap<String, Object>() );
            module.login();
            module.commit();
            Set< Principal > principals = m_subject.getPrincipals();
            Assertions.assertEquals( 1, principals.size() );
            Assertions.assertTrue( principals.contains( new WikiPrincipal( "user", WikiPrincipal.LOGIN_NAME ) ) );
            Assertions.assertFalse( principals.contains( Role.AUTHENTICATED ) );
            Assertions.assertFalse( principals.contains( Role.ALL ) );

            // Login with a user that IS in the database
            m_subject = new Subject();
            handler = new WikiCallbackHandler( m_engine, null, "janne", "myP@5sw0rd" );
            module = new UserDatabaseLoginModule();
            module.initialize( m_subject, handler,
                              new HashMap<String, Object>(),
                              new HashMap<String, Object>() );
            module.login();
            module.commit();
            principals = m_subject.getPrincipals();
            Assertions.assertEquals( 1, principals.size() );
            Assertions.assertTrue( principals.contains( new WikiPrincipal( "janne", WikiPrincipal.LOGIN_NAME ) ) );
            Assertions.assertFalse( principals.contains( Role.AUTHENTICATED ) );
            Assertions.assertFalse( principals.contains( Role.ALL ) );
        }
        catch( final LoginException e )
        {
            System.err.println( e.getMessage() );
            Assertions.fail();
        }
    }

    @Test
    public final void testLogout()
    {
        try
        {
            final CallbackHandler handler = new WikiCallbackHandler( m_engine, null, "user", "password" );
            final LoginModule module = new UserDatabaseLoginModule();
            module.initialize( m_subject, handler,
                              new HashMap<String, Object>(),
                              new HashMap<String, Object>() );
            module.login();
            module.commit();
            final Set< Principal > principals = m_subject.getPrincipals();
            Assertions.assertEquals( 1, principals.size() );
            Assertions.assertTrue( principals.contains( new WikiPrincipal( "user",  WikiPrincipal.LOGIN_NAME ) ) );
            Assertions.assertFalse( principals.contains( Role.AUTHENTICATED ) );
            Assertions.assertFalse( principals.contains( Role.ALL ) );
            module.logout();
            Assertions.assertEquals( 0, principals.size() );
        }
        catch( final LoginException e )
        {
            System.err.println( e.getMessage() );
            Assertions.fail();
        }
    }

    /**
     *
     */
    @BeforeEach
    public void setUp() throws Exception
    {
        final Properties props = TestEngine.getTestProperties();
        props.put(XMLUserDatabase.PROP_USERDATABASE, "target/test-classes/userdatabase.xml" );
        m_engine  = new TestEngine(props);
        m_db = new XMLUserDatabase();
        m_subject = new Subject();
        try
        {
            m_db.initialize( m_engine, props );
        }
        catch( final NoRequiredPropertyException e )
        {
            System.err.println( e.getMessage() );
            Assertions.fail();
        }
    }

    private UserDatabase newDatabaseWithJanneLockedUntil( final Date lockExpiry ) throws Exception {
        final Properties props = TestEngine.getTestProperties();
        final File file = new File( "target/UserDatabaseLoginModuleTest" + UUID.randomUUID() + ".xml" );
        FileUtils.copyFile( new File( "src/test/resources/userdatabase.xml" ), file );
        props.put( XMLUserDatabase.PROP_USERDATABASE, file.getAbsolutePath() );
        m_engine = new TestEngine( props );
        final UserDatabase db = m_engine.getManager( UserManager.class ).getUserDatabase();
        final UserProfile profile = db.findByLoginName( "janne" );
        profile.setLockExpiry( lockExpiry );
        db.save( profile );
        return db;
    }

    private void login( final String username, final String password ) throws LoginException {
        final CallbackHandler handler = new WikiCallbackHandler( m_engine, null, username, password );
        final LoginModule module = new UserDatabaseLoginModule();
        module.initialize( m_subject, handler, new HashMap<String, Object>(), new HashMap<String, Object>() );
        module.login();
        module.commit();
    }

    /** A locked profile cannot log in, even with the right password. */
    @Test
    public final void testLockedProfileCannotLogin() throws Exception {
        newDatabaseWithJanneLockedUntil( new Date( System.currentTimeMillis() + 3_600_000L ) );

        Assertions.assertThrows( AccountLockedException.class, () -> login( "janne", "myP@5sw0rd" ) );
        Assertions.assertTrue( m_subject.getPrincipals().isEmpty() );
    }

    /** A locked profile does not tell that it is locked to someone who does not know the password. */
    @Test
    public final void testLockedProfileWithWrongPasswordIsAnOrdinaryFailure() throws Exception {
        newDatabaseWithJanneLockedUntil( new Date( System.currentTimeMillis() + 3_600_000L ) );

        final LoginException e = Assertions.assertThrows( LoginException.class, () -> login( "janne", "wrong" ) );
        Assertions.assertFalse( e instanceof AccountLockedException );
        Assertions.assertInstanceOf( FailedLoginException.class, e );
    }

    /** Once the lock has expired the profile can log in again. */
    @Test
    public final void testProfileWithExpiredLockCanLogin() throws Exception {
        newDatabaseWithJanneLockedUntil( new Date( System.currentTimeMillis() - 1_000L ) );

        login( "janne", "myP@5sw0rd" );

        Assertions.assertTrue( m_subject.getPrincipals().contains( new WikiPrincipal( "janne", WikiPrincipal.LOGIN_NAME ) ) );
    }

    /** Only the locked profile is affected. */
    @Test
    public final void testOtherProfilesCanLoginWhileOneIsLocked() throws Exception {
        newDatabaseWithJanneLockedUntil( new Date( System.currentTimeMillis() + 3_600_000L ) );

        login( "user", "password" );

        Assertions.assertTrue( m_subject.getPrincipals().contains( new WikiPrincipal( "user", WikiPrincipal.LOGIN_NAME ) ) );
    }

}
