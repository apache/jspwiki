/*
    JSPWiki - a JSP-based WikiWiki clone.

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

import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.wiki.NoRequiredPropertyException;
import org.apache.wiki.TestEngine;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.login.UserDatabaseLoginModule;
import org.apache.wiki.auth.login.WikiCallbackHandler;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.XMLUserDatabase;

import junit.framework.TestCase;


/**
 * @author Andrew R. Jaquith
 */
public class UserDatabaseLoginModuleTest extends TestCase
{
    UserDatabase db;

    Subject      subject;
    
    private TestEngine m_engine = null;

    public final void testLogin()
    {
        try
        {
            // Log in with a user that isn't in the database
            CallbackHandler handler = new WikiCallbackHandler( m_engine, Locale.getDefault(), "user", "password" );
            LoginModule module = new UserDatabaseLoginModule();
            module.initialize(subject, handler, 
                              new HashMap<String, Object>(), 
                              new HashMap<String, Object>());
            module.login();
            module.commit();
            Set principals = subject.getPrincipals();
            assertEquals( 1, principals.size() );
            assertTrue( principals.contains( new WikiPrincipal( "user", WikiPrincipal.LOGIN_NAME ) ) );
            assertFalse( principals.contains( Role.AUTHENTICATED ) );
            assertFalse( principals.contains( Role.ALL ) );
            
            // Login with a user that IS in the database
            subject = new Subject();
            handler = new WikiCallbackHandler( m_engine, Locale.getDefault(), "janne", "myP@5sw0rd" );
            module = new UserDatabaseLoginModule();
            module.initialize(subject, handler, 
                              new HashMap<String, Object>(), 
                              new HashMap<String, Object>());
            module.login();
            module.commit();
            principals = subject.getPrincipals();
            assertEquals( 1, principals.size() );
            assertTrue( principals.contains( new WikiPrincipal( "janne", WikiPrincipal.LOGIN_NAME ) ) );
            assertFalse( principals.contains( Role.AUTHENTICATED ) );
            assertFalse( principals.contains( Role.ALL ) );            
        }
        catch( LoginException e )
        {
            System.err.println( e.getMessage() );
            assertTrue( false );
        }
    }

    public final void testLogout()
    {
        try
        {
            CallbackHandler handler = new WikiCallbackHandler( m_engine, Locale.getDefault(), "user", "password" );
            LoginModule module = new UserDatabaseLoginModule();
            module.initialize(subject, handler, 
                              new HashMap<String, Object>(), 
                              new HashMap<String, Object>());
            module.login();
            module.commit();
            Set principals = subject.getPrincipals();
            assertEquals( 1, principals.size() );
            assertTrue( principals.contains( new WikiPrincipal( "user",  WikiPrincipal.LOGIN_NAME ) ) );
            assertFalse( principals.contains( Role.AUTHENTICATED ) );
            assertFalse( principals.contains( Role.ALL ) );
            module.logout();
            assertEquals( 0, principals.size() );
        }
        catch( LoginException e )
        {
            System.err.println( e.getMessage() );
            assertTrue( false );
        }
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        props.put(XMLUserDatabase.PROP_USERDATABASE, "tests/etc/userdatabase.xml");
       m_engine  = new TestEngine(props);
        db = new XMLUserDatabase();
        subject = new Subject();
        try
        {
            db.initialize( m_engine, props );
        }
        catch( NoRequiredPropertyException e )
        {
            System.err.println( e.getMessage() );
            assertTrue( false );
        }
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        m_engine.shutdown();
    }
}