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
package org.apache.wiki.oidc;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Logs in, reads the identity and logs out through the filters of {@code web.xml}, in an embedded Tomcat, against an OpenID Connect
 * provider that runs in the test. The wiki itself is not started: a servlet that says what the request knows about the user stands in
 * for it, which is what {@code WikiServletFilter} reads.
 */
class OidcLoginFlowTest {

    private static FakeOidcProvider provider;
    private static Tomcat tomcat;
    private static Path settings;
    private static int port;

    private HttpClient browser;

    @BeforeAll
    static void start() throws Exception {
        provider = new FakeOidcProvider();
        try( ServerSocket free = new ServerSocket( 0 ) ) {
            port = free.getLocalPort();
        }
        // The settings are read from the classpath, so the file goes where the classpath of the tests starts
        settings = Path.of( OidcLoginFlowTest.class.getProtectionDomain().getCodeSource().getLocation().toURI() ).resolve( "jspwiki-oidc.properties" );
        Files.writeString( settings, "oidc.clientId=" + FakeOidcProvider.CLIENT_ID + "\n"
                                   + "oidc.secret=" + FakeOidcProvider.CLIENT_SECRET + "\n"
                                   + "oidc.discoveryUri=" + provider.discoveryUri() + "\n"
                                   + "oidc.callbackUrl=http://localhost:" + port + "/oidc/callback\n"
                                   + "oidc.rolesClaim=realm_access.roles\n", StandardCharsets.UTF_8 );

        final Path base = Files.createTempDirectory( "oidc-tomcat" );
        tomcat = new Tomcat();
        tomcat.setBaseDir( base.toString() );
        tomcat.setPort( port );
        tomcat.getConnector();
        final Context context = tomcat.addContext( "", base.toString() );
        filter( context, "OidcCallbackFilter", "org.pac4j.jee.filter.CallbackFilter", "/oidc/callback",
                "defaultUrl", "/Wiki.jsp", "renewSession", "true", "defaultClient", OidcSettings.CLIENT_NAME );
        filter( context, "OidcSecurityFilter", "org.pac4j.jee.filter.SecurityFilter", "/Login.jsp",
                "configFactory", OidcConfigFactory.class.getName(), "clients", OidcSettings.CLIENT_NAME );
        filter( context, "OidcLogoutFilter", "org.pac4j.jee.filter.LogoutFilter", "/Logout.jsp",
                "configFactory", OidcConfigFactory.class.getName(), "defaultUrl", "/Wiki.jsp", "localLogout", "true",
                "centralLogout", "true", "destroySession", "true" );
        filter( context, "OidcIdentityFilter", OidcIdentityFilter.class.getName(), "/*" );
        Tomcat.addServlet( context, "who", new HttpServlet() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet( final HttpServletRequest req, final HttpServletResponse resp ) throws IOException {
                resp.setContentType( "text/plain" );
                resp.getWriter().write( "user=" + req.getRemoteUser() + ";admin=" + req.isUserInRole( "Admin" )
                                        + ";other=" + req.isUserInRole( "Other" ) );
            }
        } );
        context.addServletMappingDecoded( "/*", "who" );
        tomcat.start();
    }

    @AfterAll
    static void stop() throws Exception {
        if( tomcat != null ) {
            tomcat.stop();
            tomcat.destroy();
        }
        if( provider != null ) {
            provider.stop();
        }
        if( settings != null ) {
            Files.deleteIfExists( settings );
        }
    }

    @BeforeEach
    void newBrowser() {
        provider.username = "kcuser";
        provider.email = "kcuser@example.org";
        provider.roles = List.of( "Admin", "viewer" );
        provider.logouts = 0;
        browser = HttpClient.newBuilder().cookieHandler( new CookieManager( null, CookiePolicy.ACCEPT_ALL ) )
                            .followRedirects( HttpClient.Redirect.NEVER ).build();
    }

    @Test
    void anonymousVisitorHasNoIdentity() throws Exception {
        Assertions.assertEquals( "user=null;admin=false;other=false", get( "/Wiki.jsp" ) );
    }

    @Test
    void loginPageSendsTheVisitorToTheProviderAndBackWithTheNameAndTheRoles() throws Exception {
        Assertions.assertEquals( "user=kcuser;admin=true;other=false", get( "/Login.jsp" ) );
        Assertions.assertTrue( provider.lastScope.contains( "openid" ), provider.lastScope );
    }

    @Test
    void identityStaysForTheNextRequestsOfTheSession() throws Exception {
        get( "/Login.jsp" );

        Assertions.assertEquals( "user=kcuser;admin=true;other=false", get( "/Wiki.jsp" ) );
        Assertions.assertEquals( "user=kcuser;admin=true;other=false", get( "/Edit.jsp" ) );
    }

    @Test
    void visitorWithoutTheAdminRoleIsNotAdmin() throws Exception {
        provider.roles = List.of( "viewer" );

        Assertions.assertEquals( "user=kcuser;admin=false;other=false", get( "/Login.jsp" ) );
    }

    @Test
    void emailIsTheNameWhenTheProviderSendsNoUsername() throws Exception {
        provider.username = null;

        Assertions.assertEquals( "user=kcuser@example.org;admin=true;other=false", get( "/Login.jsp" ) );
    }

    @Test
    void logoutEndsTheSessionAtTheProviderAndComesBackAnonymous() throws Exception {
        get( "/Login.jsp" );

        final String after = get( "/Logout.jsp" );

        Assertions.assertEquals( 1, provider.logouts );
        Assertions.assertNotNull( provider.lastIdTokenHint, "the provider is told which session to end" );
        Assertions.assertEquals( "user=null;admin=false;other=false", after );
        Assertions.assertEquals( "user=null;admin=false;other=false", get( "/Wiki.jsp" ) );
    }

    /** Gets the page and follows the redirects, as a browser does, and returns the body of the last answer. */
    private String get( final String path ) throws Exception {
        URI uri = URI.create( "http://localhost:" + port + path );
        for( int redirects = 0; redirects < 10; redirects++ ) {
            final HttpResponse< String > response = browser.send( HttpRequest.newBuilder( uri ).GET().build(), HttpResponse.BodyHandlers.ofString() );
            if( response.statusCode() / 100 != 3 ) {
                Assertions.assertEquals( 200, response.statusCode(), response.body() );
                return response.body();
            }
            uri = uri.resolve( response.headers().firstValue( "Location" ).orElseThrow() );
        }
        throw new AssertionError( "too many redirects" );
    }

    private static void filter( final Context context, final String name, final String className, final String pattern, final String... params ) {
        final FilterDef def = new FilterDef();
        def.setFilterName( name );
        def.setFilterClass( className );
        for( int i = 0; i < params.length; i += 2 ) {
            def.addInitParameter( params[ i ], params[ i + 1 ] );
        }
        context.addFilterDef( def );
        final FilterMap map = new FilterMap();
        map.setFilterName( name );
        map.addURLPattern( pattern );
        context.addFilterMap( map );
    }

}
