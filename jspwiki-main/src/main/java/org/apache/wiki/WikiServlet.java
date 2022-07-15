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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.engine.EngineLifecycleExtension;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.url.URLConstructor;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * This provides a master servlet for dealing with short urls. It mostly does redirects to the proper JSP pages.
 * It also intercepts the servlet shutdown events and uses it to signal wiki shutdown.
 *
 * @since 2.2
 */
public class WikiServlet extends HttpServlet {

    private static final long serialVersionUID = 3258410651167633973L;
    private Engine m_engine;
    private static final Logger LOG = LogManager.getLogger( WikiServlet.class.getName() );

    /**
     * {@inheritDoc}
     */
    @Override
    public void init( final ServletConfig config ) throws ServletException {
        super.init( config );
        m_engine = Wiki.engine().find( config );
        LOG.info( "WikiServlet initialized." );
    }

    /**
     * Destroys the WikiServlet; called by the servlet container when shutting down the webapp. This method calls the
     * protected method {@link WikiEngine#stop()}, which sends {@link org.apache.wiki.event.WikiEngineEvent#SHUTDOWN}
     * events to registered listeners, as well as notifying available {@link EngineLifecycleExtension EngineLifecycleExtension}s.
     *
     * @see javax.servlet.GenericServlet#destroy()
     */
    @Override
    public void destroy() {
        LOG.info( "WikiServlet shutdown." );
        m_engine.stop();
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
        String pageName = URLConstructor.parsePageFromURL( req, m_engine.getContentEncoding() );

        LOG.info( "Request for page: {}", pageName );
        if( pageName == null ) {
            pageName = m_engine.getFrontPage(); // FIXME: Add special pages as well
        }

        final String jspPage = m_engine.getManager( URLConstructor.class ).getForwardPage( req );
        final RequestDispatcher dispatcher = req.getRequestDispatcher( "/" + jspPage + "?page=" +
                                                                       m_engine.encodeName( pageName ) + "&" + req.getQueryString() );

        dispatcher.forward( req, res );
    }

}
