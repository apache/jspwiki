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
package com.ecyrd.jspwiki.web;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;
import org.hsqldb.jdbc.jdbcDataSource;
import org.mortbay.http.*;
import org.mortbay.http.handler.SecurityHandler;
import org.mortbay.jetty.plus.DefaultDataSourceService;
import org.mortbay.jetty.plus.Server;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.jndi.ContextFactory;
import org.mortbay.jndi.InitialContextFactory;
import org.mortbay.jndi.NamingContext;
import org.mortbay.jndi.Util;
import org.mortbay.util.Password;

import com.ecyrd.jspwiki.auth.Users;

/**
 * Lightweight wrapper that starts and stops an embedded Jetty server on a
 * hard-coded port {@link #HTTP_PORT}. The server can be shut down by sending a
 * request to the shutdown port, which is hard-coded to {@link #SHUTDOWN_PORT}.
 */
public class TestContainer
{
    private final Server server;

    /**
     * High port that listens for web application requests.
     */
    public static final int HTTP_PORT = 10024;

    /**
     * High port that listens for shutdown requests.
     */
    public static final int SHUTDOWN_PORT = 19041;
    
    public static final String INITIAL_CONTEXT_FACTORY = "java.naming.factory.initial";

    // Minimum and maximum number of jetty threads
    public static final int MIN_JETTY_THREADS = 1;

    public static final int MAX_JETTY_THREADS = 1024;

    public static final int DEFAULT_JETTY_THREADS = 512;

    /** Number of jetty threads for the server. */
    private static int jettyThreads = DEFAULT_JETTY_THREADS;

