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
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Locale;
import java.util.Vector;


/**
 * Mocks {@link TestEngine}'s http interactions. As this is a general usage class, tests using {@code MockitoExtension}
 * and {@link TestEngine} should also use {@code @MockitoSettings( strictness = Strictness.LENIENT )}.
 */
public class HttpMockFactory {

    public static FilterChain createFilterChain() {
        return Mockito.mock( FilterChain.class );
    }

    public static FilterConfig createFilterConfig() {
        return createFilterConfig( createServletContext( "/JSPWiki" ) );
    }

    public static FilterConfig createFilterConfig( final ServletContext sc ) {
        final FilterConfig fc = Mockito.mock( FilterConfig.class );
        Mockito.doReturn( sc ).when( fc ).getServletContext();
        return fc;
    }

    public static ServletConfig createServletConfig( final String contextName ) {
        final ServletConfig sc = Mockito.mock( ServletConfig.class );
        Mockito.doReturn( createServletContext( contextName ) ).when( sc ).getServletContext();
        return sc;
    }

    public static ServletContext createServletContext( final String contextName ) {
        final ServletContext sc = Mockito.mock( ServletContext.class );
        Mockito.doReturn( 6 ).when( sc ).getMajorVersion();
        Mockito.doReturn( 0 ).when( sc ).getMinorVersion();
        Mockito.doReturn( "/" + contextName ).when( sc ).getContextPath();
        Mockito.doReturn( contextName ).when( sc ).getServletContextName();
        Mockito.doAnswer( ( Answer< ServletContext > ) invocationOnMock -> {
            final String uriPath = invocationOnMock.getArgument( 0 );
            if( uriPath.startsWith( contextName ) ) {
                return sc;
            } else {
                return null;
            }
        } ).when( sc ).getContext( Mockito.anyString() );
        try {
            Mockito.doAnswer( invocationOnMock -> {
                String name = invocationOnMock.getArguments()[0].toString();
                while( name.startsWith( "/" ) ) {
                    name = name.substring( 1 );
                }
                return Thread.currentThread().getContextClassLoader().getResource( name );
            } ).when( sc ).getResource( Mockito.anyString() );
        } catch( MalformedURLException mue ) {
            throw new RuntimeException( mue );
        }
        return sc;
    }

    public static HttpSession createHttpSession() {
        final HttpSession session = Mockito.mock( HttpSession.class );
        Mockito.doReturn( "mock-session" ).when( session ).getId();
        return session;
    }

    /**
     * Creates a correctly-instantiated mock HttpServletRequest with an associated
     * HttpSession.
     * @return the new request
     */
    public static HttpServletRequest createHttpRequest() {
        return createHttpRequest( "/JSPWiki", "/Wiki.jsp" );
    }

    /**
     * Creates a correctly-instantiated mock HttpServletRequest with an associated
     * HttpSession and path.
     * @param path the path relative to the wiki context, for example "/Wiki.jsp"
     * @return the new request
     */
    public static HttpServletRequest createHttpRequest( final String path ) {
        return createHttpRequest( "/JSPWiki", path );
    }

    /**
     * Creates a correctly-instantiated mock HttpServletRequest with an associated
     * HttpSession and path.
     * @param path the path relative to the wiki context, for example "/Wiki.jsp"
     * @return the new request
     */
    public static HttpServletRequest createHttpRequest( final String contextName, final String path ) {
        final HttpSession session = createHttpSession();
        final HttpServletRequest request = Mockito.mock( HttpServletRequest.class );
        Mockito.doReturn( createServletContext( contextName ) ).when( request ).getServletContext();
        Mockito.doReturn( new Locale( "", "" ) ).when( request ).getLocale();
        Mockito.doReturn( new Vector<>( List.of( new Locale( "", "" ) ) ).elements() ).when( request ).getLocales();
        Mockito.doReturn( session ).when( request ).getSession();
        Mockito.doReturn( session ).when( request ).getSession( Mockito.anyBoolean() );
        Mockito.doReturn( "/JSPWiki" ).when( request ).getContextPath();
        Mockito.doReturn( "127.0.0.1" ).when( request ).getRemoteAddr();
        Mockito.doReturn( path ).when( request ).getServletPath();
        Mockito.doReturn( "/JSPWiki" + path /* + "/pathinfo" */ ).when( request ).getRequestURI();
        Mockito.doReturn( new StringBuffer( "http://localhost:8080/JSPWiki" + path /* + "/pathinfo" */ ) ).when( request ).getRequestURL();

        final RequestDispatcher rd = Mockito.mock( RequestDispatcher.class );
        Mockito.doReturn( rd ).when( request ).getRequestDispatcher( Mockito.anyString() );

        return request;
    }

    /**
     * Creates a correctly-instantiated mock HttpServletRequest with an associated
     * HttpSession.
     * @return the new request
     */
    public static HttpServletResponse createHttpResponse() {
        return Mockito.mock( HttpServletResponse.class );
    }

}
