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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.wiki.HsqlDbUtils;
import org.apache.wiki.auth.Users;
import org.eclipse.jetty.jndi.InitialContextFactory;
import org.eclipse.jetty.jndi.NamingContext;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.webapp.WebAppContext;
import org.hsqldb.jdbc.JDBCDataSource;


/**
 * Lightweight wrapper that starts and stops an embedded Jetty server on a
 * hard-coded port {@link #HTTP_PORT}. The server can be shut down by sending a
 * request containing the hard-coded string {@link #SHUTDOWN_CMD}.
 */
public class TestContainer
{
    private final Server server;

    /**
     * High port that listens for web application requests.
     */
    public static final int HTTP_PORT = 10024;

    public static final String SHUTDOWN_CMD = "/GO_AWAY";
    
    public static final String INITIAL_CONTEXT_FACTORY = "java.naming.factory.initial";
    public static final String INITIAL_CONTEXT_FACTORY_JETTY = "org.eclipse.jetty.jndi.InitialContextFactory";
    public static final String JNDI_ENV_ROOT = "java:comp/env";

    private static HsqlDbUtils m_hu   = new HsqlDbUtils();

    private static final Logger log = Logger.getLogger( TestContainer.class );

    private static Context initCtx ;
    private static Resource userDB = null;        
    private static Resource groupDB = null;        

    private static ContextHandlerCollection handlerCollection = new ContextHandlerCollection();
    
    /**
     * Starts up a test server for a particular web application on the specified
     * port (or default if no port was specified).
     * 
     * @param args the command-line arguments
     * @throws Exception - you know, just in case.
     */
    public static void main( String[] args ) throws Exception
    {
        // Extract key-value pairs that represent test contexts and directories
        Map<String, String> apps = extractApps( args );
        if( apps.size() == 0 )
        {
            throw new IllegalArgumentException( "No apps supplied!" );
        }

        // If we get here, then everything parsed ok...

        // Create a new server and load up the webapps
        TestContainer container = new TestContainer();
        for( Map.Entry<String, String> app : apps.entrySet() )
        {
            String context = app.getKey();
            String path = app.getValue();
            log.error( "Adding context " + context + " at path " + path );
            container.addWebApp( context, path );
        }

        handlerCollection.addHandler( new DefaultHandler() );

        // setup the hsqldb database engine
        m_hu.setUp();

        // Create the connection pool
        JDBCDataSource cpds = new JDBCDataSource();
        cpds.setDatabase( "jdbc:hsqldb:hsql://localhost/jspwiki" );
        cpds.setLoginTimeout( 10 );
        cpds.setUser( "SA" );
        cpds.setPassword( null );

        // Configure and bind DataSource to JNDI for user database
        userDB = new Resource( "jdbc/UserDatabase", cpds );
        log.error( "Configured datasource " + userDB);
        userDB.bindToENC("jdbc/UserDatabase");
        
        // Configure and bind DataSource to JNDI for group database
        groupDB = new Resource( "jdbc/GroupDatabase", cpds );        
        log.error( "Configured datasource " + groupDB);
        userDB.bindToENC("jdbc/GroupDatabase");
        
        // Start the server
        try
        {
            log.error( "Starting up test container." );
            container.server.setHandler( handlerCollection );
            Handler[] currentHandlers = container.server.getHandlers();
            log.error( "dumping current handlers" );
            for( Handler handler : currentHandlers )
            {
                if( handler instanceof HandlerCollection )
                {
                    Handler[] collection = ((HandlerCollection) handler).getHandlers();
                    for( Handler h : collection )
                    {
                        log.error( "handler: " + h );
                    }
                }
            }
            container.start();
        }
        catch( Throwable t )
        {
            // userDB.unbindENC();
            // groupDB.unbindENC();
            t.printStackTrace();
            log.error( t.getMessage() );
            System.exit( 1 );
        }
    }

    private static Map<String, String> extractApps( String[] args )
    {
        Map<String, String> apps = new HashMap<String, String>();
        for( int i = 0; i < args.length; i++ )
        {
            String[] pair = args[i].split( "=" );

            // Right length?
            if( pair.length != 2 )
            {
                throw new IllegalArgumentException( "Malformed argument '" + args[i] + "'; expected 'context=path' pattern." );
            }

            // Extract and sanitize first arg
            String context = pair[0].trim();
            if( !context.startsWith( "/" ) )
            {
                context = "/" + context;
            }

            // Extract and verify the path
            String path = pair[1].trim();
            File file = new File( path );
            if( !file.exists() )
            {
                throw new IllegalArgumentException( "Path " + path + " does not exist." );
            }
            if( !file.isDirectory() )
            {
                throw new IllegalArgumentException( "Path " + path + " cannot be a file; it must be a directory." );
            }

            apps.put( context, path );
        }

        return apps;
    }

