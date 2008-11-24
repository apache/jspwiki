package com.ecyrd.jspwiki.action;

import java.util.Properties;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.validation.ValidationErrors;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.SessionMonitor;
import com.ecyrd.jspwiki.auth.Users;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;

public class LoginActionBeanTest extends TestCase
{
    public static Test suite()
    {
        return new TestSuite( LoginActionBeanTest.class );
    }

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

    public void testLogin() throws Exception
    {
        MockRoundtrip trip;
        LoginActionBean bean;
        ValidationErrors errors;

        // Verify that the initial user is anonymous
        trip = m_engine.guestTrip( "/Login.action" );
        HttpServletRequest request = trip.getRequest();
        WikiSession wikiSession = SessionMonitor.getInstance( m_engine ).find( request.getSession() );
        assertNotSame( Users.JANNE, wikiSession.getLoginPrincipal().getName() );

        // Log in
        trip.setParameter( "j_username", Users.JANNE );
        trip.setParameter( "j_password", Users.JANNE_PASS );
        trip.execute( "login" );

        // Verify we logged in correctly
        bean = trip.getActionBean( LoginActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( Users.JANNE, wikiSession.getLoginPrincipal().getName() );
        assertEquals( "/Wiki.jsp", trip.getDestination() );

        // Should be just one cookie (the JSPWiki asserted name cookie)
        Cookie[] cookies = trip.getResponse().getCookies();
        assertEquals( 1, cookies.length );
        assertEquals( CookieAssertionLoginModule.PREFS_COOKIE_NAME, cookies[0].getName() );
        assertEquals( "Janne+Jalkanen", cookies[0].getValue() );
    }

    public void testLoginNoParams() throws Exception
    {
        MockRoundtrip trip;
        LoginActionBean bean;
        ValidationErrors errors;

        // Verify that the initial user is anonymous
        trip = m_engine.guestTrip( "/Login.action" );
        HttpServletRequest request = trip.getRequest();
        WikiSession wikiSession = SessionMonitor.getInstance( m_engine ).find( request.getSession() );
        assertNotSame( Users.JANNE, wikiSession.getLoginPrincipal().getName() );

        // Log in; should see two errors (because we did not specify a username
        // or password)
        trip.execute( "login" );
        bean = trip.getActionBean( LoginActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 2, errors.size() );

        // Log in again with just a password; should see one error
        trip = m_engine.guestTrip( "/Login.action" );
        trip.setParameter( "j_password", Users.JANNE_PASS );
        trip.execute( "login" );
        bean = trip.getActionBean( LoginActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 1, errors.size() );

        // Log in again with just a username; should see one error
        trip = m_engine.guestTrip( "/Login.action" );
        trip.setParameter( "j_username", Users.JANNE );
        trip.execute( "login" );
        bean = trip.getActionBean( LoginActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 1, errors.size() );
    }

    public void testLoginRedirect() throws Exception
    {
        MockRoundtrip trip;
        LoginActionBean bean;
        ValidationErrors errors;

        // Verify that the initial user is anonymous
        trip = m_engine.guestTrip( "/Login.action" );
        HttpServletRequest request = trip.getRequest();
        WikiSession wikiSession = SessionMonitor.getInstance( m_engine ).find( request.getSession() );
        assertNotSame( Users.JANNE, wikiSession.getLoginPrincipal().getName() );

        // Log in
        trip.setParameter( "j_username", Users.JANNE );
        trip.setParameter( "j_password", Users.JANNE_PASS );
        trip.setParameter( "redirect", "Foo" );
        trip.execute( "login" );

        // Verify we logged in correctly
        bean = trip.getActionBean( LoginActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( Users.JANNE, wikiSession.getLoginPrincipal().getName() );
        assertEquals( "/Wiki.jsp?page=Foo", trip.getDestination() );
    }

    public void testLoginRememberMe() throws Exception
    {
        MockRoundtrip trip;
        LoginActionBean bean;
        ValidationErrors errors;

        // Verify that the initial user is anonymous
        trip = m_engine.guestTrip( "/Login.action" );
        HttpServletRequest request = trip.getRequest();
        WikiSession wikiSession = SessionMonitor.getInstance( m_engine ).find( request.getSession() );
        assertNotSame( Users.JANNE, wikiSession.getLoginPrincipal().getName() );

        // Log in
        trip.setParameter( "j_username", Users.JANNE );
        trip.setParameter( "j_password", Users.JANNE_PASS );
        trip.setParameter( "j_remember", "true" );
        trip.execute( "login" );

        // Verify we logged in correctly
        bean = trip.getActionBean( LoginActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( Users.JANNE, wikiSession.getLoginPrincipal().getName() );
        assertEquals( "/Wiki.jsp", trip.getDestination() );

        // Should be two cookies (the JSPWiki asserted name cookie plus the
        // Remember Me? cookie)
        Cookie[] cookies = trip.getResponse().getCookies();
        assertEquals( 2, cookies.length );
    }

    public void testLogout() throws Exception
    {
        MockRoundtrip trip;
        LoginActionBean bean;
        ValidationErrors errors;

        // Start with an authenticated user
        trip = m_engine.authenticatedTrip( Users.JANNE, Users.JANNE_PASS, LoginActionBean.class );
        HttpServletRequest request = trip.getRequest();
        WikiSession wikiSession = SessionMonitor.getInstance( m_engine ).find( request.getSession() );
        assertEquals( Users.JANNE, wikiSession.getLoginPrincipal().getName() );

        // Log out
        trip.execute( "logout" );

        // Verify we logged out correctly
        bean = trip.getActionBean( LoginActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertNotSame( Users.JANNE, wikiSession.getLoginPrincipal().getName() );
        assertEquals( "/Wiki.jsp", trip.getDestination() );

        // Should be just one cookie (the JSPWiki asserted name cookie), and it
        // should be empty
        Cookie[] cookies = trip.getResponse().getCookies();
        assertEquals( 1, cookies.length );
        assertEquals( CookieAssertionLoginModule.PREFS_COOKIE_NAME, cookies[0].getName() );
        assertEquals( "", cookies[0].getValue() );
    }

}
