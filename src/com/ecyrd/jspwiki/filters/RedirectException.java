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
package com.ecyrd.jspwiki.filters;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;

/**
 *  This exception may be thrown if a filter wants to reject something and
 *  redirect the user elsewhere. In addition to being a subclass of FilterException,
 *  this class also implements the Stripes {@link net.sourceforge.stripes.action.Resolution}
 *  interface, which means it can be caught and used by Stripes for redirection.
 *
 *  @since 2.1.112
 */
public class RedirectException
    extends FilterException implements Resolution
{
    private static final long serialVersionUID = 1L;

    private final String m_where;

    private Resolution m_resolution = null;

    /**
     *  Constructs a new RedirectException.
     *  
     *  @param msg The message for the exception
     *  @param redirect The redirect URI.
     */
    public RedirectException( String msg, String redirect )
    {
        super( msg );

        m_where = redirect;

        m_resolution = new RedirectResolution( redirect );
    }

    /**
     *  Get the URI for redirection.
     *  
     *  @return The URI given in the constructor.
     */
    public String getRedirect()
    {
        return m_where;
    }

    /**
     * Sets the Resolution executed by {@link #execute(HttpServletRequest, HttpServletResponse)}. Calling
     * this method overrides the default RedirectResolution created during construction.
     * @param resolution the Resolution to set
     */
    public void setResolution( Resolution resolution )
    {
        m_resolution = resolution;
    }
    
    /**
     * Returns the Resolution that will be executed by {@link #execute(HttpServletRequest, HttpServletResponse)}.
     * If not set explicitly by {@link #setResolution(Resolution)}, this method returns a
     * {@link net.sourceforge.stripes.action.RedirectResolution} that redirects to the URL supplied during
     * construction.
     * @return the Resolution
     */
    public Resolution getResolution()
    {
        return m_resolution;
    }
    
    /**
     * Executes the Stripes redirect activity by calling
     * {@link net.sourceforge.stripes.action.Resolution#execute(HttpServletRequest, HttpServletResponse)}
     * for the Resolution returned by {@link #getResolution()}.
     */
    public void execute( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        m_resolution.execute( request, response );
    }
}
