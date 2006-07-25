package com.ecyrd.jspwiki.auth;

import java.util.Collection;
import java.util.Properties;

import javax.security.auth.Subject;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.authorize.DefaultGroupManager;
import com.ecyrd.jspwiki.auth.authorize.DefaultGroupManagerTest;
import com.ecyrd.jspwiki.auth.authorize.Group;
import com.ecyrd.jspwiki.auth.authorize.Role;

/**
 * Tests the AuthorizationManager class.
 * @author Janne Jalkanen
 */
public class AuthenticationManagerTest extends TestCase
{
    private AuthenticationManager m_auth;

    private TestEngine            m_engine;
 
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
    }
    
    public void testLoginCustom()
    {
        WikiSession session = WikiSession.guestSession( m_engine );
        m_auth.login( session, "janne", "myP@5sw0rd" );
        Subject subject = session.getSubject();
        Collection principals = subject.getPrincipals();
        assertTrue( principals.contains( Role.ALL ) );
        assertTrue( principals.contains( Role.AUTHENTICATED ) );
        assertTrue( principals.contains( new WikiPrincipal( "Janne Jalkanen" ) ) );
        assertTrue( principals.contains( new WikiPrincipal( "janne" ) ) );
        assertTrue( principals.contains( new WikiPrincipal( "JanneJalkanen" ) ) );
    }
    
    public void testLoginCustomWithGroup() throws Exception
    {
        // Flush any pre-existing pages (left over from previous failures, perhaps)
        DefaultGroupManagerTest.flushPage( m_engine, "GroupTest1" );
        DefaultGroupManagerTest.flushPage( m_engine, "GroupTest2" );
        
        // Log in 'janne' and verify there are 5 principals in the subject
        // (ALL, AUTHENTICATED, login, fullname, wikiname Principals)
        WikiSession session = WikiSession.guestSession( m_engine );
        m_auth.login( session, "janne", "myP@5sw0rd" );
        Subject subject = session.getSubject();
        Collection principals = subject.getPrincipals();
        assertEquals( 5, principals.size() );
        assertTrue( principals.contains( new WikiPrincipal( "JanneJalkanen" ) ) );
        
        // Listen for any group add events
        DefaultGroupManager manager = (DefaultGroupManager)m_engine.getGroupManager();
        SecurityEventTrap trap = new SecurityEventTrap();
        manager.addWikiEventListener( trap );

        // Create two groups; one with Janne in it, and one without
        String text;
        text = "Foobar.\n\n[{SET members=JanneJalkanen, Bob, Charlie}]\n\nBlood.";
        m_engine.saveText( "GroupTest1", text );
        text = "Foobar.\n\n[{SET members=Alice, Bob, Charlie}]\n\nBlood.";
        m_engine.saveText( "GroupTest2", text );
        
        // We should see eight security events (one for each group create, plus one for each member)
        // We should also see a GroupPrincipal for group Test1, but not Test2
        String wiki = m_engine.getApplicationName();
        assertEquals( 8, trap.events().length );
        Group groupTest1 = (Group)manager.findRole( "Test1" );
        Group groupTest2 = (Group)manager.findRole( "Test2" );
        assertTrue( principals.contains( new GroupPrincipal( wiki, groupTest1.getName() ) ) );
        assertFalse( principals.contains( new GroupPrincipal( wiki, groupTest2.getName() ) ) );
        
        // If we remove Test1, the GroupPrincipal should disappear
        DefaultGroupManagerTest.flushPage( m_engine, "GroupTest1" );
        assertFalse( principals.contains( new GroupPrincipal( wiki, groupTest1.getName() ) ) );
        assertFalse( principals.contains( new GroupPrincipal( wiki, groupTest2.getName() ) ) );
        
        // Now, add 'JanneJalkanen' to Test2 group manually; we should see the GroupPrincipal
        groupTest2.add( new WikiPrincipal( "JanneJalkanen" ) );
        assertFalse( principals.contains( new GroupPrincipal( wiki, groupTest1.getName() ) ) );
        assertTrue( principals.contains( new GroupPrincipal( wiki, groupTest2.getName() ) ) );
        
        // Remove 'JanneJalkenen' manually; the GroupPrincipal should disappear
        groupTest2.remove( new WikiPrincipal( "JanneJalkanen" ) );
        assertFalse( principals.contains( new GroupPrincipal( wiki, groupTest1.getName() ) ) );
        assertFalse( principals.contains( new GroupPrincipal( wiki, groupTest2.getName() ) ) );
    }
    
    public static Test suite()
    {
        TestSuite suite = new TestSuite("Authentication Manager test");
        suite.addTestSuite( AuthenticationManagerTest.class );
        return suite;
    }
}