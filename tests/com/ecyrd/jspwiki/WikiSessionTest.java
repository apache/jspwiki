package com.ecyrd.jspwiki;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.sourceforge.stripes.mock.*;

import org.apache.commons.lang.ArrayUtils;
import org.apache.jspwiki.api.WikiException;

import com.ecyrd.jspwiki.auth.AuthenticationManager;
import com.ecyrd.jspwiki.auth.Users;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;
import com.ecyrd.jspwiki.auth.login.CookieAuthenticationLoginModule;
import com.ecyrd.jspwiki.ui.WikiServletFilter;

public class WikiSessionTest extends TestCase
{

    private TestEngine m_engine = null;
    
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
        MockHttpServletRequest request;
        WikiSession wikiSession;
        
        // A naked HTTP request without userPrincipal/remoteUser should be anonymous
        request = m_engine.newHttpRequest();
        request.setUserPrincipal( null );
        runSecurityFilter(m_engine, request);
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertTrue( wikiSession.isAnonymous());
    }
    
    public void testUserPrincipal() throws ServletException, IOException
    {
        MockHttpServletRequest request;
        WikiSession wikiSession;
        
        // Changing the UserPrincipal value should cause the user to be authenticated...
        request = m_engine.newHttpRequest();
        request.setUserPrincipal( new WikiPrincipal( "Fred Flintstone") );
        runSecurityFilter(m_engine, request);
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertTrue( wikiSession.isAuthenticated());
        assertEquals( "Fred Flintstone", wikiSession.getUserPrincipal().getName() );
    }
        
    public void testAssertionCookie() throws ServletException, IOException
    {
        MockHttpServletRequest request;
        WikiSession wikiSession;
        
        // Adding the magic "assertion cookie" should  set asserted status.
        request = m_engine.newHttpRequest();
        request.setUserPrincipal( null );
        String cookieName = CookieAssertionLoginModule.PREFS_COOKIE_NAME;
        request.setCookies( new Cookie[] { new Cookie( cookieName, "FredFlintstone" ) } );
        runSecurityFilter(m_engine, request);
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertTrue( wikiSession.isAsserted());
        assertEquals( "FredFlintstone", wikiSession.getUserPrincipal().getName() );
    }

    public void testAuthenticationCookieDefaults() throws ServletException, IOException
    {
        MockHttpServletRequest request;
        WikiSession wikiSession;
        
        // Set the authentication cookie first
        MockHttpServletResponse response = new MockHttpServletResponse();
        CookieAuthenticationLoginModule.setLoginCookie( m_engine, response, "Fred Flintstone" );
        Cookie[] cookies = response.getCookies();
        assertEquals(1, cookies.length);
        String uid = cookies[0].getValue();
        
        // Adding the magic "authentication cookie" should NOT count as authenticated in the default case
        // (because cookie authentication is OFF).
        request = m_engine.newHttpRequest();
        request.setUserPrincipal( null );
        request.setCookies( new Cookie[] { new Cookie( "JSPWikiUID", uid ) } );
        runSecurityFilter(m_engine, request);
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertTrue( wikiSession.isAnonymous());
        assertFalse( wikiSession.isAuthenticated());
        assertEquals( "127.0.0.1", wikiSession.getUserPrincipal().getName() );
        
        // Clear the authentication cookie
        response = new MockHttpServletResponse();
        CookieAuthenticationLoginModule.clearLoginCookie( m_engine, request, response );
    }
    
    public void testAuthenticationCookieWhenOn() throws WikiException, ServletException, IOException
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        props.setProperty( AuthenticationManager.PROP_ALLOW_COOKIE_AUTH, "true");
        m_engine = new TestEngine( props );
        
        MockHttpServletRequest request;
        WikiSession wikiSession;
        
        // Set the authentication cookie first
        MockHttpServletResponse response = new MockHttpServletResponse();
        CookieAuthenticationLoginModule.setLoginCookie( m_engine, response, "Fred Flintstone" );
        Cookie[] cookies = response.getCookies();
        assertEquals(1, cookies.length);
        String uid = cookies[0].getValue();
        
        // Adding the magic "authentication cookie" should count as authenticated
        request = m_engine.newHttpRequest();
        request.setUserPrincipal( null );
        request.setCookies( new Cookie[] { new Cookie( "JSPWikiUID", uid ) } );
        runSecurityFilter(m_engine, request);
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertFalse( wikiSession.isAnonymous());
        assertTrue( wikiSession.isAuthenticated());
        assertEquals( "Fred Flintstone", wikiSession.getUserPrincipal().getName() );
        
        // Clear the authentication cookie
        response = new MockHttpServletResponse();
        CookieAuthenticationLoginModule.clearLoginCookie( m_engine, request, response );
    }
    
    /**
     * Creates an anonymous user session.
     * @param engine the wiki engine
     * @return the new session
     * @throws Exception
     */
    public static WikiSession anonymousSession( TestEngine engine ) throws Exception
    {
        // Build anon session
        MockHttpServletRequest request = engine.newHttpRequest();
        
        // Log in
        runSecurityFilter( engine, request );
        
        // Make sure the user is actually anonymous
        WikiSession session = WikiSession.getWikiSession( engine, request );
        if ( !session.isAnonymous() )
        {
            throw new IllegalStateException( "Session is not anonymous." );
        }
        return session;
    }

    public static WikiSession assertedSession( TestEngine engine, String name ) throws Exception
    {
        return assertedSession( engine, name, new Principal[0] );
    }
    
    public static WikiSession assertedSession( TestEngine engine, String name, Principal[] roles ) throws Exception
    {
        // We can use cookies right?
        if ( !engine.getAuthenticationManager().allowsCookieAssertions() )
        {
            throw new IllegalStateException( "Couldn't set up asserted user: login config doesn't allow cookies." );
        }
        
        // Build anon session
        MockHttpServletRequest request = engine.newHttpRequest();
        Set<String> r = new HashSet<String>();
        for ( int i = 0; i < roles.length; i++ )
        {
            r.add( roles[i].getName() );
        }
        request.setRoles( r );
        
        // Set cookie
        Cookie cookie = new Cookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME, name );
        request.setCookies( new Cookie[] { cookie } );
        
        // Log in
        runSecurityFilter(engine, request);
        
        // Make sure the user is actually asserted
        WikiSession session = WikiSession.getWikiSession( engine, request );
        return session;
    }
    
    public static WikiSession adminSession( TestEngine engine ) throws Exception
    {
        return authenticatedSession( engine, Users.ADMIN, Users.ADMIN_PASS );
    }
    
    public static WikiSession authenticatedSession( TestEngine engine, String id, String password ) throws Exception
    {
        // Build anon session
        MockHttpServletRequest request = engine.newHttpRequest();
        
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
    
    public static WikiSession containerAuthenticatedSession( TestEngine engine, String id, Principal[] roles ) throws Exception
    {
        // Build container session
        MockHttpServletRequest request = engine.newHttpRequest();
        Set<String> r = new HashSet<String>();
        for ( int i = 0; i < roles.length; i++ )
        {
            r.add( roles[i].getName() );
        }
        request.setRoles( r );
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
    
    /**
     * "Scaffolding" method that runs the session security filter on a mock request. We do this by creating a
     * complete mock servlet context and filter chain, and running the request through it. 
     * @param engine the wiki engine
     * @param request the mock request to pass itnto the 
     * @throws ServletException
     * @throws IOException
     */
    private static void runSecurityFilter(WikiEngine engine, HttpServletRequest request) throws ServletException, IOException
    {
        // Create a mock servlet context and stash the wiki engine in it
        ServletContext servletCtx = new MockServletContext( "JSPWiki" );
        servletCtx.setAttribute( "com.ecyrd.jspwiki.WikiEngine", engine );
        
        // Create a mock filter configuration and add the servlet context we just created
        MockFilterConfig filterConfig = new MockFilterConfig();
        filterConfig.setFilterName( "WikiServletFilter" );
        filterConfig.setServletContext( servletCtx );
        
        // Create the security filter and run the request  through it
        Filter filter = new WikiServletFilter();
        MockFilterChain chain = new MockFilterChain();
        chain.addFilter( filter );
        Servlet servlet = new MockServlet();
        chain.setServlet( servlet );
        filter.init(filterConfig);
        filter.doFilter(request, null, chain );
    }

    private static class MockServlet implements Servlet
    {
        private ServletConfig m_config;
        
        public void destroy() { }

        public ServletConfig getServletConfig()
        {
            return m_config;
        }

        public String getServletInfo()
        {
            return "Mock servlet";
        }

        public void init( ServletConfig config ) throws ServletException
        {
            m_config = config;
        }

        public void service( ServletRequest request, ServletResponse response ) throws ServletException, IOException { }
    }
    
    public static Test suite() 
    {
        return new TestSuite( WikiSessionTest.class );
    }

}
