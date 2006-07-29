package com.ecyrd.jspwiki;

import java.security.Principal;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.Cookie;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.lang.ArrayUtils;

import com.ecyrd.jspwiki.auth.AuthenticationManager;
import com.ecyrd.jspwiki.auth.Users;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;

public class WikiSessionTest extends TestCase
{

    private WikiEngine m_engine = null;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        m_engine = new TestEngine( props );
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testRoles() throws Exception
    {
        WikiSession session;
        Principal[] principals;
        
        // Test roles for guest session
        session = WikiSession.guestSession( m_engine );
        principals = session.getRoles();
        assertTrue(  session.isAnonymous() );
        assertFalse( session.isAuthenticated() );
        assertTrue(  ArrayUtils.contains( principals, Role.ALL ) );
        assertTrue(  ArrayUtils.contains( principals, Role.ANONYMOUS ) );
        assertFalse( ArrayUtils.contains( principals, Role.ASSERTED ) );
        assertFalse( ArrayUtils.contains( principals, Role.AUTHENTICATED ) );
        
        // Test roles for anonymous session
        session = anonymousSession( m_engine );
        principals = session.getRoles();
        assertTrue(  session.isAnonymous() );
        assertFalse( session.isAuthenticated() );
        assertTrue(  ArrayUtils.contains( principals, Role.ALL ) );
        assertTrue(  ArrayUtils.contains( principals, Role.ANONYMOUS ) );
        assertFalse( ArrayUtils.contains( principals, Role.ASSERTED ) );
        assertFalse( ArrayUtils.contains( principals, Role.AUTHENTICATED ) );
        
        // Test roles for authenticated session
        session = authenticatedSession( m_engine, 
                Users.JANNE, 
                Users.JANNE_PASS );
        principals = session.getRoles();
        assertFalse( session.isAnonymous() );
        assertTrue(  session.isAuthenticated() );
        assertTrue(  ArrayUtils.contains( principals, Role.ALL ) );
        assertFalse( ArrayUtils.contains( principals, Role.ANONYMOUS ) );
        assertFalse( ArrayUtils.contains( principals, Role.ASSERTED ) );
        assertTrue(  ArrayUtils.contains( principals, Role.AUTHENTICATED ) );
        
        // Test roles for admin session
        session = adminSession( m_engine );
        principals = session.getRoles();
        assertFalse( session.isAnonymous() );
        assertTrue(  session.isAuthenticated() );
        assertTrue(  ArrayUtils.contains( principals, Role.ALL ) );
        assertFalse( ArrayUtils.contains( principals, Role.ANONYMOUS ) );
        assertFalse( ArrayUtils.contains( principals, Role.ASSERTED ) );
        assertTrue(  ArrayUtils.contains( principals, Role.AUTHENTICATED ) );
    }

    public void testIsIPAddress()
    {
        assertFalse( WikiSession.isIPV4Address( "Me" ) );
        assertFalse( WikiSession.isIPV4Address( "Guest" ) );
        assertTrue( WikiSession.isIPV4Address( "127.0.0.1" ) );
        assertFalse( WikiSession.isIPV4Address( "1207.0.0.1" ) );
        assertFalse( WikiSession.isIPV4Address( "127..0.1" ) );
        assertFalse( WikiSession.isIPV4Address( "1207.0.0." ) );
        assertFalse( WikiSession.isIPV4Address( ".0.0.1" ) );
        assertFalse( WikiSession.isIPV4Address( "..." ) );
    }
    
