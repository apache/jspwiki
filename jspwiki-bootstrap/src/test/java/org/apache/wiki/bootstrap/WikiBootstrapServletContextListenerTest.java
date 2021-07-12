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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.util.Properties;


@ExtendWith( MockitoExtension.class )
public class WikiBootstrapServletContextListenerTest {

    @Mock
    ServletContext sc;

    @Test
    public void testWikiInit() {
        final ServletContextEvent sce = new ServletContextEvent( sc );
        final WikiBootstrapServletContextListener listener = new WikiBootstrapServletContextListener();
        final Properties properties = listener.initWikiSPIs( sce );

        Assertions.assertEquals( 19, properties.size() );
    }

    @Test
    public void testLoggingFrameworkInit() {
        final ServletContextEvent sce = new ServletContextEvent( sc );
        final WikiBootstrapServletContextListener listener = new WikiBootstrapServletContextListener();
        final Properties properties = listener.initWikiSPIs( sce );

        Assertions.assertTrue( listener.initWikiLoggingFramework( properties ) );
        properties.setProperty( "jspwiki.use.external.logconfig", "true" );
        //Assertions.assertFalse( listener.initWikiLoggingFramework( properties ) );
    }

    @Test
    public void testServletContextListenerLifeCycle() {
        final ServletContextEvent sce = new ServletContextEvent( sc );
        final WikiBootstrapServletContextListener listener = new WikiBootstrapServletContextListener();
        Assertions.assertDoesNotThrow( () -> listener.contextInitialized( sce ) );
        Assertions.assertDoesNotThrow( () -> listener.contextDestroyed( sce ) );
    }

}
