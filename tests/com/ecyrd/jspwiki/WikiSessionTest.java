package com.ecyrd.jspwiki;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.lang.ArrayUtils;

import com.ecyrd.jspwiki.auth.AuthenticationManager;
import com.ecyrd.jspwiki.auth.Users;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;
import com.ecyrd.jspwiki.auth.login.CookieAuthenticationLoginModule;
import com.ecyrd.jspwiki.ui.WikiServletFilter;

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
    
    public void testIPAddress() throws ServletException, IOException
    {
        TestHttpSession session = new TestHttpSession();
        TestHttpServletRequest request;
        WikiSession wikiSession;
        
        // A naked HTTP request without userPrincipal/remoteUser should be anonymous
        request = new TestHttpServletRequest();
        request.setUserPrincipal( null );
        request.setRemoteUser( null );
        request.setRemoteAddr( "127.0.0.1" );
        request.m_session = session;
        runSecurityFilter(m_engine, request);
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertTrue( wikiSession.isAnonymous());
    }
    
    public void testUserPrincipal() throws ServletException, IOException
    {
        TestHttpSession session = new TestHttpSession();
        TestHttpServletRequest request;
        WikiSession wikiSession;
        
        // Changing the UserPrincipal value should cause the user to be authenticated...
        request = new TestHttpServletRequest();
        request.setUserPrincipal( new WikiPrincipal( "Fred Flintstone") );
        request.setRemoteUser( null );
        request.setRemoteAddr( "127.0.0.1" );
        request.m_session = session;
        runSecurityFilter(m_engine, request);
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertTrue( wikiSession.isAuthenticated());
        assertEquals( "Fred Flintstone", wikiSession.getUserPrincipal().getName() );
    }
        
    public void testRemoteUser() throws ServletException, IOException
    {
        TestHttpSession session = new TestHttpSession();
        TestHttpServletRequest request;
        WikiSession wikiSession;
            
        // If we set the remoteUser field is set, that's what will count as authenticated
        request = new TestHttpServletRequest();
        request.setRemoteUser( "fred" );
        request.setRemoteAddr( "127.0.0.1" );
        request.m_session = session;
        runSecurityFilter(m_engine, request);
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertTrue( wikiSession.isAuthenticated());
        assertEquals( "fred", wikiSession.getUserPrincipal().getName() );
    }
        
    public void testUserPrincipalAndRemoteUser() throws ServletException, IOException
    {
        TestHttpSession session = new TestHttpSession();
        TestHttpServletRequest request;
        WikiSession wikiSession;
        
        // If we twiddle the remoteUser field too, it should still prefer the UserPrincipal value...
        request = new TestHttpServletRequest();
        request.setUserPrincipal( new WikiPrincipal( "Fred Flintstone") );
        request.setRemoteUser( "fred" );
        request.setRemoteAddr( "127.0.0.1" );
        request.m_session = session;
        runSecurityFilter(m_engine, request);
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertTrue( wikiSession.isAuthenticated());
        assertEquals( "Fred Flintstone", wikiSession.getUserPrincipal().getName() );
    }
        
    public void testAssertionCookie() throws ServletException, IOException
    {
        TestHttpSession session = new TestHttpSession();
        TestHttpServletRequest request;
        WikiSession wikiSession;
        
        // Adding the magic "assertion cookie" should  set asserted status.
        request = new TestHttpServletRequest();
        request.setUserPrincipal( null );
        request.setRemoteUser( null );
        request.setRemoteAddr( "127.0.0.1" );
        request.m_session = session;
        String cookieName = CookieAssertionLoginModule.PREFS_COOKIE_NAME;
        request.m_cookies = new Cookie[] { new Cookie( cookieName, "FredFlintstone" ) };
        runSecurityFilter(m_engine, request);
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertTrue( wikiSession.isAsserted());
        assertEquals( "FredFlintstone", wikiSession.getUserPrincipal().getName() );
    }

    public void testAuthenticationCookieDefaults() throws ServletException, IOException
    {
        TestHttpSession session = new TestHttpSession();
        TestHttpServletRequest request;
        WikiSession wikiSession;
        
        // Set the authentication cookie first
        TestHttpServletResponse response = new TestHttpServletResponse();
        CookieAuthenticationLoginModule.setLoginCookie( m_engine, response, "Fred Flintstone" );
        Cookie[] cookies = response.getCookies();
        assertEquals(1, cookies.length);
        String uid = cookies[0].getValue();
        
        // Adding the magic "authentication cookie" should NOT count as authenticated in the default case
        // (because cookie authentication is OFF).
        request = new TestHttpServletRequest();
        request.setUserPrincipal( null );
        request.setRemoteUser( null );
        request.setRemoteAddr( "127.0.0.1" );
        request.m_session = session;
        request.m_cookies = new Cookie[] { new Cookie( "JSPWikiUID", uid ) };
        runSecurityFilter(m_engine, request);
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertTrue( wikiSession.isAnonymous());
        assertFalse( wikiSession.isAuthenticated());
        assertEquals( "127.0.0.1", wikiSession.getUserPrincipal().getName() );
        
        // Clear the authentication cookie
        response = new TestHttpServletResponse();
        CookieAuthenticationLoginModule.clearLoginCookie( m_engine, request, response );
    }
    
    public void testAuthenticationCookieWhenOn() throws WikiException, ServletException, IOException
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        props.setProperty( AuthenticationManager.PROP_ALLOW_COOKIE_AUTH, "true");
        m_engine = new TestEngine( props );
        
        TestHttpSession session = new TestHttpSession();
        TestHttpServletRequest request;
        WikiSession wikiSession;
        
        // Set the authentication cookie first
        TestHttpServletResponse response = new TestHttpServletResponse();
        CookieAuthenticationLoginModule.setLoginCookie( m_engine, response, "Fred Flintstone" );
        Cookie[] cookies = response.getCookies();
        assertEquals(1, cookies.length);
        String uid = cookies[0].getValue();
        
        // Adding the magic "authentication cookie" should count as authenticated
        request = new TestHttpServletRequest();
        request.setUserPrincipal( null );
        request.setRemoteUser( null );
        request.setRemoteAddr( "127.0.0.1" );
        request.m_session = session;
        request.m_cookies = new Cookie[] { new Cookie( "JSPWikiUID", uid ) };
        runSecurityFilter(m_engine, request);
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertFalse( wikiSession.isAnonymous());
        assertTrue( wikiSession.isAuthenticated());
        assertEquals( "Fred Flintstone", wikiSession.getUserPrincipal().getName() );
        
        // Clear the authentication cookie
        response = new TestHttpServletResponse();
        CookieAuthenticationLoginModule.clearLoginCookie( m_engine, request, response );
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
        runSecurityFilter(engine, request);
        
        // Make sure the user is actually anonymous
        WikiSession session = WikiSession.getWikiSession( engine, request );
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
        if ( !engine.getAuthenticationManager().allowsCookieAssertions() )
        {
            throw new IllegalStateException( "Couldn't set up asserted user: login config doesn't allow cookies." );
        }
        
        // Build anon session
        TestHttpServletRequest request = new TestHttpServletRequest();
        Set<String> r = new HashSet<String>();
        for ( int i = 0; i < roles.length; i++ )
        {
            r.add( roles[i].getName() );
        }
        request.setRoles( r.toArray( new String[r.size()]) );
        request.setRemoteAddr( "53.33.128.9" );
        
        // Set cookie
        Cookie cookie = new Cookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME, name );
        request.setCookies( new Cookie[] { cookie } );
        
        // Log in
        runSecurityFilter(engine, request);
        
        // Make sure the user is actually asserted
        WikiSession session = WikiSession.getWikiSession( engine, request );
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
        runSecurityFilter(engine, request);
        
        // Log in the user with credentials
        WikiSession session = WikiSession.getWikiSession( engine, request );
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
        Set<String> r = new HashSet<String>();
        for ( int i = 0; i < roles.length; i++ )
        {
            r.add( roles[i].getName() );
        }
        request.setRoles( r.toArray( new String[r.size()]) );
        request.setRemoteAddr( "53.33.128.9" );
        request.setUserPrincipal( new WikiPrincipal( id ) );
        
        // Log in
        runSecurityFilter(engine,request);
        
        // Make sure the user is actually authenticated
        WikiSession session = WikiSession.getWikiSession( engine, request );
        if ( !session.isAuthenticated() )
        {
            throw new IllegalStateException( "Could not log in authenticated user '" + id + "'" );
        }
        return session;
    }
    
    private static void runSecurityFilter(WikiEngine engine, HttpServletRequest request) throws ServletException, IOException
    {
        Filter filter = new WikiServletFilter();
        FilterConfig filterConfig = new TestFilterConfig(new TestServletContext(engine));
        filter.init(filterConfig);
        filter.doFilter(request, null, new TestFilterChain());
    }

    public static Test suite() 
    {
        return new TestSuite( WikiSessionTest.class );
    }

}