    private static final Logger log = Logger.getLogger( TestContainer.class );

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
            log.info( "Adding context " + context + " at path " + path );
            container.addWebApp( context, path );
        }

        // Create the DataSource service
        DefaultDataSourceService dss = new DefaultDataSourceService();
        dss.setName( "DataSourceService" );

        // Create the connection pool
        jdbcDataSource cpds = new jdbcDataSource();
        cpds.setDatabase( "jdbc:hsqldb:hsql://localhost/jspwiki" );
        cpds.setLoginTimeout( 10 );
        cpds.setUser( "SA" );
        cpds.setPassword( null );

        // Configure and bind DataSource to JNDI for user database
        dss.addDataSource( "jdbc/UserDatabase", cpds );
        container.server.addService( dss );
        dss.getDataSource( "jdbc/UserDatabase" );
        
        // Configure and bind DataSource to JNDI for group database
        dss.addDataSource( "jdbc/GroupDatabase", cpds );
        container.server.addService( dss );
        dss.getDataSource( "jdbc/GroupDatabase" );
        
        System.out.println( "Configured datasources." );

        // Start the server
        try
        {
            System.out.println( "Starting up test container." );
            container.start();
        }
        catch( Throwable t )
        {
            t.printStackTrace();
            System.err.println( t.getMessage() );
            System.exit( 1 );
        }
        System.out.println( "Started." );

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
            System.setProperty( INITIAL_CONTEXT_FACTORY, "org.mortbay.jndi.InitialContextFactory" );
            ContextFactory.setNameParser( new InitialContextFactory.DefaultParser() );
            log.info( "No JNDI context factory found; using org.mortbay.jndi.InitialContextFactory." );
        }
        log.info( "Initialized JNDI with context factory class=" + contextFactoryClass + "." );
        
        // Bind the "java:comp" namespace if not bound already
        Context initCtx = new InitialContext();
        try 
        {
            initCtx.lookup( "java:comp" );
        }
        catch ( NameNotFoundException e )
        {
            Util.bind( initCtx, "java:comp", new NamingContext() );
            NamingContext compCtx = (NamingContext) initCtx.lookup( "java:comp" );
            compCtx.setNameParser( new InitialContextFactory.DefaultParser() );
            log.info( "No JNDI java:comp namespace found; creating it," );
            // Context envCtx = compCtx.createSubcontext( "env" );
            // System.out.println( envCtx );
        }
        log.info( "Initialized JNDI java:comp namespace.=" + contextFactoryClass );
        
        // Initialize new Jetty server
        log.info( "Creating new test container." );
        System.setProperty( "org.mortbay.xml.XmlParser.NotValidating", "true" );
        server = new Server();
        server.setStopAtShutdown( true );
        
        // Create HTTP listener
        SocketListener listener = new SocketListener();
        listener.setHost( "localhost" );
        listener.setMaxIdleTimeMs( 60000 );
        listener.setMaxThreads( jettyThreads );
        listener.setPort( HTTP_PORT );
        server.addListener( listener );
        log.info( "...added HTTP listener for port " + HTTP_PORT );

        // Create shutdown listener
        listener = new SocketListener();
        listener.setHost( "localhost" );
        listener.setMaxThreads( jettyThreads );
        listener.setPort( SHUTDOWN_PORT );
        listener.setHttpHandler( new ShutdownHandler() );
        server.addListener( listener );
        log.info( "...added shutdown listener for port " + SHUTDOWN_PORT );
        
        // Set the default users and roles for the realm (note that realm name *must* match web.xml <realm-name>
        HashUserRealm realm = new HashUserRealm( "JSPWikiRealm" );
        realm.put( Users.ADMIN, new Password( Users.ADMIN_PASS ) );
        realm.addUserToRole( Users.ADMIN, "Authenticated" );
        realm.addUserToRole( Users.ADMIN, "Admin" );
        realm.put( Users.JANNE, new Password( Users.JANNE_PASS ) );
        realm.addUserToRole( Users.JANNE, "Authenticated" );
        server.addRealm( realm );
    }

    /**
     * Configures a test web application
     * 
     * @param m_context the name of the web m_context; must start with "/"
     * @param path the file path for the WAR file, or expanded WAR directory
     * @return the configured web application
     * @throws IOException
     */
    public WebApplicationContext addWebApp( String context, String path ) throws IOException
    {
        WebApplicationContext webapp = server.addWebApplication( context, path );
        log.info( "Adding test webapp " + context + " for path " + path );

        // Add a security handler for any constraints enabled by web.xml
        SecurityHandler sh = new SecurityHandler();
        webapp.addHandler( sh );

        return webapp;
    }

    /**
     * Starts the Jetty server
     */
    public void start() throws Exception
    {
        System.setProperty( "org.mortbay.http.HttpRequest.maxFormContentSize", "0" );
        server.start();
    }

    /**
     * Stops the Jetty server
     */
    public void stop()
    {
        try
        {
            server.stop();
        }
        catch( InterruptedException ex )
        {
            throw new RuntimeException( ex );
        }
    }

    /**
     * HTTP Handler that shuts down the Jetty server if a request is received on
     * the shutdown port..
     */
    public static final class ShutdownHandler implements HttpHandler
    {
        private static final long serialVersionUID = -7785141243907081919L;

        private HttpContext m_context;

        /**
         * Returns the HttpContext used to initialize this handler.
         */
        public HttpContext getHttpContext()
        {
            return m_context;
        }

        /**
         * No-op method that always returns a generic description of the
         * shutdown handler.
         */
        public String getName()
        {
            return "Shutdown HTTP handler.";
        }

        /**
         * Intercepts the HTTP request and shuts down the server instantly.
         */
        public void handle( String arg0, String arg1, HttpRequest arg2, HttpResponse arg3 ) throws HttpException, IOException
        {
        	System.exit(0);
//            System.err.println( "Shutdown request detected." );
//            try
//            {
//                m_server.stop( false );
//            }
//            catch( InterruptedException e )
//            {
//                e.printStackTrace();
//                throw new HttpException( HttpResponse.__500_Internal_Server_Error, e.getMessage() );
//            }
        }

        /**
         * No-op method that sets a reference to the HttpContext supplied to the
         * initialize method.
         */
        public void initialize( HttpContext context )
        {
            m_context = context;
        }

        /**
         * No-op method that always returns <code>true</code>.
         */
        public boolean isStarted()
        {
            return true;
        }

        /**
         * No-op method that does nothing.
         */
        public void start() throws Exception
        {
        }

        /**
         * No-op method that does nothing.
         */
        public void stop() throws InterruptedException
        {
        }
    }

}
