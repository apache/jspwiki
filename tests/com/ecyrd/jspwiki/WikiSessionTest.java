package com.ecyrd.jspwiki;

import java.util.Properties;
import java.util.Set;

import javax.servlet.http.Cookie;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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

    public void testIsAuthenticated()
    {
        WikiSession session = WikiSession.guestSession( m_engine );
        Set principals = session.getSubject().getPrincipals();
        assertFalse( session.isAuthenticated() );
        
        principals.remove( Role.ANONYMOUS );
        principals.add( new WikiPrincipal( "Janne" ) );
        principals.add( Role.ASSERTED );
        assertFalse( session.isAuthenticated() );
        
        principals.remove( Role.ANONYMOUS );
        principals.remove( Role.ASSERTED );
        principals.add( Role.AUTHENTICATED );
        assertTrue( session.isAuthenticated() );
    }

    public void testIsAnonymous()
    {
        WikiSession session = WikiSession.guestSession( m_engine );
        Set principals = session.getSubject().getPrincipals();
        assertTrue( session.isAnonymous() );
        
        principals.remove( WikiPrincipal.GUEST );
        principals.remove( Role.ANONYMOUS );
        principals.add( new WikiPrincipal( "Janne" ) );
        principals.add( Role.ASSERTED );
        assertFalse( session.isAnonymous() );
        
        principals.remove( Role.ANONYMOUS );
        principals.remove( Role.ASSERTED );
        principals.add( Role.AUTHENTICATED );
        assertFalse( session.isAnonymous() );
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
    
    public static Test suite() 
    {
        return new TestSuite( WikiSessionTest.class );
    }

}
