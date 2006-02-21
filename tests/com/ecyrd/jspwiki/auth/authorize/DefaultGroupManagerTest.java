package com.ecyrd.jspwiki.auth.authorize;

import java.security.Principal;
import java.util.List;
import java.util.Properties;

import javax.security.auth.Subject;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.SecurityEventTrap;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.WikiSecurityEvent;
import com.ecyrd.jspwiki.providers.ProviderException;

public class DefaultGroupManagerTest extends TestCase
{
    TestEngine   m_engine;

    DefaultGroupManager m_manager;
    
    SecurityEventTrap m_trap = new SecurityEventTrap();

    public DefaultGroupManagerTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );

        m_engine = new TestEngine( props );
        m_manager = (DefaultGroupManager)m_engine.getGroupManager();
        
        // Flush any pre-existing pages (left over from previous failures, perhaps)
        flushPage( m_engine, "GroupTest" );
        flushPage( m_engine, "GroupTest2" );
        flushPage( m_engine, "GroupTest3" );
        flushPage( m_engine, "BadGroupTest" );
        
        m_manager.addWikiEventListener( m_trap );

        String text1 = "Foobar.\n\n[{SET members=Alice, Bob, Charlie}]\n\nBlood.";
        m_engine.saveText( "GroupTest", text1 );

        String text2 = "[{SET members=Bob}]";
        m_engine.saveText( "GroupTest2", text2 );
        
        String text3 = "[{SET members=Fred Flintstone}]";
        m_engine.saveText( "GroupTest3", text3 );
        
        String text4 = "[{SET members=Arnold}]";
        m_engine.saveText( "BadGroupTest", text4 );
        
        assertEquals( 8, m_trap.events().length );
        m_trap.clearEvents();
    }

    public void tearDown()
    {
        try
        {
            m_engine.deletePage( "GroupTest" );
            m_engine.deletePage( "GroupTest2" );
            m_engine.deletePage( "GroupTest3" );
            m_engine.deletePage( "BadGroupTest" );
        }
        catch ( ProviderException e )
        {
        }
   }

    public void testParseMemberList()
    {
        String members = "Biff";
        List list = m_manager.parseMemberList( members );
        assertEquals( 1, list.size() );
        assertTrue ( list.contains( "Biff" ) );
        
        members = "Biff, SteveAustin, FredFlintstone";
        list = m_manager.parseMemberList( members );
        assertEquals( 3, list.size() );
        assertTrue ( list.contains( "Biff" ) );
        assertTrue ( list.contains( "SteveAustin" ) );
        assertTrue ( list.contains( "FredFlintstone" ) );
        
        members = "Biff, Steve Austin, Fred Flintstone";
        list = m_manager.parseMemberList( members );
        assertEquals( 3, list.size() );
        assertTrue ( list.contains( "Biff" ) );
        assertTrue ( list.contains( "Steve Austin" ) );
        assertTrue ( list.contains( "Fred Flintstone" ) );
    }
    
    public void testGetRoles()
    {
        Principal[] roles = m_manager.getRoles();
        boolean found = false;
        for ( int i = 0; i < roles.length; i++ )
        {
            if ( roles[i].getName().equals( "Test" ) )
            {
                found = true;
            }
        }
        assertTrue( "Didn't find group Test", found );
        for ( int i = 0; i < roles.length; i++ )
        {
            if ( roles[i].getName().equals( "Test2" ) )
            {
                found = true;
            }
        }
        assertTrue( "Didn't find group Test2", found );
    }

    public void testGroupFormation() throws Exception
    {
        Principal p = new WikiPrincipal( "Alice" );
        WikiSession session = WikiSession.guestSession();
        Subject s = session.getSubject();
        s.getPrincipals().add(p);
        assertTrue( m_manager.isUserInRole( session, new DefaultGroup( "Test" ) ) );
        assertFalse( m_manager.isUserInRole( session, new DefaultGroup( "Test2" ) ) );
        assertFalse( m_manager.isUserInRole( session, new DefaultGroup( "Test3" ) ) );
        assertFalse( m_manager.isUserInRole( session, new DefaultGroup( "NonExistant" ) ) );

        session = WikiSession.guestSession();
        s = session.getSubject();
        p = new WikiPrincipal( "Bob" );
        s.getPrincipals().add(p);
        assertTrue( m_manager.isUserInRole( session, new DefaultGroup( "Test" ) ) );
        assertTrue( m_manager.isUserInRole( session, new DefaultGroup( "Test2" ) ) );
        assertFalse( m_manager.isUserInRole( session, new DefaultGroup( "Test3" ) ) );
        assertFalse( m_manager.isUserInRole( session, new DefaultGroup( "NonExistant" ) ) );

        session = WikiSession.guestSession();
        s = session.getSubject();
        p = new WikiPrincipal( "Charlie" );
        s.getPrincipals().add(p);
        assertTrue( m_manager.isUserInRole( session, new DefaultGroup( "Test" ) ) );
        assertFalse( m_manager.isUserInRole( session, new DefaultGroup( "Test2" ) ) );
        assertFalse( m_manager.isUserInRole( session, new DefaultGroup( "Test3" ) ) );
        assertFalse( m_manager.isUserInRole( session, new DefaultGroup( "NonExistant" ) ) );

        session = WikiSession.guestSession();
        s = session.getSubject();
        p = new WikiPrincipal( "Fred Flintstone" );
        s.getPrincipals().add(p);
        assertFalse( m_manager.isUserInRole( session, new DefaultGroup( "Test" ) ) );
        assertFalse( m_manager.isUserInRole( session, new DefaultGroup( "Test2" ) ) );
        assertTrue( m_manager.isUserInRole( session, new DefaultGroup( "Test3" ) ) );
        assertFalse( m_manager.isUserInRole( session, new DefaultGroup( "NonExistant" ) ) );
        
        session = WikiSession.guestSession();
        s = session.getSubject();
        p = new WikiPrincipal( "Biff" );
        s.getPrincipals().add(p);
        assertFalse( m_manager.isUserInRole( session, new DefaultGroup( "Test" ) ) );
        assertFalse( m_manager.isUserInRole( session, new DefaultGroup( "Test2" ) ) );
        assertFalse( m_manager.isUserInRole( session, new DefaultGroup( "Test3" ) ) );
        assertFalse( m_manager.isUserInRole( session, new DefaultGroup( "NonExistant" ) ) );
    }

    public void testBadGroupFormation() throws Exception
    {
        // BadGroup doesn't start with "Group", so there should be no group created for it
        // (Groups must start with the word "Group")
        Principal p = new WikiPrincipal( "Arnold" );
        WikiSession session = WikiSession.guestSession();
        Subject s = session.getSubject();
        s.getPrincipals().add(p);
        assertFalse( m_manager.isUserInRole( session, new DefaultGroup( "BadGroup" ) ) );
    }

    public void testGroupAddEvent() throws Exception
    {
        // Flush any pre-existing pages (left over from previous failures, perhaps)
        flushPage( m_engine, "GroupEvents" );
        m_trap.clearEvents();
        
        String text1 = "Foobar.\n\n[{SET members=Alice, Bob, Charlie}]\n\nBlood.";
        m_engine.saveText( "GroupEvents", text1 );
        WikiSecurityEvent[] events = m_trap.events();
        WikiSecurityEvent event;
        Group group = (Group)m_manager.findRole( "Events" );
        
        // First event should be GROUP_ADD
        assertEquals( 4, events.length );
        event = events[0];
        assertEquals( m_manager, event.getSource() );
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
        m_engine.deletePage( "GroupEvents" ); 
    }
    
    public static Test suite()
    {
        return new TestSuite( DefaultGroupManagerTest.class );
    }
    
    public static void flushPage( WikiEngine engine, String page ) throws Exception
    {
        if ( engine.pageExists( page ) )
        {
            engine.deletePage( page );
            
            // Remove the group manually -- this is a ridiculous, horrible hack that we need to do until
            // we have some sort of 'page deleted' event support in WikiEngine and/or the page providers
            if ( page.startsWith( DefaultGroupManager.GROUP_PREFIX ) )
            {
                GroupManager manager = engine.getGroupManager();
                String groupName = page.substring(DefaultGroupManager.GROUP_PREFIX.length());
                Group group = (Group)manager.findRole( groupName );
                if ( group != null )
                {
                    manager.remove( group );
                }
            }
        }
    }

}