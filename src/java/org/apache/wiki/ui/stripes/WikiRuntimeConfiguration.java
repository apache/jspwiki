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

import javax.servlet.ServletContext;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;

import net.sourceforge.stripes.config.RuntimeConfiguration;

/**
 * Subclass of Stripes
 * {@link net.sourceforge.stripes.config.RuntimeConfiguration} that initializes
 * Stripes and starts up the WikiEngine. This Configuration is loaded at startup
 * by the {@link net.sourceforge.stripes.controller.StripesFilter}, so it is
 * one of the very first things that happens. The {@link #init()} method
 * performs all of the initialization tasks. After initialization, the current
 * StripesConfiguration can be retrieved at any time by calling
 * {@link #getConfiguration(ServletContext)}.
 * 
 * @author Andrew Jaquith
 * @since 3.0
 */
public class WikiRuntimeConfiguration extends RuntimeConfiguration
{
    private static final String STRIPES_CONFIGURATION = "WikiRuntimeConfiguration";

    private static final Logger log = LoggerFactory.getLogger( WikiRuntimeConfiguration.class );

    private WikiEngine m_engine = null;

    /**
     * Returns the Stripes WikiRuntimeConfiguration for a supplied
     * ServletContext. Only one Stripes Configuration will be stored per
     * ServletContext (and by implication, per WikiEngine).
     * 
     * @param context the servlet context
     * @return the configuration
     */
    public static WikiRuntimeConfiguration getConfiguration( ServletContext context )
    {
        WikiRuntimeConfiguration config = (WikiRuntimeConfiguration) context.getAttribute( STRIPES_CONFIGURATION );
        if( config != null )
        {
            return config;
        }
        throw new IllegalStateException( "WikiRuntimeConfiguration not found!" );
    }

    /**
     * Initializes the WikiRuntimeConfiguration by calling the superclass
     * {@link net.sourceforge.stripes.config.Configuration#init()} method, then
     * starting the WikiEngine. This method also stashes the
     * WikiRuntimeConfiguration in the ServletContext as an attribute.
     */
    @Override
    public void init()
    {
        // Initialize the Stripes configuration
        super.init();

        // Stash the Configuration so we can find it later
        ServletContext context = super.getServletContext();
        context.setAttribute( STRIPES_CONFIGURATION, this );

        // Start the JSPWiki WikiEngine
        log.info( "Attempting to start WikiEngine." );
        m_engine = WikiEngine.getInstance( context, null );
        log.info( "WikiEngine is running." );
    }

    /**
     * Returns the WikiEngine associated with this Stripes configuration.
     * 
     * @return the wiki engine
     */
    public WikiEngine getEngine()
    {
        return m_engine;
    }
}
