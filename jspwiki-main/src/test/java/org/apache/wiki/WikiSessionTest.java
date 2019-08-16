/*
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
package org.apache.wiki;
import net.sourceforge.stripes.mock.MockFilterChain;
import net.sourceforge.stripes.mock.MockFilterConfig;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import net.sourceforge.stripes.mock.MockServletContext;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.Users;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.login.CookieAssertionLoginModule;
import org.apache.wiki.auth.login.CookieAuthenticationLoginModule;
import org.apache.wiki.ui.WikiServletFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class WikiSessionTest
{

    private TestEngine m_engine = null;

    @BeforeEach
    public void setUp() throws Exception
    {
        Properties props = TestEngine.getTestProperties();
        m_engine = new TestEngine( props );
    }

    @Test
    public void testRoles() throws Exception
    {
        WikiSession session;
        Principal[] principals;

        // Test roles for guest session
        session = WikiSession.guestSession( m_engine );
        principals = session.getRoles();
        Assertions.assertTrue(  session.isAnonymous() );
        Assertions.assertFalse( session.isAuthenticated() );
        Assertions.assertTrue(  ArrayUtils.contains( principals, Role.ALL ) );
        Assertions.assertTrue(  ArrayUtils.contains( principals, Role.ANONYMOUS ) );
        Assertions.assertFalse( ArrayUtils.contains( principals, Role.ASSERTED ) );
        Assertions.assertFalse( ArrayUtils.contains( principals, Role.AUTHENTICATED ) );

        // Test roles for anonymous session

        session = anonymousSession( m_engine );
        principals = session.getRoles();
        Assertions.assertTrue(  session.isAnonymous() );
        Assertions.assertFalse( session.isAuthenticated() );
        Assertions.assertTrue(  ArrayUtils.contains( principals, Role.ALL ) );
        Assertions.assertTrue(  ArrayUtils.contains( principals, Role.ANONYMOUS ) );
        Assertions.assertFalse( ArrayUtils.contains( principals, Role.ASSERTED ) );
        Assertions.assertFalse( ArrayUtils.contains( principals, Role.AUTHENTICATED ) );

        // Test roles for authenticated session
        session = authenticatedSession( m_engine,
                                        Users.JANNE,
                                        Users.JANNE_PASS );
        principals = session.getRoles();
        Assertions.assertFalse( session.isAnonymous() );
        Assertions.assertTrue(  session.isAuthenticated() );
        Assertions.assertTrue(  ArrayUtils.contains( principals, Role.ALL ) );
        Assertions.assertFalse( ArrayUtils.contains( principals, Role.ANONYMOUS ) );
        Assertions.assertFalse( ArrayUtils.contains( principals, Role.ASSERTED ) );
        Assertions.assertTrue(  ArrayUtils.contains( principals, Role.AUTHENTICATED ) );

        // Test roles for admin session
        session = adminSession( m_engine );
        principals = session.getRoles();
        Assertions.assertFalse( session.isAnonymous() );
        Assertions.assertTrue(  session.isAuthenticated() );
        Assertions.assertTrue(  ArrayUtils.contains( principals, Role.ALL ) );
        Assertions.assertFalse( ArrayUtils.contains( principals, Role.ANONYMOUS ) );
        Assertions.assertFalse( ArrayUtils.contains( principals, Role.ASSERTED ) );
        Assertions.assertTrue(  ArrayUtils.contains( principals, Role.AUTHENTICATED ) );
    }

    @Test
    public void testIsIPAddress()
    {
        Assertions.assertFalse( WikiSession.isIPV4Address( "Me" ) );
        Assertions.assertFalse( WikiSession.isIPV4Address( "Guest" ) );
        Assertions.assertTrue( WikiSession.isIPV4Address( "127.0.0.1" ) );
        Assertions.assertFalse( WikiSession.isIPV4Address( "1207.0.0.1" ) );
        Assertions.assertFalse( WikiSession.isIPV4Address( "127..0.1" ) );
        Assertions.assertFalse( WikiSession.isIPV4Address( "1207.0.0." ) );
        Assertions.assertFalse( WikiSession.isIPV4Address( ".0.0.1" ) );
        Assertions.assertFalse( WikiSession.isIPV4Address( "..." ) );
    }

    @Test
    public void testIPAddress() throws ServletException, IOException
    {
        MockHttpServletRequest request;
        WikiSession wikiSession;

        // A naked HTTP request without userPrincipal/remoteUser should be anonymous
        request = m_engine.newHttpRequest();
        request.setUserPrincipal( null );
        runSecurityFilter(m_engine, request);
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        Assertions.assertTrue( wikiSession.isAnonymous());
    }

    @Test
    public void testUserPrincipal() throws ServletException, IOException
    {
        MockHttpServletRequest request;
        WikiSession wikiSession;

        // Changing the UserPrincipal value should cause the user to be authenticated...
        request = m_engine.newHttpRequest();
        request.setUserPrincipal( new WikiPrincipal( "Fred Flintstone") );
        runSecurityFilter(m_engine, request);
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        Assertions.assertTrue( wikiSession.isAuthenticated());
        Assertions.assertEquals( "Fred Flintstone", wikiSession.getUserPrincipal().getName() );
    }

    @Test
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
        Assertions.assertTrue( wikiSession.isAsserted());
        Assertions.assertEquals( "FredFlintstone", wikiSession.getUserPrincipal().getName() );
    }

    @Test
    public void testAuthenticationCookieDefaults() throws ServletException, IOException
    {
        MockHttpServletRequest request;
        WikiSession wikiSession;

        // Set the authentication cookie first
        MockHttpServletResponse response = new MockHttpServletResponse();
        CookieAuthenticationLoginModule.setLoginCookie( m_engine, response, "Fred Flintstone" );
        Cookie[] cookies = response.getCookies();
        Assertions.assertEquals(1, cookies.length);
        String uid = cookies[0].getValue();

        // Adding the magic "authentication cookie" should NOT count as authenticated in the default case
        // (because cookie authentication is OFF).
        request = m_engine.newHttpRequest();
        request.setUserPrincipal( null );
        request.setCookies( new Cookie[] { new Cookie( "JSPWikiUID", uid ) } );
        runSecurityFilter(m_engine, request);
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        Assertions.assertTrue( wikiSession.isAnonymous());
        Assertions.assertFalse( wikiSession.isAuthenticated());
        Assertions.assertEquals( "127.0.0.1", wikiSession.getUserPrincipal().getName() );

        // Clear the authentication cookie
        response = new MockHttpServletResponse();
        CookieAuthenticationLoginModule.clearLoginCookie( m_engine, request, response );
    }

    @Test
    public void testAuthenticationCookieWhenOn() throws WikiException, ServletException, IOException
    {
        Properties props = TestEngine.getTestProperties();
        props.setProperty( AuthenticationManager.PROP_ALLOW_COOKIE_AUTH, "true");
        m_engine = new TestEngine( props );

        MockHttpServletRequest request;
        WikiSession wikiSession;

        // Set the authentication cookie first
        MockHttpServletResponse response = new MockHttpServletResponse();
        CookieAuthenticationLoginModule.setLoginCookie( m_engine, response, "Fred Flintstone" );
        Cookie[] cookies = response.getCookies();
        Assertions.assertEquals(1, cookies.length);
        String uid = cookies[0].getValue();

        // Adding the magic "authentication cookie" should count as authenticated
        request = m_engine.newHttpRequest();
        request.setUserPrincipal( null );
        request.setCookies( new Cookie[] { new Cookie( "JSPWikiUID", uid ) } );
        runSecurityFilter(m_engine, request);
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        Assertions.assertFalse( wikiSession.isAnonymous());
        Assertions.assertTrue( wikiSession.isAuthenticated());
        Assertions.assertEquals( "Fred Flintstone", wikiSession.getUserPrincipal().getName() );

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
        engine.getAuthenticationManager().login( session, request, id, password );

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
        servletCtx.setAttribute( "org.apache.wiki.WikiEngine", engine );

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

}
