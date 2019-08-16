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
package org.apache.wiki.auth.user;

import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.util.Serializer;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * <p>
 * Implementation of UserDatabase that persists {@link DefaultUserProfile}
 * objects to a JDBC DataSource, as might typically be provided by a web
 * container. This implementation looks up the JDBC DataSource using JNDI. The
 * JNDI name of the datasource, backing table and mapped columns used by this 
 * class can be overridden by adding settings in <code>jspwiki.properties</code>.
 * </p>
 * <p>
 * Configurable properties are these:
 * </p>
 * <table>
 * <tr> <thead>
 * <th>Property</th>
 * <th>Default</th>
 * <th>Definition</th>
 * <thead> </tr>
 * <tr>
 * <td><code>jspwiki.userdatabase.datasource</code></td>
 * <td><code>jdbc/UserDatabase</code></td>
 * <td>The JNDI name of the DataSource</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.userdatabase.table</code></td>
 * <td><code>users</code></td>
 * <td>The table that stores the user profiles</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.userdatabase.attributes</code></td>
 * <td><code>attributes</code></td>
 * <td>The CLOB column containing the profile's custom attributes, stored as key/value strings, each separated by newline.</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.userdatabase.created</code></td>
 * <td><code>created</code></td>
 * <td>The column containing the profile's creation timestamp</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.userdatabase.email</code></td>
 * <td><code>email</code></td>
 * <td>The column containing the user's e-mail address</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.userdatabase.fullName</code></td>
 * <td><code>full_name</code></td>
 * <td>The column containing the user's full name</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.userdatabase.loginName</code></td>
 * <td><code>login_name</code></td>
 * <td>The column containing the user's login id</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.userdatabase.password</code></td>
 * <td><code>password</code></td>
 * <td>The column containing the user's password</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.userdatabase.modified</code></td>
 * <td><code>modified</code></td>
 * <td>The column containing the profile's last-modified timestamp</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.userdatabase.uid</code></td>
 * <td><code>uid</code></td>
 * <td>The column containing the profile's unique identifier, as a long integer</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.userdatabase.wikiName</code></td>
 * <td><code>wiki_name</code></td>
 * <td>The column containing the user's wiki name</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.userdatabase.lockExpiry</code></td>
 * <td><code>lock_expiry</code></td>
 * <td>The column containing the date/time when the profile, if locked, should be unlocked.</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.userdatabase.roleTable</code></td>
 * <td><code>roles</code></td>
 * <td>The table that stores user roles. When a new user is created, a new
 * record is inserted containing user's initial role. The table will have an ID
 * column whose name and values correspond to the contents of the user table's
 * login name column. It will also contain a role column (see next row).</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.userdatabase.role</code></td>
 * <td><code>role</code></td>
 * <td>The column in the role table that stores user roles. When a new user is
 * created, this column will be populated with the value
 * <code>Authenticated</code>. Once created, JDBCUserDatabase does not use
 * this column again; it is provided strictly for the convenience of
 * container-managed authentication services.</td>
 * </tr>
 * </table>
 * <p>
 * This class hashes passwords using SHA-1. All of the underying SQL commands
 * used by this class are implemented using prepared statements, so it is immune
 * to SQL injection attacks.
 * </p>
 * <p>
 * This class is typically used in conjunction with a web container's JNDI
 * resource factory. For example, Tomcat provides a basic
 * JNDI factory for registering DataSources. To give JSPWiki access to the JNDI
 * resource named by <code></code>, you would declare the datasource resource
 * similar to this:
 * </p>
 * <blockquote><code>&lt;Context ...&gt;<br/>
 *  &nbsp;&nbsp;...<br/>
 *  &nbsp;&nbsp;&lt;Resource name="jdbc/UserDatabase" auth="Container"<br/>
 *  &nbsp;&nbsp;&nbsp;&nbsp;type="javax.sql.DataSource" username="dbusername" password="dbpassword"<br/>
 *  &nbsp;&nbsp;&nbsp;&nbsp;driverClassName="org.hsql.jdbcDriver" url="jdbc:HypersonicSQL:database"<br/>
 *  &nbsp;&nbsp;&nbsp;&nbsp;maxActive="8" maxIdle="4"/&gt;<br/>
 *  &nbsp;...<br/>
 * &lt;/Context&gt;</code></blockquote>
 * <p>
 * To configure JSPWiki to use JDBC support, first create a database 
 * with a structure similar to that provided by the HSQL and PostgreSQL 
 * scripts in src/main/config/db.  If you have different table or column 
 * names you can either alias them with a database view and have JSPWiki
 * use the views, or alter the WEB-INF/jspwiki.properties file: the 
 * jspwiki.userdatabase.* and jspwiki.groupdatabase.* properties change the
 * names of the tables and columns that JSPWiki uses.
 * </p>
 * <p>
 * A JNDI datasource (named jdbc/UserDatabase by default but can be configured 
 * in the jspwiki.properties file) will need to be created in your servlet container.
 * JDBC driver JARs should be added, e.g. in Tomcat's <code>lib</code>
 * directory. For more Tomcat JNDI configuration examples, see <a
 * href="http://tomcat.apache.org/tomcat-7.0-doc/jndi-resources-howto.html">
 * http://tomcat.apache.org/tomcat-7.0-doc/jndi-resources-howto.html</a>.
 * Once done, restart JSPWiki in the servlet container for it to read the 
 * new properties and switch to JDBC authentication.
 * </p>
 * <p>
 * JDBCUserDatabase commits changes as transactions if the back-end database
 * supports them. If the database supports transactions, user profile changes
 * are saved to permanent storage only when the {@link #commit()} method is
 * called. If the database does <em>not</em> support transactions, then
 * changes are made immediately (during the {@link #save(UserProfile)} method),
 * and the {@linkplain #commit()} method no-ops. Thus, callers should always
 * call the {@linkplain #commit()} method after saving a profile to guarantee
 * that changes are applied.
 * </p>
 * 
 * @since 2.3
 */
public class JDBCUserDatabase extends AbstractUserDatabase {

    private static final String NOTHING = "";

    public static final String DEFAULT_DB_ATTRIBUTES = "attributes";

    public static final String DEFAULT_DB_CREATED = "created";

    public static final String DEFAULT_DB_EMAIL = "email";

    public static final String DEFAULT_DB_FULL_NAME = "full_name";

    public static final String DEFAULT_DB_JNDI_NAME = "jdbc/UserDatabase";

    public static final String DEFAULT_DB_LOCK_EXPIRY = "lock_expiry";

    public static final String DEFAULT_DB_MODIFIED = "modified";

    public static final String DEFAULT_DB_ROLE = "role";

    public static final String DEFAULT_DB_ROLE_TABLE = "roles";

    public static final String DEFAULT_DB_TABLE = "users";

    public static final String DEFAULT_DB_LOGIN_NAME = "login_name";

    public static final String DEFAULT_DB_PASSWORD = "password";

    public static final String DEFAULT_DB_UID = "uid";

    public static final String DEFAULT_DB_WIKI_NAME = "wiki_name";

    public static final String PROP_DB_ATTRIBUTES = "jspwiki.userdatabase.attributes";

    public static final String PROP_DB_CREATED = "jspwiki.userdatabase.created";

    public static final String PROP_DB_EMAIL = "jspwiki.userdatabase.email";

    public static final String PROP_DB_FULL_NAME = "jspwiki.userdatabase.fullName";

    public static final String PROP_DB_DATASOURCE = "jspwiki.userdatabase.datasource";

    public static final String PROP_DB_LOCK_EXPIRY = "jspwiki.userdatabase.lockExpiry";

    public static final String PROP_DB_LOGIN_NAME = "jspwiki.userdatabase.loginName";

    public static final String PROP_DB_MODIFIED = "jspwiki.userdatabase.modified";

    public static final String PROP_DB_PASSWORD = "jspwiki.userdatabase.password";

    public static final String PROP_DB_UID = "jspwiki.userdatabase.uid";

    public static final String PROP_DB_ROLE = "jspwiki.userdatabase.role";

    public static final String PROP_DB_ROLE_TABLE = "jspwiki.userdatabase.roleTable";

    public static final String PROP_DB_TABLE = "jspwiki.userdatabase.table";

    public static final String PROP_DB_WIKI_NAME = "jspwiki.userdatabase.wikiName";

    private DataSource m_ds = null;

    private String m_deleteUserByLoginName = null;

    private String m_deleteRoleByLoginName = null;

    private String m_findByEmail = null;

    private String m_findByFullName = null;

    private String m_findByLoginName = null;

    private String m_findByUid = null;

    private String m_findByWikiName = null;

    private String m_renameProfile = null;

    private String m_renameRoles = null;

    private String m_updateProfile = null;

    private String m_findAll = null;

    private String m_findRoles = null;

    private String m_insertProfile = null;

    private String m_insertRole = null;

    private String m_attributes = null;

    private String m_email = null;

    private String m_fullName = null;

    private String m_lockExpiry = null;

    private String m_loginName = null;

    private String m_password = null;

    private String m_uid = null;
    
    private String m_wikiName = null;

    private String m_created = null;

    private String m_modified = null;

    private boolean m_supportsCommits = false;

    /**
     * Looks up and deletes the first {@link UserProfile} in the user database
     * that matches a profile having a given login name. If the user database
     * does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}. This method is intended to be atomic;
     * results cannot be partially committed. If the commit fails, it should
     * roll back its state appropriately. Implementing classes that persist to
     * the file system may wish to make this method <code>synchronized</code>.
     * 
     * @param loginName the login name of the user profile that shall be deleted
     */
    @Override
    public void deleteByLoginName( String loginName ) throws NoSuchPrincipalException, WikiSecurityException {
        // Get the existing user; if not found, throws NoSuchPrincipalException
        findByLoginName( loginName );

        try( Connection conn = m_ds.getConnection() ; 
             PreparedStatement ps1 = conn.prepareStatement( m_deleteUserByLoginName ); 
             PreparedStatement ps2 = conn.prepareStatement( m_deleteRoleByLoginName ) )
        {
            // Open the database connection
            if( m_supportsCommits ) {
                conn.setAutoCommit( false );
            }

            // Delete user record
            ps1.setString( 1, loginName );
            ps1.execute();

            // Delete role record
            ps2.setString( 1, loginName );
            ps2.execute();

            // Commit and close connection
            if( m_supportsCommits ) {
                conn.commit();
            }
        } catch( SQLException e ) {
            throw new WikiSecurityException( e.getMessage(), e );
        }
    }

    /**
     * @see org.apache.wiki.auth.user.UserDatabase#findByEmail(java.lang.String)
     */
    @Override
    public UserProfile findByEmail( String index ) throws NoSuchPrincipalException {
        return findByPreparedStatement( m_findByEmail, index );
    }

    /**
     * @see org.apache.wiki.auth.user.UserDatabase#findByFullName(java.lang.String)
     */
    @Override
    public UserProfile findByFullName( String index ) throws NoSuchPrincipalException {
        return findByPreparedStatement( m_findByFullName, index );
    }

    /**
     * @see org.apache.wiki.auth.user.UserDatabase#findByLoginName(java.lang.String)
     */
    @Override
    public UserProfile findByLoginName( String index ) throws NoSuchPrincipalException {
        return findByPreparedStatement( m_findByLoginName, index );
    }

    /**
     * @see org.apache.wiki.auth.user.UserDatabase#findByWikiName(String)
     */
    @Override
    public UserProfile findByUid( String uid ) throws NoSuchPrincipalException {
        return findByPreparedStatement( m_findByUid, uid );
    }

    /**
     * @see org.apache.wiki.auth.user.UserDatabase#findByWikiName(String)
     */
    @Override
    public UserProfile findByWikiName( String index ) throws NoSuchPrincipalException {
        return findByPreparedStatement( m_findByWikiName, index );
    }

    /**
     * Returns all WikiNames that are stored in the UserDatabase as an array of
     * WikiPrincipal objects. If the database does not contain any profiles,
     * this method will return a zero-length array.
     * 
     * @return the WikiNames
     */
    @Override
    public Principal[] getWikiNames() throws WikiSecurityException {
        Set<Principal> principals = new HashSet<>();
        try( Connection conn = m_ds.getConnection();
             PreparedStatement ps = conn.prepareStatement( m_findAll );
             ResultSet rs = ps.executeQuery() )
        {
            while ( rs.next() ) {
                String wikiName = rs.getString( m_wikiName );
                if( wikiName == null ) {
                    log.warn( "Detected null wiki name in XMLUserDataBase. Check your user database." );
                } else {
                    Principal principal = new WikiPrincipal( wikiName, WikiPrincipal.WIKI_NAME );
                    principals.add( principal );
                }
            }
        } catch( SQLException e ) {
            throw new WikiSecurityException( e.getMessage(), e );
        }

        return principals.toArray( new Principal[principals.size()] );
    }

    /**
     * @see org.apache.wiki.auth.user.UserDatabase#initialize(org.apache.wiki.WikiEngine,
     *      java.util.Properties)
     */
    @Override
    public void initialize( WikiEngine engine, Properties props ) throws NoRequiredPropertyException, WikiSecurityException {
        String jndiName = props.getProperty( PROP_DB_DATASOURCE, DEFAULT_DB_JNDI_NAME );
        try {
            Context initCtx = new InitialContext();
            Context ctx = (Context) initCtx.lookup( "java:comp/env" );
            m_ds = (DataSource) ctx.lookup( jndiName );

            // Prepare the SQL selectors
            String userTable = props.getProperty( PROP_DB_TABLE, DEFAULT_DB_TABLE );
            m_email = props.getProperty( PROP_DB_EMAIL, DEFAULT_DB_EMAIL );
            m_fullName = props.getProperty( PROP_DB_FULL_NAME, DEFAULT_DB_FULL_NAME );
            m_lockExpiry = props.getProperty( PROP_DB_LOCK_EXPIRY, DEFAULT_DB_LOCK_EXPIRY );
            m_loginName = props.getProperty( PROP_DB_LOGIN_NAME, DEFAULT_DB_LOGIN_NAME );
            m_password = props.getProperty( PROP_DB_PASSWORD, DEFAULT_DB_PASSWORD );
            m_uid = props.getProperty( PROP_DB_UID, DEFAULT_DB_UID );
            m_wikiName = props.getProperty( PROP_DB_WIKI_NAME, DEFAULT_DB_WIKI_NAME );
            m_created = props.getProperty( PROP_DB_CREATED, DEFAULT_DB_CREATED );
            m_modified = props.getProperty( PROP_DB_MODIFIED, DEFAULT_DB_MODIFIED );
            m_attributes = props.getProperty( PROP_DB_ATTRIBUTES, DEFAULT_DB_ATTRIBUTES );

            m_findAll = "SELECT * FROM " + userTable;
            m_findByEmail = "SELECT * FROM " + userTable + " WHERE " + m_email + "=?";
            m_findByFullName = "SELECT * FROM " + userTable + " WHERE " + m_fullName + "=?";
            m_findByLoginName = "SELECT * FROM " + userTable + " WHERE " + m_loginName + "=?";
            m_findByUid = "SELECT * FROM " + userTable + " WHERE " + m_uid + "=?";
            m_findByWikiName = "SELECT * FROM " + userTable + " WHERE " + m_wikiName + "=?";

            // The user insert SQL prepared statement
            m_insertProfile = "INSERT INTO " + userTable + " ("
                              + m_uid + ","
                              + m_email + ","
                              + m_fullName + ","
                              + m_password + ","
                              + m_wikiName + ","
                              + m_modified + ","
                              + m_loginName + ","
                              + m_attributes + ","
                              + m_created
                              + ") VALUES (?,?,?,?,?,?,?,?,?)";
            
            // The user update SQL prepared statement
            m_updateProfile = "UPDATE " + userTable + " SET "
                              + m_uid + "=?,"
                              + m_email + "=?,"
                              + m_fullName + "=?,"
                              + m_password + "=?,"
                              + m_wikiName + "=?,"
                              + m_modified + "=?,"
                              + m_loginName + "=?,"
                              + m_attributes + "=?,"
                              + m_lockExpiry + "=? "
                              + "WHERE " + m_loginName + "=?";

            // Prepare the role insert SQL
            String roleTable = props.getProperty( PROP_DB_ROLE_TABLE, DEFAULT_DB_ROLE_TABLE );
            String role = props.getProperty( PROP_DB_ROLE, DEFAULT_DB_ROLE );
            m_insertRole = "INSERT INTO " + roleTable + " (" + m_loginName + "," + role + ") VALUES (?,?)";
            m_findRoles = "SELECT * FROM " + roleTable + " WHERE " + m_loginName + "=?";

            // Prepare the user delete SQL
            m_deleteUserByLoginName = "DELETE FROM " + userTable + " WHERE " + m_loginName + "=?";

            // Prepare the role delete SQL
            m_deleteRoleByLoginName = "DELETE FROM " + roleTable + " WHERE " + m_loginName + "=?";

            // Prepare the rename user/roles SQL
            m_renameProfile = "UPDATE " + userTable + " SET " + m_loginName + "=?," + m_modified + "=? WHERE " + m_loginName
                              + "=?";
            m_renameRoles = "UPDATE " + roleTable + " SET " + m_loginName + "=? WHERE " + m_loginName + "=?";
        } catch( NamingException e ) {
            log.error( "JDBCUserDatabase initialization error: " + e.getMessage() );
            throw new NoRequiredPropertyException( PROP_DB_DATASOURCE, "JDBCUserDatabase initialization error: " + e.getMessage() );
        }

        // Test connection by doing a quickie select
        try( Connection conn = m_ds.getConnection(); PreparedStatement ps = conn.prepareStatement( m_findAll ) ) {
        } catch( SQLException e ) {
            log.error( "DB connectivity error: " + e.getMessage() );
            throw new WikiSecurityException("DB connectivity error: " + e.getMessage(), e );
        }
        log.info( "JDBCUserDatabase initialized from JNDI DataSource: " + jndiName );

        // Determine if the datasource supports commits
        try( Connection conn = m_ds.getConnection() ) {
            DatabaseMetaData dmd = conn.getMetaData();
            if( dmd.supportsTransactions() ) {
                m_supportsCommits = true;
                conn.setAutoCommit( false );
                log.info( "JDBCUserDatabase supports transactions. Good; we will use them." );
            }
        } catch( SQLException e ) {
            log.warn( "JDBCUserDatabase warning: user database doesn't seem to support transactions. Reason: " + e.getMessage() );
        }
    }

    /**
     * @see org.apache.wiki.auth.user.UserDatabase#rename(String, String)
     */
    @Override
    public void rename( String loginName, String newName ) throws NoSuchPrincipalException, DuplicateUserException, WikiSecurityException {
        // Get the existing user; if not found, throws NoSuchPrincipalException
        UserProfile profile = findByLoginName( loginName );

        // Get user with the proposed name; if found, it's a collision
        try {
            UserProfile otherProfile = findByLoginName( newName );
            if( otherProfile != null ) {
                throw new DuplicateUserException( "security.error.cannot.rename", newName );
            }
        } catch( NoSuchPrincipalException e ) {
            // Good! That means it's safe to save using the new name
        }

        try( Connection conn = m_ds.getConnection(); 
             PreparedStatement ps1 = conn.prepareStatement( m_renameProfile );
             PreparedStatement ps2 = conn.prepareStatement( m_renameRoles ) )
        {
            if( m_supportsCommits ) {
                conn.setAutoCommit( false );
            }

            Timestamp ts = new Timestamp( System.currentTimeMillis() );
            Date modDate = new Date( ts.getTime() );

            // Change the login ID for the user record
            ps1.setString( 1, newName );
            ps1.setTimestamp( 2, ts );
            ps1.setString( 3, loginName );
            ps1.execute();

            // Change the login ID for the role records
            ps2.setString( 1, newName );
            ps2.setString( 2, loginName );
            ps2.execute();

            // Set the profile name and mod time
            profile.setLoginName( newName );
            profile.setLastModified( modDate );

            // Commit and close connection
            if( m_supportsCommits ) {
                conn.commit();
            }
        } catch( SQLException e ) {
            throw new WikiSecurityException( e.getMessage(), e );
        }
    }

    /**
     * @see org.apache.wiki.auth.user.UserDatabase#save(org.apache.wiki.auth.user.UserProfile)
     */
    @Override
    public void save( UserProfile profile ) throws WikiSecurityException {
        String initialRole = "Authenticated";

        // Figure out which prepared statement to use & execute it
        String loginName = profile.getLoginName();
        UserProfile existingProfile = null;

        try {
            existingProfile = findByLoginName( loginName );
        } catch( NoSuchPrincipalException e ) {
            // Existing profile will be null
        }

        // Get a clean password from the passed profile.
        // Blank password is the same as null, which means we re-use the existing one.
        String password = profile.getPassword();
        String existingPassword = (existingProfile == null) ? null : existingProfile.getPassword();
        if( NOTHING.equals( password ) ) {
            password = null;
        }
        if( password == null ) {
            password = existingPassword;
        }

        // If password changed, hash it before we save
        if( !StringUtils.equals( password, existingPassword ) ) {
            password = getHash( password );
        }

        try( Connection conn = m_ds.getConnection();
             PreparedStatement ps1 = conn.prepareStatement( m_insertProfile );
             PreparedStatement ps2 = conn.prepareStatement( m_findRoles );
             PreparedStatement ps3 = conn.prepareStatement( m_insertRole );
             PreparedStatement ps4 = conn.prepareStatement( m_updateProfile ) )
        {
            if( m_supportsCommits ) {
                conn.setAutoCommit( false );
            }

            Timestamp ts = new Timestamp( System.currentTimeMillis() );
            Date modDate = new Date( ts.getTime() );
            java.sql.Date lockExpiry = profile.getLockExpiry() == null ? null : new java.sql.Date( profile.getLockExpiry().getTime() );
            if( existingProfile == null )
            {
                // User is new: insert new user record
                ps1.setString( 1, profile.getUid() );
                ps1.setString( 2, profile.getEmail() );
                ps1.setString( 3, profile.getFullname() );
                ps1.setString( 4, password );
                ps1.setString( 5, profile.getWikiName() );
                ps1.setTimestamp( 6, ts );
                ps1.setString( 7, profile.getLoginName() );
                try {
                    ps1.setString( 8, Serializer.serializeToBase64( profile.getAttributes() ) );
                } catch ( IOException e ) {
                    throw new WikiSecurityException( "Could not save user profile attribute. Reason: " + e.getMessage(), e );
                }
                ps1.setTimestamp( 9, ts );
                ps1.execute();

                // Insert new role record
                ps2.setString( 1, profile.getLoginName() );
                int roles = 0;
                try ( ResultSet rs = ps2.executeQuery() ) {
                    while ( rs.next() ) {
                        roles++;
                    }
                }
                
                if( roles == 0 ) {
                    ps3.setString( 1, profile.getLoginName() );
                    ps3.setString( 2, initialRole );
                    ps3.execute();
                }

                // Set the profile creation time
                profile.setCreated( modDate );
            } else {
                // User exists: modify existing record
                ps4.setString( 1, profile.getUid() );
                ps4.setString( 2, profile.getEmail() );
                ps4.setString( 3, profile.getFullname() );
                ps4.setString( 4, password );
                ps4.setString( 5, profile.getWikiName() );
                ps4.setTimestamp( 6, ts );
                ps4.setString( 7, profile.getLoginName() );
                try
                {
                    ps4.setString( 8, Serializer.serializeToBase64( profile.getAttributes() ) );
                }
                catch ( IOException e )
                {
                    throw new WikiSecurityException( "Could not save user profile attribute. Reason: " + e.getMessage(), e );
                }
                ps4.setDate( 9, lockExpiry );
                ps4.setString( 10, profile.getLoginName() );
                ps4.execute();
            }
            // Set the profile mod time
            profile.setLastModified( modDate );

            // Commit and close connection
            if( m_supportsCommits ) {
                conn.commit();
            }
        }
        catch( SQLException e )
        {
            throw new WikiSecurityException( e.getMessage(), e );
        }
    }

    /**
     * Private method that returns the first {@link UserProfile} matching a
     * named column's value. This method will also set the UID if it has not yet been set.     
     * @param sql the SQL statement that should be prepared; it must have one parameter
     * to set (either a String or a Long)
     * @param index the value to match
     * @return the resolved UserProfile
     * @throws SQLException
     */
    private UserProfile findByPreparedStatement( String sql, Object index ) throws NoSuchPrincipalException
    {
        UserProfile profile = null;
        boolean found = false;
        boolean unique = true;
        try( Connection conn = m_ds.getConnection(); PreparedStatement ps = conn.prepareStatement( sql ) ) {
            if( m_supportsCommits ) {
                conn.setAutoCommit( false );
            }
            
            // Set the parameter to search by
            if ( index instanceof String ) {
                ps.setString( 1, (String)index );
            } else if ( index instanceof Long ) {
                ps.setLong( 1, ( (Long)index).longValue() );
            } else {
                throw new IllegalArgumentException( "Index type not recognized!" );
            }
            
            // Go and get the record!
            try ( ResultSet rs = ps.executeQuery() ) {
                while ( rs.next() ) {
                    if( profile != null ) {
                        unique = false;
                        break;
                    }
                    profile = newProfile();
                    
                    // Fetch the basic user attributes
                    profile.setUid( rs.getString( m_uid ) );
                    if ( profile.getUid() == null ) {
                        profile.setUid( generateUid( this ) );
                    }
                    profile.setCreated( rs.getTimestamp( m_created ) );
                    profile.setEmail( rs.getString( m_email ) );
                    profile.setFullname( rs.getString( m_fullName ) );
                    profile.setLastModified( rs.getTimestamp( m_modified ) );
                    Date lockExpiry = rs.getDate( m_lockExpiry );
                    profile.setLockExpiry( rs.wasNull() ? null : lockExpiry );
                    profile.setLoginName( rs.getString( m_loginName ) );
                    profile.setPassword( rs.getString( m_password ) );
                    
                    // Fetch the user attributes
                    String rawAttributes = rs.getString( m_attributes );
                    if ( rawAttributes != null ) {
                        try {
                            Map<String,? extends Serializable> attributes = Serializer.deserializeFromBase64( rawAttributes );
                            profile.getAttributes().putAll( attributes );
                        } catch ( IOException e ) {
                            log.error( "Could not parse user profile attributes!", e );
                        }
                    }
                    found = true;
                }
            }
        } catch( SQLException e ) {
            throw new NoSuchPrincipalException( e.getMessage() );
        }

        if( !found ) {
            throw new NoSuchPrincipalException( "Could not find profile in database!" );
        }
        if( !unique ) {
            throw new NoSuchPrincipalException( "More than one profile in database!" );
        }
        return profile;
    }

}
