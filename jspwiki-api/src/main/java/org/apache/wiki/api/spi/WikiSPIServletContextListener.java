package org.apache.wiki.api.spi;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;


public class WikiSPIServletContextListener implements ServletContextListener {

    /** {@inheritDoc} */
    @Override
    public void contextInitialized( final ServletContextEvent sce ) {
        Wiki.init( sce.getServletContext() );
    }

    /** {@inheritDoc} */
    @Override
    public void contextDestroyed( final ServletContextEvent sce ) {
    }

}
