package com.ecyrd.jspwiki.auth.authorize;

import java.io.File;
import java.security.Principal;
import java.sql.Connection;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.WikiSecurityException;

/**
 * @author Andrew Jaquith
 */
public class JDBCGroupDatabaseTest extends TestCase
{
    private Connection        m_conn = null;

    private JDBCGroupDatabase m_db  = null;
    
    private String            m_wiki;
    
    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        WikiEngine engine = new TestEngine( props );
        m_wiki = engine.getApplicationName();
        
        // Set up the mock JNDI initial context
        TestJNDIContext.initialize();
        Context initCtx = new InitialContext();
        initCtx.bind( "java:comp/env", new TestJNDIContext() );
        Context ctx = (Context) initCtx.lookup( "java:comp/env" );
        DataSource ds = new TestJDBCDataSource( new File( "build.properties" ) );
        ctx.bind( JDBCGroupDatabase.DEFAULT_GROUPDB_DATASOURCE, ds );

        // Get the JDBC connection and init tables
        m_conn = ds.getConnection();

        // Initialize the user database
        m_db = new JDBCGroupDatabase();
        m_db.initialize( engine, new Properties() );
    }

    public void tearDown() throws Exception
    {
        if ( m_conn != null )
        {
            m_conn.close();
        }
    }

    public void testDelete() throws WikiException
    {
        // First, count the number of groups in the db now.
        int oldUserCount = m_db.groups().length;
        
        // Create a new group with random name
        String name = "TestGroup" + String.valueOf( System.currentTimeMillis() );
        Group group = new Group( name, m_wiki );
        Principal al = new WikiPrincipal( "Al" );
        Principal bob = new WikiPrincipal( "Bob" );
        group.add( al );
        group.add( bob );
        m_db.save(group, new WikiPrincipal( "Tester") );
        
        // Make sure the profile saved successfully
        group = backendGroup( name );
        assertEquals( name, group.getName() );
        assertEquals( oldUserCount+1, m_db.groups().length );

        // Now delete the profile; should be back to old count
        m_db.delete( group );
        assertEquals( oldUserCount, m_db.groups().length );
    }
    
    public void testGroups() throws WikiSecurityException
    {
        // Test file has 4 groups in it: TV, Literature, Art, and Admin
        Group[] groups = m_db.groups();
        assertEquals( 4, groups.length );
        
        Group group;
        
        // Group TV has 3 members
        group = backendGroup( "TV" );
        assertEquals("TV", group.getName() );
        assertEquals( 3, group.members().length );
        
        // Group Literature has 2 members
        group = backendGroup( "Literature" );
        assertEquals("Literature", group.getName() );
        assertEquals( 2, group.members().length );
        
        // Group Art has no members
        group = backendGroup( "Art" );
        assertEquals("Art", group.getName() );
        assertEquals( 0, group.members().length );
        
        // Group Admin has 1 member (Administrator)
        group = backendGroup( "Admin" );
        assertEquals("Admin", group.getName() );
        assertEquals( 1, group.members().length );
        assertEquals( "Administrator", group.members()[0].getName() );
        
        // Group Archaeology doesn't exist
        try {
            group = backendGroup( "Archaeology" );
            // We should never get here 
            assertTrue(false);
        }
        catch (NoSuchPrincipalException e) {
            assertTrue(true);
        }
    }

    public void testSave() throws Exception
    {
        // Create a new group with random name
        String name = "TestGroup" + String.valueOf( System.currentTimeMillis() );
        Group group = new Group( name, m_wiki );
        Principal al = new WikiPrincipal( "Al" );
        Principal bob = new WikiPrincipal( "Bob" );
        Principal cookie = new WikiPrincipal( "Cookie" );
        group.add( al );
        group.add( bob );
        group.add( cookie );
        m_db.save(group, new WikiPrincipal( "Tester" ) );
        
        // Make sure the profile saved successfully
        group = backendGroup( name );
        assertEquals( name, group.getName() );
        assertEquals( 3, group.members().length );
        assertTrue( group.isMember( new WikiPrincipal( "Al" ) ) );
        assertTrue( group.isMember( new WikiPrincipal( "Bob" ) ) );
        assertTrue( group.isMember( new WikiPrincipal( "Cookie" ) ) );
        
        // The back-end should have timestamped the create/modify fields
        assertNotNull( group.getCreator() );
        assertEquals( "Tester", group.getCreator() );
        assertNotNull( group.getCreated() );
        assertNotNull( group.getModifier() );
        assertEquals( "Tester", group.getModifier() );
        assertNotNull( group.getLastModified() );
        assertNotSame( group.getCreated(), group.getLastModified() );
        
        // Remove the group
        m_db.delete( group );
    }
    
    public void testResave() throws Exception
    {
        // Create a new group with random name & 3 members
        String name = "TestGroup" + String.valueOf( System.currentTimeMillis() );
        Group group = new Group( name, m_wiki );
        Principal al = new WikiPrincipal( "Al" );
        Principal bob = new WikiPrincipal( "Bob" );
        Principal cookie = new WikiPrincipal( "Cookie" );
        group.add( al );
        group.add( bob );
        group.add( cookie );
        m_db.save(group, new WikiPrincipal( "Tester" ) );
        
        // Make sure the profile saved successfully
        group = backendGroup( name );
        assertEquals( name, group.getName() );
        
        // Modify the members by adding the group; re-add Al while we're at it
        Principal dave = new WikiPrincipal( "Dave" );
        group.add( al );
        group.add( dave );
        m_db.save(group, new WikiPrincipal( "SecondTester" ) );
        
        // We should see 4 members and new timestamp info
        Principal[] members = group.members();
        assertEquals( 4, members.length );
        assertNotNull( group.getCreator() );
        assertEquals( "Tester", group.getCreator() );
        assertNotNull( group.getCreated() );
        assertNotNull( group.getModifier() );
        assertEquals( "SecondTester", group.getModifier() );
        assertNotNull( group.getLastModified() );
        
        // Check the back-end; We should see the same thing
        group = backendGroup( name );
        members = group.members();
        assertEquals( 4, members.length );
        assertNotNull( group.getCreator() );
        assertEquals( "Tester", group.getCreator() );
        assertNotNull( group.getCreated() );
        assertNotNull( group.getModifier() );
        assertEquals( "SecondTester", group.getModifier() );
        assertNotNull( group.getLastModified() );
        
        // Remove the group
        m_db.delete( group );
    }
    
    private Group backendGroup( String name ) throws WikiSecurityException
    {
        Group[] groups = m_db.groups();
        for ( int i = 0; i < groups.length; i++ )
        {
            Group group = groups[i];
            if ( group.getName().equals( name ) )
            {
                return group;
            }
        }
        throw new NoSuchPrincipalException( "No group named " + name );
    }
}