    public void testIsContainerStatusChanged()
    {
        TestHttpSession session = new TestHttpSession();
        TestHttpServletRequest request;
        WikiSession wikiSession;
        
        // A naked HTTP request without userPrincipal/remoteUser shouldn't count as changed
        request = new TestHttpServletRequest();
        request.setUserPrincipal( null );
        request.setRemoteUser( null );
        request.setRemoteAddr( "127.0.0.1" );
        request.m_session = session;
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertFalse( wikiSession.isContainerStatusChanged( request ) );
        
        // Let's send another request from a different IP address but
        // associated with the same HTTP session (improbable, I know...).
        // This request should also not count as changed...
        TestHttpServletRequest request2;
        WikiSession wikiSession2;
        request2 = new TestHttpServletRequest();
        request2.setUserPrincipal( null );
        request2.setRemoteUser( null );
        request2.setRemoteAddr( "127.1.1.1" );
        request2.m_session = session;
        wikiSession2 = WikiSession.getWikiSession( m_engine, request2 );
        assertFalse( wikiSession2.isContainerStatusChanged( request2 ) );
        
        // ...and the WikiSessions should be the same
        assertEquals( wikiSession, wikiSession2 );
        
        // Changing the UserPrincipal value should trigger a change...
        request = new TestHttpServletRequest();
        request.setUserPrincipal( new WikiPrincipal( "Fred Flintstone ") );
        request.setRemoteUser( null );
        request.setRemoteAddr( "127.0.0.1" );
        request.m_session = session;
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertTrue( wikiSession.isContainerStatusChanged( request ) );
        
        // ...but if the next request has the same UserPrincipal, it shouldn't.
        request = new TestHttpServletRequest();
        request.setUserPrincipal( new WikiPrincipal( "Fred Flintstone ") );
        request.setRemoteUser( null );
        request.setRemoteAddr( "127.0.0.1" );
        request.m_session = session;
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertFalse( wikiSession.isContainerStatusChanged( request ) );
        
        // If we twiddle the remoteUser field, it should trigger a change again...
        request = new TestHttpServletRequest();
        request.setUserPrincipal( new WikiPrincipal( "Fred Flintstone ") );
        request.setRemoteUser( "fred" );
        request.setRemoteAddr( "127.0.0.1" );
        request.m_session = session;
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertTrue( wikiSession.isContainerStatusChanged( request ) );
        
        // ...but not if we follow up with a similar request again.
        request = new TestHttpServletRequest();
        request.setUserPrincipal( new WikiPrincipal( "Fred Flintstone ") );
        request.setRemoteUser( "fred" );
        request.setRemoteAddr( "127.0.0.1" );
        request.m_session = session;
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertFalse( wikiSession.isContainerStatusChanged( request ) );
        
        // And finally, if we null the UserPrincipal and remoteUser again, 
        // it should not trigger a change.
        request = new TestHttpServletRequest();
        request.setUserPrincipal( null );
        request.setRemoteUser( null );
        request.setRemoteAddr( "127.0.0.1" );
        request.m_session = session;
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertFalse( wikiSession.isContainerStatusChanged( request ) );
        
        // Adding the magic "assertion cookie" should trigger a change in status.
        request = new TestHttpServletRequest();
        request.setUserPrincipal( null );
        request.setRemoteUser( null );
        request.setRemoteAddr( "127.0.0.1" );
        request.m_session = session;
        String cookieName = CookieAssertionLoginModule.PREFS_COOKIE_NAME;
        request.m_cookies = new Cookie[] { new Cookie( cookieName, "FredFlintstone" ) };
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertTrue( wikiSession.isContainerStatusChanged( request ) );
    }

    public void testGetStatus()
    {
    }
    
    /**
     * Creates an anonymous user session.
     * @param engine the wiki engine
     * @return the new session
     * @throws Exception
     */
    public static WikiSession anonymousSession( WikiEngine engine ) throws Exception
    {
        // Build anon session
        TestHttpServletRequest request = new TestHttpServletRequest();
        request.setRemoteAddr( "53.33.128.9" );
        
        // Log in
        boolean loggedIn = engine.getAuthenticationManager().login( request );
        if ( !loggedIn )
        {
            throw new IllegalStateException( "Couldn't set up anonymous user." );
        }
        
        WikiSession session = WikiSession.getWikiSession( engine, request );
        
        // Make sure the user is actually anonymous
        if ( !session.isAnonymous() )
        {
            throw new IllegalStateException( "Session is not anonymous." );
        }
        return session;
    }

