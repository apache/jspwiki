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
package org.apache.wiki.auth.authorize;

import java.security.Principal;
import java.sql.*;
import java.util.*;
import java.util.Date;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.WikiSecurityException;

/**
 * <p>
 * Implementation of GroupDatabase that persists {@link Group} objects to a JDBC
 * DataSource, as might typically be provided by a web container. This
 * implementation looks up the JDBC DataSource using JNDI. The JNDI name of the
 * datasource, backing table and mapped columns used by this class can be
 * overridden by adding settings in <code>jspwiki.properties</code>.
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
 * <td><code>jspwiki.groupdatabase.datasource</code></td>
 * <td><code>jdbc/GroupDatabase</code></td>
 * <td>The JNDI name of the DataSource</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.groupdatabase.table</code></td>
 * <td><code>groups</code></td>
 * <td>The table that stores the groups</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.groupdatabase.membertable</code></td>
 * <td><code>group_members</code></td>
 * <td>The table that stores the names of group members</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.groupdatabase.created</code></td>
 * <td><code>created</code></td>
 * <td>The column containing the group's creation timestamp</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.groupdatabase.creator</code></td>
 * <td><code>creator</code></td>
 * <td>The column containing the group creator's name</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.groupdatabase.name</code></td>
 * <td><code>name</code></td>
 * <td>The column containing the group's name</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.groupdatabase.member</code></td>
 * <td><code>member</code></td>
 * <td>The column containing the group member's name</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.groupdatabase.modified</code></td>
 * <td><code>modified</code></td>
 * <td>The column containing the group's last-modified timestamp</td>
 * </tr>
 * <tr>
 * <td><code>jspwiki.groupdatabase.modifier</code></td>
 * <td><code>modifier</code></td>
 * <td>The column containing the name of the user who last modified the group</td>
 * </tr>
 * </table>
 * <p>
 * This class is typically used in conjunction with a web container's JNDI
 * resource factory. For example, Tomcat versions 4 and higher provide a basic
 * JNDI factory for registering DataSources. To give JSPWiki access to the JNDI
 * resource named by <code>jdbc/GroupDatabase</code>, you would declare the
 * datasource resource similar to this:
 * </p>
 * <blockquote><code>&lt;Context ...&gt;<br/>
 *  &nbsp;&nbsp;...<br/>
 *  &nbsp;&nbsp;&lt;Resource name="jdbc/GroupDatabase" auth="Container"<br/>
 *  &nbsp;&nbsp;&nbsp;&nbsp;type="javax.sql.DataSource" username="dbusername" password="dbpassword"<br/>
 *  &nbsp;&nbsp;&nbsp;&nbsp;driverClassName="org.hsql.jdbcDriver" url="jdbc:HypersonicSQL:database"<br/>
 *  &nbsp;&nbsp;&nbsp;&nbsp;maxActive="8" maxIdle="4"/&gt;<br/>
 *  &nbsp;...<br/>
 * &lt;/Context&gt;</code></blockquote>
 * <p>
 * JDBC driver JARs should be added to Tomcat's <code>common/lib</code>
 * directory. For more Tomcat 5.5 JNDI configuration examples, see <a
 * href="http://tomcat.apache.org/tomcat-5.5-doc/jndi-resources-howto.html">
 * http://tomcat.apache.org/tomcat-5.5-doc/jndi-resources-howto.html</a>.
 * </p>
 * <p>
 * JDBCGroupDatabase commits changes as transactions if the back-end database
 * supports them. If the database supports transactions, group changes are saved
 * to permanent storage only when the {@link #commit()} method is called. If the
 * database does <em>not</em> support transactions, then changes are made
 * immediately (during the {@link #save(Group, Principal)} method), and the
 * {@linkplain #commit()} method no-ops. Thus, callers should always call the
 * {@linkplain #commit()} method after saving a profile to guarantee that
 * changes are applied.
 * </p>
 * 
 * @since 2.3
 */
public class JDBCGroupDatabase implements GroupDatabase {
	
    /** Default column name that stores the JNDI name of the DataSource. */
    public static final String DEFAULT_GROUPDB_DATASOURCE = "jdbc/GroupDatabase";

