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
package org.apache.wiki.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.HsqlDbUtils;
import org.apache.wiki.auth.Users;
import org.eclipse.jetty.jndi.InitialContextFactory;
import org.eclipse.jetty.jndi.NamingContext;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.webapp.WebAppContext;
import org.hsqldb.jdbc.JDBCDataSource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;


/**
 * Lightweight wrapper that starts and stops an embedded Jetty server on a
 * hard-coded port {@link #HTTP_PORT}. The server can be shut down by sending a
 * request containing the hard-coded string {@link #SHUTDOWN_CMD}.
 */
public class TestContainer {
    private final Server server;

    /**
     * High port that listens for web application requests.
     */
    public static final int HTTP_PORT = 10024;

    public static final String SHUTDOWN_CMD = "/GO_AWAY";
    
    public static final String INITIAL_CONTEXT_FACTORY = "java.naming.factory.initial";
    public static final String INITIAL_CONTEXT_FACTORY_JETTY = "org.eclipse.jetty.jndi.InitialContextFactory";
    public static final String JNDI_ENV_ROOT = "java:comp/env";

    private static final HsqlDbUtils m_hu   = new HsqlDbUtils();

    private static final Logger LOG = LogManager.getLogger( TestContainer.class );

    private static Context initCtx ;
    private static Resource userDB;
    private static Resource groupDB;

    private static final ContextHandlerCollection handlerCollection = new ContextHandlerCollection();
    
    /**
     * Starts up a test server for a particular web application on the specified
     * port (or default if no port was specified).
     * 
     * @param args the command-line arguments
     * @throws Exception - you know, just in case.
     */
    public static void main(final String[] args ) throws Exception {
        // Extract key-value pairs that represent test contexts and directories
        final Map<String, String> apps = extractApps( args );
        if( apps.size() == 0 ) {
            throw new IllegalArgumentException( "No apps supplied!" );
        }

        // If we get here, then everything parsed ok...

        // Create a new server and load up the webapps
        final TestContainer container = new TestContainer();
        for( final Map.Entry<String, String> app : apps.entrySet() ) {
            final String context = app.getKey();
            final String path = app.getValue();
            LOG.error( "Adding context " + context + " at path " + path );
            container.addWebApp( context, path );
        }

        handlerCollection.addHandler( new DefaultHandler() );

        // set up the hsqldb database engine
        m_hu.setUp();

        // Create the connection pool
        final JDBCDataSource cpds = new JDBCDataSource();
        cpds.setDatabase( "jdbc:hsqldb:hsql://localhost/jspwiki" );
        cpds.setLoginTimeout( 10 );
        cpds.setUser( "SA" );
        cpds.setPassword( null );

        // Configure and bind DataSource to JNDI for user database
        userDB = new Resource( "jdbc/UserDatabase", cpds );
        LOG.error( "Configured datasource " + userDB);
        userDB.bindToENC("jdbc/UserDatabase");
        
        // Configure and bind DataSource to JNDI for group database
        groupDB = new Resource( "jdbc/GroupDatabase", cpds );        
        LOG.error( "Configured datasource " + groupDB);
        userDB.bindToENC("jdbc/GroupDatabase");
        
        // Start the server
        try {
            LOG.error( "Starting up test container." );
            container.server.setHandler( handlerCollection );
            final Handler[] currentHandlers = container.server.getHandlers();
            LOG.error( "dumping current handlers" );
            for( final Handler handler : currentHandlers )
            {
                if( handler instanceof HandlerCollection )
                {
                    final Handler[] collection = ((HandlerCollection) handler).getHandlers();
                    for( final Handler h : collection )
                    {
                        LOG.error( "handler: " + h );
                    }
                }
            }
            container.start();
        } catch( final Throwable t ) {
            // userDB.unbindENC();
            // groupDB.unbindENC();
            t.printStackTrace();
            LOG.error( t.getMessage() );
            System.exit( 1 );
        }
    }

    private static Map<String, String> extractApps(final String[] args )
    {
        final Map<String, String> apps = new HashMap<String, String>();
        for( final String arg : args ) {
            final String[] pair = arg.split( "=" );

            // Right length?
            if( pair.length != 2 ) {
                throw new IllegalArgumentException( "Malformed argument '" + arg + "'; expected 'context=path' pattern." );
            }

            // Extract and sanitize first arg
            String context = pair[ 0 ].trim();
            if( !context.startsWith( "/" ) ) {
                context = "/" + context;
            }

            // Extract and verify the path
            final String path = pair[ 1 ].trim();
            final File file = new File( path );
            if( !file.exists() ) {
                throw new IllegalArgumentException( "Path " + path + " does not exist." );
            }
            if( !file.isDirectory() ) {
                throw new IllegalArgumentException( "Path " + path + " cannot be a file; it must be a directory." );
            }

            apps.put( context, path );
        }

        return apps;
    }

