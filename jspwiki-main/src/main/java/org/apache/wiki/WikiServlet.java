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

import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.apache.wiki.url.DefaultURLConstructor;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * This provides a master servlet for dealing with short urls.  It mostly does
 * redirects to the proper JSP pages. It also intercepts the servlet
 * shutdown events and uses it to signal wiki shutdown.
 *
 * @since 2.2
 */
public class WikiServlet extends HttpServlet {

    private static final long serialVersionUID = 3258410651167633973L;
    private WikiEngine m_engine;
    private static final Logger log = Logger.getLogger( WikiServlet.class.getName() );

    /**
     * {@inheritDoc}
     */
    @Override
    public void init( final ServletConfig config ) throws ServletException {
        super.init( config );
        m_engine = WikiEngine.getInstance( config );
        log.info( "WikiServlet initialized." );
    }

    /**
     * Destroys the WikiServlet; called by the servlet container
     * when shutting down the webapp. This method calls the
     * protected method {@link WikiEngine#shutdown()}, which
     * sends {@link org.apache.wiki.event.WikiEngineEvent#SHUTDOWN}
     * events to registered listeners.
     *
     * @see javax.servlet.GenericServlet#destroy()
     */
    @Override
    public void destroy() {
        log.info( "WikiServlet shutdown." );
        CacheManager.getInstance().shutdown();
        m_engine.shutdown();
        super.destroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doPost( final HttpServletRequest req, final HttpServletResponse res ) throws IOException, ServletException {
        doGet( req, res );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doGet( final HttpServletRequest req, final HttpServletResponse res ) throws IOException, ServletException {
        String pageName = DefaultURLConstructor.parsePageFromURL( req, m_engine.getContentEncoding() );

        log.info( "Request for page: " + pageName );
        if( pageName == null ) {
            pageName = m_engine.getFrontPage(); // FIXME: Add special pages as well
        }

        final String jspPage = m_engine.getURLConstructor().getForwardPage( req );
        final RequestDispatcher dispatcher = req.getRequestDispatcher( "/" + jspPage + "?page=" +
                                                                       m_engine.encodeName( pageName ) + "&" + req.getQueryString() );

        dispatcher.forward( req, res );
    }

}