    /** Default table name for the table that stores groups. */
    public static final String DEFAULT_GROUPDB_TABLE = "groups";

    /** Default column name that stores the names of group members. */
    public static final String DEFAULT_GROUPDB_MEMBER_TABLE = "group_members";

    /** Default column name that stores the the group creation timestamps. */
    public static final String DEFAULT_GROUPDB_CREATED = "created";

    /** Default column name that stores group creator names. */
    public static final String DEFAULT_GROUPDB_CREATOR = "creator";

    /** Default column name that stores the group names. */
    public static final String DEFAULT_GROUPDB_NAME = "name";

    /** Default column name that stores group member names. */
    public static final String DEFAULT_GROUPDB_MEMBER = "member";

    /** Default column name that stores group last-modified timestamps. */
    public static final String DEFAULT_GROUPDB_MODIFIED = "modified";

    /** Default column name that stores names of users who last modified groups. */
    public static final String DEFAULT_GROUPDB_MODIFIER = "modifier";

    /** The JNDI name of the DataSource. */
    public static final String PROP_GROUPDB_DATASOURCE = "jspwiki.groupdatabase.datasource";

    /** The table that stores the groups. */
    public static final String PROP_GROUPDB_TABLE = "jspwiki.groupdatabase.table";

    /** The table that stores the names of group members. */
    public static final String PROP_GROUPDB_MEMBER_TABLE = "jspwiki.groupdatabase.membertable";

    /** The column containing the group's creation timestamp. */
    public static final String PROP_GROUPDB_CREATED = "jspwiki.groupdatabase.created";

    /** The column containing the group creator's name. */
    public static final String PROP_GROUPDB_CREATOR = "jspwiki.groupdatabase.creator";

    /** The column containing the group's name. */
    public static final String PROP_GROUPDB_NAME = "jspwiki.groupdatabase.name";

    /** The column containing the group member's name. */
    public static final String PROP_GROUPDB_MEMBER = "jspwiki.groupdatabase.member";

    /** The column containing the group's last-modified timestamp. */
    public static final String PROP_GROUPDB_MODIFIED = "jspwiki.groupdatabase.modified";

    /** The column containing the name of the user who last modified the group. */
    public static final String PROP_GROUPDB_MODIFIER = "jspwiki.groupdatabase.modifier";

    protected static final Logger log = Logger.getLogger( JDBCGroupDatabase.class );

    private DataSource m_ds = null;

    private String m_created = null;

    private String m_creator = null;

    private String m_name = null;

    private String m_member = null;

    private String m_modified = null;

    private String m_modifier = null;

    private String m_findAll = null;

    private String m_findGroup = null;

    private String m_findMembers = null;

    private String m_insertGroup = null;

    private String m_insertGroupMembers = null;

    private String m_updateGroup = null;

    private String m_deleteGroup = null;

    private String m_deleteGroupMembers = null;

    private boolean m_supportsCommits = false;

    private WikiEngine m_engine = null;

    /**
     * Looks up and deletes a {@link Group} from the group database. If the
     * group database does not contain the supplied Group. this method throws a
     * {@link NoSuchPrincipalException}. The method commits the results of the
     * delete to persistent storage.
     * 
     * @param group the group to remove
     * @throws WikiSecurityException if the database does not contain the
     *             supplied group (thrown as {@link NoSuchPrincipalException})
     *             or if the commit did not succeed
     */
    public void delete( Group group ) throws WikiSecurityException
    {
        if( !exists( group ) )
        {
            throw new NoSuchPrincipalException( "Not in database: " + group.getName() );
        }

        String groupName = group.getName();
        Connection conn = null;
        PreparedStatement ps = null;
        try
        {
            // Open the database connection
            conn = m_ds.getConnection();
            if( m_supportsCommits )
            {
                conn.setAutoCommit( false );
            }

            ps = conn.prepareStatement( m_deleteGroup );
            ps.setString( 1, groupName );
            ps.execute();
            ps.close();

            ps = conn.prepareStatement( m_deleteGroupMembers );
            ps.setString( 1, groupName );
            ps.execute();

            // Commit and close connection
            if( m_supportsCommits )
            {
                conn.commit();
            }
        }
        catch( SQLException e )
        {
        	closeQuietly( conn, ps, null );
            throw new WikiSecurityException( "Could not delete group " + groupName + ": " + e.getMessage(), e );
        }
        finally
        {
            closeQuietly( conn, ps, null );
        }
    }

