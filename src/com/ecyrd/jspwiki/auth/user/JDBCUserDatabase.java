/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 JSPWiki Development Group

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.auth.user;

import java.security.Principal;
import java.sql.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
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
 *   <tr>
 *     <td><code>jspwiki.userdatabase.roleTable</code></td>
 *     <td><code>roles</code></td>
 *     <td>The table that stores user roles. When a new user is created,
 *       a new record is inserted containing user's initial role. The
 *       table will have an ID column whose name and values correspond
 *       to the contents of the user table's login name column. It will
 *       also contain a role column (see next row).</td>
 *   </tr>
 *   <tr>
 *     <td><code>jspwiki.userdatabase.role</code></td>
 *     <td><code>role</code></td>
 *     <td>The column in the role table that stores user roles. When a new user
 *       is created, this column will be populated with the value
 *       <code>Authenticated</code>. Once created, JDBCUserDatabase does not
 *       use this column again; it is provided strictly for the convenience
 *       of container-managed authentication services.</td>
 *   </tr>
 *   <tr>
 *     <td><code>jspwiki.userdatabase.hashPrefix</code></td>
 *     <td><code>true</code></td>
 *     <td>Whether or not to prepend a prefix for the hash algorithm, <em>e.g.</em>,
 *         <code>{SHA}</code>.</td>
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
 * http://tomcat.apache.org/tomcat-5.5-doc/jndi-resources-howto.html</a>.</p>
 * <p>JDBCUserDatabase commits changes as transactions if the back-end database supports them.
 * If the database supports transactions, user profile changes are saved
 * to permanent storage only when the {@link #commit()} method is called. If the database does <em>not</em>
 * support transactions, then changes are made immediately (during the {@link #save(UserProfile)}
 * method), and the {@linkplain #commit()} method no-ops. Thus, callers should always call the
 * {@linkplain #commit()} method after saving a profile to guarantee that changes are applied.</p>
 * @author Andrew R. Jaquith
 * @since 2.3
 */public class JDBCUserDatabase extends AbstractUserDatabase
{

    private static final String NOTHING = "";

    public static final String DEFAULT_DB_CREATED    = "created";

    public static final String DEFAULT_DB_EMAIL      = "email";

    public static final String DEFAULT_DB_FULL_NAME  = "full_name";

    public static final String DEFAULT_DB_HASH_PREFIX = "true";

    public static final String DEFAULT_DB_JNDI_NAME  = "jdbc/UserDatabase";

    public static final String DEFAULT_DB_MODIFIED   = "modified";

    public static final String DEFAULT_DB_ROLE       = "role";

    public static final String DEFAULT_DB_ROLE_TABLE = "roles";

    public static final String DEFAULT_DB_TABLE      = "users";

    public static final String DEFAULT_DB_LOGIN_NAME = "login_name";

    public static final String DEFAULT_DB_PASSWORD   = "password";

    public static final String DEFAULT_DB_WIKI_NAME  = "wiki_name";

    public static final String PROP_DB_CREATED       = "jspwiki.userdatabase.created";

    public static final String PROP_DB_EMAIL         = "jspwiki.userdatabase.email";

    public static final String PROP_DB_FULL_NAME     = "jspwiki.userdatabase.fullName";

    public static final String PROP_DB_DATASOURCE    = "jspwiki.userdatabase.datasource";

    public static final String PROP_DB_HASH_PREFIX   = "jspwiki.userdatabase.hashPrefix";

    public static final String PROP_DB_LOGIN_NAME    = "jspwiki.userdatabase.loginName";

    public static final String PROP_DB_MODIFIED      = "jspwiki.userdatabase.modified";

    public static final String PROP_DB_PASSWORD      = "jspwiki.userdatabase.password";

    public static final String PROP_DB_ROLE          = "jspwiki.userdatabase.role";

    public static final String PROP_DB_ROLE_TABLE    = "jspwiki.userdatabase.roleTable";

    public static final String PROP_DB_TABLE         = "jspwiki.userdatabase.table";

    public static final String PROP_DB_WIKI_NAME     = "jspwiki.userdatabase.wikiName";

    private DataSource         m_ds                  = null;
    private String m_deleteUserByLoginName = null;
    private String m_deleteRoleByLoginName = null;
    private String m_findByEmail = null;
    private String m_findByFullName = null;
    private String m_findByLoginName = null;
    private String m_findByWikiName = null;
    private String m_renameProfile = null;
    private String m_renameRoles = null;
    private String m_updateProfile = null;
    private String m_findAll = null;
    private String m_findRoles = null;
    private String m_initialRole = "Authenticated";
    private String m_insertProfile = null;
    private String m_insertRole = null;
    private String m_userTable = null;
    private String m_email = null;
    private String m_fullName = null;
    private boolean m_hashPrefix = true;
    private String m_loginName = null;
    private String m_password = null;
    private String m_role = null;
    private String m_roleTable = null;
    private String m_wikiName = null;
    private String m_created = null;
    private String m_modified = null;
    private boolean m_sharedWithContainer = false;
    private boolean m_supportsCommits = false;

    /**
     * Looks up and deletes the first {@link UserProfile} in the user database
     * that matches a profile having a given login name. If the user database
     * does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}. This method is intended to be atomic;
     * results cannot be partially committed. If the commit fails, it should
     * roll back its state appropriately. Implementing classes that persist
     * to the file system may wish to make this method <code>synchronized</code>.
     * @param loginName the login name of the user profile that shall be deleted
     */
    public void deleteByLoginName( String loginName ) throws NoSuchPrincipalException, WikiSecurityException
    {
        // Get the existing user; if not found, throws NoSuchPrincipalException
        findByLoginName( loginName );

        try
        {
            // Open the database connection
            Connection conn = m_ds.getConnection();
            if ( m_supportsCommits )
            {
                conn.setAutoCommit( false );
            }

            PreparedStatement ps;
            // Delete user record
            ps = conn.prepareStatement( m_deleteUserByLoginName );
            ps.setString(1, loginName );
            ps.execute();
            ps.close();

            // Delete role record
            ps = conn.prepareStatement( m_deleteRoleByLoginName );
            ps.setString(1, loginName );
            ps.execute();
            ps.close();

            // Commit and close connection
            if ( m_supportsCommits )
            {
                conn.commit();
            }
            conn.close();
        }
        catch ( SQLException e )
        {
            throw new WikiSecurityException( e.getMessage() );
        }
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
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByWikiName(String)
     */
    public UserProfile findByWikiName( String index ) throws NoSuchPrincipalException
    {
        return findByPreparedStatement( m_findByWikiName, index );
    }

    /**
     * Returns all WikiNames that are stored in the UserDatabase
     * as an array of WikiPrincipal objects. If the database does not
     * contain any profiles, this method will return a zero-length
     * array.
     * @return the WikiNames
     */
    public Principal[] getWikiNames() throws WikiSecurityException
    {
        Set principals = new HashSet();
        try
        {
            Connection conn = m_ds.getConnection();
            PreparedStatement ps = conn.prepareStatement( m_findAll );
            ResultSet rs = ps.executeQuery();
            while ( rs.next() )
            {
                String wikiName = rs.getString( m_wikiName );
                if ( wikiName == null )
                {
                    log.warn( "Detected null wiki name in XMLUserDataBase. Check your user database." );
                }
                else
                {
                    Principal principal = new WikiPrincipal( wikiName, WikiPrincipal.WIKI_NAME );
                    principals.add( principal );
                }
            }
            ps.close();
            conn.close();
        }
        catch ( SQLException e )
        {
            throw new WikiSecurityException( e.getMessage() );
        }

        return (Principal[])principals.toArray( new Principal[principals.size()] );
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
            m_userTable = props.getProperty( PROP_DB_TABLE, DEFAULT_DB_TABLE );
            m_email     = props.getProperty( PROP_DB_EMAIL, DEFAULT_DB_EMAIL );
            m_fullName  = props.getProperty( PROP_DB_FULL_NAME, DEFAULT_DB_FULL_NAME );
            m_hashPrefix = Boolean.valueOf( props.getProperty( PROP_DB_HASH_PREFIX, DEFAULT_DB_HASH_PREFIX ) ).booleanValue();
            m_loginName = props.getProperty( PROP_DB_LOGIN_NAME, DEFAULT_DB_LOGIN_NAME );
            m_password  = props.getProperty( PROP_DB_PASSWORD, DEFAULT_DB_PASSWORD );
            m_wikiName  = props.getProperty( PROP_DB_WIKI_NAME, DEFAULT_DB_WIKI_NAME );
            m_created   = props.getProperty( PROP_DB_CREATED, DEFAULT_DB_CREATED );
            m_modified  = props.getProperty( PROP_DB_MODIFIED, DEFAULT_DB_MODIFIED );

            m_findAll         = "SELECT * FROM " + m_userTable;
            m_findByEmail     = "SELECT * FROM " + m_userTable + " WHERE " + m_email + "=?";
            m_findByFullName  = "SELECT * FROM " + m_userTable + " WHERE " + m_fullName + "=?";
            m_findByLoginName = "SELECT * FROM " + m_userTable + " WHERE " + m_loginName + "=?";
            m_findByWikiName  = "SELECT * FROM " + m_userTable + " WHERE " + m_wikiName + "=?";

            // Prepare the user isert/update SQL
            m_insertProfile   = "INSERT INTO " + m_userTable + " ("
                              + m_email + ","
                              + m_fullName + ","
                              + m_password + ","
                              + m_wikiName + ","
                              + m_modified + ","
                              + m_loginName + ","
                              + m_created
                              + ") VALUES (?,?,?,?,?,?,?)";
            m_updateProfile   = "UPDATE " + m_userTable + " SET "
                              + m_email + "=?,"
                              + m_fullName + "=?,"
                              + m_password + "=?,"
                              + m_wikiName + "=?,"
                              + m_modified + "=? WHERE " + m_loginName + "=?";

            // Prepare the role insert SQL
            m_roleTable = props.getProperty( PROP_DB_ROLE_TABLE, DEFAULT_DB_ROLE_TABLE );
            m_role = props.getProperty( PROP_DB_ROLE, DEFAULT_DB_ROLE );
            m_insertRole      = "INSERT INTO " + m_roleTable + " ("
                              + m_loginName + ","
                              + m_role
                              + ") VALUES (?,?)";
            m_findRoles       = "SELECT * FROM " + m_roleTable + " WHERE " + m_loginName + "=?";

            // Prepare the user delete SQL
            m_deleteUserByLoginName = "DELETE FROM " + m_userTable + " WHERE " + m_loginName + "=?";

            // Prepare the role delete SQL
            m_deleteRoleByLoginName = "DELETE FROM " + m_roleTable + " WHERE " + m_loginName + "=?";

            // Prepare the rename user/roles SQL
            m_renameProfile   = "UPDATE " + m_userTable + " SET "
                              + m_loginName + "=?,"
                              + m_modified  + "=? WHERE " + m_loginName + "=?";
            m_renameRoles     = "UPDATE " + m_roleTable + " SET "
                              + m_loginName + "=? WHERE " + m_loginName + "=?";

            // Set the "share users with container flag"
            m_sharedWithContainer = TextUtil.isPositive( props.getProperty( PROP_SHARED_WITH_CONTAINER, "false" ) );
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
            ps.executeQuery();
            ps.close();
            conn.close();
        }
        catch ( SQLException e )
        {
            log.error( "JDBCUserDatabase initialization error: " + e.getMessage() );
            throw new NoRequiredPropertyException( PROP_DB_DATASOURCE, "JDBCUserDatabase initialization error: " + e.getMessage() );
        }
        log.info( "JDBCUserDatabase initialized from JNDI DataSource: " + jndiName );

        // Determine if the datasource supports commits
        try
        {
            Connection conn = m_ds.getConnection();
            DatabaseMetaData dmd = conn.getMetaData();
            if ( dmd.supportsTransactions() )
            {
                m_supportsCommits = true;
                conn.setAutoCommit( false );
                log.info("JDBCUserDatabase supports transactions. Good; we will use them." );
            }
            conn.close();
        }
        catch ( SQLException e )
        {
            log.warn("JDBCUserDatabase warning: user database doesn't seem to support transactions. Reason: " + e.getMessage() );
            throw new NoRequiredPropertyException( PROP_DB_DATASOURCE, "JDBCUserDatabase initialization error: " + e.getMessage() );
        }
    }

    /**
     * Determines whether the user database shares user/password data with the
     * web container; returns <code>true</code> if the JSPWiki property
     * <code>jspwiki.userdatabase.isSharedWithContainer</code> is <code>true</code>.
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#isSharedWithContainer()
     */
    public boolean isSharedWithContainer()
    {
        return m_sharedWithContainer;
    }

    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#rename(String, String)
     */
    public void rename(String loginName, String newName)
        throws NoSuchPrincipalException, DuplicateUserException, WikiSecurityException
    {
        // Get the existing user; if not found, throws NoSuchPrincipalException
        UserProfile profile = findByLoginName( loginName );

        // Get user with the proposed name; if found, it's a collision
        try
        {
            UserProfile otherProfile = findByLoginName( newName );
            if ( otherProfile != null )
            {
                throw new DuplicateUserException( "Cannot rename: the login name '" + newName + "' is already taken." );
            }
        }
        catch ( NoSuchPrincipalException e )
        {
            // Good! That means it's safe to save using the new name
        }

        try
        {
            // Open the database connection
            Connection conn = m_ds.getConnection();
            if ( m_supportsCommits )
            {
                conn.setAutoCommit( false );
            }

            Timestamp ts = new Timestamp( System.currentTimeMillis() );
            Date modDate = new Date( ts.getTime() );

            // Change the login ID for the user record
            PreparedStatement ps = conn.prepareStatement( m_renameProfile );
            ps.setString( 1, newName );
            ps.setTimestamp( 2, ts );
            ps.setString( 3, loginName );
            ps.execute();
            ps.close();

            // Change the login ID for the role records
            ps = conn.prepareStatement( m_renameRoles );
            ps.setString( 1, newName );
            ps.setString( 2, loginName );
            ps.execute();
            ps.close();

            // Set the profile name and mod time
            profile.setLoginName( newName );
            profile.setLastModified( modDate );

            // Commit and close connection
            if ( m_supportsCommits )
            {
                conn.commit();
            }
            conn.close();
        }
        catch ( SQLException e )
        {
            throw new WikiSecurityException( e.getMessage() );
        }
    }

    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#save(com.ecyrd.jspwiki.auth.user.UserProfile)
     */
    public void save( UserProfile profile ) throws WikiSecurityException
    {
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
        // Blank password is the same as null, which means we re-use the existing one.
        String password = profile.getPassword();
        String existingPassword = ( existingProfile == null ) ? null : existingProfile.getPassword();
        if ( NOTHING.equals( password ) )
        {
            password = null;
        }
        if ( password == null)
        {
            password = existingPassword;
        }

        // If password changed, hash it before we save
        if ( !password.equals( existingPassword ) )
        {
            password =  m_hashPrefix ? SHA_PREFIX + getHash( password ) : getHash( password );
        }

        try
        {
            // Open the database connection
            Connection conn = m_ds.getConnection();
            if ( m_supportsCommits )
            {
                conn.setAutoCommit( false );
            }

            Timestamp ts = new Timestamp( System.currentTimeMillis() );
            Date modDate = new Date( ts.getTime() );
            if ( existingProfile == null )
            {
                // User is new: insert new user record
                ps = conn.prepareStatement( m_insertProfile );
                ps.setString(1, profile.getEmail() );
                ps.setString(2, profile.getFullname() );
                ps.setString(3, password );
                ps.setString(4, profile.getWikiName() );
                ps.setTimestamp(5, ts );
                ps.setString(6, profile.getLoginName() );
                ps.setTimestamp(7, ts );
                ps.execute();
                ps.close();

                // Insert role record if no roles yet
                if ( m_sharedWithContainer )
                {
                    ps = conn.prepareStatement( m_findRoles );
                    ps.setString( 1, profile.getLoginName() );
                    ResultSet rs = ps.executeQuery();
                    int roles = 0;
                    while ( rs.next() )
                    {
                        roles++;
                    }
                    ps.close();
                    if ( roles == 0 )
                    {
                        ps = conn.prepareStatement( m_insertRole );
                        ps.setString( 1, profile.getLoginName() );
                        ps.setString( 2, m_initialRole );
                        ps.execute();
                        ps.close();
                    }
                }

                // Set the profile creation time
                profile.setCreated( modDate );
            }
            else
            {
                // User exists: modify existing record
                ps = conn.prepareStatement( m_updateProfile );
                ps.setString(1, profile.getEmail() );
                ps.setString(2, profile.getFullname() );
                ps.setString(3, password );
                ps.setString(4, profile.getWikiName() );
                ps.setTimestamp(5, ts );
                ps.setString(6, profile.getLoginName() );
                ps.execute();
                ps.close();
            }
            // Set the profile mod time
            profile.setLastModified( modDate );

            // Commit and close connection
            if ( m_supportsCommits )
            {
                conn.commit();
            }
            conn.close();
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
        UserProfile profile = null;
        boolean found = false;
        boolean unique = true;
        try
        {
            // Open the database connection
            Connection conn = m_ds.getConnection();
            if ( m_supportsCommits )
            {
                conn.setAutoCommit( false );
            }

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
                found = true;
            }
            ps.close();

            // Close connection
            conn.close();
        }
        catch ( SQLException e )
        {
            throw new NoSuchPrincipalException( e.getMessage() );
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
