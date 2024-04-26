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
package org.apache.wiki.http.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.spi.Wiki;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * CSRF protection Filter which uses the synchronizer token pattern – an anti-CSRF token is created and stored in the
 * user session and in a hidden field on subsequent form submits. At every submit the server checks the token from the
 * session matches the one submitted from the form.
 */
public class CsrfProtectionFilter implements Filter {

    private static final Logger LOG = LogManager.getLogger( CsrfProtectionFilter.class );

    public static final String ANTICSRF_PARAM = "X-XSRF-TOKEN";

    /** {@inheritDoc} */
    @Override
    public void init( final FilterConfig filterConfig ) {
    }

    /** {@inheritDoc} */
    @Override
    public void doFilter( final ServletRequest request, final ServletResponse response, final FilterChain chain ) throws IOException, ServletException {
        if( isPost( ( HttpServletRequest ) request ) ) {
            final Engine engine = Wiki.engine().find( request.getServletContext(), null );
            final Session session = Wiki.session().find( engine, ( HttpServletRequest ) request );
            if( !requestContainsValidCsrfToken( request, session ) ) {
                LOG.error( "Incorrect {} param with value '{}' received for {}",
                           ANTICSRF_PARAM, request.getParameter( ANTICSRF_PARAM ), ( ( HttpServletRequest ) request ).getPathInfo() );
                ( ( HttpServletResponse ) response ).sendRedirect( "/error/Forbidden.html" );
                return;
            }
        }
        chain.doFilter( request, response );
    }

    public static boolean isCsrfProtectedPost( final HttpServletRequest request ) {
        if( isPost( request ) ) {
            final Engine engine = Wiki.engine().find( request.getServletContext(), null );
            final Session session = Wiki.session().find( engine, request );
            return requestContainsValidCsrfToken( request, session );
        }
        return false;
    }

    private static boolean requestContainsValidCsrfToken( final ServletRequest request, final Session session ) {
        return session.antiCsrfToken().equals( request.getParameter( ANTICSRF_PARAM ) );
    }

    static boolean isPost( final HttpServletRequest request ) {
        return "POST".equalsIgnoreCase( request.getMethod() );
    }

    /** {@inheritDoc} */
    @Override
    public void destroy() {
    }

}
