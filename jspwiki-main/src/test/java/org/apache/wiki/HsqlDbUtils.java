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
package org.apache.wiki;

import org.apache.log4j.Logger;
import org.hsqldb.Server;
import org.hsqldb.cmdline.SqlFile;

import java.io.*;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;


/**
 * Helper class to handle Hypersonic Server connections and SQL scripts executions.
 * 
 * Standard usage:
 * <code>
 * <ol>
 *   <li>HsqlDbUtils hsqldbu = new HsqlDbUtils()</li>
 *   <li>hsqldbu.start()</li>
 *   <li>hsqldbu.exec( "target/etc/db/hsql/hsql-userdb-setup.ddl" )</li>
 *   <li>hsqldbu.exec( "target/etc/db/hsql/hsql-userdb-teardown.ddl" )</li>
 *   <li>hsqldbu.shutdown()</li>
 * </ol>
 * </code>
 * There are also a couple of convenience methods for unit testing:
 * <code>
 * <ol>
 *   <li>hsqldbu.setUp()</li>
 *   <li>hsqldbu.tearDown()</li>
 * </ol>
 * </code>
 */
public class HsqlDbUtils
{
    
    private static final Logger LOG = Logger.getLogger( HsqlDbUtils.class );
    
    Server hsqlServer = null;
    int localPort = 0;
    
    /**
     * Convenience Hypersonic startup method for unit tests.
     */
    public void setUp() {
        try {
            startOnRandomPort();
        } catch( final Exception e ) {
            LOG.error( e.getMessage(), e );
        }
        exec( "src/test/config/hsql-userdb-setup.ddl" );
    }
    
    /**
     * Convenience Hypersonic shutdown method for unit tests.
     */
    public void tearDown() {
        exec( "src/test/config/hsql-userdb-teardown.ddl" );
        shutdown();
    }
    
    /**
     * Starts the Hypersonic server.
     */
    public void start() throws Exception {
        final Properties hProps = loadPropertiesFrom( "target/test-classes/jspwiki-custom.properties" );
        localPort =  Integer.parseInt( hProps.getProperty( "server.port" ) );
        startHsqlServer();
    }

    /**
     * Starts the Hypersonic server.
     */
    public void startOnRandomPort() throws Exception {
        localPort = findFreeTcpPort();
        startHsqlServer();
    }

    int findFreeTcpPort() throws Exception {
        try( final ServerSocket socket = new ServerSocket( 0 ) ) {
            socket.setReuseAddress( true );
            return socket.getLocalPort();
        }
    }

    void startHsqlServer() throws Exception {
        // start Hypersonic server
        final Properties hProps = loadPropertiesFrom( "target/test-classes/jspwiki-custom.properties" );

        hsqlServer = new Server();
        hsqlServer.setSilent( true );     // be quiet during junit tests
        hsqlServer.setLogWriter( null );  // and even more quiet
        // pre-checks
        hsqlServer.checkRunning( false ); // throws RuntimeException if running

        // configure
        hsqlServer.setPort( localPort );
        hsqlServer.setDatabaseName( 0, hProps.getProperty( "server.dbname.0" ) );
        hsqlServer.setDatabasePath( 0, hProps.getProperty( "server.database.0" ) );
        hsqlServer.start();

        Class.forName( "org.hsqldb.jdbc.JDBCDriver" );
        hsqlServer.checkRunning( true ); // throws RuntimeException if not running
    }
    
    /**
     * Executes the given script.
     * 
     * @param file script to execute.
     */
    public void exec(final String file )
    {
        try( final Connection conn = getConnection() )
        {
            final SqlFile userDbSetup = new SqlFile( new File( file ) );
            userDbSetup.setConnection(conn);
            userDbSetup.execute();
        }
        catch( final Exception e )
        {
            LOG.error( e.getMessage(), e );
        }
    }

    /**
     * Shutdown the Hypersonic server.
     */
    public void shutdown() {
        LOG.info( "Shutting down Hypersonic JDBC server on localhost." );
        if( hsqlServer != null ) {
            try( final Connection conn = getConnection() ) {
                conn.setAutoCommit( true );
                conn.prepareStatement( "SHUTDOWN" ).execute();
            } catch( final Exception e ) {
                LOG.error( e.getMessage(), e );
            }

            hsqlServer.shutdown();
        }
    }
    
    /**
     * Obtains a {@link Connection}.
     * 
     * @return the obtained {@link Connection}.
     * @throws IOException problems occurred loading jdbc properties file.
     * @throws SQLException problems occurred obtaining the {@link Connection}.
     */
    Connection getConnection() throws IOException, SQLException {
        final Properties jProps = loadPropertiesFrom( "target/test-classes/jspwiki-custom.properties" );
        return DriverManager.getConnection( getDriverUrl(),
                                            jProps.getProperty( "jdbc.admin.id" ),
                                            jProps.getProperty( "jdbc.admin.password" ) );
    }

    public String getDriverUrl() throws IOException{
        final Properties jProps = loadPropertiesFrom( "target/test-classes/jspwiki-custom.properties" );
        return jProps.getProperty( "jdbc.driver.url" ).replace( ":9321", ":" + localPort );
    }
    
    /**
     * Loads a {@link Properties} object with {@code fileLocation}.
     * 
     * @param fileLocation properties file
     * @return {@link Properties} holding {@code fileLocation} properties.
     * @throws IOException if {@code fileLocation} cannot be readed.
     */
    Properties loadPropertiesFrom( final String fileLocation ) throws IOException {
        final Properties p = new Properties();
        final InputStream inStream = new BufferedInputStream( new FileInputStream( fileLocation ) );
        p.load( inStream );
        inStream.close();
        return p;
    }
    
}
