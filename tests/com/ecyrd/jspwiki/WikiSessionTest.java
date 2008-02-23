package com.ecyrd.jspwiki;

import java.security.Principal;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.Cookie;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.mock.MockRoundtrip;

import org.apache.commons.lang.ArrayUtils;

import com.ecyrd.jspwiki.action.ViewActionBean;
import com.ecyrd.jspwiki.auth.AuthenticationManager;
import com.ecyrd.jspwiki.auth.Users;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;

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
    
    public void testIsContainerStatusChanged()
    {
        MockRoundtrip trip;
        MockHttpSession session;
        MockHttpServletRequest request;
        WikiSession wikiSession;
        String servletContext;
        
        // A naked HTTP request without userPrincipal/remoteUser shouldn't count as changed
        trip = m_engine.guestTrip( ViewActionBean.class );
        request = trip.getRequest();
        session = (MockHttpSession)request.getSession();
        servletContext = "/" + m_engine.getServletContext().getServletContextName();
        request.setUserPrincipal( null );
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        assertFalse( wikiSession.isContainerStatusChanged( request ) );
        
        // Changing the UserPrincipal value should trigger a change...
        request = new MockHttpServletRequest(servletContext, ViewActionBean.class.getAnnotation(UrlBinding.class).value());
        request.setSession(session);
        request.setUserPrincipal( new WikiPrincipal( "Fred Flintstone") );
        assertTrue( wikiSession.isContainerStatusChanged( request ) );
        
        // ...but if the next request has the same UserPrincipal, it shouldn't.
        request = new MockHttpServletRequest(servletContext, ViewActionBean.class.getAnnotation(UrlBinding.class).value());
        request.setSession(session);
        request = m_engine.guestTrip( ViewActionBean.class ).getRequest();
        request.setUserPrincipal( new WikiPrincipal( "Fred Flintstone") );
        assertFalse( wikiSession.isContainerStatusChanged( request ) );
        
        // If we twiddle the remoteUser field, it should trigger a change again...
        request = new MockHttpServletRequest(servletContext, ViewActionBean.class.getAnnotation(UrlBinding.class).value());
        request.setSession(session);
        request.setUserPrincipal( new WikiPrincipal( "Fred") );
        assertTrue( wikiSession.isContainerStatusChanged( request ) );
        
        // ...but not if we follow up with a similar request again.
        request = new MockHttpServletRequest(servletContext, ViewActionBean.class.getAnnotation(UrlBinding.class).value());
        request.setSession(session);
        request.setUserPrincipal( new WikiPrincipal( "Fred") );
        assertFalse( wikiSession.isContainerStatusChanged( request ) );
        
        // And finally, if we null the UserPrincipal again, 
        // it should not trigger a change.
        request = new MockHttpServletRequest(servletContext, ViewActionBean.class.getAnnotation(UrlBinding.class).value());
        request.setSession(session);
        request.setUserPrincipal( null );
        assertFalse( wikiSession.isContainerStatusChanged( request ) );
        
        // Adding the magic "assertion cookie" should trigger a change in status.
        request = new MockHttpServletRequest(servletContext, ViewActionBean.class.getAnnotation(UrlBinding.class).value());
        request.setSession(session);
        request.setUserPrincipal( null );
        String cookieName = CookieAssertionLoginModule.PREFS_COOKIE_NAME;
        request.setCookies( new Cookie[] { new Cookie( cookieName, "FredFlintstone" ) });
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
    public static WikiSession anonymousSession( TestEngine engine ) throws Exception
    {
        // Build anon session
        MockHttpServletRequest request = engine.guestTrip( ViewActionBean.class ).getRequest();
        
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

    public static WikiSession assertedSession( TestEngine engine, String name ) throws Exception
    {
        return assertedSession( engine, name, new Principal[0] );
    }
    
    public static WikiSession assertedSession( TestEngine engine, String name, Principal[] roles ) throws Exception
    {
        // We can use cookies right?
        if ( !AuthenticationManager.allowsCookieAssertions() )
        {
            throw new IllegalStateException( "Couldn't set up asserted user: login config doesn't allow cookies." );
        }
        
        // Build anon session
        MockHttpServletRequest request = engine.guestTrip( ViewActionBean.class ).getRequest();
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
    
    public static WikiSession adminSession( TestEngine engine ) throws Exception
    {
        return authenticatedSession( engine, Users.ADMIN, Users.ADMIN_PASS );
    }
    
    public static WikiSession authenticatedSession( TestEngine engine, String id, String password ) throws Exception
    {
        // Build anon session
        MockHttpServletRequest request = engine.guestTrip( ViewActionBean.class ).getRequest();
        
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
    
    public static WikiSession containerAuthenticatedSession( TestEngine engine, String id, Principal[] roles ) throws Exception
    {
        // Build container session
        MockHttpServletRequest request = engine.guestTrip( ViewActionBean.class ).getRequest();
        Set<String> r = new HashSet<String>();
        for ( int i = 0; i < roles.length; i++ )
        {
            r.add( roles[i].getName() );
        }
        request.setRoles( r );
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