    /**
     * Returns all wiki groups that are stored in the GroupDatabase as an array
     * of Group objects. If the database does not contain any groups, this
     * method will return a zero-length array. This method causes back-end
     * storage to load the entire set of group; thus, it should be called
     * infrequently (e.g., at initialization time).
     * 
     * @return the wiki groups
     * @throws WikiSecurityException if the groups cannot be returned by the
     *             back-end
     */
    public Group[] groups() throws WikiSecurityException
    {
        Set<Group> groups = new HashSet<Group>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            // Open the database connection
            conn = m_ds.getConnection();

            ps = conn.prepareStatement( m_findAll );
            rs = ps.executeQuery();
            while ( rs.next() )
            {
                String groupName = rs.getString( m_name );
                if( groupName == null )
                {
                    log.warn( "Detected null group name in JDBCGroupDataBase. Check your group database." );
                }
                else
                {
                    Group group = new Group( groupName, m_engine.getApplicationName() );
                    group.setCreated( rs.getTimestamp( m_created ) );
                    group.setCreator( rs.getString( m_creator ) );
                    group.setLastModified( rs.getTimestamp( m_modified ) );
                    group.setModifier( rs.getString( m_modifier ) );
                    populateGroup( group );
                    groups.add( group );
                }
            }
        }
        catch( SQLException e )
        {
        	closeQuietly( conn, ps, rs );
            throw new WikiSecurityException( e.getMessage(), e );
        }
        finally
        {
            closeQuietly( conn, ps, rs );
        }

