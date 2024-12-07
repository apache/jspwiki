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

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.Users;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.login.CookieAssertionLoginModule;
import org.apache.wiki.auth.login.CookieAuthenticationLoginModule;
import org.apache.wiki.ui.WikiServletFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class WikiSessionTest {

    static TestEngine m_engine = TestEngine.build();

    @Test
    void testRoles() throws Exception {
        Session session;
        Principal[] principals;

        // Test roles for guest session
        session = Wiki.session().guest( m_engine );
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
    void testIPAddress() throws ServletException, IOException {
        final HttpServletRequest request;
        final Session wikiSession;

        // A naked HTTP request without userPrincipal/remoteUser should be anonymous
        request = HttpMockFactory.createHttpRequest();
        Mockito.doReturn( null ).when( request ).getUserPrincipal();
        runSecurityFilter( m_engine, request );
        wikiSession = Wiki.session().find( m_engine, request );
        Assertions.assertTrue( wikiSession.isAnonymous());
    }

    @Test
    void testUserPrincipal() throws ServletException, IOException {
        final HttpServletRequest request;
        final Session wikiSession;

        // Changing the UserPrincipal value should cause the user to be authenticated...
        request = HttpMockFactory.createHttpRequest();
        Mockito.doReturn( new WikiPrincipal( "Fred Flintstone") ).when( request ).getUserPrincipal();
        runSecurityFilter( m_engine, request );
        wikiSession = Wiki.session().find( m_engine, request );
        Assertions.assertTrue( wikiSession.isAuthenticated());
        Assertions.assertEquals( "Fred Flintstone", wikiSession.getUserPrincipal().getName() );
    }

    @Test
    void testAssertionCookie() throws ServletException, IOException {
        final HttpServletRequest request;
        final Session wikiSession;

        // Adding the magic "assertion cookie" should  set asserted status.
        request = HttpMockFactory.createHttpRequest();
        Mockito.doReturn( null ).when( request ).getUserPrincipal();
        final String cookieName = CookieAssertionLoginModule.PREFS_COOKIE_NAME;
        Mockito.doReturn( new Cookie[] { new Cookie( cookieName, "FredFlintstone" ) } ).when( request ).getCookies();
        runSecurityFilter( m_engine, request );
        wikiSession = Wiki.session().find( m_engine, request );
        Assertions.assertTrue( wikiSession.isAsserted());
        Assertions.assertEquals( "FredFlintstone", wikiSession.getUserPrincipal().getName() );
    }

    @Test
    void testAuthenticationCookieDefaults() throws ServletException, IOException, WikiException {
        final HttpServletRequest request;
        final Session wikiSession;
        m_engine = new TestEngine( TestEngine.getTestProperties() );

        // Set the authentication cookie first
        HttpServletResponse response = HttpMockFactory.createHttpResponse();
        CookieAuthenticationLoginModule.setLoginCookie( m_engine, response, "Fred Flintstone" );
        final ArgumentCaptor< Cookie > captor = ArgumentCaptor.forClass( Cookie.class );
        Mockito.verify( response ).addCookie( captor.capture() );
        final String uid = captor.getValue().getValue();

        // Adding the magic "authentication cookie" should NOT count as authenticated in the default case
        // (because cookie authentication is OFF).
        request = HttpMockFactory.createHttpRequest();
        Mockito.doReturn( null ).when( request ).getUserPrincipal();
        Mockito.doReturn( new Cookie[] { new Cookie( "JSPWikiUID", uid ) } ).when( request ).getCookies();
        runSecurityFilter( m_engine, request );
        wikiSession = Wiki.session().find( m_engine, request );
        Assertions.assertTrue( wikiSession.isAnonymous());
        Assertions.assertFalse( wikiSession.isAuthenticated());
        Assertions.assertEquals( "127.0.0.1", wikiSession.getUserPrincipal().getName() );

        // Clear the authentication cookie
        response = HttpMockFactory.createHttpResponse();
        CookieAuthenticationLoginModule.clearLoginCookie( m_engine, request, response );
    }

    @Test
    void testAuthenticationCookieWhenOn() throws WikiException, ServletException, IOException {
        final Properties props = TestEngine.getTestProperties();
        props.setProperty( AuthenticationManager.PROP_ALLOW_COOKIE_AUTH, "true" );
        m_engine = new TestEngine( props );

        final HttpServletRequest request;
        final Session wikiSession;

        // Set the authentication cookie first
        HttpServletResponse response = HttpMockFactory.createHttpResponse();
        CookieAuthenticationLoginModule.setLoginCookie( m_engine, response, "Fred Flintstone" );
        final ArgumentCaptor< Cookie > captor = ArgumentCaptor.forClass( Cookie.class );
        Mockito.verify( response ).addCookie( captor.capture() );
        final String uid = captor.getValue().getValue();

        // Adding the magic "authentication cookie" should count as authenticated
        request = HttpMockFactory.createHttpRequest();
        Mockito.doReturn( null ).when( request ).getUserPrincipal();
        Mockito.doReturn( new Cookie[] { new Cookie( "JSPWikiUID", uid ) } ).when( request ).getCookies();
        runSecurityFilter( m_engine, request );
        wikiSession = Wiki.session().find( m_engine, request );
        Assertions.assertFalse( wikiSession.isAnonymous() );
        Assertions.assertTrue( wikiSession.isAuthenticated() );
        Assertions.assertEquals( "Fred Flintstone", wikiSession.getUserPrincipal().getName() );

        // Clear the authentication cookie
        response = HttpMockFactory.createHttpResponse();
        CookieAuthenticationLoginModule.clearLoginCookie( m_engine, request, response );
    }

    /**
     * Creates an anonymous user session.
     * @param engine the wiki engine
     * @return the new session
     * @throws Exception session not anonymous.
     */
    public static Session anonymousSession( final TestEngine engine ) throws Exception {
        // Build anon session
        final HttpSession httpSession = Mockito.mock( HttpSession.class );
        Mockito.doReturn( "mock-session-" + System.currentTimeMillis() ).when( httpSession ).getId();
        final HttpServletRequest request = HttpMockFactory.createHttpRequest();
        Mockito.doReturn( httpSession ).when( request ).getSession();

        // Log in
        runSecurityFilter( engine, request );

        // Make sure the user is actually anonymous
        final Session session = Wiki.session().find( engine, request );
        if ( !session.isAnonymous() ) {
            throw new IllegalStateException( "Session is not anonymous." );
        }
        return session;
    }

    public static Session assertedSession( final TestEngine engine, final String name ) throws Exception {
        return assertedSession( engine, name, new Principal[0] );
    }

    public static Session assertedSession( final TestEngine engine, final String name, final Principal[] roles ) throws Exception {
        // We can use cookies right?
        if ( !engine.getManager( AuthenticationManager.class ).allowsCookieAssertions() ) {
            throw new IllegalStateException( "Couldn't set up asserted user: login config doesn't allow cookies." );
        }

        // Build anon session
        final HttpSession session = Mockito.mock( HttpSession.class );
        Mockito.doReturn( "mock-session-asserted" ).when( session ).getId();
        final HttpServletRequest request = HttpMockFactory.createHttpRequest();
        Mockito.doReturn( session ).when( request ).getSession();
        Mockito.doReturn( session ).when( request ).getSession( Mockito.anyBoolean() );
        final Set< String > r = new HashSet<>();
        for( final Principal role : roles ) {
            r.add( role.getName() );
        }
        Mockito.doAnswer( invocation -> r.contains( invocation.getArguments()[0].toString() ) ).when( request ).isUserInRole( Mockito.anyString() );

        // Set cookie
        final Cookie cookie = new Cookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME, name );
        Mockito.doReturn( new Cookie[] { cookie } ).when( request ).getCookies();

        // Log in
        runSecurityFilter( engine, request );

        // Make sure the user is actually asserted
        return Wiki.session().find( engine, request );
    }

    public static Session adminSession( final TestEngine engine ) throws Exception {
        return authenticatedSession( engine, Users.ADMIN, Users.ADMIN_PASS );
    }

    public static Session authenticatedSession( final TestEngine engine, final String id, final String password ) throws Exception {
        // Build anon session
        final HttpServletRequest request = HttpMockFactory.createHttpRequest();

        // Log in as anon
        runSecurityFilter(engine, request);

        // Log in the user with credentials
        final Session session = Wiki.session().find( engine, request );
        engine.getManager( AuthenticationManager.class ).login( session, request, id, password );

        // Make sure the user is actually authenticated
        if ( !session.isAuthenticated() ) {
            throw new IllegalStateException( "Could not log in authenticated user '" + id + "'" );
        }
        return session;
    }

    public static Session containerAuthenticatedSession( final TestEngine engine, final String id, final Principal[] roles ) throws Exception {
        // Build container session
        final HttpSession httpSession = Mockito.mock( HttpSession.class );
        Mockito.doReturn( "mock-session-cauth" ).when( httpSession ).getId();
        final HttpServletRequest request = HttpMockFactory.createHttpRequest();
        Mockito.doReturn( httpSession ).when( request ).getSession();
        Mockito.doReturn( httpSession ).when( request ).getSession( Mockito.anyBoolean() );
        final Set< String > r = new HashSet<>();
        for( final Principal role : roles ) {
            r.add( role.getName() );
        }
        Mockito.doAnswer( invocation -> r.contains( invocation.getArguments()[0].toString() ) ).when( request ).isUserInRole( Mockito.anyString() );
        Mockito.doReturn( new WikiPrincipal( id ) ).when( request ).getUserPrincipal();

        // Log in
        runSecurityFilter( engine, request );

        // Make sure the user is actually authenticated
        final Session session = Wiki.session().find( engine, request );
        if ( !session.isAuthenticated() ) {
            throw new IllegalStateException( "Could not log in authenticated user '" + id + "'" );
        }
        return session;
    }

    /**
     * "Scaffolding" method that runs the session security filter on a mock request.
     *
     * @param engine the wiki engine
     * @param request the mock request to pass itnto the
     * @throws ServletException error building servlet context or running the request through it
     * @throws IOException error building servlet context or running the request through it
     */
    private static void runSecurityFilter( final WikiEngine engine, final HttpServletRequest request) throws ServletException, IOException {
        // Create a mock servlet context and stash the wiki engine in it
        final ServletContext servletCtx = HttpMockFactory.createServletContext( "JSPWiki" );
        Mockito.doReturn( engine ).when( servletCtx ).getAttribute( "org.apache.wiki.WikiEngine" );

        // Create a mock filter configuration and add the servlet context we just created
        final FilterConfig filterConfig = HttpMockFactory.createFilterConfig( servletCtx );

        // Create the security filter and run the request  through it
        final WikiServletFilter filter = new WikiServletFilter();
        final FilterChain chain = HttpMockFactory.createFilterChain();
        filter.init( filterConfig );
        filter.doFilter( request, null, chain );
    }

}
