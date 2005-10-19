package com.ecyrd.jspwiki.auth.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiSecurityException;

/**
 * <p>Implementation of UserDatabase that persists {@link DefaultUserProfile}
 * objects to a JDBC DataSource, as might typically be provided by a web 
 * container. This implementation looks up the JDBC DataSource using JNDI. 
 * The JNDI name of the datasource, backing table and mapped columns used 
 * by this class are configured via settings in <code>jspwiki.properties</code>.</p>
 * <p>Configurable properties are these:</p>
 * <table>
 *   <tr>
 *   <thead>
 *     <th>Property</th>
 *     <th>Default</th>
 *     <th>Definition</th>
 *   <thead>
 *   </tr>
 *   <tr>
 *     <td><code>jspwiki.userdatabase.datasource</code></td>
 *     <td><code>jdbc/UserDatabase</code></td>
 *     <td>The JNDI name of the DataSource</td>
 *   </tr>
 *   <tr>
 *     <td><code>jspwiki.userdatabase.table</code></td>
 *     <td><code>users</code></td>
 *     <td>The table that stores the user profiles</td>
 *   </tr>
 *   <tr>
 *     <td><code>jspwiki.userdatabase.created</code></td>
 *     <td><code>created</code></td>
 *     <td>The column containing the profile's creation timestamp</td>
 *   </tr>
 *   <tr>
 *     <td><code>jspwiki.userdatabase.email</code></td>
 *     <td><code>email</code></td>
 *     <td>The column containing the user's e-mail address</td>
 *   </tr>
 *   <tr>
 *     <td><code>jspwiki.userdatabase.fullName</code></td>
 *     <td><code>full_name</code></td>
 *     <td>The column containing the user's full name</td>
 *   </tr>
 *   <tr>
 *     <td><code>jspwiki.userdatabase.loginName</code></td>
 *     <td><code>login_name</code></td>
 *     <td>The column containing the user's login id</td>
 *   </tr>
 *   <tr>
 *     <td><code>jspwiki.userdatabase.password</code></td>
 *     <td><code>password</code></td>
 *     <td>The column containing the user's password</td>
 *   </tr>
 *   <tr>
 *     <td><code>jspwiki.userdatabase.modified</code></td>
 *     <td><code>modified</code></td>
 *     <td>The column containing the profile's last-modified timestamp</td>
 *   </tr>
 *   <tr>
 *     <td><code>jspwiki.userdatabase.wikiName</code></td>
 *     <td><code>wiki_name</code></td>
 *     <td>The column containing the user's wiki name</td>
 *   </tr>
 * </table>
 * <p>This class hashes passwords using SHA-1. All of the underying SQL commands used by this class are implemented using
 * prepared statements, so it is immune to SQL injection attacks.</p>
 * <p>This class is typically used in conjunction with a web container's JNDI resource
 * factory. For example, Tomcat versions 4 and higher provide a basic JNDI factory
 * for registering DataSources. To give JSPWiki access to the JNDI resource named
 * by <code></code>, you would declare the datasource resource similar to this:</p>
 * <blockquote><code>&lt;Context ...&gt;<br/>
 *  &nbsp;&nbsp;...<br/>
 *  &nbsp;&nbsp;&lt;Resource name="jdbc/UserDatabase" auth="Container"<br/>
 *  &nbsp;&nbsp;&nbsp;&nbsp;type="javax.sql.DataSource" username="dbusername" password="dbpassword"<br/>
 *  &nbsp;&nbsp;&nbsp;&nbsp;driverClassName="org.hsql.jdbcDriver" url="jdbc:HypersonicSQL:database"<br/>
 *  &nbsp;&nbsp;&nbsp;&nbsp;maxActive="8" maxIdle="4"/&gt;<br/>
 *  &nbsp;...<br/>
 * &lt;/Context&gt;</code></blockquote>
 * <p>JDBC driver JARs should be added to Tomcat's <code>common/lib</code> directory.
 * For more Tomcat 5.5 JNDI configuration examples, 
 * see <a href="http://tomcat.apache.org/tomcat-5.5-doc/jndi-resources-howto.html">
 * http://tomcat.apache.org/tomcat-5.5-doc/jndi-resources-howto.html</a>.
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1 $ $Date: 2005-10-19 12:09:19 $
 * @since 2.3
 */public class JDBCUserDatabase extends AbstractUserDatabase
{

    public static final String DEFAULT_DB_CREATED    = "created";

    public static final String DEFAULT_DB_EMAIL      = "email";

    public static final String DEFAULT_DB_FULL_NAME  = "full_name";

    public static final String DEFAULT_DB_JNDI_NAME  = "jdbc/UserDatabase";

    public static final String DEFAULT_DB_MODIFIED   = "modified";

    public static final String DEFAULT_DB_TABLE      = "users";

    public static final String DEFAULT_DB_LOGIN_NAME = "login_name";
 
    public static final String DEFAULT_DB_PASSWORD   = "password";
    
    public static final String DEFAULT_DB_WIKI_NAME  = "wiki_name";

    public static final String PROP_DB_CREATED       = "jspwiki.userdatabase.created";

    public static final String PROP_DB_EMAIL         = "jspwiki.userdatabase.email";

    public static final String PROP_DB_FULL_NAME     = "jspwiki.userdatabase.fullName";

    public static final String PROP_DB_DATASOURCE    = "jspwiki.userdatabase.datasource";

    public static final String PROP_DB_LOGIN_NAME    = "jspwiki.userdatabase.loginName";

    public static final String PROP_DB_MODIFIED      = "jspwiki.userdatabase.modified";

    public static final String PROP_DB_PASSWORD      = "jspwiki.userdatabase.password";

    public static final String PROP_DB_TABLE         = "jspwiki.userdatabase.table";

    public static final String PROP_DB_WIKI_NAME     = "jspwiki.userdatabase.wikiName";

    private DataSource         m_ds                  = null;
    private String m_findByEmail = null;
    private String m_findByFullName = null;
    private String m_findByLoginName = null;
    private String m_findByWikiName = null;
    private String m_updateProfile = null;
    private String m_findAll = null;
    private String m_insertProfile = null;
    private String m_table = null;
    private String m_email = null;
    private String m_fullName = null;
    private String m_loginName = null;
    private String m_password = null;
    private String m_wikiName = null;
    private String m_created = null;
    private String m_modified = null;

    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#commit()
     */
    public void commit() throws WikiSecurityException
    {
        log.info("Committing transactions.");
    }

    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByEmail(java.lang.String)
     */
    public UserProfile findByEmail( String index ) throws NoSuchPrincipalException
    {
        return findByPreparedStatement( m_findByEmail, index );
    }

    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByFullName(java.lang.String)
     */
    public UserProfile findByFullName( String index ) throws NoSuchPrincipalException
    {
        return findByPreparedStatement( m_findByFullName, index );
    }

    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByLoginName(java.lang.String)
     */
    public UserProfile findByLoginName( String index ) throws NoSuchPrincipalException
    {
        return findByPreparedStatement( m_findByLoginName, index );
    }

    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByWikiName(java.lang.String)
     */
    public UserProfile findByWikiName( String index ) throws NoSuchPrincipalException
    {
        return findByPreparedStatement( m_findByWikiName, index );
    }

    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#initialize(com.ecyrd.jspwiki.WikiEngine,
     * java.util.Properties)
     */
    public void initialize( WikiEngine engine, Properties props ) throws NoRequiredPropertyException
    {
        String jndiName = props.getProperty( PROP_DB_DATASOURCE, DEFAULT_DB_JNDI_NAME );
        try
        {
            Context initCtx = new InitialContext();
            Context ctx = (Context) initCtx.lookup("java:comp/env");
            m_ds = (DataSource) ctx.lookup( jndiName );
            
            // Prepare the SQL selectors
            m_table     = props.getProperty( PROP_DB_TABLE, DEFAULT_DB_TABLE );
            m_email     = props.getProperty( PROP_DB_EMAIL, DEFAULT_DB_EMAIL );
            m_fullName  = props.getProperty( PROP_DB_FULL_NAME, DEFAULT_DB_FULL_NAME );
            m_loginName = props.getProperty( PROP_DB_LOGIN_NAME, DEFAULT_DB_LOGIN_NAME );
            m_password  = props.getProperty( PROP_DB_PASSWORD, DEFAULT_DB_PASSWORD );
            m_wikiName  = props.getProperty( PROP_DB_WIKI_NAME, DEFAULT_DB_WIKI_NAME );
            m_created   = props.getProperty( PROP_DB_CREATED, DEFAULT_DB_CREATED );
            m_modified  = props.getProperty( PROP_DB_MODIFIED, DEFAULT_DB_MODIFIED );

            m_findAll         = "SELECT * FROM " + m_table;
            m_findByEmail     = "SELECT * FROM " + m_table + " WHERE " + m_email + "=?";
            m_findByFullName  = "SELECT * FROM " + m_table + " WHERE " + m_fullName + "=?";
            m_findByLoginName = "SELECT * FROM " + m_table + " WHERE " + m_loginName + "=?";
            m_findByWikiName  = "SELECT * FROM " + m_table + " WHERE " + m_wikiName + "=?";
            m_insertProfile   = "INSERT INTO " + m_table + " ("
                              + m_email + "," 
                              + m_fullName + ","
                              + m_password + ","
                              + m_wikiName + ","
                              + m_modified + ","
                              + m_loginName + ","
                              + m_created
                              + ") VALUES (?,?,?,?,?,?,?)";
            m_updateProfile   = "UPDATE " + m_table + " SET "
                              + m_email + "=?,"
                              + m_fullName + "=?,"
                              + m_password + "=?,"
                              + m_wikiName + "=?,"
                              + m_modified + "=? WHERE " + m_loginName + "=?";
            
        }
        catch( NamingException e )
        {
            log.error( "JDBCUserDatabase initialization error: " + e.getMessage() );
            throw new NoRequiredPropertyException( PROP_DB_DATASOURCE, "JDBCUserDatabase initialization error: " + e.getMessage() );
        }
        
        // Test connection by doing a quickie select
        try
        {
            Connection conn = m_ds.getConnection();
            PreparedStatement ps = conn.prepareStatement( m_findAll );
            ResultSet rs = ps.executeQuery();
            conn.close();
        }
        catch ( SQLException e )
        {
            log.error( "JDBCUserDatabase initialization error: " + e.getMessage() );
            throw new NoRequiredPropertyException( PROP_DB_DATASOURCE, "JDBCUserDatabase initialization error: " + e.getMessage() );
        }
        log.info( "JDBCUserDatabase initialized from JNDI DataSource: " + jndiName );
    }

    /**
     * Attempts to find the user profile first by LoginName.
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#save(com.ecyrd.jspwiki.auth.user.UserProfile)
     */
    public void save( UserProfile profile ) throws WikiSecurityException
    {
        // Get database connection
        Connection conn;
        try 
        {
            conn = m_ds.getConnection();
        }
        catch ( Exception e )
        {
            throw new WikiSecurityException( e.getMessage() );
        }
        
        // Figure out which prepared statement to use & execute it
        String loginName = profile.getLoginName();
        PreparedStatement ps = null;
        UserProfile existingProfile = null;
        try 
        {
            existingProfile = findByLoginName( loginName );
        }
        catch ( NoSuchPrincipalException e )
        {
            // Existing profile will be null
        }
        
        // Get a clean password from the passed profile.
        // Hash the password if it isn't null, doesn't exist, or has changed.
        // Blank password is the same as null
        String password = profile.getPassword();
        String existingPassword = ( existingProfile == null ) ? null : existingProfile.getPassword();
        if ( password.equals("") )
        {
            password = null;
        }
        if ( password != null) 
            {
            if ( existingPassword != null || !password.equals( existingPassword ) ) 
            {
                password =  SHA_PREFIX + getHash( password );
            }
        }
        
        try
        {
            Timestamp ts = new Timestamp( System.currentTimeMillis() );
            if ( existingProfile == null )
            {
                // User is new
                ps = conn.prepareStatement( m_insertProfile );
                ps.setString(1, profile.getEmail() );
                ps.setString(2, profile.getFullname() );
                ps.setString(3, password );
                ps.setString(4, profile.getWikiName() );
                ps.setTimestamp(5, ts );
                ps.setString(6, profile.getLoginName() );
                ps.setTimestamp(7, ts );
            }
            else
            {
                // User exists
                ps = conn.prepareStatement( m_updateProfile );
                ps.setString(1, profile.getEmail() );
                ps.setString(2, profile.getFullname() );
                ps.setString(3, password );
                ps.setString(4, profile.getWikiName() );
                ps.setTimestamp(5, ts );
                ps.setString(6, profile.getLoginName() );
            }
            ps.execute();
        }
        catch ( SQLException e )
        {
            throw new WikiSecurityException( e.getMessage() );
        }
    }

    /**
     * 
     * @param rs
     * @return
     * @throws SQLException
     */
    private UserProfile findByPreparedStatement( String sql, String index ) throws NoSuchPrincipalException
    {
        Connection conn = null;
        UserProfile profile = null;
        boolean found = false;
        boolean unique = true;
        try {
            conn = m_ds.getConnection();
            PreparedStatement ps = conn.prepareStatement( sql );
            ps.setString( 1, index );
            ResultSet rs = ps.executeQuery();
            while ( rs.next() )
            {
                if ( profile != null )
                {
                    unique = false;
                    break;
                }
                profile = new DefaultUserProfile();
                profile.setCreated( rs.getTimestamp( m_created ) );
                profile.setEmail( rs.getString( m_email ) );
                profile.setFullname( rs.getString( m_fullName) );
                profile.setLastModified( rs.getTimestamp( m_modified ) );
                profile.setLoginName( rs.getString( m_loginName ) ) ;
                profile.setPassword( rs.getString( m_password ) );
                profile.setWikiName( rs.getString( m_wikiName ) );
                found = true;
            }
        }
        catch ( SQLException e )
        {
            throw new NoSuchPrincipalException( e.getMessage() );
        }
        
        // Close connection
        try 
        {
            if ( conn != null )
            {
                conn.close();
            }
        }
        catch ( SQLException e )
        {
        }
        
        if ( !found )
        {
            throw new NoSuchPrincipalException("Could not find profile in database!");
        }
        if ( !unique )
        {
            throw new NoSuchPrincipalException("More than one profile in database!");
        }
        return profile;

    }

}