        return groups.toArray( new Group[groups.size()] );
    }

    /**
     * Saves a Group to the group database. Note that this method <em>must</em>
     * fail, and throw an <code>IllegalArgumentException</code>, if the
     * proposed group is the same name as one of the built-in Roles: e.g.,
     * Admin, Authenticated, etc. The database is responsible for setting
     * create/modify timestamps, upon a successful save, to the Group. The
     * method commits the results of the delete to persistent storage.
     * 
     * @param group the Group to save
     * @param modifier the user who saved the Group
     * @throws WikiSecurityException if the Group could not be saved
     *             successfully
     */
    public void save( Group group, Principal modifier ) throws WikiSecurityException
    {
        if( group == null || modifier == null )
        {
            throw new IllegalArgumentException( "Group or modifier cannot be null." );
        }

        boolean exists = exists( group );
        Connection conn = null;
        PreparedStatement ps = null;
        try
        {
            // Open the database connection
            conn = m_ds.getConnection();
            if( m_supportsCommits )
            {
                conn.setAutoCommit( false );
            }
            
            Timestamp ts = new Timestamp( System.currentTimeMillis() );
            Date modDate = new Date( ts.getTime() );
            if( !exists )
            {
                // Group is new: insert new group record
                ps = conn.prepareStatement( m_insertGroup );
                ps.setString( 1, group.getName() );
                ps.setTimestamp( 2, ts );
                ps.setString( 3, modifier.getName() );
                ps.setTimestamp( 4, ts );
                ps.setString( 5, modifier.getName() );
                ps.execute();

                // Set the group creation time
                group.setCreated( modDate );
                group.setCreator( modifier.getName() );
                ps.close();
            }
            else
            {
                // Modify existing group record
                ps = conn.prepareStatement( m_updateGroup );
                ps.setTimestamp( 1, ts );
                ps.setString( 2, modifier.getName() );
                ps.setString( 3, group.getName() );
                ps.execute();
                ps.close();
            }
            // Set the group modified time
            group.setLastModified( modDate );
            group.setModifier( modifier.getName() );

            // Now, update the group member list

            // First, delete all existing member records
            ps = conn.prepareStatement( m_deleteGroupMembers );
            ps.setString( 1, group.getName() );
            ps.execute();
            ps.close();

            // Insert group member records
            ps = conn.prepareStatement( m_insertGroupMembers );
            Principal[] members = group.members();
            for( int i = 0; i < members.length; i++ )
            {
                Principal member = members[i];
                ps.setString( 1, group.getName() );
                ps.setString( 2, member.getName() );
                ps.execute();
            }

            // Commit and close connection
            if( m_supportsCommits )
            {
                conn.commit();
            }
        }
        catch( SQLException e )
        {
        	closeQuietly(conn, ps, null );
            throw new WikiSecurityException( e.getMessage(), e );
        }
        finally
        {
            closeQuietly(conn, ps, null );
        }
    }

    /**
     * Initializes the group database based on values from a Properties object.
     * 
     * @param engine the wiki engine
     * @param props the properties used to initialize the group database
     * @throws WikiSecurityException if the database could not be initialized
     *             successfully
     * @throws NoRequiredPropertyException if a required property is not present
     */
    public void initialize( WikiEngine engine, Properties props ) throws NoRequiredPropertyException, WikiSecurityException
    {
        String table;
        String memberTable;

        m_engine = engine;

        String jndiName = props.getProperty( PROP_GROUPDB_DATASOURCE, DEFAULT_GROUPDB_DATASOURCE );
        try
        {
            Context initCtx = new InitialContext();
            Context ctx = (Context) initCtx.lookup( "java:comp/env" );
            m_ds = (DataSource) ctx.lookup( jndiName );

            // Prepare the SQL selectors
            table = props.getProperty( PROP_GROUPDB_TABLE, DEFAULT_GROUPDB_TABLE );
            memberTable = props.getProperty( PROP_GROUPDB_MEMBER_TABLE, DEFAULT_GROUPDB_MEMBER_TABLE );
            m_name = props.getProperty( PROP_GROUPDB_NAME, DEFAULT_GROUPDB_NAME );
            m_created = props.getProperty( PROP_GROUPDB_CREATED, DEFAULT_GROUPDB_CREATED );
            m_creator = props.getProperty( PROP_GROUPDB_CREATOR, DEFAULT_GROUPDB_CREATOR );
            m_modifier = props.getProperty( PROP_GROUPDB_MODIFIER, DEFAULT_GROUPDB_MODIFIER );
            m_modified = props.getProperty( PROP_GROUPDB_MODIFIED, DEFAULT_GROUPDB_MODIFIED );
            m_member = props.getProperty( PROP_GROUPDB_MEMBER, DEFAULT_GROUPDB_MEMBER );

            m_findAll = "SELECT DISTINCT * FROM " + table;
            m_findGroup = "SELECT DISTINCT * FROM " + table + " WHERE " + m_name + "=?";
            m_findMembers = "SELECT * FROM " + memberTable + " WHERE " + m_name + "=?";

            // Prepare the group insert/update SQL
            m_insertGroup = "INSERT INTO " + table + " (" + m_name + "," + m_modified + "," + m_modifier + "," + m_created + ","
                            + m_creator + ") VALUES (?,?,?,?,?)";
            m_updateGroup = "UPDATE " + table + " SET " + m_modified + "=?," + m_modifier + "=? WHERE " + m_name + "=?";

            // Prepare the group member insert SQL
            m_insertGroupMembers = "INSERT INTO " + memberTable + " (" + m_name + "," + m_member + ") VALUES (?,?)";

            // Prepare the group delete SQL
            m_deleteGroup = "DELETE FROM " + table + " WHERE " + m_name + "=?";
            m_deleteGroupMembers = "DELETE FROM " + memberTable + " WHERE " + m_name + "=?";
        }
        catch( NamingException e )
        {
            log.error( "JDBCGroupDatabase initialization error: " + e );
            throw new NoRequiredPropertyException( PROP_GROUPDB_DATASOURCE, "JDBCGroupDatabase initialization error: " + e);
        }

        // Test connection by doing a quickie select
        Connection conn = null;
        PreparedStatement ps = null;
        try
        {
            conn = m_ds.getConnection();
            ps = conn.prepareStatement( m_findAll );
            ps.executeQuery();
            ps.close();
        }
        catch( SQLException e )
        {
        	closeQuietly( conn, ps, null );
            log.error( "DB connectivity error: " + e.getMessage() );
            throw new WikiSecurityException("DB connectivity error: " + e.getMessage(), e );
        }
        finally
        {
            closeQuietly( conn, ps, null );
        }
        log.info( "JDBCGroupDatabase initialized from JNDI DataSource: " + jndiName );

        // Determine if the datasource supports commits
        try
        {
            conn = m_ds.getConnection();
            DatabaseMetaData dmd = conn.getMetaData();
            if( dmd.supportsTransactions() )
            {
                m_supportsCommits = true;
                conn.setAutoCommit( false );
                log.info( "JDBCGroupDatabase supports transactions. Good; we will use them." );
            }
        }
        catch( SQLException e )
        {
        	closeQuietly( conn, null, null );
            log.warn( "JDBCGroupDatabase warning: user database doesn't seem to support transactions. Reason: " + e);
        }
        finally
        {
            closeQuietly( conn, null, null );
        }
    }

    /**
     * Returns <code>true</code> if the Group exists in back-end storage.
     * 
     * @param group the Group to look for
     * @return the result of the search
     */
    private boolean exists( Group group )
    {
        String index = group.getName();
        try
        {
            findGroup( index );
            return true;
        }
        catch( NoSuchPrincipalException e )
        {
            return false;
        }
    }

    /**
     * Loads and returns a Group from the back-end database matching a supplied
     * name.
     * 
     * @param index the name of the Group to find
     * @return the populated Group
     * @throws NoSuchPrincipalException if the Group cannot be found
     * @throws SQLException if the database query returns an error
     */
    private Group findGroup( String index ) throws NoSuchPrincipalException
    {
        Group group = null;
        boolean found = false;
        boolean unique = true;
        ResultSet rs = null;
    	PreparedStatement ps = null;
        Connection conn = null;
        try
        {
            // Open the database connection
            conn = m_ds.getConnection();

            ps = conn.prepareStatement( m_findGroup );
            ps.setString( 1, index );
            rs = ps.executeQuery();
            while ( rs.next() )
            {
                if( group != null )
                {
                    unique = false;
                    break;
                }
                group = new Group( index, m_engine.getApplicationName() );
                group.setCreated( rs.getTimestamp( m_created ) );
                group.setCreator( rs.getString( m_creator ) );
                group.setLastModified( rs.getTimestamp( m_modified ) );
                group.setModifier( rs.getString( m_modifier ) );
                populateGroup( group );
                found = true;
            }
        }
        catch( SQLException e )
        {
        	closeQuietly( conn, ps, rs );
            throw new NoSuchPrincipalException( e.getMessage() );
        }
        finally
        {
            closeQuietly( conn, ps, rs );
        }

        if( !found )
        {
            throw new NoSuchPrincipalException( "Could not find group in database!" );
        }
        if( !unique )
        {
            throw new NoSuchPrincipalException( "More than one group in database!" );
        }
        return group;
    }

    /**
     * Fills a Group with members.
     * 
     * @param group the group to populate
     * @return the populated Group
     */
    private Group populateGroup( Group group )
    {
    	ResultSet rs = null;
    	PreparedStatement ps = null;
        Connection conn = null;
        try
        {
            // Open the database connection
            conn = m_ds.getConnection();

            ps = conn.prepareStatement( m_findMembers );
            ps.setString( 1, group.getName() );
            rs = ps.executeQuery();
            while ( rs.next() )
            {
                String memberName = rs.getString( m_member );
                if( memberName != null )
                {
                    WikiPrincipal principal = new WikiPrincipal( memberName, WikiPrincipal.UNSPECIFIED );
                    group.add( principal );
                }
            }
        }
        catch( SQLException e )
        {
            // I guess that means there aren't any principals...
        }
        finally
        {
            closeQuietly( conn, ps, rs );
        }
        return group;
    }
    
    void closeQuietly( Connection conn, PreparedStatement ps, ResultSet rs ) {
    	if( conn != null ) {
    		try {
    		    conn.close();
    		} catch( Exception e ) {
    		}
    	}
		if( ps != null )  {
			try {
			    ps.close();
			} catch( Exception e ) {
			}
		}
		if( rs != null )  {
			try {
			    rs.close();
			} catch( Exception e ) {
			}
		}
	}

}
