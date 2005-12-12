package com.ecyrd.jspwiki.auth.user;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.TestJDBCDataSource;
import com.ecyrd.jspwiki.TestJNDIContext;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiSecurityException;

/**
 * @author Andrew Jaquith
 */
public class JDBCUserDatabaseTest extends TestCase
{
    private Connection       m_conn = null;

    private JDBCUserDatabase m_db   = null;

    private Date             m_createStamp;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        // Set up the mock JNDI initial context
        TestJNDIContext.initialize();
        Context initCtx = new InitialContext();
        initCtx.bind( "java:comp/env", new TestJNDIContext() );
        Context ctx = (Context) initCtx.lookup( "java:comp/env" );
        DataSource ds = new TestJDBCDataSource( new File( "build.properties" ) );
        ctx.bind( JDBCUserDatabase.DEFAULT_DB_JNDI_NAME, ds );

        // Get the JDBC connection and init tables
        Connection m_conn = ds.getConnection();
        Statement stmt = m_conn.createStatement();
        String sql;

        sql = "DELETE FROM " + JDBCUserDatabase.DEFAULT_DB_TABLE + ";";
        stmt.executeUpdate( sql );

        // Create a new test user 'janne'
        m_createStamp = new Timestamp( System.currentTimeMillis() );
        sql = "INSERT INTO users (" + JDBCUserDatabase.DEFAULT_DB_EMAIL + "," + JDBCUserDatabase.DEFAULT_DB_FULL_NAME
                + "," + JDBCUserDatabase.DEFAULT_DB_LOGIN_NAME + "," + JDBCUserDatabase.DEFAULT_DB_PASSWORD + ","
                + JDBCUserDatabase.DEFAULT_DB_WIKI_NAME + "," + JDBCUserDatabase.DEFAULT_DB_CREATED + ") VALUES ("
                + "'janne@ecyrd.com'," + "'Janne Jalkanen'," + "'janne',"
                + "'{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee'," + "'JanneJalkanen'," + "'"
                + new Timestamp( m_createStamp.getTime() ).toString() + "'" + ");";
        stmt.executeUpdate( sql );

        // Create a new test user 'user'
        sql = "INSERT INTO users (" + JDBCUserDatabase.DEFAULT_DB_EMAIL + "," + JDBCUserDatabase.DEFAULT_DB_LOGIN_NAME
                + "," + JDBCUserDatabase.DEFAULT_DB_PASSWORD + "," + JDBCUserDatabase.DEFAULT_DB_CREATED + ") VALUES ("
                + "'user@example.com'," + "'user'," + "'{SHA}5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8'," + "'"
                + new Timestamp( m_createStamp.getTime() ).toString() + "'" + ");";
        stmt.executeUpdate( sql );

        // Initialize the user database
        m_db = new JDBCUserDatabase();
        m_db.initialize( null, new Properties() );
    }

    public void tearDown() throws Exception
    {
        if ( m_conn != null )
        {
            m_conn.close();
        }
    }

    public void testFindByEmail()
    {
        try
        {
            UserProfile profile = m_db.findByEmail( "janne@ecyrd.com" );
            assertEquals( "janne", profile.getLoginName() );
            assertEquals( "Janne Jalkanen", profile.getFullname() );
            assertEquals( "JanneJalkanen", profile.getWikiName() );
            assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            assertEquals( "janne@ecyrd.com", profile.getEmail() );
            assertNotNull( profile.getCreated() );
            assertNull( profile.getLastModified() );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( false );
        }
        try
        {
            m_db.findByEmail( "foo@bar.org" );
            // We should never get here
            assertTrue( false );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( true );
        }
    }

    public void testFindByFullName()
    {
        try
        {
            UserProfile profile = m_db.findByFullName( "Janne Jalkanen" );
            assertEquals( "janne", profile.getLoginName() );
            assertEquals( "Janne Jalkanen", profile.getFullname() );
            assertEquals( "JanneJalkanen", profile.getWikiName() );
            assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            assertEquals( "janne@ecyrd.com", profile.getEmail() );
            assertNotNull( profile.getCreated() );
            assertNull( profile.getLastModified() );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( false );
        }
        try
        {
            m_db.findByEmail( "foo@bar.org" );
            // We should never get here
            assertTrue( false );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( true );
        }
    }

    public void testFindByWikiName()
    {
        try
        {
            UserProfile profile = m_db.findByWikiName( "JanneJalkanen" );
            assertEquals( "janne", profile.getLoginName() );
            assertEquals( "Janne Jalkanen", profile.getFullname() );
            assertEquals( "JanneJalkanen", profile.getWikiName() );
            assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            assertEquals( "janne@ecyrd.com", profile.getEmail() );
            assertNotNull( profile.getCreated() );
            assertNull( profile.getLastModified() );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( false );
        }
        try
        {
            m_db.findByEmail( "foo" );
            // We should never get here
            assertTrue( false );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( true );
        }
    }

    public void testFindByLoginName()
    {
        try
        {
            UserProfile profile = m_db.findByLoginName( "janne" );
            assertEquals( "janne", profile.getLoginName() );
            assertEquals( "Janne Jalkanen", profile.getFullname() );
            assertEquals( "JanneJalkanen", profile.getWikiName() );
            assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            assertEquals( "janne@ecyrd.com", profile.getEmail() );
            assertNotNull( profile.getCreated() );
            assertNull( profile.getLastModified() );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( false );
        }
        try
        {
            m_db.findByEmail( "FooBar" );
            // We should never get here
            assertTrue( false );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( true );
        }
    }

    public void testSave()
    {
        try
        {
            // Overwrite existing user
            UserProfile profile = new DefaultUserProfile();
            profile.setEmail( "user@example.com" );
            profile.setFullname( "Test User" );
            profile.setLoginName( "user" );
            profile.setPassword( "password" );
            profile.setWikiName( "TestUser" );
            m_db.save( profile );
            profile = m_db.findByEmail( "user@example.com" );
            assertEquals( "user@example.com", profile.getEmail() );
            assertEquals( "Test User", profile.getFullname() );
            assertEquals( "user", profile.getLoginName() );
            assertEquals( "{SHA}5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8", profile.getPassword() );
            assertEquals( "TestUser", profile.getWikiName() );
            assertNotNull( profile.getCreated() );
            assertNotNull( profile.getLastModified() );
            assertNotSame( profile.getCreated(), profile.getLastModified() );

            // Create new user
            profile = new DefaultUserProfile();
            profile.setEmail( "user2@example.com" );
            profile.setFullname( "Test User 2" );
            profile.setLoginName( "user2" );
            profile.setPassword( "password" );
            profile.setWikiName( "TestUser2" );
            m_db.save( profile );
            profile = m_db.findByEmail( "user2@example.com" );
            assertEquals( "user2@example.com", profile.getEmail() );
            assertEquals( "Test User 2", profile.getFullname() );
            assertEquals( "user2", profile.getLoginName() );
            assertEquals( "{SHA}5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8", profile.getPassword() );
            assertEquals( "TestUser2", profile.getWikiName() );
            assertNotNull( profile.getCreated() );
            assertNotNull( profile.getLastModified() );
            assertEquals( profile.getCreated(), profile.getLastModified() );

            m_db.commit();
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( false );
        }
        catch( WikiSecurityException e )
        {
            assertTrue( false );
        }
    }

    public void testValidatePassword()
    {
        assertFalse( m_db.validatePassword( "janne", "test" ) );
        assertTrue( m_db.validatePassword( "janne", "myP@5sw0rd" ) );
        assertTrue( m_db.validatePassword( "user", "password" ) );
    }

}
