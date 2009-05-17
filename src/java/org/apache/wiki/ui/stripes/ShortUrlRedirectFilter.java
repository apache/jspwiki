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
package org.apache.wiki.ui.stripes;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.RedirectResolution;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.action.ViewActionBean;
import org.apache.wiki.url.ShortURLConstructor;
import org.apache.wiki.util.TextUtil;

/**
 * <p>
 * Intercepts HTTP requests intended for a "short URL" addresses and redirects
 * them to ActionBean URLs. Because this filter needs access to Stripes methods
 * to do its work, the <code>web.xml</code> file must configure the filter
 * order so that {@link StripesFilter} executes before this one.
 * </p>
 * <p>
 * This filter exists purely to provide backwards-compatibility with URLs
 * generated using the short URL scheme. It guarantees that old URLs will still
 * resolve to the correct page. Wikis that did not use the short URL scheme, or
 * do not require backwards compatibility, can simply remove this filter
 * configuration from <code>web.xml</code>.
 * </p>
 */
public class ShortUrlRedirectFilter implements Filter
{

    private String m_urlPrefix = null;

    private String m_urlPrefixNoTrailingSlash = null;

    private WikiEngine m_engine = null;

    /**
     * {@inheritDoc}
     */
    public void destroy()
    {
    }

    /**
     * <p>
     * Examines the URI of the incoming request and redirects the user if it
     * matches the prefix of a "short URL." The short URL prefix is set in
     * <code>jspwiki.properties</code> via key
     * {@link ShortURLConstructor#PROP_PREFIX}. The default is
     * <code>/wiki/</code>, relative to the webapp context. If the URL
     * consists only of <code>/wiki</code> or <code>/wiki/</code>, the user
     * is redirected to {@link ViewActionBean} and its hander event
     * <code>view</code> -- that is, the front page of the wiki. URIs that
     * contain trailing page names and/or <code>do</code> parameters are
     * redirected to the appropriate ActionBean and handler event.
     * </p>
     */
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
                                                                                               throws IOException,
                                                                                                   ServletException
    {
        // Reconstruct the path (not including the host, scheme or webapp
        // context)
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String uri = httpRequest.getRequestURI();

        // Special case: if URI exactly matches the prefix, redirect to front
        // page
        if( m_urlPrefix.equals( uri ) || m_urlPrefixNoTrailingSlash.equals( uri ) )
        {
            new RedirectResolution( ViewActionBean.class ).execute( httpRequest, httpResponse );
            return;
        }

        // If no URI match, proceed with the other filters
        if( !uri.startsWith( m_urlPrefix ) )
        {
            chain.doFilter( request, response );
            return;
        }

        // Extract the page name and wiki context
        String requestContext = request.getParameter( "do" );
        if( requestContext == null )
        {
            requestContext = WikiContext.VIEW;
        }
        requestContext = requestContext.toLowerCase();

        String wikiPath = uri.substring( m_urlPrefix.length(), uri.length() );
        if( wikiPath.length() == 0 )
            wikiPath = null;

        // Figure out the ActionBean+event to redirect to
        HandlerInfo handler = m_engine.getWikiContextFactory().findEventHandler( requestContext );
        RedirectResolution r = new RedirectResolution( handler.getActionBeanClass(), handler.getEventName() );
        if( wikiPath != null )
        {
            r.addParameter( "page", wikiPath );
        }
        r.execute( httpRequest, (HttpServletResponse) response );
    }

    /**
     * Initializes the filter.
     */
    public void init( FilterConfig filterConfig ) throws ServletException
    {
        // Look up the Short URL prefix
        ServletContext servletContext = filterConfig.getServletContext();
        WikiEngine engine = WikiEngine.getInstance( servletContext, null );
        m_engine = engine;

        Properties props = engine.getWikiProperties();
        m_urlPrefix = TextUtil.getStringProperty( props, ShortURLConstructor.PROP_PREFIX, null );
        if( m_urlPrefix == null )
        {
            m_urlPrefix = "wiki/";
        }
        if( !m_urlPrefix.startsWith( "/" ) )
        {
            m_urlPrefix = "/" + m_urlPrefix;
        }
        if( !m_urlPrefix.endsWith( "/" ) )
        {
            m_urlPrefix = m_urlPrefix + "/";
        }

        // Add the servlet context
        m_urlPrefix = "/" + servletContext.getServletContextName() + m_urlPrefix;
        m_urlPrefixNoTrailingSlash = m_urlPrefix.substring( 0, m_urlPrefix.length() - 1 );
    }

}
