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

import java.security.Principal;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.Cookie;

import junit.framework.TestCase;
import net.sourceforge.stripes.mock.MockHttpServletRequest;

import org.apache.wiki.NoRequiredPropertyException;
import org.apache.wiki.TestEngine;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.XMLUserDatabase;


/**
 */
public class CookieAssertionLoginModuleTest extends TestCase
{
    UserDatabase m_db;

    Subject      m_subject;

    private TestEngine m_engine;

    public final void testLogin() throws WikiSecurityException
    {
        MockHttpServletRequest request = m_engine.newHttpRequest();
        try
        {
            // We can use cookies right?
            assertTrue( m_engine.getAuthenticationManager().allowsCookieAssertions() );

            // Test using Cookie and IP address (AnonymousLoginModule succeeds)
            Cookie cookie = new Cookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME, "Bullwinkle" );
            request.setCookies( new Cookie[] { cookie } );
            m_subject = new Subject();
            CallbackHandler handler = new WebContainerCallbackHandler( m_engine, request );
            LoginModule module = new CookieAssertionLoginModule();
            module.initialize( m_subject, handler, 
                              new HashMap<String, Object>(), 
                              new HashMap<String, Object>() );
            module.login();
            module.commit();
            Set<Principal> principals = m_subject.getPrincipals();
            assertEquals( 1, principals.size() );
            assertTrue( principals.contains( new WikiPrincipal( "Bullwinkle" ) ) );
            assertFalse( principals.contains( Role.ASSERTED ) );
            assertFalse( principals.contains( Role.ALL ) );
        }
        catch( LoginException e )
        {
            System.err.println( e.getMessage() );
            assertTrue( false );
        }
    }

    public final void testLogout() throws WikiSecurityException
    {
        MockHttpServletRequest request = m_engine.newHttpRequest();
        Cookie cookie = new Cookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME, "Bullwinkle" );
        request.setCookies( new Cookie[] { cookie } );
        try
        {
            CallbackHandler handler = new WebContainerCallbackHandler( m_engine, request );
            LoginModule module = new CookieAssertionLoginModule();
            module.initialize( m_subject, handler, 
                              new HashMap<String, Object>(), 
                              new HashMap<String, Object>() );
            module.login();
            module.commit();
            Set<Principal> principals = m_subject.getPrincipals();
            assertEquals( 1, principals.size() );
            assertTrue( principals.contains( new WikiPrincipal( "Bullwinkle" ) ) );
            assertFalse( principals.contains( Role.ANONYMOUS ) );
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
        m_engine = new TestEngine(props);
        m_db = new XMLUserDatabase();
        m_subject = new Subject();
        try
        {
            m_db.initialize( m_engine, props );
        }
        catch( NoRequiredPropertyException e )
        {
            System.err.println( e.getMessage() );
            assertTrue( false );
        }
    }

}
