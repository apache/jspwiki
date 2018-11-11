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

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * Mock JDBC DataSource class that manages JDBC connections to a database whose
 * driver class, JDBC JAR location and connection details are specified in an
 * arbitrary propreties file. Gemerally, we pass on any exceptions encountered
 * as unchecked, since it means that the test case that references this class is
 * failing somehow.
 */
public class TestJDBCDataSource implements DataSource
{
    private static Driver       m_driver;

    protected static final String PROPERTY_DRIVER_CLASS  = "jdbc.driver.class";

    protected static final String PROPERTY_DRIVER_JAR    = "jdbc.driver.jar";

    protected static final String PROPERTY_DRIVER_URL    = "jdbc.driver.url";

    protected static final String PROPERTY_USER_ID       = "jdbc.user.id";

    protected static final String PROPERTY_USER_PASSWORD = "jdbc.user.password";

    protected String              m_jdbcPassword         = null;

    protected String              m_jdbcURL              = null;

    protected String              m_jdbcUser             = null;

    protected int                 m_timeout              = 0;

    protected PrintWriter         m_writer               = null;
    
   /**
     * Constructs a new instance of this class, using a supplied properties
     * File as the source for JDBC driver properties.
     * @param file the properties file containing JDBC properties
     * @throws Exception
     */
    public TestJDBCDataSource( File file ) throws Exception
    {
        super();
        initializeJDBC( file );
    }

    /**
     * Returns a JDBC connection using the specified username and password.
     * @return the database connection
     * @see javax.sql.DataSource#getConnection()
     */
    @Override
    public Connection getConnection() throws SQLException
    {
        return getConnection( m_jdbcUser, m_jdbcPassword );
    }

    /**
     * Returns a JDBC connection to the database.
     * @return the database connection
     * @see javax.sql.DataSource#getConnection(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Connection getConnection( String username, String password ) throws SQLException
    {
        Properties connProperties = new Properties();
        connProperties.put( "user", m_jdbcUser );
        connProperties.put( "password", m_jdbcPassword );
        Connection connection = m_driver.connect( m_jdbcURL, connProperties );
        return connection;
    }

    /**
     * Returns the login timeout for the data source.
     * @return the login timeout, in seconds
     * @see javax.sql.DataSource#getLoginTimeout()
     */
    @Override
    public int getLoginTimeout() throws SQLException
    {
        return m_timeout;
    }

    /**
     * Returns the log writer for the data source.
     * @return the log writer
     * @see javax.sql.DataSource#getLogWriter()
     */
    @Override
    public PrintWriter getLogWriter() throws SQLException
    {
        return m_writer;
    }

    /**
     * Sets the login timeout for the data source. Doesn't do anything, really.
     * @param seconds the login timeout, in seconds
     * @see javax.sql.DataSource#setLoginTimeout(int)
     */
    @Override
    public void setLoginTimeout( int seconds ) throws SQLException
    {
        this.m_timeout = seconds;
    }

    /**
     * Sets the log writer for the data source. Isn't used for anything, really.
     * @param out the log writer
     * @see javax.sql.DataSource#setLogWriter(java.io.PrintWriter)
     */
    @Override
    public void setLogWriter( PrintWriter out ) throws SQLException
    {
        this.m_writer = out;
    }

    /**
     * Return the parent Logger of all the Loggers used by this data source. This should be 
     * the Logger farthest from the root Logger that is still an ancestor of all of the Loggers used by this data source. Configuring this Logger will affect all of the log messages generated by the data source. In the worst case, this may be the root Logger.
     * @return the parent Logger for this data source
     */
    @Override
    public Logger getParentLogger() 
    {
        return null;
    }

    /**
     * Initialization method that reads a File, and attempts to locate and load
     * the JDBC driver from properties specified therein.
     * @param file the file containing the JDBC properties
     * @throws SQLException
     */
    protected void initializeJDBC( File file ) throws Exception
    {
        // Load the properties JDBC properties file
        Properties properties;
        properties = new Properties();
        FileInputStream is = new FileInputStream( file );
        properties.load( is );
        is.close();
        m_jdbcURL = properties.getProperty( PROPERTY_DRIVER_URL );
        m_jdbcUser = properties.getProperty( PROPERTY_USER_ID );
        m_jdbcPassword = properties.getProperty( PROPERTY_USER_PASSWORD );

        // Identifiy the class and JAR we need to load
        String clazz = properties.getProperty( PROPERTY_DRIVER_CLASS );
        String driverFile = properties.getProperty( PROPERTY_DRIVER_JAR );

        // Construct an URL for loading the file
        final URL driverURL = new URL( "file:" + driverFile );

        // Load the driver using the sytem class loader
        final ClassLoader parent = ClassLoader.getSystemClassLoader();
        URLClassLoader loader = AccessController.doPrivileged( new PrivilegedAction<URLClassLoader>() {
            @Override
            public URLClassLoader run() {
                return new URLClassLoader( new URL[] { driverURL }, parent );
            }
        });
        Class< ? > driverClass = loader.loadClass( clazz );

        // Cache the driver
        m_driver = (Driver) driverClass.newInstance();
    }

    @Override
    public boolean isWrapperFor( Class<?> arg0 ) throws SQLException
    {
        // unused interface methods required for JDK 6
        return false;
    }

    @Override
    public <T> T unwrap( Class<T> arg0 ) throws SQLException
    {
        // unused interface methods required for JDK 6
        return null;
    }

}
