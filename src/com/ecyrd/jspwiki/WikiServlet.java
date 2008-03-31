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
package com.ecyrd.jspwiki;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.url.DefaultURLConstructor;

/**
 *  This provides a master servlet for dealing with short urls.  It mostly does
 *  redirects to the proper JSP pages. It also intercepts the servlet
 *  shutdown events and uses it to signal wiki shutdown.
 *  
 *  @author Andrew Jaquith
 *  @since 2.2
 */
public class WikiServlet
    extends HttpServlet
{
    private static final long serialVersionUID = 3258410651167633973L;
    private WikiEngine m_engine;
    static final Logger log = Logger.getLogger(WikiServlet.class.getName());

    /**
     *  {@inheritDoc}
     */
    public void init( ServletConfig config )
        throws ServletException 
    {
        super.init( config );

        m_engine         = WikiEngine.getInstance( config );

        log.info("WikiServlet initialized.");
    }

    /**
     * Destroys the WikiServlet; called by the servlet container
     * when shutting down the webapp. This method calls the
     * protected method {@link WikiEngine#shutdown()}, which
     * sends {@link com.ecyrd.jspwiki.event.WikiEngineEvent#SHUTDOWN}
     * events to registered listeners.
     * @see javax.servlet.GenericServlet#destroy()
     */
    public void destroy()
    {
        log.info("WikiServlet shutdown.");
        m_engine.shutdown();
        super.destroy();
    }

    /**
     *  {@inheritDoc}
     */
    public void doPost( HttpServletRequest req, HttpServletResponse res )
        throws IOException, ServletException
    {
        doGet( req, res );
    }
    
    /**
     *  {@inheritDoc}
     */
    public void doGet( HttpServletRequest req, HttpServletResponse res ) 
        throws IOException, ServletException 
    {
        String pageName = DefaultURLConstructor.parsePageFromURL( req,
                                                                  m_engine.getContentEncoding() );

        log.info("Request for page: "+pageName);

        if( pageName == null ) pageName = m_engine.getFrontPage(); // FIXME: Add special pages as well
        
        String jspPage = m_engine.getURLConstructor().getForwardPage( req );
        
        RequestDispatcher dispatcher = req.getRequestDispatcher("/"+jspPage+"?page="+m_engine.encodeName(pageName)+"&"+req.getQueryString() );

        dispatcher.forward( req, res );
    }
}


