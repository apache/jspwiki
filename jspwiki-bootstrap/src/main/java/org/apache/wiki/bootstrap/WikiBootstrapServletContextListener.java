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
package org.apache.wiki.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.properties.PropertiesConfigurationFactory;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.util.TextUtil;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class WikiBootstrapServletContextListener implements ServletContextListener {

    private static final Logger LOG = LogManager.getLogger( WikiBootstrapServletContextListener.class );

    /** {@inheritDoc} */
    @Override
    public void contextInitialized( final ServletContextEvent sce ) {
        final Properties properties = initWikiSPIs( sce );
        initWikiLoggingFramework( properties );
    }

    /**
     * Locate and init JSPWiki SPIs' implementations
     *
     * @param sce associated servlet context.
     * @return JSPWiki configuration properties.
     */
    Properties initWikiSPIs( final ServletContextEvent sce ) {
        return Wiki.init( sce.getServletContext() );
    }

    /**
     * Initialize the logging framework(s). By default we try to load the log config statements from jspwiki.properties,
     * unless the property jspwiki.use.external.logconfig=true, in that case we let the logging framework figure out the
     * logging configuration.
     *
     * @param properties JSPWiki configuration properties.
     * @return {@code true} if configuration was read from jspwiki.properties, {@code false} otherwise.
     */
    boolean initWikiLoggingFramework( final Properties properties ) {
        final String useExternalLogConfig = TextUtil.getStringProperty( properties, "jspwiki.use.external.logconfig", "false" );
        if ( useExternalLogConfig.equals( "false" ) ) {
            final ConfigurationSource source = createConfigurationSource( properties );
            if( source != null ) {
                final PropertiesConfigurationFactory factory = new PropertiesConfigurationFactory();
                final LoggerContext ctx = ( LoggerContext ) LogManager.getContext( this.getClass().getClassLoader(), false );
                final Configuration conf = factory.getConfiguration( ctx, source );
                conf.initialize();
                ctx.setConfiguration( conf );
                LOG.info( "Log configuration reloaded from Wiki properties" );
            }
        }
        return useExternalLogConfig.equals( "false" );
    }

    ConfigurationSource createConfigurationSource( final Properties properties ) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            properties.store( out, null );
            final InputStream in = new ByteArrayInputStream( out.toByteArray() );
            return new ConfigurationSource( in );
        } catch( final IOException ioe ) {
            LOG.error( "Unable to load the properties file into Log4j2, default Log4J2 configuration will be applied.", ioe );
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void contextDestroyed( final ServletContextEvent sce ) {
    }

}
