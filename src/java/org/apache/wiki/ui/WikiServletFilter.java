/*
    JSPWiki - a JSP-based WikiWiki clone.

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
package org.apache.wiki.ui;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.SessionMonitor;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;


/**
 * Filter that verifies that the {@link org.apache.wiki.WikiEngine} is running, and
 * sets the authentication status for the user's WikiSession. Each HTTP request
 * processed by this filter is wrapped by a {@link WikiRequestWrapper}. The wrapper's
 * primary responsibility is to return the correct <code>userPrincipal</code> and
 * <code>remoteUser</code> for authenticated JSPWiki users (whether 
 * authenticated by container or by JSPWiki's custom system).
 * The wrapper's other responsibility is to incorporate JSPWiki built-in roles
 * into the role-checking algorithm for {@link  HttpServletRequest#isUserInRole(String)}.
 * Just before the request is wrapped, the method {@link AuthenticationManager#login(HttpServletRequest)} executes;
 * this method contains all of the logic needed to grab any user login credentials set 
 * by the container or by cookies.
 *  
 *
 */
public class WikiServletFilter implements Filter
{
    protected static final Logger log = LoggerFactory.getLogger( WikiServletFilter.class );
    private WikiEngine m_engine = null;

    /**
     *  Creates a Wiki Servlet Filter.
     */
    public WikiServletFilter()
    {
        super();
    }

    /**
     * Initializes the WikiServletFilter.
     * 
     * @param config The FilterConfig.
     * @throws ServletException If a WikiEngine cannot be started.
     */
    public void init( FilterConfig config ) throws ServletException
    {
    }

    /**
     * Destroys the WikiServletFilter.
     */
    public void destroy()
    {
    }

    /**
    * Checks that the WikiEngine is running ok, wraps the current
    * HTTP request, and sets the correct authentication state for the users's
    * WikiSession. First, the method {@link AuthenticationManager#login(HttpServletRequest)}
    * executes, which sets the authentication state. Then, the request is wrapped with a
    * {@link WikiRequestWrapper}.
    * @param request the current HTTP request object
    * @param response the current HTTP response object
    * @param chain The Filter chain passed down.
    * @throws ServletException if {@link AuthenticationManager#login(HttpServletRequest)} fails for any reason
    * @throws IOException If writing to the servlet response fails. 
    */
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain ) throws IOException, ServletException
    {
        //
        //  Sanity check; it might be true in some conditions, but we need to know where.
        //
        if( chain == null )
        {
            throw new ServletException("FilterChain is null, but it should not be!");
        }
        
        // Lazily obtain the WikiEngine
        if ( m_engine == null )
        {
            ServletContext servletContext = ((HttpServletRequest)request).getSession().getServletContext();
            m_engine = WikiEngine.getInstance( servletContext, null );
            if ( m_engine == null )
            {
                throw new ServletException( "WikiEngine was not started!" );
            }
        }
        
        // If we haven't done so, wrap the request
        if ( !isWrapped( request ) )
        {
            // Prepare the WikiSession
            try
            {
                // Execute the login stack
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                m_engine.getAuthenticationManager().login( httpRequest );
                WikiSession wikiSession = SessionMonitor.getInstance( m_engine ).find( httpRequest.getSession() );
                
                // Wrap the request
                httpRequest = new WikiRequestWrapper( m_engine, httpRequest );
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Executed security filters for user=" + wikiSession.getLoginPrincipal().getName() + ", path=" + httpRequest.getRequestURI() );
                }
            }
            catch ( WikiSecurityException e )
            {
                throw new ServletException( e );
            }
        }

        try
        {
            chain.doFilter( request, response );
        }
        finally
        {
            m_engine.release(); // No longer used until next request.
        }

    }

    /** 
     * Determines whether the request has been previously wrapped with a WikiRequestWrapper. 
     * We find the wrapper by recursively unwrapping successive request wrappers, if they have been supplied.
     * @param request the current HTTP request
     * @return <code>true</code> if the request has previously been wrapped;
     * <code>false</code> otherwise
     */
    private boolean isWrapped( ServletRequest request )
    {
        while ( !(request instanceof WikiRequestWrapper )
            && request != null
            && request instanceof HttpServletRequestWrapper )
        {
            request = ((HttpServletRequestWrapper) request).getRequest();
        }
        return request instanceof WikiRequestWrapper ? true : false;
    }

}
