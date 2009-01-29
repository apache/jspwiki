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
package com.ecyrd.jspwiki.action;

import java.util.Properties;

import javax.servlet.http.Cookie;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import net.sourceforge.stripes.mock.MockRoundtrip;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.Users;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;

public class UserPreferencesActionBeanTest extends TestCase
{
    TestEngine m_engine;

    public void setUp()
    {
        // Start the WikiEngine, and stash reference
        Properties props = new Properties();
        try
        {
            props.load( TestEngine.findTestProperties() );
            m_engine = new TestEngine( props );
        }
        catch( Exception e )
        {
            throw new RuntimeException( "Could not set up TestEngine: " + e.getMessage() );
        }
    }

    public void tearDown()
    {
        m_engine.shutdown();
    }
    

    public void testCreateAssertedName() throws Exception
    {
        MockRoundtrip trip;
        UserPreferencesActionBean bean;

        // Create session; set 'assertion' param; verify it got saved
        trip = m_engine.guestTrip( "/UserPreferences.action" );
        trip.setParameter( "assertedName", "MyAssertedIdentity" );
        trip.execute( "createAssertedName" );
        bean = trip.getActionBean( UserPreferencesActionBean.class );
        assertEquals( "/Wiki.action", trip.getDestination() );

        // Verify that the asserted name cookie is present in the Response
        MockHttpServletResponse response = (MockHttpServletResponse) bean.getContext().getResponse();
        Cookie[] cookies = response.getCookies();
        assertEquals( 1, cookies.length );
        Cookie cookie = cookies[0];
        assertEquals( CookieAssertionLoginModule.PREFS_COOKIE_NAME, cookie.getName() );
        assertEquals( "MyAssertedIdentity", cookie.getValue() );
    }

    public void testCreateAssertedNameAfterLogin() throws Exception
    {
        MockRoundtrip trip;
        UserPreferencesActionBean bean;

        // Create session; login in as Janne
        trip = m_engine.guestTrip( "/UserPreferences.action" );
        MockHttpServletRequest request = trip.getRequest();
        WikiSession wikiSession = WikiSession.getWikiSession( m_engine, request );
        boolean login = m_engine.getAuthenticationManager().login( wikiSession, Users.JANNE, Users.JANNE_PASS );
        assertTrue( "Could not log in.", login );

        // Set 'assertion' param; verify redirect to front page
        trip.setParameter( "assertedName", "MyAssertedIdentity" );
        trip.execute( "createAssertedName" );
        bean = trip.getActionBean( UserPreferencesActionBean.class );
        assertEquals( "/Wiki.action", trip.getDestination() );

        // Verify that the asserted name cookie is NOT present in the Response
        // (authenticated users cannot set the assertion cookie)
        MockHttpServletResponse response = (MockHttpServletResponse) bean.getContext().getResponse();
        Cookie[] cookies = response.getCookies();
        assertEquals( 0, cookies.length );
    }

    public void testClearAssertedName() throws Exception
    {
        MockRoundtrip trip;
        UserPreferencesActionBean bean;

        // Create session; set 'assertion' param; verify it got saved
        trip = m_engine.guestTrip( "/UserPreferences.action" );
        MockHttpServletRequest request = trip.getRequest();
        Cookie cookie = new Cookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME, "MyAssertedIdentity" );
        request.setCookies( new Cookie[] { cookie } );
        trip.execute( "clearAssertedName" );
        bean = trip.getActionBean( UserPreferencesActionBean.class );
        assertEquals( "/Login.action?logout=", trip.getDestination() );

        // Verify that the asserted name cookie is gone from the Response
        MockHttpServletResponse response = (MockHttpServletResponse) bean.getContext().getResponse();
        Cookie[] cookies = response.getCookies();
        assertEquals( 1, cookies.length );
        cookie = cookies[0];
        assertEquals( CookieAssertionLoginModule.PREFS_COOKIE_NAME, cookie.getName() );
        assertEquals( "", cookie.getValue() );
    }

    public static Test suite()
    {
        return new TestSuite( UserPreferencesActionBeanTest.class );
    }

}
