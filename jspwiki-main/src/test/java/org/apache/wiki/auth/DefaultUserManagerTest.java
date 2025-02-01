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

package org.apache.wiki.auth;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Command;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.event.WikiEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.security.auth.Subject;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.security.Permission;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class DefaultUserManagerTrimTest {

    private Engine engine;
    private DefaultUserManager userManager;

    @BeforeEach
    public void setUp() throws Exception {
        // Initialize a minimal test engine.
        engine = new TestEngine();
        // Get the UserManager (which should be an instance of DefaultUserManager).
        userManager = ( DefaultUserManager ) engine.getManager( UserManager.class );
    }

    @Test
    public void testParseProfileTrimsFields() throws WikiException {
        // Create a dummy HttpServletRequest that returns parameters with extra whitespace.
        HttpServletRequest request = new DummyHttpServletRequest();
        // Create a dummy Context that returns our dummy request and a dummy Session.
        Context dummyContext = new DummyContext( engine, request );

        // Call parseProfile, which should trim the input values.
        UserProfile profile = userManager.parseProfile( dummyContext );

        // Verify that the login name, fullname, and email have been trimmed.
        Assertions.assertEquals( "admin", profile.getLoginName(), "Login name should be trimmed" );
        Assertions.assertEquals( "Administrator", profile.getFullname(), "Full name should be trimmed" );
        Assertions.assertEquals( "admin@example.com", profile.getEmail(), "Email should be trimmed" );
    }

    // --- Minimal dummy implementations ---

    // A very basic HttpServletRequest implementation supporting getParameter.
    private static class DummyHttpServletRequest implements HttpServletRequest {
        @Override
        public String getParameter( String name ) {
            switch( name ) {
                case "loginname":
                    return "  admin  "; // with leading and trailing whitespace
                case "password":
                    return "password"; // no extra whitespace
                case "fullname":
                    return "  Administrator  ";
                case "email":
                    return " admin@example.com ";
                default:
                    return null;
            }
        }

        @Override
        public Enumeration< String > getParameterNames() {
            return null;
        }

        @Override
        public String[] getParameterValues( String name ) {
            return new String[ 0 ];
        }

        @Override
        public Map< String, String[] > getParameterMap() {
            return Map.of();
        }

        // For brevity, all other methods throw UnsupportedOperationException or return defaults.
        @Override
        public Object getAttribute( String name ) {
            return null;
        }

        @Override
        public Enumeration< String > getAttributeNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public String getCharacterEncoding() {
            return null;
        }

        @Override
        public void setCharacterEncoding( String env ) throws java.io.UnsupportedEncodingException {
        }

        @Override
        public int getContentLength() {
            return 0;
        }

        @Override
        public long getContentLengthLong() {
            return 0;
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public String getScheme() {
            return null;
        }

        @Override
        public String getServerName() {
            return null;
        }

        @Override
        public int getServerPort() {
            return 0;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRemoteAddr() {
            return null;
        }

        @Override
        public String getRemoteHost() {
            return null;
        }

        @Override
        public void setAttribute( String name, Object o ) {
        }

        @Override
        public void removeAttribute( String name ) {
        }

        @Override
        public Locale getLocale() {
            return Locale.getDefault();
        }

        @Override
        public Enumeration< Locale > getLocales() {
            return Collections.enumeration( Collections.singleton( Locale.getDefault() ) );
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public RequestDispatcher getRequestDispatcher( String path ) {
            return null;
        }

        @Override
        public String getRealPath( String path ) {
            return null;
        }

        @Override
        public int getRemotePort() {
            return 0;
        }

        @Override
        public String getLocalName() {
            return null;
        }

        @Override
        public String getLocalAddr() {
            return null;
        }

        @Override
        public int getLocalPort() {
            return 0;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public AsyncContext startAsync() throws IllegalStateException {
            return null;
        }

        @Override
        public AsyncContext startAsync( ServletRequest servletRequest, ServletResponse servletResponse ) throws IllegalStateException {
            return null;
        }


        @Override
        public boolean isAsyncStarted() {
            return false;
        }

        @Override
        public boolean isAsyncSupported() {
            return false;
        }

        @Override
        public AsyncContext getAsyncContext() {
            return null;
        }

        @Override
        public DispatcherType getDispatcherType() {
            return null;
        }

        @Override
        public String getAuthType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Cookie[] getCookies() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getDateHeader( String name ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getHeader( String name ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Enumeration< String > getHeaders( String name ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Enumeration< String > getHeaderNames() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getIntHeader( String name ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getMethod() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getPathInfo() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getPathTranslated() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getContextPath() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getQueryString() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRemoteUser() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isUserInRole( String role ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Principal getUserPrincipal() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRequestedSessionId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRequestURI() {
            throw new UnsupportedOperationException();
        }

        @Override
        public StringBuffer getRequestURL() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getServletPath() {
            throw new UnsupportedOperationException();
        }

        @Override
        public HttpSession getSession( boolean create ) {
            return new DummyHttpSession();
        }

        @Override
        public HttpSession getSession() {
            return new DummyHttpSession();
        }

        @Override
        public String changeSessionId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRequestedSessionIdFromUrl() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean authenticate( HttpServletResponse response ) throws IOException, ServletException {
            return false;
        }

        @Override
        public void login( String username, String password ) throws ServletException {

        }

        @Override
        public void logout() throws ServletException {

        }

        @Override
        public Collection< Part > getParts() throws IOException, ServletException {
            return List.of();
        }

        @Override
        public Part getPart( String name ) throws IOException, ServletException {
            return null;
        }

        @Override
        public < T extends HttpUpgradeHandler > T upgrade( Class< T > handlerClass ) throws IOException, ServletException {
            return null;
        }
    }

    // Minimal dummy HttpSession implementation.
    private static class DummyHttpSession implements HttpSession {
        @Override
        public long getCreationTime() {
            return 0;
        }

        @Override
        public String getId() {
            return "dummy-session";
        }

        @Override
        public long getLastAccessedTime() {
            return 0;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public void setMaxInactiveInterval( int interval ) {
        }

        @Override
        public int getMaxInactiveInterval() {
            return 0;
        }

        @Override
        public HttpSessionContext getSessionContext() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getAttribute( String name ) {
            return null;
        }

        @Override
        public Object getValue( String name ) {
            return null;
        }

        @Override
        public Enumeration< String > getAttributeNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public String[] getValueNames() {
            return new String[ 0 ];
        }

        @Override
        public void setAttribute( String name, Object value ) {
        }

        @Override
        public void putValue( String name, Object value ) {
        }

        @Override
        public void removeAttribute( String name ) {
        }

        @Override
        public void removeValue( String name ) {
        }

        @Override
        public void invalidate() {
        }

        @Override
        public boolean isNew() {
            return false;
        }
    }

    // Minimal dummy Context implementation.
    private static class DummyContext implements Context {
        private final Engine engine;
        private final HttpServletRequest request;
        private final Session session;

        public DummyContext( Engine engine, HttpServletRequest request ) {
            this.engine = engine;
            this.request = request;
            this.session = new DummySession();
        }

        @Override
        public Page getPage() {
            return null;
        }

        @Override
        public void setPage( Page wikiPage ) {

        }

        @Override
        public Page getRealPage() {
            return null;
        }

        @Override
        public Page setRealPage( Page wikiPage ) {
            return null;
        }

        @Override
        public Engine getEngine() {
            return engine;
        }

        @Override
        public void setRequestContext( String context ) {

        }

        @Override
        public HttpServletRequest getHttpRequest() {
            return request;
        }

        @Override
        public void setTemplate( String dir ) {

        }

        @Override
        public String getTemplate() {
            return "";
        }

        @Override
        public Session getWikiSession() {
            return session;
        }

        @Override
        public Principal getCurrentUser() {
            return null;
        }

        @Override
        public boolean hasAdminPermissions() {
            return false;
        }

        @Override
        public String getViewURL( String WikiPage ) {
            return "";
        }

        @Override
        public String getRedirectURL() {
            return "";
        }

        @Override
        public Command getCommand() {
            return null;
        }

        @Override
        public Context clone() {
            return null;
        }

        @Override
        public Context deepClone() {
            return null;
        }


        @Override
        public Command targetedCommand( Object target ) {
            return null;
        }

        @Override
        public String getContentTemplate() {
            return "";
        }

        @Override
        public String getJSP() {
            return "";
        }

        @Override
        public String getName() {
            return "";
        }

        @Override
        public String getRequestContext() {
            return "";
        }

        @Override
        public Permission requiredPermission() {
            return null;
        }

        @Override
        public Object getTarget() {
            return null;
        }

        @Override
        public String getURLPattern() {
            return "";
        }


        @Override
        public void setVariable( String key, Object value ) {
        }

        @Override
        public boolean getBooleanWikiProperty( String key, boolean defValue ) {
            return false;
        }

        @Override
        public String getHttpParameter( String paramName ) {
            return "";
        }

        @Override
        public Object getVariable( String key ) {
            return null;
        }

    }

    // Minimal dummy Session implementation.
    private static class DummySession implements Session {
        @Override
        public boolean isAsserted() {
            return false;
        }

        @Override
        public boolean isAuthenticated() {
            return false;
        }

        @Override
        public boolean isAnonymous() {
            return false;
        }

        @Override
        public Principal getLoginPrincipal() {
            return null;
        }

        @Override
        public Principal getUserPrincipal() {
            return null;
        }

        @Override
        public String antiCsrfToken() {
            return "";
        }

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public void addMessage( String message ) {

        }

        @Override
        public void addMessage( String key, String message ) {
        }

        @Override
        public void clearMessages() {

        }

        @Override
        public void clearMessages( String topic ) {

        }

        @Override
        public String[] getMessages() {
            return new String[ 0 ];
        }

        @Override
        public String[] getMessages( String key ) {
            return new String[ 0 ];
        }

        @Override
        public Principal[] getPrincipals() {
            return new Principal[ 0 ];
        }

        @Override
        public Principal[] getRoles() {
            return new Principal[ 0 ];
        }

        @Override
        public boolean hasPrincipal( Principal principal ) {
            return false;
        }

        @Override
        public void invalidate() {

        }

        @Override
        public String getStatus() {
            return "";
        }

        @Override
        public Subject getSubject() {
            return null;
        }


        @Override
        public void actionPerformed( WikiEvent event ) {

        }
    }
}
