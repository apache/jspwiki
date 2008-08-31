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
package com.ecyrd.jspwiki.ui;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.AuthenticationManager;
import com.ecyrd.jspwiki.auth.SessionMonitor;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.tags.WikiTagBase;

/**
 * Filter that verifies that the {@link com.ecyrd.jspwiki.WikiEngine} is running, and
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
 *  @author Andrew Jaquith
 *
 */
public class WikiServletFilter implements Filter
{
    protected static final Logger log = Logger.getLogger( WikiServletFilter.class );
    protected WikiEngine m_engine = null;

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
        ServletContext context = config.getServletContext();
        m_engine = WikiEngine.getInstance( context, null );
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
            throw new ServletException("FilterChain is null, even if it should not be.  Please report this to the jspwiki development team.");
        }
        
        if( m_engine == null )
        {
            PrintWriter out = response.getWriter();
            out.print("<html><head><title>Fatal problem with JSPWiki</title></head>");
            out.print("<body>");
            out.print("<h1>JSPWiki has not been started</h1>");
            out.print("<p>JSPWiki is not running.  This is probably due to a configuration error in your jspwiki.properties file, ");
            out.print("or a problem with your servlet container.  Please double-check everything before issuing a bug report ");
            out.print("at jspwiki.org.</p>");
            out.print("<p>We apologize for the inconvenience.  No, really, we do.  We're trying to ");
            out.print("JSPWiki as easy as we can, but there is only so much we have time to test ");
            out.print("platforms.</p>");
            out.print( "<p>Please go to the <a href='Install.jsp'>installer</a> to continue.</p>" );
            out.print("</body></html>");
            return;
        }   
        
        // If we haven't done so, wrap the request
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Set the character encoding
        httpRequest.setCharacterEncoding( m_engine.getContentEncoding() );
        
        if( m_engine.getBaseURL().length() == 0 && !httpRequest.getRequestURI().endsWith("Install.jsp") )
        {
            PrintWriter out = response.getWriter();
            
            out.print( "<html><head><title>JSPWiki installation start</title></head>" );
            out.print( "<body>" );
            out.print( "<h1>JSPWiki installation</h1>" );
            out.print( "<p>Hello!  It appears that this is your first jspwiki installation." );
            out.print( "(Or, you have removed jspwiki.baseURL from your property file.) " );
            out.print( "Therefore, you will need to start the installation process. " );
            out.print( "Please <a href='Install.jsp'>continue to the installer</a>." );
            out.print( "</p>");
            out.print( "<p>If you just used the installer, then please restart your servlet container to get rid of this message.</p>" );
            out.print("</body></html>");            
            return;
        }
        
        if ( !isWrapped( request ) )
        {
            // Prepare the WikiSession
            try
            {
                m_engine.getAuthenticationManager().login( httpRequest );
                WikiSession wikiSession = SessionMonitor.getInstance( m_engine ).find( httpRequest.getSession() );
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
            NDC.push( m_engine.getApplicationName()+":"+httpRequest.getRequestURL() );
            
            chain.doFilter( httpRequest, response );
        }
        finally
        {
            NDC.pop();
            NDC.remove();
        }

    }

    /**
     *  Figures out the wiki context from the request.  This method does not create the
     *  context if it does not exist.
     *  
     *  @param request The request to examine
     *  @return A valid WikiContext value (or null, if the context could not be located).
     */
    protected WikiContext getWikiContext( ServletRequest  request )
    {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
    
        WikiContext ctx = (WikiContext) httpRequest.getAttribute( WikiTagBase.ATTR_CONTEXT );
        
        return ctx;
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
