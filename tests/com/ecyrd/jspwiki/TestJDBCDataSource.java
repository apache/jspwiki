package com.ecyrd.jspwiki;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

/**
 * Mock JDBC DataSource class that manages JDBC connections to a database whose
 * driver class, JDBC JAR location and connection details are specified in an
 * arbitrary propreties file. Gemerally, we pass on any exceptions encountered
 * as unchecked, since it means that the test case that references this class is
 * failing somehow.
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1 $ $Date: 2005-10-19 12:13:23 $
 */
public class TestJDBCDataSource implements DataSource
{
    protected static Driver       m_driver;

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
    public int getLoginTimeout() throws SQLException
    {
        return m_timeout;
    }

    /**
     * Returns the log writer for the data source.
     * @return the log writer
     * @see javax.sql.DataSource#getLogWriter()
     */
    public PrintWriter getLogWriter() throws SQLException
    {
        return m_writer;
    }

    /**
     * Sets the login timeout for the data source. Doesn't do anything, really.
     * @param seconds the login timeout, in seconds
     * @see javax.sql.DataSource#setLoginTimeout(int)
     */
    public void setLoginTimeout( int seconds ) throws SQLException
    {
        this.m_timeout = seconds;
    }

    /**
     * Sets the log writer for the data source. Isn't used for anything, really.
     * @param out the log writer
     * @see javax.sql.DataSource#setLogWriter(java.io.PrintWriter)
     */
    public void setLogWriter( PrintWriter out ) throws SQLException
    {
        this.m_writer = out;
    }

    /**
     * Initialization method that reads a File, and attempts to locate and load
     * the JDBC driver from properties specified therein.
     * @throws SQLException
     * @param file the file containing the JDBC properties
     */
    protected void initializeJDBC( File file ) throws Exception
    {
        // Load the properties JDBC properties file
        Properties properties;
        properties = new Properties();
        properties.load( new FileInputStream( file ) );
        m_jdbcURL = properties.getProperty( PROPERTY_DRIVER_URL );
        m_jdbcUser = properties.getProperty( PROPERTY_USER_ID );
        m_jdbcPassword = properties.getProperty( PROPERTY_USER_PASSWORD );

        // Identifiy the class and JAR we need to load
        String clazz = properties.getProperty( PROPERTY_DRIVER_CLASS );
        String driverFile = properties.getProperty( PROPERTY_DRIVER_JAR );

        // Construct an URL for loading the file
        URL driverURL = new URL( "file:" + driverFile );

        // Load the driver using the sytem class loader
        ClassLoader parent = ClassLoader.getSystemClassLoader();
        URLClassLoader loader = new URLClassLoader( new URL[]
        { driverURL }, parent );
        Class driverClass = loader.loadClass( clazz );

        // Cache the driver
        m_driver = (Driver) driverClass.newInstance();
    }

}