    public static WikiSession assertedSession( WikiEngine engine, String name ) throws Exception
    {
        return assertedSession( engine, name, new Principal[0] );
    }
    
    public static WikiSession assertedSession( WikiEngine engine, String name, Principal[] roles ) throws Exception
    {
        // We can use cookies right?
        if ( !AuthenticationManager.allowsCookieAssertions() )
        {
            throw new IllegalStateException( "Couldn't set up asserted user: login config doesn't allow cookies." );
        }
        
        // Build anon session
        TestHttpServletRequest request = new TestHttpServletRequest();
        Set r = new HashSet();
        for ( int i = 0; i < roles.length; i++ )
        {
            r.add( roles[i].getName() );
        }
        request.setRoles( (String[])r.toArray( new String[r.size()]) );
        request.setRemoteAddr( "53.33.128.9" );
        
        // Set cookie
        Cookie cookie = new Cookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME, name );
        request.setCookies( new Cookie[] { cookie } );
        
        // Log in
        boolean loggedIn = engine.getAuthenticationManager().login( request );
        if ( !loggedIn )
        {
            throw new IllegalStateException( "Couldn't log in asserted user." );
        }
        
        WikiSession session = WikiSession.getWikiSession( engine, request );
        
        // Make sure the user is actually asserted
        if ( !session.hasPrincipal( Role.ASSERTED ) )
        {
            throw new IllegalStateException( "Didn't find Role.ASSERTED in session." );
        }
        return session;
    }
    
    public static WikiSession adminSession( WikiEngine engine ) throws Exception
    {
        return authenticatedSession( engine, Users.ADMIN, Users.ADMIN_PASS );
    }
    
    public static WikiSession authenticatedSession( WikiEngine engine, String id, String password ) throws Exception
    {
        // Build anon session
        TestHttpServletRequest request = new TestHttpServletRequest();
        request.setRemoteAddr( "53.33.128.9" );
        
        // Log in as anon
        boolean loggedIn = engine.getAuthenticationManager().login( request );
        if ( !loggedIn )
        {
            throw new IllegalStateException( "Couldn't log in anonymous user." );
        }
        
        WikiSession session = WikiSession.getWikiSession( engine, request );
        
        // Log in the user with credentials
        engine.getAuthenticationManager().login( session, id, password );
        
        // Make sure the user is actually authenticated
        if ( !session.isAuthenticated() )
        {
            throw new IllegalStateException( "Could not log in authenticated user '" + id + "'" );
        }
        return session;
    }
    
    public static WikiSession containerAuthenticatedSession( WikiEngine engine, String id, Principal[] roles ) throws Exception
    {
        // Build container session
        TestHttpServletRequest request = new TestHttpServletRequest();
        Set r = new HashSet();
        for ( int i = 0; i < roles.length; i++ )
        {
            r.add( roles[i].getName() );
        }
        request.setRoles( (String[])r.toArray( new String[r.size()]) );
        request.setRemoteAddr( "53.33.128.9" );
        request.setUserPrincipal( new WikiPrincipal( id ) );
        
        // Log in as anon
        boolean loggedIn = engine.getAuthenticationManager().login( request );
        if ( !loggedIn )
        {
            throw new IllegalStateException( "Couldn't log in anonymous user." );
        }
        
        WikiSession session = WikiSession.getWikiSession( engine, request );
        
        // Log in the user with credentials
        engine.getAuthenticationManager().login( request );
        
        // Make sure the user is actually authenticated
        if ( !session.isAuthenticated() )
        {
            throw new IllegalStateException( "Could not log in authenticated user '" + id + "'" );
        }
        return session;
    }

    public static Test suite() 
    {
        return new TestSuite( WikiSessionTest.class );
    }

}
