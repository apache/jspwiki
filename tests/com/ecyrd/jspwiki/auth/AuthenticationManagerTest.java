package com.ecyrd.jspwiki.auth;

import java.security.Principal;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.WikiSessionTest;
import com.ecyrd.jspwiki.auth.authorize.Group;
import com.ecyrd.jspwiki.auth.authorize.GroupManager;
import com.ecyrd.jspwiki.auth.authorize.Role;

/**
 * Tests the AuthorizationManager class.
 * @author Janne Jalkanen
 */
public class AuthenticationManagerTest extends TestCase
{
    private AuthenticationManager m_auth;

    private TestEngine            m_engine;
    
    private GroupManager          m_groupMgr;
 
    private WikiSession           m_session;
    
    private String                m_wiki;
    
    public AuthenticationManagerTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        m_engine = new TestEngine( props );
        m_auth = m_engine.getAuthenticationManager();
        m_groupMgr = m_engine.getGroupManager();
        m_session = WikiSessionTest.adminSession( m_engine );
        m_wiki = m_engine.getApplicationName();
    }
    
    public void testIsUserPrincipal()
    {
        assertTrue( AuthenticationManager.isUserPrincipal( new WikiPrincipal( "Foo" ) ) );
        assertFalse( AuthenticationManager.isUserPrincipal( new GroupPrincipal( m_wiki, "Group1" ) ) );
        assertFalse( AuthenticationManager.isUserPrincipal( new Role( "Role1" ) ) );
        assertFalse( AuthenticationManager.isUserPrincipal( Role.ANONYMOUS ) );
    }
    
    public void testLoginCustom() throws Exception
    {
        WikiSession session = WikiSessionTest.authenticatedSession( m_engine, Users.JANNE, Users.JANNE_PASS );
        assertTrue( session.hasPrincipal( Role.ALL ) );
        assertTrue( session.hasPrincipal( Role.AUTHENTICATED ) );
        assertTrue( session.hasPrincipal( new WikiPrincipal( "Janne Jalkanen" ) ) );
        assertTrue( session.hasPrincipal( new WikiPrincipal( Users.JANNE ) ) );
        assertTrue( session.hasPrincipal( new WikiPrincipal( "JanneJalkanen" ) ) );
    }
    
    public void testLoginCustomWithGroup() throws Exception
    {
        // Flush any pre-existing groups (left over from previous failures, perhaps)
        try
        {
            m_groupMgr.removeGroup( "Test1" );
            m_groupMgr.removeGroup( "Test2" );
        }
        catch ( NoSuchPrincipalException e )
        {
            
        }
        
        // Log in 'janne' and verify there are 5 principals in the subject
        // (ALL, AUTHENTICATED, login, fullname, wikiname Principals)
        WikiSession session = WikiSession.guestSession( m_engine );
        m_auth.login( session, Users.JANNE, Users.JANNE_PASS );
        assertEquals( 3, session.getPrincipals().length );
        assertEquals( 2, session.getRoles().length );
        assertTrue( session.hasPrincipal( new WikiPrincipal( "JanneJalkanen" ) ) );
        
        // Listen for any group add events
        GroupManager manager = m_engine.getGroupManager();
        SecurityEventTrap trap = new SecurityEventTrap();
        manager.addWikiEventListener( trap );

        // Create two groups; one with Janne in it, and one without
        Group groupTest1 = m_groupMgr.parseGroup( "Test1", "JanneJalkanen \n Bob \n Charlie", true );
        m_groupMgr.setGroup( m_session, groupTest1 );
        groupTest1 = m_groupMgr.getGroup( "Test1" );
        Principal principalTest1 = groupTest1.getPrincipal();
        
        Group groupTest2 = m_groupMgr.parseGroup( "Test2", "Alice \n Bob \n Charlie", true );
        m_groupMgr.setGroup( m_session, groupTest2 );
        groupTest2 = m_groupMgr.getGroup( "Test2" );
        Principal principalTest2 = groupTest2.getPrincipal();
        
        // We should see two security events (one for each group create)
        // We should also see a GroupPrincipal for group Test1, but not Test2
        assertEquals( 2, trap.events().length );
        assertTrue( session.hasPrincipal( principalTest1 ) );
        assertFalse( session.hasPrincipal( principalTest2 ) );
        
        // If we remove Test1, the GroupPrincipal should disappear
        m_groupMgr.removeGroup( "Test1" );
        assertFalse( session.hasPrincipal( principalTest1 ) );
        assertFalse( session.hasPrincipal( principalTest2 ) );
        
        // Now, add 'JanneJalkanen' to Test2 group manually; we should see the GroupPrincipal
        groupTest2.add( new WikiPrincipal( "JanneJalkanen" ) );
        assertFalse( session.hasPrincipal( principalTest1 ) );
        assertTrue( session.hasPrincipal( principalTest2 ) );
        
        // Remove 'JanneJalkenen' manually; the GroupPrincipal should disappear
        groupTest2.remove( new WikiPrincipal( "JanneJalkanen" ) );
        assertFalse( session.hasPrincipal( principalTest1 ) );
        assertFalse( session.hasPrincipal( principalTest2 ) );
        
        // Clean up
        m_groupMgr.removeGroup( "Test2" );
    }
    
    public static Test suite()
    {
        TestSuite suite = new TestSuite("Authentication Manager test");
        suite.addTestSuite( AuthenticationManagerTest.class );
        return suite;
    }
}