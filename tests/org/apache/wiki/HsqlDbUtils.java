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
package org.apache.wiki;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.hsqldb.Server;
import org.hsqldb.util.SqlFile;


/**
 * Helper class to handle Hypersonic Server connections and SQL scripts executions.
 * 
 * Standard usage:
 * <code>
 * <ol>
 *   <li>HsqlDbUtils hsqldbu = new HsqlDbUtils()</li>
 *   <li>hsqldbu.start()</li>
 *   <li>hsqldbu.exec( "tests/etc/db/hsql/userdb-setup.ddl" )</li>
 *   <li>hsqldbu.exec( "tests/etc/db/hsql/userdb-teardown.ddl" )</li>
 *   <li>hsqldbu.stop()</li>
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
    
    Properties hProps = null;
    Server hsqlServer = null;
    
    /**
     * Convenience Hypersonic startup method for unit tests.
     */
    public void setUp() 
    {
        try
        {
            start();
        }
        catch( Exception e )
        {
            LOG.error( e.getMessage(), e );
        }
        exec( "tests/etc/db/hsql/userdb-setup.ddl" );
    }
    
    /**
     * Convenience Hypersonic shutdown method for unit tests.
     */
    public void tearDown() 
    {
        exec( "tests/etc/db/hsql/userdb-teardown.ddl" );
        stop();
    }
    
    /**
     * Starts the Hypersonic server.
     */
    public void start() throws Exception
    {
        // pre-checks
        File lock = new File( "tests/etc/db/hsql/jspwiki.lck" );
        if( lock.exists() ) 
        {
            lock.delete();
        }
        
        Thread.sleep( 5000L );
        
        if( lock.exists() ) 
        {
            throw new InternalWikiException( "Another HSQL server seems to be running" );
        }
        
        // start Hypersonic server
        Properties hProps = loadPropertiesFrom( "tests/etc/db/hsql/server.properties" );
        
        hsqlServer = new Server();
        hsqlServer.setPort( Integer.valueOf( hProps.getProperty( "server.port" ) ) );
        hsqlServer.setDatabaseName( 0, hProps.getProperty( "server.dbname.0" ) );
        hsqlServer.setDatabasePath( 0, hProps.getProperty( "server.database.0" ) );
        hsqlServer.start();
        
        Class.forName( "org.hsqldb.jdbcDriver" );
        
        if( !lock.exists() ) 
        {
            throw new InternalWikiException( "Unable to start HSQL server, check logs" );
        }
    }
    
    /**
     * Executes the given script.
     * 
     * @param file script to execute.
     */
    public void exec( String file ) 
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            SqlFile userDbSetup = new SqlFile( new File( file ), false, null );
            userDbSetup.execute( conn, true );
        }
        catch( Exception e ) 
        {
            LOG.error( e.getMessage(), e );
        }
        finally
        {
            close( conn ); 
        }
    }

    /**
     * Stops the Hypersonic server.
     */
    public void stop() 
    {
        LOG.info( "Shutting down Hypersonic JDBC server on localhost." );
        if( hsqlServer != null ) 
        {
            Connection conn = null;
            try
            {
                conn = getConnection();
                conn.setAutoCommit( true );
                conn.prepareStatement( "SHUTDOWN" ).execute();
            }
            catch( Exception e )
            {
                LOG.error( e.getMessage(), e );
            }
            finally
            {
                close( conn );
            }
            
            hsqlServer.stop();
        }
    }
    
    /**
     * Obtains a {@link Connection}.
     * 
     * @return the obtained {@link Connection}.
     * @throws IOException problems occurred loading jdbc properties file.
     * @throws SQLException problems occurred obtaining the {@link Connection}.
     */
    Connection getConnection() throws IOException, SQLException
    {
        Connection conn;
        Properties jProps = loadPropertiesFrom( "tests/etc/db/jdbc.properties" );
        conn = DriverManager.getConnection( jProps.getProperty( "jdbc.driver.url" ), 
                                            jProps.getProperty( "jdbc.admin.id" ),
                                            jProps.getProperty( "jdbc.admin.password" ) );
        return conn;
    }
    
    /**
     * Closes the given {@link Connection}.
     * 
     * @param conn given {@link Connection}.
     */
    void close( Connection conn ) 
    {
        if( conn != null ) 
        {
            try
            {
                conn.close();
            }
            catch( SQLException e )
            {
                conn = null;
            }
        } 
    }
    
    /**
     * Loads a {@link Properties} object with {@code fileLocation}.
     * 
     * @param fileLocation properties file
     * @return {@link Properties} holding {@code fileLocation} properties.
     * @throws IOException if {@code fileLocation} cannot be readed.
     */
    Properties loadPropertiesFrom( String fileLocation ) throws IOException 
    {
        Properties p = new Properties();
        InputStream inStream = new BufferedInputStream( new FileInputStream( fileLocation ) );
        p.load( inStream );
        inStream.close();
        return p;
    }
    
}
