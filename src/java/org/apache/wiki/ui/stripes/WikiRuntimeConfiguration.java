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
 * {@link net.sourceforge.stripes.config.RuntimeConfiguration} that keeps a
 * reference to the running WikiEngine. This Configuration is loaded at startup
 * by the {@link net.sourceforge.stripes.controller.StripesFilter}, so it is
 * one of the very first things that happens. Because it loads first, it creates
 * the WikiEngine.
 * 
 * @author Andrew Jaquith
 */
public class WikiRuntimeConfiguration extends RuntimeConfiguration
{
    public static final String STRIPES_CONFIGURATION = "WikiRuntimeConfiguration";

    private static final Logger log = LoggerFactory.getLogger(WikiRuntimeConfiguration.class);

    private WikiEngine m_engine = null;

    /**
     * Initializes the WikiRuntimeConfiguration by calling the superclass
     * {@link net.sourceforge.stripes.config.Configuration#init()} method, then
     * setting the internally cached WikiEngine reference.
     */
    @Override
    public void init()
    {
        // Initialize the Stripes configuration
        super.init();

        // Retrieve the WikiEngine
        log.info("Attempting to retrieve WikiEngine.");
        ServletContext context = super.getServletContext();
        m_engine = WikiEngine.getInstance(context, null);
        log.info("WikiEngine is running.");
        
        // Stash the Configuration so JSPWiki (e.g., URLConstructors) can find it
        context.setAttribute( STRIPES_CONFIGURATION, this );
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
