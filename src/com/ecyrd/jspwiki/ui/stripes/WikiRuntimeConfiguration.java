package com.ecyrd.jspwiki.ui.stripes;

import javax.servlet.ServletContext;

import net.sourceforge.stripes.config.RuntimeConfiguration;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;

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
    private Logger log = LoggerFactory.getLogger(WikiRuntimeConfiguration.class);

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