    /**
     * Prepares a Jetty server with its HTTP and shutdown handlers. Callers must
     * start the server by calling {@link #start()}.
     * 
     * @throws Exception you know, just in case
     */
    public TestContainer() throws Exception
    {
        // Initialize JNDI for the server, using the Jetty JNDI packages if not set yet
        // Normally this is set at JVM startup by property -Djava.naming.factory.initial=classname
        String contextFactoryClass = System.getProperty( INITIAL_CONTEXT_FACTORY );
        if ( contextFactoryClass == null )
        {
            System.setProperty( INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY_JETTY );
//            ContextFactory.setNameParser( new InitialContextFactory.DefaultParser() );
            log.error( "No JNDI context factory found; using org.eclipse.jndi.InitialContextFactory." );
            contextFactoryClass = INITIAL_CONTEXT_FACTORY_JETTY;
        }
        log.error( "Initialized JNDI with context factory class=" + contextFactoryClass + "." );
        
        // Bind the "java:comp/env" namespace if not bound already
        initCtx = new InitialContext();
        try 
        {
            initCtx.lookup( JNDI_ENV_ROOT );
        }
        catch ( NameNotFoundException e )
        {
            initCtx.bind( JNDI_ENV_ROOT, new NamingContext(new Hashtable<String, Object>(), JNDI_ENV_ROOT, null, new InitialContextFactory.DefaultParser()) );
            log.error( "No JNDI " + JNDI_ENV_ROOT + " namespace found; creating it," );
        }
        log.info( "Initialized JNDI " + JNDI_ENV_ROOT + " namespace.=" + contextFactoryClass );
        
        // Initialize new Jetty server
        log.info( "Creating new test container." );
        System.setProperty( "org.eclipse.xml.XmlParser.NotValidating", "true" );
        server = new Server();
        server.setStopAtShutdown( true );
        
        // Create HTTP listener
        SocketConnector connector = new SocketConnector();
        connector.setHost( "localhost" );
        connector.setPort( HTTP_PORT );
        connector.setMaxIdleTime( 60000 );

        server.setConnectors( new Connector[] {connector} );
        log.info( "added HTTP listener for port " + HTTP_PORT );

        // add the shutdown handler
        ContextHandler shutDownContextHandler = new ContextHandler(SHUTDOWN_CMD);
        shutDownContextHandler.setHandler( new ShutdownHandler());
        handlerCollection.addHandler( shutDownContextHandler );
     }

    /**
     * Configures a test web application
     * 
     * @param context the name of the web m_context; must start with "/"
     * @param path the file path for the WAR file, or expanded WAR directory
     * @throws IOException
     */
    public void addWebApp( String context, String path ) throws IOException
    {
        // Set the default users and roles for the realm (note that realm name *must* match web.xml <realm-name>
        HashLoginService loginService = new HashLoginService( "JSPWikiRealm" );
        loginService.putUser( Users.ADMIN, new Password(Users.ADMIN_PASS), new String[] {"Authenticated", "Admin"} );
        loginService.putUser( Users.JANNE, new Password(Users.JANNE_PASS), new String[] {"Authenticated"} );

        WebAppContext webAppContext = new WebAppContext(path, context);

        // Add a security handler.
        SecurityHandler csh = new ConstraintSecurityHandler();
        csh.setLoginService( loginService );
        webAppContext.setSecurityHandler( csh );

        log.error( "Adding webapp " + context + " for path " + path );
        handlerCollection.addHandler( webAppContext );

    }

    /**
     * Starts the Jetty server
     */
    public void start() throws Exception
    {
        System.setProperty( "org.eclipse.http.HttpRequest.maxFormContentSize", "0" );
        server.start();
        log.error("jetty server started");
    }

    /**
     * Stops the Jetty server
     */
    public void stop()
    {
        try
        {
            server.stop();
            log.error("jetty server stopped");
        }
        catch( Exception ex )
        {
            throw new RuntimeException( ex );
        }
    }
    
    
    
    /**
     * Handler that shuts down the Jetty server if a request is received containing the shutdown string {@link TestContainer#SHUTDOWN_CMD} .
     */

    public static final class ShutdownHandler extends AbstractHandler
    {

        public void handle( String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response )
                                                                                                                          throws IOException,
                                                                                                                              ServletException
        {
            if( request.getRequestURI().indexOf( SHUTDOWN_CMD ) != -1 )
            {
                log.error( "stop cmd received, shutting down server" );
                System.exit( 0 );
            } else {
                log.error("ignoring request " + request.getRequestURI());
            }
        }
    }
}