    /**
     * Prepares a Jetty server with its HTTP and shutdown handlers. Callers must start the server by calling {@link #start()}.
     * 
     * @throws Exception you know, just in case
     */
    public TestContainer() throws Exception {
        // Initialize JNDI for the server, using the Jetty JNDI packages if not set yet
        // Normally this is set at JVM startup by property -Djava.naming.factory.initial=classname
        String contextFactoryClass = System.getProperty( INITIAL_CONTEXT_FACTORY );
        if ( contextFactoryClass == null ) {
            System.setProperty( INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY_JETTY );
//            ContextFactory.setNameParser( new InitialContextFactory.DefaultParser() );
            LOG.error( "No JNDI context factory found; using org.eclipse.jndi.InitialContextFactory." );
            contextFactoryClass = INITIAL_CONTEXT_FACTORY_JETTY;
        }
        LOG.error( "Initialized JNDI with context factory class=" + contextFactoryClass + "." );
        
        // Bind the "java:comp/env" namespace if not bound already
        initCtx = new InitialContext();
        try {
            initCtx.lookup( JNDI_ENV_ROOT );
        } catch( final NameNotFoundException e ) {
            initCtx.bind( JNDI_ENV_ROOT, new NamingContext(new Hashtable<String, Object>(), JNDI_ENV_ROOT, null, new InitialContextFactory.DefaultParser()) );
            LOG.error( "No JNDI " + JNDI_ENV_ROOT + " namespace found; creating it," );
        }
        LOG.info( "Initialized JNDI " + JNDI_ENV_ROOT + " namespace.=" + contextFactoryClass );
        
        // Initialize new Jetty server
        LOG.info( "Creating new test container." );
        System.setProperty( "org.eclipse.xml.XmlParser.NotValidating", "true" );
        server = new Server();
        server.setStopAtShutdown( true );
        
        // Create HTTP listener
        final NetworkTrafficServerConnector connector = new NetworkTrafficServerConnector( server );
        connector.setHost( "localhost" );
        connector.setPort( HTTP_PORT );
        connector.setIdleTimeout( 60_000 );

        server.setConnectors( new Connector[] {connector} );
        LOG.info( "added HTTP listener for port " + HTTP_PORT );

        // add the shutdown handler
        final ContextHandler shutDownContextHandler = new ContextHandler(SHUTDOWN_CMD);
        shutDownContextHandler.setHandler( new ShutdownHandler());
        handlerCollection.addHandler( shutDownContextHandler );
     }

    /**
     * Configures a test web application
     * 
     * @param context the name of the web m_context; must start with "/"
     * @param path the file path for the WAR file, or expanded WAR directory
     */
    public void addWebApp( final String context, final String path ) {
        // Set the default users and roles for the realm (note that realm name *must* match web.xml <realm-name>
        final UserStore userStore = new UserStore();
        userStore.addUser( Users.ADMIN, new Password(Users.ADMIN_PASS), new String[] {"Authenticated", "Admin"} );
        userStore.addUser( Users.JANNE, new Password(Users.JANNE_PASS), new String[] {"Authenticated"} );
        final HashLoginService loginService = new HashLoginService( "JSPWikiRealm" );
        loginService.setUserStore( userStore );

        final WebAppContext webAppContext = new WebAppContext(path, context);

        // Add a security handler.
        final SecurityHandler csh = new ConstraintSecurityHandler();
        csh.setLoginService( loginService );
        webAppContext.setSecurityHandler( csh );

        LOG.error( "Adding webapp " + context + " for path " + path );
        handlerCollection.addHandler( webAppContext );
    }

    /**
     * Starts the Jetty server
     */
    public void start() throws Exception {
        System.setProperty( "org.eclipse.http.HttpRequest.maxFormContentSize", "0" );
        server.start();
        LOG.error("jetty server started");
    }

    /**
     * Stops the Jetty server
     */
    public void stop() {
        try {
            server.stop();
            LOG.error("jetty server stopped");
        } catch( final Exception ex ) {
            throw new RuntimeException( ex );
        }
    }
    
    /**
     * Handler that shuts down the Jetty server if a request is received containing the shutdown string {@link TestContainer#SHUTDOWN_CMD} .
     */
    public static final class ShutdownHandler extends AbstractHandler {

        @Override
        public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response ) throws IOException, ServletException {
            if( request.getRequestURI().contains( SHUTDOWN_CMD ) ) {
                LOG.error( "stop cmd received, shutting down server" );
                System.exit( 0 );
            } else {
                LOG.error("ignoring request " + request.getRequestURI());
            }
        }
    }
}
