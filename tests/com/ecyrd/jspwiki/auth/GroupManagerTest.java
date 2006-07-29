package com.ecyrd.jspwiki.auth;

import java.security.Principal;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.WikiSessionTest;
import com.ecyrd.jspwiki.auth.authorize.Group;
import com.ecyrd.jspwiki.auth.authorize.GroupManager;

public class GroupManagerTest extends TestCase
{
    private TestEngine        m_engine;

    private GroupManager      m_groupMgr;
    
    private SecurityEventTrap m_trap = new SecurityEventTrap();

    private WikiSession       m_session;
    
    private String            m_wiki;
    
    public GroupManagerTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );

        m_engine = new TestEngine( props );
        m_groupMgr = m_engine.getGroupManager();
        m_session = WikiSessionTest.adminSession( m_engine );
        m_wiki = m_engine.getApplicationName();
        
        // Flush any pre-existing groups (left over from previous failures, perhaps)
        try {
            m_groupMgr.removeGroup( "Test" );
            m_groupMgr.removeGroup( "Test2" );
            m_groupMgr.removeGroup( "Test3" );
        }
        catch ( NoSuchPrincipalException e )
        {
            // It's not a problem if we can't find the principals...
        }
        
        m_groupMgr.addWikiEventListener( m_trap );

        // Add 3 test groups
        Group group;
        group = m_groupMgr.parseGroup( "Test", "Alice \n Bob \n Charlie", true );
        m_groupMgr.setGroup( m_session, group );
        group = m_groupMgr.parseGroup( "Test2", "Bob", true );
        m_groupMgr.setGroup( m_session, group );
        group = m_groupMgr.parseGroup( "Test3", "Fred Flintstone", true );
        m_groupMgr.setGroup( m_session, group );

        // We should see 3 events: 1 for each group add
        assertEquals( 3, m_trap.events().length );
        m_trap.clearEvents();
    }

    public void tearDown() throws WikiException
    {
        m_groupMgr.removeGroup( "Test" );
        m_groupMgr.removeGroup( "Test2" );
        m_groupMgr.removeGroup( "Test3" );
   }

    public void testParseGroup() throws WikiSecurityException
    {
        String members = "Biff";
        Group group = m_groupMgr.parseGroup( "Group1", members, true );
        assertEquals( 1, group.members().length );
        assertTrue ( group.isMember( new WikiPrincipal( "Biff" ) ) );
        
        members = "Biff \n SteveAustin \n FredFlintstone";
        group = m_groupMgr.parseGroup( "Group2", members, true );
        assertEquals( 3, group.members().length );
        assertTrue ( group.isMember( new WikiPrincipal( "Biff" ) ) );
        assertTrue ( group.isMember( new WikiPrincipal( "SteveAustin" ) ) );
        assertTrue ( group.isMember( new WikiPrincipal( "FredFlintstone" ) ) );
    }
    
    public void testGetRoles()
    {
        Principal[] roles = m_groupMgr.getRoles();
        assertTrue( "Found Test", ArrayUtils.contains( roles, new GroupPrincipal( m_wiki, "Test" ) ) );
        assertTrue( "Found Test2", ArrayUtils.contains( roles, new GroupPrincipal( m_wiki, "Test2" ) ) );
        assertTrue( "Found Test3", ArrayUtils.contains( roles, new GroupPrincipal( m_wiki, "Test3" ) ) );
    }

    public void testGroupMembership() throws Exception
    {
        WikiSession s;
        
        // Anonymous; should belong to NO groups
        s = WikiSessionTest.anonymousSession( m_engine );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test2" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test3" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "NonExistant" ) ) );
        
        // Alice is asserted; should belong to NO groups
        s = WikiSessionTest.assertedSession( m_engine, Users.ALICE );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test2" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test3" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "NonExistant" ) ) );

        // Alice is authenticated; should belong to Test
        s = WikiSessionTest.authenticatedSession( m_engine, Users.ALICE, Users.ALICE_PASS );
        assertTrue( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test2" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test3" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "NonExistant" ) ) );
        
        // Bob is authenticated; should belong to Test & Test2
        s = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        assertTrue( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test" ) ) );
        assertTrue( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test2" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test3" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "NonExistant" ) ) );

        // Charlie is authenticated; should belong to Test
        s = WikiSessionTest.authenticatedSession( m_engine, Users.CHARLIE, Users.CHARLIE_PASS );
        assertTrue( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test2" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test3" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "NonExistant" ) ) );

        // Fred is authenticated; should belong to Test3
        s = WikiSessionTest.authenticatedSession( m_engine, Users.FRED, Users.FRED_PASS );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test2" ) ) );
        assertTrue( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test3" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "NonExistant" ) ) );
        
        // Nobody loves Biff!
        s = WikiSessionTest.authenticatedSession( m_engine, Users.BIFF, Users.BIFF_PASS );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test2" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "Test3" ) ) );
        assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( m_wiki, "NonExistant" ) ) );
    }

    public void testGroupAddEvents() throws Exception
    {
        // Flush any pre-existing groups (left over from previous failures, perhaps)
        try {
            m_groupMgr.removeGroup( "Events" );
        }
        catch ( NoSuchPrincipalException e )
        {
            // It's not a problem if we get here...
        }
        m_trap.clearEvents();

        Group group = m_groupMgr.parseGroup( "Events", "", true );
        m_groupMgr.setGroup( m_session, group );
        WikiSecurityEvent event;
        group = m_groupMgr.getGroup( "Events" );
        group.add( new WikiPrincipal( "Alice" ) );
        group.add( new WikiPrincipal( "Bob" ) );
        group.add( new WikiPrincipal( "Charlie" ) );
        
        // First event should be GROUP_ADD
        WikiSecurityEvent[] events = m_trap.events();
        assertEquals( 4, events.length );
        event = events[0];
        assertEquals( m_groupMgr, event.getSource() );
        assertEquals( WikiSecurityEvent.GROUP_ADD, event.getType() );
        assertEquals( group, event.getTarget() );
        
        // Second, third and fourth should be the GROUP_ADD_MEMBER events
        event = events[1];
        assertEquals( group, event.getSource() );
        assertEquals( WikiSecurityEvent.GROUP_ADD_MEMBER, event.getType() );
        assertEquals( new WikiPrincipal( "Alice" ), event.getTarget() );
        
        event = events[2];
        assertEquals( group, event.getSource() );
        assertEquals( WikiSecurityEvent.GROUP_ADD_MEMBER, event.getType() );
        assertEquals( new WikiPrincipal( "Bob" ), event.getTarget() );
        
        event = events[3];
        assertEquals( group, event.getSource() );
        assertEquals( WikiSecurityEvent.GROUP_ADD_MEMBER, event.getType() );
        assertEquals( new WikiPrincipal( "Charlie" ), event.getTarget() );
        
        // Clean up
        m_groupMgr.removeGroup( "Events" );
    }
    
    public static Test suite()
    {
        return new TestSuite( GroupManagerTest.class );
    }
    
}