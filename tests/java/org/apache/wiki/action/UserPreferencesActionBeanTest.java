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
package org.apache.wiki.action;

import java.util.Locale;
import java.util.Properties;

import javax.servlet.http.Cookie;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.action.UserPreferencesActionBean;
import org.apache.wiki.auth.Users;
import org.apache.wiki.auth.login.CookieAssertionLoginModule;
import org.apache.wiki.preferences.Preferences;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import net.sourceforge.stripes.mock.MockRoundtrip;


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
    

    public void testSave() throws Exception
    {
        MockRoundtrip trip;
        UserPreferencesActionBean bean;

        // Create session; set 'assertion' param; verify it got saved
        trip = m_engine.guestTrip( "/UserPreferences.jsp" );
        trip.setParameter( "assertedName", "MyAssertedIdentity" );
        trip.setParameter( "editor", "plain" );
        trip.setParameter( "locale", Locale.GERMANY.toString() );
        trip.setParameter( "orientation", Preferences.Orientation.RIGHT.name() );
        trip.setParameter( "sectionEditing", "true" );
        trip.setParameter( "skin", "Smart" );
        trip.setParameter( "timeFormat", "YYYY dd-mm" );
        trip.setParameter( "timeZone", Preferences.AVAILABLE_TIME_ZONES.get( 1 ).getID() );
        trip.execute( "save" );
        bean = trip.getActionBean( UserPreferencesActionBean.class );
        assertEquals( "/Wiki.jsp", trip.getDestination() );

        // Verify that the asserted name cookie is present in the Response
        MockHttpServletResponse response = (MockHttpServletResponse) bean.getContext().getResponse();
        Cookie[] cookies = response.getCookies();
        assertEquals( 8, cookies.length );
        boolean foundCookie = false;
        for ( Cookie cookie : response.getCookies() )
        {
            if ( CookieAssertionLoginModule.PREFS_COOKIE_NAME.equals( cookie.getName() ) )
            {
                if ( "MyAssertedIdentity".equals( cookie.getValue() ) )
                {
                    foundCookie = true;
                    break;
                }
            }
        }
        assertTrue( foundCookie );
        
        // Verify that the Preference objects were set properly
        Preferences prefs = Preferences.getPreferences( trip.getRequest() );
        assertEquals( "plain", prefs.get( Preferences.PREFS_EDITOR ) );
        assertEquals( Locale.GERMANY, prefs.get( Preferences.PREFS_LOCALE ) );
        assertEquals( Preferences.Orientation.RIGHT, prefs.get( Preferences.PREFS_ORIENTATION ) );
        assertEquals( true, prefs.get( Preferences.PREFS_SECTION_EDITING ) );
        assertEquals( "Smart", prefs.get( Preferences.PREFS_SKIN ) );
        assertEquals( "YYYY dd-mm", prefs.get( Preferences.PREFS_TIME_FORMAT ) );
        assertEquals( Preferences.AVAILABLE_TIME_ZONES.get( 1 ), prefs.get( Preferences.PREFS_TIME_ZONE ) );
    }

    public void testCreateAssertedNameAfterLogin() throws Exception
    {
        MockRoundtrip trip;
        UserPreferencesActionBean bean;

        // Create session; login in as Janne
        trip = m_engine.guestTrip( "/UserPreferences.jsp" );
        MockHttpServletRequest request = trip.getRequest();
        WikiSession wikiSession = WikiSession.getWikiSession( m_engine, request );
        boolean login = m_engine.getAuthenticationManager().login( wikiSession, request, Users.JANNE, Users.JANNE_PASS );
        assertTrue( "Could not log in.", login );

        // Set 'assertion' param; verify redirect to front page
        trip.setParameter( "assertedName", "MyAssertedIdentity" );
        trip.setParameter( "editor", "plain" );
        trip.setParameter( "locale", Locale.GERMANY.toString() );
        trip.setParameter( "orientation", Preferences.Orientation.RIGHT.name() );
        trip.setParameter( "sectionEditing", "true" );
        trip.setParameter( "skin", "Smart" );
        trip.setParameter( "timeFormat", "YYYY dd-mm" );
        trip.setParameter( "timeZone", Preferences.AVAILABLE_TIME_ZONES.get( 1 ).getID() );
        trip.execute( "save" );
        bean = trip.getActionBean( UserPreferencesActionBean.class );
        assertEquals( "/Wiki.jsp", trip.getDestination() );

        // Verify that the asserted name cookie is NOT present in the Response
        // (authenticated users cannot set the assertion cookie)
        MockHttpServletResponse response = (MockHttpServletResponse) bean.getContext().getResponse();
        boolean foundCookie = false;
        for ( Cookie cookie : response.getCookies() )
        {
            if ( CookieAssertionLoginModule.PREFS_COOKIE_NAME.equals( cookie.getName() ) )
            {
                foundCookie = true;
            }
        }
        assertFalse( foundCookie );
    }

    public void testClearAssertedName() throws Exception
    {
        MockRoundtrip trip;
        UserPreferencesActionBean bean;

        // Create session; set 'assertion' param; verify it got saved
        trip = m_engine.guestTrip( "/UserPreferences.jsp" );
        MockHttpServletRequest request = trip.getRequest();
        Cookie cookie = new Cookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME, "MyAssertedIdentity" );
        request.setCookies( new Cookie[] { cookie } );
        trip.execute( "clearAssertedName" );
        bean = trip.getActionBean( UserPreferencesActionBean.class );
        assertEquals( "/Login.jsp?logout=", trip.getDestination() );

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
